package com.nursing.user.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.config.SmsProperties;
import com.nursing.user.entity.SmsOutboxEvent;
import com.nursing.user.entity.SmsRecord;
import com.nursing.user.entity.SmsRecordStatusTransition;
import com.nursing.user.entity.SmsSendRequest;
import com.nursing.user.mapper.SmsOutboxEventMapper;
import com.nursing.user.mapper.SmsRecordMapper;
import com.nursing.user.mapper.SmsRecordStatusTransitionMapper;
import com.nursing.user.mapper.SmsSendRequestMapper;
import com.nursing.user.sms.SmsSendCommand;
import com.nursing.user.sms.SmsSendResult;
import com.nursing.user.sms.SmsCodeGenerator;
import com.nursing.user.security.SmsCodeDigest;
import com.nursing.user.sms.SmsSender;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class SmsOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(SmsOutboxWorker.class);
    private static final int SMS_RECORD_PENDING = 0;
    private static final String PROVIDER_MOCK = "mock";
    private static final String SMS_CODE_KEY_PREFIX = "sms:code:";

    private final SmsProperties smsProperties;
    private final SmsOutboxEventMapper smsOutboxEventMapper;
    private final SmsSendRequestMapper smsSendRequestMapper;
    private final SmsRecordMapper smsRecordMapper;
    private final SmsRecordStatusTransitionMapper smsRecordStatusTransitionMapper;
    private final SmsRateLimiter smsRateLimiter;
    private final SmsSender smsSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final SmsCodeGenerator smsCodeGenerator;
    private final SmsCodeDigest smsCodeDigest;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final String workerId = UUID.randomUUID().toString();

    public SmsOutboxWorker(SmsProperties smsProperties,
                           SmsOutboxEventMapper smsOutboxEventMapper,
                           SmsSendRequestMapper smsSendRequestMapper,
                           SmsRecordMapper smsRecordMapper,
                           SmsRecordStatusTransitionMapper smsRecordStatusTransitionMapper,
                           SmsRateLimiter smsRateLimiter,
                           SmsSender smsSender,
                           RedisTemplate<String, String> redisTemplate,
                           SnowflakeIdWorker snowflakeIdWorker,
                           SmsCodeGenerator smsCodeGenerator,
                           SmsCodeDigest smsCodeDigest,
                           PasswordEncoder passwordEncoder,
                           TransactionTemplate transactionTemplate) {
        this.smsProperties = smsProperties;
        this.smsOutboxEventMapper = smsOutboxEventMapper;
        this.smsSendRequestMapper = smsSendRequestMapper;
        this.smsRecordMapper = smsRecordMapper;
        this.smsRecordStatusTransitionMapper = smsRecordStatusTransitionMapper;
        this.smsRateLimiter = smsRateLimiter;
        this.smsSender = smsSender;
        this.redisTemplate = redisTemplate;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.smsCodeGenerator = smsCodeGenerator;
        this.smsCodeDigest = smsCodeDigest;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${nursing.sms.outbox-fixed-delay-millis:1000}")
    public void dispatchPending() {
        recoverStaleClaims();
        smsOutboxEventMapper.selectPending(smsProperties.getOutboxBatchSize(), LocalDateTime.now())
                .forEach(this::claimAndDispatch);
    }

    private void claimAndDispatch(SmsOutboxEvent event) {
        LocalDateTime now = LocalDateTime.now();
        if (smsOutboxEventMapper.claim(
                event.getId(),
                workerId,
                now.plusSeconds(smsProperties.getOutboxProcessingTimeoutSeconds()),
                now) != 1) {
            return;
        }

        SmsSendRequest request = smsSendRequestMapper.selectById(event.getRequestId());
        if (request == null || smsSendRequestMapper.markProcessing(request.getId(), now) != 1) {
            markUnknown(event, request == null ? null : request.getId(), null, "SMS_REQUEST_STATE_INVALID");
            return;
        }

        dispatch(event, request);
    }

    private void dispatch(SmsOutboxEvent event, SmsSendRequest request) {
        String code;
        Long recordId = null;
        try {
            code = smsCodeGenerator.generate();
            recordId = createPendingRecord(request, code);
        } catch (Exception exception) {
            retryPreparationOrDeadLetter(event, request, "SMS_PRE_SEND_PREPARATION_FAILED");
            return;
        }

        SmsSendResult result;
        try {
            result = smsSender.send(new SmsSendCommand(
                    request.getPhone(),
                    request.getSmsType(),
                    code));
        } catch (Exception exception) {
            markUnknown(event, request.getId(), recordId, "SMS_PROVIDER_CALL_UNKNOWN");
            return;
        }

        if (!result.success()) {
            if (result.resultUnknown()) {
                markUnknown(event, request.getId(), recordId, result.failureReason());
            } else {
                markFailed(event, request, recordId, result.failureReason());
            }
            return;
        }

        try {
            saveCodeToRedis(request.getPhone(), request.getSmsType(), code);
            markSent(event, request.getId(), recordId, result.providerRequestId());
        } catch (Exception exception) {
            redisTemplate.delete(smsCodeKey(request.getSmsType(), request.getPhone()));
            markUnknown(event, request.getId(), recordId, "SMS_POST_SEND_PERSISTENCE_UNKNOWN");
        }
    }

    private Long createPendingRecord(SmsSendRequest request, String code) {
        return transactionTemplate.execute(status -> {
            LocalDateTime now = LocalDateTime.now();
            SmsRecord record = new SmsRecord();
            record.setId(snowflakeIdWorker.nextId());
            record.setPhone(request.getPhone());
            record.setSmsType(request.getSmsType());
            record.setCode(passwordEncoder.encode(code));
            record.setStatus(SMS_RECORD_PENDING);
            record.setRequestIp(request.getRequestIp());
            record.setProvider(PROVIDER_MOCK);
            record.setSendTime(now);
            record.setExpireTime(now.plusSeconds(smsProperties.getExpireSeconds()));
            record.setCreateTime(now);
            record.setUpdateTime(now);
            smsRecordMapper.insert(record);
            writeTransition(record.getId(), null, SMS_RECORD_PENDING, "SMS_RECORD_CREATED", null, now);
            return record.getId();
        });
    }

    private void markSent(SmsOutboxEvent event, Long requestId, Long recordId, String providerRequestId) {
        transactionTemplate.executeWithoutResult(status -> {
            if (smsRecordMapper.markSent(recordId, providerRequestId, LocalDateTime.now()) != 1) {
                throw new IllegalStateException("Failed to mark SMS record as sent");
            }
            writeTransition(recordId, SMS_RECORD_PENDING, 1, "SMS_SENT_ACCEPTED", providerRequestId, LocalDateTime.now());
            if (smsSendRequestMapper.markSent(requestId, LocalDateTime.now()) != 1) {
                throw new IllegalStateException("Failed to mark SMS request as sent");
            }
            if (smsOutboxEventMapper.markCompleted(event.getId(), workerId, LocalDateTime.now()) != 1) {
                throw new IllegalStateException("Failed to complete SMS outbox event");
            }
        });
    }

    private void markFailed(SmsOutboxEvent event, SmsSendRequest request, Long recordId, String failureReason) {
        boolean completed = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            if (smsOutboxEventMapper.markCompleted(event.getId(), workerId, LocalDateTime.now()) != 1) {
                return false;
            }
            if (smsSendRequestMapper.markFailed(request.getId(), truncate(failureReason), LocalDateTime.now()) != 1) {
                throw new IllegalStateException("Failed to mark SMS request as failed");
            }
            if (smsRecordMapper.markFailed(recordId, truncate(failureReason), LocalDateTime.now()) != 1) {
                throw new IllegalStateException("Failed to mark SMS record as failed");
            }
            writeTransition(recordId, SMS_RECORD_PENDING, 2, "SMS_PROVIDER_REJECTED", null, LocalDateTime.now());
            return true;
        }));
        // A provider rejection happens after the request transaction committed, so only release cooldown.
        if (completed) {
            smsRateLimiter.releaseRate(request.getPhone(), request.getSmsType());
        }
    }

    private void markUnknown(SmsOutboxEvent event, Long requestId, Long recordId, String failureReason) {
        markUnknown(event, requestId, recordId, failureReason, workerId);
    }

    private void markUnknown(SmsOutboxEvent event,
                             Long requestId,
                             Long recordId,
                             String failureReason,
                             String leaseOwner) {
        transactionTemplate.executeWithoutResult(status -> {
            if (smsOutboxEventMapper.markUnknown(event.getId(), leaseOwner, truncate(failureReason), LocalDateTime.now()) != 1) {
                return;
            }
            if (requestId != null) {
                if (smsSendRequestMapper.markUnknown(requestId, truncate(failureReason), LocalDateTime.now()) != 1) {
                    throw new IllegalStateException("Failed to mark SMS request as unknown");
                }
            }
            if (recordId != null) {
                if (smsRecordMapper.markUnknown(recordId, truncate(failureReason), LocalDateTime.now()) != 1) {
                    throw new IllegalStateException("Failed to mark SMS record as unknown");
                }
                writeTransition(recordId, SMS_RECORD_PENDING, 5, "SMS_PROVIDER_OUTCOME_UNKNOWN", null, LocalDateTime.now());
            }
        });
    }

    private void retryPreparationOrDeadLetter(SmsOutboxEvent event,
                                              SmsSendRequest request,
                                              String failureReason) {
        int nextRetryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        boolean deadLettered = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            if (nextRetryCount > smsProperties.getOutboxMaxRetries()) {
                if (smsOutboxEventMapper.markDeadLetter(event.getId(), workerId, failureReason, LocalDateTime.now()) != 1) {
                    return false;
                }
                if (smsSendRequestMapper.markFailed(request.getId(), failureReason, LocalDateTime.now()) != 1) {
                    throw new IllegalStateException("Failed to mark dead-letter SMS request as failed");
                }
                return true;
            }
            if (smsOutboxEventMapper.reschedule(
                    event.getId(),
                    workerId,
                    LocalDateTime.now().plusSeconds(retryDelaySeconds(nextRetryCount)),
                    failureReason,
                    LocalDateTime.now()) != 1) {
                throw new IllegalStateException("Failed to reschedule SMS outbox event");
            }
            if (smsSendRequestMapper.resetPending(request.getId(), LocalDateTime.now()) != 1) {
                throw new IllegalStateException("Failed to reset SMS request for retry");
            }
            return false;
        }));
        if (deadLettered) {
            smsRateLimiter.releaseRate(request.getPhone(), request.getSmsType());
            log.warn("SMS outbox event {} entered dead letter after {} safe preparation retries", event.getId(), nextRetryCount);
        }
    }

    private void recoverStaleClaims() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(smsProperties.getOutboxProcessingTimeoutSeconds());
        smsOutboxEventMapper.selectStaleProcessing(before, smsProperties.getOutboxBatchSize())
                .forEach(event -> markUnknown(
                        event,
                        event.getRequestId(),
                        null,
                        "SMS_WORKER_LEASE_EXPIRED",
                        event.getLeaseOwner()));
    }

    private long retryDelaySeconds(int retryCount) {
        long multiplier = 1L << Math.min(retryCount - 1, 20);
        return Math.min(smsProperties.getOutboxRetryMaxSeconds(),
                smsProperties.getOutboxRetryBaseSeconds() * multiplier);
    }

    private void saveCodeToRedis(String phone, String smsType, String code) {
        redisTemplate.opsForValue().set(
                smsCodeKey(smsType, phone), smsCodeDigest.digest(code), smsProperties.getExpireSeconds(), TimeUnit.SECONDS);
    }

    private String smsCodeKey(String smsType, String phone) {
        return SMS_CODE_KEY_PREFIX + smsType + ":" + phone;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    private void writeTransition(Long recordId,
                                 Integer fromStatus,
                                 Integer toStatus,
                                 String reason,
                                 String providerReceipt,
                                 LocalDateTime now) {
        SmsRecordStatusTransition transition = new SmsRecordStatusTransition();
        transition.setId(snowflakeIdWorker.nextId());
        transition.setSmsRecordId(recordId);
        transition.setFromStatus(fromStatus);
        transition.setToStatus(toStatus);
        transition.setTransitionReason(reason);
        transition.setProviderReceipt(providerReceipt);
        transition.setTransitionTime(now);
        transition.setCreateTime(now);
        smsRecordStatusTransitionMapper.insert(transition);
    }
}
