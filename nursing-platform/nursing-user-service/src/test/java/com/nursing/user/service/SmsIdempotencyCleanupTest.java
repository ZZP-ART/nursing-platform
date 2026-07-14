package com.nursing.user.service;

import com.nursing.user.config.SmsProperties;
import com.nursing.user.mapper.SmsSendRequestMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmsIdempotencyCleanupTest {

    @Test
    void removesExpiredTerminalRequestsInConfiguredBatches() {
        SmsProperties properties = new SmsProperties();
        properties.setIdempotentCleanupBatchSize(37);
        SmsSendRequestMapper mapper = mock(SmsSendRequestMapper.class);

        new SmsIdempotencyCleanup(properties, mapper).deleteExpiredTerminalRequests();

        verify(mapper).deleteExpiredTerminal(any(), eq(37));
    }
}
