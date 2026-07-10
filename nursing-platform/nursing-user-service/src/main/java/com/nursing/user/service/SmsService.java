package com.nursing.user.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.config.SmsProperties;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.request.SmsCodeRequest;
import com.nursing.user.dto.response.SmsCodeResponse;
import com.nursing.user.entity.SmsRecord;
import com.nursing.user.entity.User;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.mapper.SmsRecordMapper;
import com.nursing.user.mapper.UserMapper;
import com.nursing.user.security.ClientIpResolver;
import com.nursing.user.sms.SmsSendCommand;
import com.nursing.user.sms.SmsSendResult;
import com.nursing.user.sms.SmsSender;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class SmsService {

    private static final String SMS_TYPE_REGISTER = "register";
    private static final String PROVIDER_ALIYUN = "aliyun";
    private static final int STATUS_PENDING = 0;
    private static final String SMS_CODE_KEY_PREFIX = "sms:code:";
    private static final String SMS_VERIFY_FAIL_KEY_PREFIX = "sms:verify:fail:";

    private final SmsProperties smsProperties;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserMapper userMapper;
    private final SmsRecordMapper smsRecordMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;
    private final SmsRateLimiter smsRateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final TransactionTemplate transactionTemplate;

    public SmsService(SmsProperties smsProperties,
                      RedisTemplate<String, String> redisTemplate,
                      UserMapper userMapper,
                      SmsRecordMapper smsRecordMapper,
                      SnowflakeIdWorker snowflakeIdWorker,
                      PasswordEncoder passwordEncoder,
                      SmsSender smsSender,
                      SmsRateLimiter smsRateLimiter,
                      ClientIpResolver clientIpResolver,
                      TransactionTemplate transactionTemplate) {
        this.smsProperties = smsProperties;
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
        this.smsRecordMapper = smsRecordMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
        this.smsRateLimiter = smsRateLimiter;
        this.clientIpResolver = clientIpResolver;
        this.transactionTemplate = transactionTemplate;
    }

    public SmsCodeResponse sendSmsCode(SmsCodeRequest request, HttpServletRequest httpRequest) {
        String phone = request.getPhone();
        String smsType = request.getSmsType();
        String requestIp = clientIpResolver.resolve(httpRequest);

        validatePhoneForSmsType(phone, smsType);
        smsRateLimiter.checkAndReserve(phone, smsType, requestIp);

        String code = generateCode();
        Long recordId = createPendingRecord(phone, smsType, requestIp, code);

        try {
            SmsSendResult sendResult = smsSender.send(new SmsSendCommand(
                    phone,
                    smsType,
                    code,
                    smsProperties.getSignName(),
                    smsProperties.templateCodeFor(smsType)));
            if (!sendResult.success()) {
                if (sendResult.resultUnknown()) {
                    unknownSend(recordId, sendResult.failureReason());
                }
                failSend(recordId, phone, smsType, sendResult.failureReason());
            }

            saveCodeToRedis(phone, smsType, code);
            markSent(recordId, sendResult.providerRequestId());
            return new SmsCodeResponse(smsProperties.getExpireSeconds());
        } catch (UserBusinessException e) {
            throw e;
        } catch (Exception e) {
            redisTemplate.delete(smsCodeKey(smsType, phone));
            unknownSend(recordId, e.getMessage());
            throw smsSendFailed();
        }
    }

    public void verifySmsCode(String phone, String smsType, String smsCode) {
        String key = smsCodeKey(smsType, phone);
        String cachedCode = redisTemplate.opsForValue().get(key);
        if (cachedCode == null) {
            throw new UserBusinessException(
                    HttpStatus.BAD_REQUEST,
                    UserErrorCode.SMS_CODE_EXPIRED,
                    "验证码已过期");
        }

        enforceVerifyAttempts(phone, smsType, key);
        if (!cachedCode.equals(smsCode)) {
            recordVerifyFailure(phone, smsType, key);
            throw new UserBusinessException(
                    HttpStatus.BAD_REQUEST,
                    UserErrorCode.SMS_CODE_INVALID,
                    "验证码错误");
        }

        redisTemplate.delete(key);
        redisTemplate.delete(verifyFailKey(smsType, phone));
        markLatestVerified(phone, smsType);
    }

    private Long createPendingRecord(String phone, String smsType, String requestIp, String code) {
        return transactionTemplate.execute(status -> {
            LocalDateTime now = LocalDateTime.now();
            SmsRecord record = new SmsRecord();
            record.setId(snowflakeIdWorker.nextId());
            record.setPhone(phone);
            record.setSmsType(smsType);
            record.setCode(passwordEncoder.encode(code));
            record.setStatus(STATUS_PENDING);
            record.setRequestIp(requestIp);
            record.setProvider(PROVIDER_ALIYUN);
            record.setSendTime(now);
            record.setExpireTime(now.plusSeconds(smsProperties.getExpireSeconds()));
            record.setCreateTime(now);
            record.setUpdateTime(now);
            smsRecordMapper.insert(record);
            return record.getId();
        });
    }

    private void markSent(Long recordId, String providerRequestId) {
        transactionTemplate.executeWithoutResult(status -> {
            int updated = smsRecordMapper.markSent(recordId, providerRequestId, LocalDateTime.now());
            if (updated != 1) {
                throw new IllegalStateException("Failed to mark SMS record as sent: " + recordId);
            }
        });
    }

    private void markFailed(Long recordId, String failureReason) {
        transactionTemplate.executeWithoutResult(status ->
                smsRecordMapper.markFailed(recordId, truncate(failureReason), LocalDateTime.now()));
    }

    private void markUnknown(Long recordId, String failureReason) {
        transactionTemplate.executeWithoutResult(status ->
                smsRecordMapper.markUnknown(recordId, truncate(failureReason), LocalDateTime.now()));
    }

    private void markLatestVerified(String phone, String smsType) {
        transactionTemplate.executeWithoutResult(status ->
                smsRecordMapper.markLatestVerified(phone, smsType, LocalDateTime.now()));
    }

    private void failSend(Long recordId, String phone, String smsType, String failureReason) {
        markFailed(recordId, failureReason);
        smsRateLimiter.releaseRate(phone, smsType);
        throw smsSendFailed();
    }

    private void unknownSend(Long recordId, String failureReason) {
        markUnknown(recordId, failureReason);
        throw smsSendFailed();
    }

    private void saveCodeToRedis(String phone, String smsType, String code) {
        redisTemplate.opsForValue().set(
                smsCodeKey(smsType, phone),
                code,
                smsProperties.getExpireSeconds(),
                TimeUnit.SECONDS);
    }

    private void enforceVerifyAttempts(String phone, String smsType, String codeKey) {
        String failKey = verifyFailKey(smsType, phone);
        String value = redisTemplate.opsForValue().get(failKey);
        if (value == null) {
            return;
        }
        int attempts = Integer.parseInt(value);
        if (attempts >= smsProperties.getVerifyMaxAttempts()) {
            redisTemplate.delete(codeKey);
            throw new UserBusinessException(
                    HttpStatus.BAD_REQUEST,
                    UserErrorCode.SMS_CODE_INVALID,
                    "验证码错误次数过多，请重新获取");
        }
    }

    private void recordVerifyFailure(String phone, String smsType, String codeKey) {
        String failKey = verifyFailKey(smsType, phone);
        Long attempts = redisTemplate.opsForValue().increment(failKey);
        if (attempts != null && attempts == 1L) {
            Long ttl = redisTemplate.getExpire(codeKey, TimeUnit.SECONDS);
            long expireSeconds = ttl == null || ttl <= 0 ? smsProperties.getExpireSeconds() : ttl;
            redisTemplate.expire(failKey, expireSeconds, TimeUnit.SECONDS);
        }
    }

    private void validatePhoneForSmsType(String phone, String smsType) {
        User user = userMapper.selectByPhone(phone);
        if (SMS_TYPE_REGISTER.equals(smsType) && user != null) {
            throw new UserBusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    UserErrorCode.PHONE_ALREADY_REGISTERED,
                    "手机号已被注册");
        }
        if (!SMS_TYPE_REGISTER.equals(smsType) && user == null) {
            throw new UserBusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    UserErrorCode.PHONE_NOT_REGISTERED,
                    "手机号未注册");
        }
    }

    private UserBusinessException smsSendFailed() {
        return new UserBusinessException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                UserErrorCode.SMS_SEND_FAILED,
                "短信发送失败，请稍后重试");
    }

    private String smsCodeKey(String smsType, String phone) {
        return SMS_CODE_KEY_PREFIX + smsType + ":" + phone;
    }

    private String verifyFailKey(String smsType, String phone) {
        return SMS_VERIFY_FAIL_KEY_PREFIX + smsType + ":" + phone;
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }
}
