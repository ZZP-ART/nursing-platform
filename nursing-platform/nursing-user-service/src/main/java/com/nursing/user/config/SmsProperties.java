package com.nursing.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nursing.sms")
public class SmsProperties {

    private long rateLimitSeconds = 60L;
    private int phoneDailyLimit = 10;
    private int ipHourlyLimit = 60;
    private int ipDailyLimit = 300;
    private int verifyMaxAttempts = 5;
    private long expireSeconds = 300L;
    private int idempotentRetentionHours = 24;
    private long idempotentCleanupFixedDelayMillis = 3_600_000L;
    private int idempotentCleanupBatchSize = 500;
    private long outboxFixedDelayMillis = 1000L;
    private int outboxBatchSize = 20;
    private long outboxProcessingTimeoutSeconds = 120L;
    private int outboxMaxRetries = 3;
    private long outboxRetryBaseSeconds = 5L;
    private long outboxRetryMaxSeconds = 300L;

    public long getRateLimitSeconds() {
        return rateLimitSeconds;
    }

    public void setRateLimitSeconds(long rateLimitSeconds) {
        this.rateLimitSeconds = rateLimitSeconds;
    }

    public int getPhoneDailyLimit() {
        return phoneDailyLimit;
    }

    public void setPhoneDailyLimit(int phoneDailyLimit) {
        this.phoneDailyLimit = phoneDailyLimit;
    }

    public int getIpHourlyLimit() {
        return ipHourlyLimit;
    }

    public void setIpHourlyLimit(int ipHourlyLimit) {
        this.ipHourlyLimit = ipHourlyLimit;
    }

    public int getIpDailyLimit() {
        return ipDailyLimit;
    }

    public void setIpDailyLimit(int ipDailyLimit) {
        this.ipDailyLimit = ipDailyLimit;
    }

    public int getVerifyMaxAttempts() {
        return verifyMaxAttempts;
    }

    public void setVerifyMaxAttempts(int verifyMaxAttempts) {
        this.verifyMaxAttempts = verifyMaxAttempts;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public int getIdempotentRetentionHours() {
        return idempotentRetentionHours;
    }

    public void setIdempotentRetentionHours(int idempotentRetentionHours) {
        this.idempotentRetentionHours = idempotentRetentionHours;
    }

    public long getIdempotentCleanupFixedDelayMillis() { return idempotentCleanupFixedDelayMillis; }
    public void setIdempotentCleanupFixedDelayMillis(long value) { this.idempotentCleanupFixedDelayMillis = value; }
    public int getIdempotentCleanupBatchSize() { return idempotentCleanupBatchSize; }
    public void setIdempotentCleanupBatchSize(int value) { this.idempotentCleanupBatchSize = value; }

    public long getOutboxFixedDelayMillis() {
        return outboxFixedDelayMillis;
    }

    public void setOutboxFixedDelayMillis(long outboxFixedDelayMillis) {
        this.outboxFixedDelayMillis = outboxFixedDelayMillis;
    }

    public int getOutboxBatchSize() {
        return outboxBatchSize;
    }

    public void setOutboxBatchSize(int outboxBatchSize) {
        this.outboxBatchSize = outboxBatchSize;
    }

    public long getOutboxProcessingTimeoutSeconds() {
        return outboxProcessingTimeoutSeconds;
    }

    public void setOutboxProcessingTimeoutSeconds(long outboxProcessingTimeoutSeconds) {
        this.outboxProcessingTimeoutSeconds = outboxProcessingTimeoutSeconds;
    }

    public int getOutboxMaxRetries() { return outboxMaxRetries; }
    public void setOutboxMaxRetries(int outboxMaxRetries) { this.outboxMaxRetries = outboxMaxRetries; }
    public long getOutboxRetryBaseSeconds() { return outboxRetryBaseSeconds; }
    public void setOutboxRetryBaseSeconds(long outboxRetryBaseSeconds) { this.outboxRetryBaseSeconds = outboxRetryBaseSeconds; }
    public long getOutboxRetryMaxSeconds() { return outboxRetryMaxSeconds; }
    public void setOutboxRetryMaxSeconds(long outboxRetryMaxSeconds) { this.outboxRetryMaxSeconds = outboxRetryMaxSeconds; }

}
