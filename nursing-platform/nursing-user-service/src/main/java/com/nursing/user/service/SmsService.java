package com.nursing.user.service;

import com.nursing.user.config.SmsProperties;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.request.SmsCodeRequest;
import com.nursing.user.dto.response.SmsCodeResponse;
import com.nursing.user.entity.User;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.mapper.SmsRecordMapper;
import com.nursing.user.mapper.UserMapper;
import com.nursing.user.security.ClientIpResolver;
import com.nursing.user.security.SmsCodeDigest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class SmsService {

    private static final String SMS_TYPE_REGISTER = "register";
    private static final String SMS_CODE_KEY_PREFIX = "sms:code:";
    private static final String SMS_VERIFY_FAIL_KEY_PREFIX = "sms:verify:fail:";
    private static final String VERIFY_OK = "OK";
    private static final String VERIFY_EXPIRED = "EXPIRED";
    private static final String VERIFY_TOO_MANY = "TOO_MANY";

    private static final String VERIFY_AND_CONSUME_LUA = """
            local code = redis.call('get', KEYS[1])
            if not code then
                return 'EXPIRED'
            end
            local attempts = tonumber(redis.call('get', KEYS[2]) or '0')
            if attempts >= tonumber(ARGV[2]) then
                redis.call('del', KEYS[1])
                redis.call('del', KEYS[2])
                return 'TOO_MANY'
            end
            if code ~= ARGV[1] then
                local nextAttempts = redis.call('incr', KEYS[2])
                if nextAttempts == 1 then
                    local ttl = redis.call('ttl', KEYS[1])
                    if ttl > 0 then redis.call('expire', KEYS[2], ttl) end
                end
                return 'INVALID'
            end
            redis.call('del', KEYS[1])
            redis.call('del', KEYS[2])
            return 'OK'
            """;

    private final SmsProperties smsProperties;
    private final UserMapper userMapper;
    private final SmsRecordMapper smsRecordMapper;
    private final SmsSendRequestService smsSendRequestService;
    private final ClientIpResolver clientIpResolver;
    private final SmsCodeDigest smsCodeDigest;
    private final TransactionTemplate transactionTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<String> verifyAndConsumeScript;

    public SmsService(SmsProperties smsProperties,
                      UserMapper userMapper,
                      SmsRecordMapper smsRecordMapper,
                      SmsSendRequestService smsSendRequestService,
                      ClientIpResolver clientIpResolver,
                      SmsCodeDigest smsCodeDigest,
                      TransactionTemplate transactionTemplate,
                      RedisTemplate<String, String> redisTemplate) {
        this.smsProperties = smsProperties;
        this.userMapper = userMapper;
        this.smsRecordMapper = smsRecordMapper;
        this.smsSendRequestService = smsSendRequestService;
        this.clientIpResolver = clientIpResolver;
        this.smsCodeDigest = smsCodeDigest;
        this.transactionTemplate = transactionTemplate;
        this.redisTemplate = redisTemplate;
        this.verifyAndConsumeScript = new DefaultRedisScript<>(VERIFY_AND_CONSUME_LUA, String.class);
    }

    public SmsCodeResponse sendSmsCode(SmsCodeRequest request,
                                       HttpServletRequest httpRequest,
                                       String idempotencyKey) {
        String phone = request.getPhone();
        String smsType = request.getSmsType();
        validatePhoneForSmsType(phone, smsType);
        return smsSendRequestService.accept(phone, smsType, clientIpResolver.resolve(httpRequest), idempotencyKey);
    }

    public SmsCodeResponse querySmsRequest(Long requestId, String idempotencyKey) {
        return smsSendRequestService.query(requestId, idempotencyKey);
    }

    public void verifySmsCode(String phone, String smsType, String smsCode) {
        String outcome = redisTemplate.execute(
                verifyAndConsumeScript,
                List.of(smsCodeKey(smsType, phone), verifyFailKey(smsType, phone)),
                smsCodeDigest.digest(smsCode),
                String.valueOf(smsProperties.getVerifyMaxAttempts()));
        if (VERIFY_OK.equals(outcome)) {
            transactionTemplate.executeWithoutResult(status ->
                    smsRecordMapper.markLatestVerified(phone, smsType, java.time.LocalDateTime.now()));
            return;
        }
        if (VERIFY_EXPIRED.equals(outcome)) {
            throw new UserBusinessException(HttpStatus.BAD_REQUEST,
                    UserErrorCode.SMS_CODE_EXPIRED,
                    "验证码已过期");
        }
        if (VERIFY_TOO_MANY.equals(outcome)) {
            throw new UserBusinessException(HttpStatus.BAD_REQUEST,
                    UserErrorCode.SMS_CODE_INVALID,
                    "验证码错误次数过多，请重新获取");
        }
        throw new UserBusinessException(HttpStatus.BAD_REQUEST,
                UserErrorCode.SMS_CODE_INVALID,
                "验证码错误");
    }

    private void validatePhoneForSmsType(String phone, String smsType) {
        User user = userMapper.selectByPhone(phone);
        if (SMS_TYPE_REGISTER.equals(smsType) && user != null) {
            throw new UserBusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    UserErrorCode.PHONE_ALREADY_REGISTERED,
                    "手机号已被注册");
        }
        if (!SMS_TYPE_REGISTER.equals(smsType) && user == null) {
            throw new UserBusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    UserErrorCode.PHONE_NOT_REGISTERED,
                    "手机号未注册");
        }
    }

    private String smsCodeKey(String smsType, String phone) {
        return SMS_CODE_KEY_PREFIX + smsType + ":" + phone;
    }

    private String verifyFailKey(String smsType, String phone) {
        return SMS_VERIFY_FAIL_KEY_PREFIX + smsType + ":" + phone;
    }
}
