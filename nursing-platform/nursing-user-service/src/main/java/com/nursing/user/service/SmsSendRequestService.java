package com.nursing.user.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.common.constant.ApiCode;
import com.nursing.user.config.SmsProperties;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.response.SmsCodeResponse;
import com.nursing.user.entity.SmsOutboxEvent;
import com.nursing.user.entity.SmsSendRequest;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.mapper.SmsOutboxEventMapper;
import com.nursing.user.mapper.SmsSendRequestMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
public class SmsSendRequestService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PROCESSING = 1;
    private static final int STATUS_SENT = 2;
    private static final int STATUS_FAILED = 3;
    private static final int STATUS_UNKNOWN = 4;
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final SmsProperties smsProperties;
    private final SmsRateLimiter smsRateLimiter;
    private final SmsSendRequestMapper smsSendRequestMapper;
    private final SmsOutboxEventMapper smsOutboxEventMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final TransactionTemplate transactionTemplate;

    public SmsSendRequestService(SmsProperties smsProperties,
                                 SmsRateLimiter smsRateLimiter,
                                 SmsSendRequestMapper smsSendRequestMapper,
                                 SmsOutboxEventMapper smsOutboxEventMapper,
                                 SnowflakeIdWorker snowflakeIdWorker,
                                 TransactionTemplate transactionTemplate) {
        this.smsProperties = smsProperties;
        this.smsRateLimiter = smsRateLimiter;
        this.smsSendRequestMapper = smsSendRequestMapper;
        this.smsOutboxEventMapper = smsOutboxEventMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.transactionTemplate = transactionTemplate;
    }

    public SmsCodeResponse accept(String phone, String smsType, String requestIp, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        String fingerprint = fingerprint(phone, smsType);
        SmsSendRequest existing = smsSendRequestMapper.selectByIdempotencyKey(idempotencyKey);
        if (isExpiredTerminal(existing, LocalDateTime.now())) {
            smsSendRequestMapper.deleteExpiredTerminalByIdempotencyKey(idempotencyKey, LocalDateTime.now());
            existing = smsSendRequestMapper.selectByIdempotencyKey(idempotencyKey);
        }
        if (existing != null) {
            return existingResponse(existing, fingerprint);
        }

        AtomicReference<SmsRateLimiter.Reservation> rateReservation = new AtomicReference<>();
        try {
            SmsSendRequest created = transactionTemplate.execute(status -> {
                LocalDateTime now = LocalDateTime.now();
                SmsSendRequest request = new SmsSendRequest();
                request.setId(snowflakeIdWorker.nextId());
                request.setIdempotencyKey(idempotencyKey);
                request.setRequestFingerprint(fingerprint);
                request.setPhone(phone);
                request.setSmsType(smsType);
                request.setRequestIp(requestIp);
                request.setStatus(STATUS_PENDING);
                request.setResponseSnapshot(initialResponseSnapshot(request.getId()));
                request.setIdempotentExpireTime(now.plusHours(smsProperties.getIdempotentRetentionHours()));
                request.setCreateTime(now);
                request.setUpdateTime(now);
                smsSendRequestMapper.insert(request);

                rateReservation.set(smsRateLimiter.checkAndReserve(phone, smsType, requestIp));

                SmsOutboxEvent event = new SmsOutboxEvent();
                event.setId(snowflakeIdWorker.nextId());
                event.setRequestId(request.getId());
                event.setEventKey("sms-send:" + request.getId());
                event.setEventType("SMS_SEND");
                event.setStatus(STATUS_PENDING);
                event.setNextExecuteTime(now);
                event.setRetryCount(0);
                event.setCreateTime(now);
                event.setUpdateTime(now);
                smsOutboxEventMapper.insert(event);
                return request;
            });
            return toResponse(created);
        } catch (DuplicateKeyException exception) {
            cancelReservationIfNeeded(rateReservation);
            SmsSendRequest duplicate = smsSendRequestMapper.selectByIdempotencyKey(idempotencyKey);
            if (duplicate == null) {
                throw exception;
            }
            return existingResponse(duplicate, fingerprint);
        } catch (RuntimeException exception) {
            cancelReservationIfNeeded(rateReservation);
            throw exception;
        }
    }

    public SmsCodeResponse query(Long requestId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        SmsSendRequest request = smsSendRequestMapper.selectByIdAndIdempotencyKey(requestId, idempotencyKey);
        if (request == null) {
            throw new UserBusinessException(HttpStatus.NOT_FOUND,
                    UserErrorCode.SMS_SEND_REQUEST_NOT_FOUND,
                    "短信发送请求不存在");
        }
        return toResponse(request);
    }

    private SmsCodeResponse existingResponse(SmsSendRequest request, String fingerprint) {
        if (!fingerprint.equals(request.getRequestFingerprint())) {
            throw new UserBusinessException(HttpStatus.CONFLICT,
                    UserErrorCode.SMS_IDEMPOTENCY_CONFLICT,
                    "幂等键不能用于不同的短信请求");
        }
        return toResponse(request);
    }

    private boolean isExpiredTerminal(SmsSendRequest request, LocalDateTime now) {
        return request != null
                && request.getIdempotentExpireTime() != null
                && !request.getIdempotentExpireTime().isAfter(now)
                && (Integer.valueOf(STATUS_SENT).equals(request.getStatus())
                || Integer.valueOf(STATUS_FAILED).equals(request.getStatus())
                || Integer.valueOf(STATUS_UNKNOWN).equals(request.getStatus()));
    }

    private void cancelReservationIfNeeded(AtomicReference<SmsRateLimiter.Reservation> rateReservation) {
        SmsRateLimiter.Reservation reservation = rateReservation.getAndSet(null);
        if (reservation != null) {
            smsRateLimiter.cancelReservation(reservation);
        }
    }

    private SmsCodeResponse toResponse(SmsSendRequest request) {
        SmsCodeResponse response = new SmsCodeResponse(smsProperties.getExpireSeconds());
        response.setRequestId(String.valueOf(request.getId()));
        response.setStatus(statusName(request.getStatus()));
        return response;
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey) || !UUID_PATTERN.matcher(idempotencyKey).matches()) {
            throw new UserBusinessException(HttpStatus.BAD_REQUEST,
                    ApiCode.PARAM_ERROR,
                    "Idempotency-Key必须是UUID");
        }
    }

    private String fingerprint(String phone, String smsType) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((phone + ':' + smsType).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String initialResponseSnapshot(Long requestId) {
        return "{\"requestId\":\"" + requestId
                + "\",\"status\":\"PENDING\",\"expireSeconds\":"
                + smsProperties.getExpireSeconds() + '}';
    }

    private String statusName(Integer status) {
        return switch (status) {
            case STATUS_PENDING -> "PENDING";
            case STATUS_PROCESSING -> "PROCESSING";
            case STATUS_SENT -> "SENT_ACCEPTED";
            case STATUS_FAILED -> "FAILED";
            case STATUS_UNKNOWN -> "UNKNOWN";
            default -> "UNKNOWN";
        };
    }
}
