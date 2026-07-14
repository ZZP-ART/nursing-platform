package com.nursing.user.service;

import com.nursing.user.config.SmsProperties;
import com.nursing.user.mapper.SmsSendRequestMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SmsIdempotencyCleanup {
    private final SmsProperties smsProperties;
    private final SmsSendRequestMapper smsSendRequestMapper;

    public SmsIdempotencyCleanup(SmsProperties smsProperties, SmsSendRequestMapper smsSendRequestMapper) {
        this.smsProperties = smsProperties;
        this.smsSendRequestMapper = smsSendRequestMapper;
    }

    @Scheduled(fixedDelayString = "${nursing.sms.idempotent-cleanup-fixed-delay-millis:3600000}")
    public void deleteExpiredTerminalRequests() {
        smsSendRequestMapper.deleteExpiredTerminal(
                LocalDateTime.now(), smsProperties.getIdempotentCleanupBatchSize());
    }
}
