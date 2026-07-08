package com.nursing.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nursing.sms")
public class SmsProperties {

    private boolean mock = true;
    private long rateLimitSeconds = 60L;
    private int dailyLimit = 5;
    private long expireSeconds = 300L;

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    public long getRateLimitSeconds() {
        return rateLimitSeconds;
    }

    public void setRateLimitSeconds(long rateLimitSeconds) {
        this.rateLimitSeconds = rateLimitSeconds;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
