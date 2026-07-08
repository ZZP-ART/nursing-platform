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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class SmsService {

    private static final String SMS_TYPE_REGISTER = "register";
    private static final String SMS_RATE_KEY_PREFIX = "sms:rate:";
    private static final String SMS_CODE_KEY_PREFIX = "sms:code:";

    private final SmsProperties smsProperties;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserMapper userMapper;
    private final SmsRecordMapper smsRecordMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final PasswordEncoder passwordEncoder;

    public SmsService(SmsProperties smsProperties,
                      RedisTemplate<String, String> redisTemplate,
                      UserMapper userMapper,
                      SmsRecordMapper smsRecordMapper,
                      SnowflakeIdWorker snowflakeIdWorker,
                      PasswordEncoder passwordEncoder) {
        this.smsProperties = smsProperties;
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
        this.smsRecordMapper = smsRecordMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SmsCodeResponse sendSmsCode(SmsCodeRequest request) {
        validatePhoneForSmsType(request.getPhone(), request.getSmsType());
        enforceRateLimit(request.getPhone());

        int todayCount = smsRecordMapper.countTodayByPhone(request.getPhone());
        if (todayCount >= smsProperties.getDailyLimit()) {
            throw new UserBusinessException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    UserErrorCode.SMS_DAILY_LIMIT_REACHED,
                    "今日发送次数已达上限");
        }

        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusSeconds(smsProperties.getExpireSeconds());

        redisTemplate.opsForValue().set(
                smsCodeKey(request.getSmsType(), request.getPhone()),
                code,
                smsProperties.getExpireSeconds(),
                TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(
                SMS_RATE_KEY_PREFIX + request.getPhone(),
                "1",
                smsProperties.getRateLimitSeconds(),
                TimeUnit.SECONDS);

        SmsRecord record = new SmsRecord();
        record.setId(snowflakeIdWorker.nextId());
        record.setPhone(request.getPhone());
        record.setSmsType(request.getSmsType());
        record.setCode(passwordEncoder.encode(code));
        record.setStatus(1);
        record.setSendTime(now);
        record.setExpireTime(expireTime);
        record.setCreateTime(now);
        smsRecordMapper.insert(record);

        return new SmsCodeResponse(smsProperties.getExpireSeconds());
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
        if (!cachedCode.equals(smsCode)) {
            throw new UserBusinessException(
                    HttpStatus.BAD_REQUEST,
                    UserErrorCode.SMS_CODE_INVALID,
                    "验证码错误");
        }
        redisTemplate.delete(key);
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

    private void enforceRateLimit(String phone) {
        Boolean exists = redisTemplate.hasKey(SMS_RATE_KEY_PREFIX + phone);
        if (Boolean.TRUE.equals(exists)) {
            throw new UserBusinessException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    UserErrorCode.SMS_SEND_TOO_FREQUENT,
                    "发送过于频繁，请 60 秒后重试");
        }
    }

    private String smsCodeKey(String smsType, String phone) {
        return SMS_CODE_KEY_PREFIX + smsType + ":" + phone;
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
