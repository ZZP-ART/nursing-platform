package com.nursing.user.service;

import com.nursing.user.config.SmsProperties;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.exception.SmsRateLimitException;
import com.nursing.user.exception.UserBusinessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class SmsRateLimiter {

    private static final String OK = "OK";
    private static final String RATE_LIMITED = "RATE_LIMITED";
    private static final String PHONE_DAILY_LIMITED = "PHONE_DAILY_LIMITED";
    private static final String IP_HOURLY_LIMITED = "IP_HOURLY_LIMITED";
    private static final String IP_DAILY_LIMITED = "IP_DAILY_LIMITED";

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private static final String LUA = """
            if redis.call('exists', KEYS[1]) == 1 then
                return 'RATE_LIMITED'
            end
            local phoneCount = tonumber(redis.call('get', KEYS[2]) or '0')
            if phoneCount >= tonumber(ARGV[2]) then
                return 'PHONE_DAILY_LIMITED'
            end
            local ipHourCount = tonumber(redis.call('get', KEYS[3]) or '0')
            if ipHourCount >= tonumber(ARGV[3]) then
                return 'IP_HOURLY_LIMITED'
            end
            local ipDayCount = tonumber(redis.call('get', KEYS[4]) or '0')
            if ipDayCount >= tonumber(ARGV[4]) then
                return 'IP_DAILY_LIMITED'
            end
            redis.call('set', KEYS[1], ARGV[7], 'EX', tonumber(ARGV[1]))
            local nextPhoneCount = redis.call('incr', KEYS[2])
            if nextPhoneCount == 1 then redis.call('expire', KEYS[2], tonumber(ARGV[5])) end
            local nextIpHourCount = redis.call('incr', KEYS[3])
            if nextIpHourCount == 1 then redis.call('expire', KEYS[3], tonumber(ARGV[6])) end
            local nextIpDayCount = redis.call('incr', KEYS[4])
            if nextIpDayCount == 1 then redis.call('expire', KEYS[4], tonumber(ARGV[5])) end
            return 'OK'
            """;

    private static final String CANCEL_RESERVATION_LUA = """
            if redis.call('get', KEYS[1]) ~= ARGV[1] then
                return 0
            end
            redis.call('del', KEYS[1])
            for index = 2, #KEYS do
                local count = tonumber(redis.call('get', KEYS[index]) or '0')
                if count <= 1 then
                    redis.call('del', KEYS[index])
                else
                    redis.call('decr', KEYS[index])
                end
            end
            return 1
            """;

    private final SmsProperties smsProperties;
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<String> reserveScript;
    private final DefaultRedisScript<Long> cancelReservationScript;

    public SmsRateLimiter(SmsProperties smsProperties, RedisTemplate<String, String> redisTemplate) {
        this.smsProperties = smsProperties;
        this.redisTemplate = redisTemplate;
        this.reserveScript = new DefaultRedisScript<>(LUA, String.class);
        this.cancelReservationScript = new DefaultRedisScript<>(CANCEL_RESERVATION_LUA, Long.class);
    }

    public Reservation checkAndReserve(String phone, String smsType, String ip) {
        LocalDateTime now = LocalDateTime.now();
        List<String> keys = List.of(
                rateKey(phone, smsType),
                phoneDailyKey(phone, now),
                ipHourlyKey(ip, now),
                ipDailyKey(ip, now));
        String token = UUID.randomUUID().toString();
        String result = redisTemplate.execute(
                reserveScript,
                keys,
                String.valueOf(smsProperties.getRateLimitSeconds()),
                String.valueOf(smsProperties.getPhoneDailyLimit()),
                String.valueOf(smsProperties.getIpHourlyLimit()),
                String.valueOf(smsProperties.getIpDailyLimit()),
                String.valueOf(secondsUntilTomorrow(now)),
                String.valueOf(secondsUntilNextHour(now)),
                token);
        if (OK.equals(result)) {
            return new Reservation(token, keys);
        }
        throw toException(result, keys.get(0));
    }

    public void cancelReservation(Reservation reservation) {
        redisTemplate.execute(cancelReservationScript, reservation.keys(), reservation.token());
    }

    public void releaseRate(String phone, String smsType) {
        redisTemplate.delete(rateKey(phone, smsType));
    }

    public record Reservation(String token, List<String> keys) { }

    public String rateKey(String phone, String smsType) {
        return "sms:rate:" + smsType + ":" + phone;
    }

    private String phoneDailyKey(String phone, LocalDateTime now) {
        return "sms:quota:phone:day:" + phone + ":" + now.format(DAY_FORMATTER);
    }

    private String ipHourlyKey(String ip, LocalDateTime now) {
        return "sms:quota:ip:hour:" + ip + ":" + now.format(HOUR_FORMATTER);
    }

    private String ipDailyKey(String ip, LocalDateTime now) {
        return "sms:quota:ip:day:" + ip + ":" + now.format(DAY_FORMATTER);
    }

    private long secondsUntilTomorrow(LocalDateTime now) {
        return Math.max(1, java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).getSeconds());
    }

    private long secondsUntilNextHour(LocalDateTime now) {
        return Math.max(1, java.time.Duration.between(now, now.plusHours(1).withMinute(0).withSecond(0).withNano(0)).getSeconds());
    }

    private UserBusinessException toException(String result, String rateKey) {
        if (RATE_LIMITED.equals(result)) {
            return new SmsRateLimitException(HttpStatus.TOO_MANY_REQUESTS,
                    UserErrorCode.SMS_SEND_TOO_FREQUENT,
                    "发送过于频繁，请稍后重试",
                    retryAfterSeconds(rateKey));
        }
        if (PHONE_DAILY_LIMITED.equals(result)) {
            return new UserBusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    UserErrorCode.SMS_DAILY_LIMIT_REACHED,
                    "今日发送次数已达上限");
        }
        if (IP_HOURLY_LIMITED.equals(result) || IP_DAILY_LIMITED.equals(result)) {
            return new UserBusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    UserErrorCode.SMS_DAILY_LIMIT_REACHED,
                    "请求过于频繁，请稍后重试");
        }
        return new UserBusinessException(HttpStatus.TOO_MANY_REQUESTS,
                UserErrorCode.SMS_SEND_TOO_FREQUENT,
                "发送过于频繁，请稍后重试");
    }

    private long retryAfterSeconds(String rateKey) {
        Long ttl = redisTemplate.getExpire(rateKey, java.util.concurrent.TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return smsProperties.getRateLimitSeconds();
        }
        return ttl;
    }
}
