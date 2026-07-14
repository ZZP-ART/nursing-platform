package com.nursing.user.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.config.SmsProperties;
import com.nursing.user.dto.response.SmsCodeResponse;
import com.nursing.user.entity.SmsOutboxEvent;
import com.nursing.user.entity.SmsSendRequest;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.mapper.SmsOutboxEventMapper;
import com.nursing.user.mapper.SmsSendRequestMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsSendRequestServiceTest {

    @Test
    void acceptsNewRequestAndCreatesOutboxInTheSameTransaction() {
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        TransactionTemplate transactionTemplate = transactionTemplate();
        SmsProperties properties = new SmsProperties();
        SmsSendRequestService service = new SmsSendRequestService(
                properties,
                rateLimiter,
                requestMapper,
                outboxMapper,
                new SnowflakeIdWorker(1, 1),
                transactionTemplate);

        String key = "8eab9d84-5d0e-4a31-867e-57bf68f0e1e6";
        SmsCodeResponse response = service.accept("13800138000", "register", "127.0.0.1", key);

        ArgumentCaptor<SmsSendRequest> requestCaptor = ArgumentCaptor.forClass(SmsSendRequest.class);
        ArgumentCaptor<SmsOutboxEvent> eventCaptor = ArgumentCaptor.forClass(SmsOutboxEvent.class);
        verify(requestMapper).insert(requestCaptor.capture());
        verify(rateLimiter).checkAndReserve("13800138000", "register", "127.0.0.1");
        verify(outboxMapper).insert(eventCaptor.capture());
        assertThat(requestCaptor.getValue().getIdempotencyKey()).isEqualTo(key);
        assertThat(eventCaptor.getValue().getRequestId()).isEqualTo(requestCaptor.getValue().getId());
        assertThat(requestCaptor.getValue().getResponseSnapshot())
                .contains("requestId")
                .contains("PENDING");
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("SMS_SEND");
        assertThat(eventCaptor.getValue().getRetryCount()).isZero();
        assertThat(response.getRequestId()).isEqualTo(String.valueOf(requestCaptor.getValue().getId()));
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void replaysExistingRequestWithoutReapplyingRateLimit() {
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        SmsProperties properties = new SmsProperties();
        SmsSendRequest existing = new SmsSendRequest();
        existing.setId(100L);
        existing.setStatus(2);
        existing.setRequestFingerprint("0ae9c272a782c0f0776fbd56c427b124db4ac012b1ee1ef7439e0b5f0df58b0f");
        when(requestMapper.selectByIdempotencyKey("8eab9d84-5d0e-4a31-867e-57bf68f0e1e6")).thenReturn(existing);
        SmsSendRequestService service = new SmsSendRequestService(
                properties,
                rateLimiter,
                requestMapper,
                outboxMapper,
                new SnowflakeIdWorker(1, 1),
                transactionTemplate());

        // The fingerprint is produced by the service for this exact phone and SMS type.
        existing.setRequestFingerprint(fingerprintFor("13800138000", "register"));
        SmsCodeResponse response = service.accept(
                "13800138000", "register", "127.0.0.1", "8eab9d84-5d0e-4a31-867e-57bf68f0e1e6");

        assertThat(response.getRequestId()).isEqualTo("100");
        assertThat(response.getStatus()).isEqualTo("SENT_ACCEPTED");
        verify(rateLimiter, never()).checkAndReserve(any(), any(), any());
        verify(outboxMapper, never()).insert(any());
    }

    @Test
    void acceptsNewRequestAfterExpiredTerminalIdempotencyRecordIsRemoved() {
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        SmsSendRequest expired = new SmsSendRequest();
        expired.setId(100L);
        expired.setStatus(2);
        expired.setIdempotentExpireTime(LocalDateTime.now().minusSeconds(1));
        String key = "8eab9d84-5d0e-4a31-867e-57bf68f0e1e6";
        when(requestMapper.selectByIdempotencyKey(key)).thenReturn(expired, (SmsSendRequest) null);
        when(requestMapper.deleteExpiredTerminalByIdempotencyKey(eq(key), any())).thenReturn(1);
        SmsSendRequestService service = service(requestMapper, outboxMapper, rateLimiter);

        SmsCodeResponse response = service.accept("13800138000", "register", "127.0.0.1", key);

        verify(requestMapper).deleteExpiredTerminalByIdempotencyKey(eq(key), any());
        verify(requestMapper).insert(any());
        verify(outboxMapper).insert(any());
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void rejectsSameIdempotencyKeyWithDifferentRequestFingerprint() {
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsSendRequest existing = new SmsSendRequest();
        existing.setRequestFingerprint(fingerprintFor("13800138000", "register"));
        when(requestMapper.selectByIdempotencyKey("8eab9d84-5d0e-4a31-867e-57bf68f0e1e6")).thenReturn(existing);
        SmsSendRequestService service = service(requestMapper, mock(SmsOutboxEventMapper.class), mock(SmsRateLimiter.class));

        assertThatThrownBy(() -> service.accept(
                "13900139000", "register", "127.0.0.1", "8eab9d84-5d0e-4a31-867e-57bf68f0e1e6"))
                .isInstanceOfSatisfying(UserBusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(2019));
    }

    @Test
    void replaysConcurrentDuplicateAfterDatabaseUniqueConstraintWins() {
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        SmsSendRequest existing = new SmsSendRequest();
        existing.setId(101L);
        existing.setStatus(1);
        existing.setRequestFingerprint(fingerprintFor("13800138000", "register"));
        when(requestMapper.selectByIdempotencyKey("8eab9d84-5d0e-4a31-867e-57bf68f0e1e6"))
                .thenReturn(null, existing);
        doThrow(new DuplicateKeyException("uk_sms_request_idempotency"))
                .when(requestMapper).insert(any());
        SmsSendRequestService service = service(requestMapper, outboxMapper, rateLimiter);

        SmsCodeResponse response = service.accept(
                "13800138000", "register", "127.0.0.1", "8eab9d84-5d0e-4a31-867e-57bf68f0e1e6");

        assertThat(response.getRequestId()).isEqualTo("101");
        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        verify(rateLimiter, never()).checkAndReserve(any(), any(), any());
        verify(outboxMapper, never()).insert(any());
    }

    @Test
    void cancelsTheExactRateReservationWhenOutboxPersistenceFails() {
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        SmsRateLimiter.Reservation reservation = new SmsRateLimiter.Reservation("reservation-token", java.util.List.of("rate", "phone", "ip-hour", "ip-day"));
        when(rateLimiter.checkAndReserve("13800138000", "register", "127.0.0.1")).thenReturn(reservation);
        doThrow(new IllegalStateException("outbox insert failed")).when(outboxMapper).insert(any());
        SmsSendRequestService service = service(requestMapper, outboxMapper, rateLimiter);

        assertThatThrownBy(() -> service.accept(
                "13800138000", "register", "127.0.0.1", "8eab9d84-5d0e-4a31-867e-57bf68f0e1e6"))
                .isInstanceOf(IllegalStateException.class);

        verify(rateLimiter).cancelReservation(reservation);
    }

    private static SmsSendRequestService service(SmsSendRequestMapper requestMapper,
                                                  SmsOutboxEventMapper outboxMapper,
                                                  SmsRateLimiter rateLimiter) {
        return new SmsSendRequestService(
                new SmsProperties(),
                rateLimiter,
                requestMapper,
                outboxMapper,
                new SnowflakeIdWorker(1, 1),
                transactionTemplate());
    }

    private static TransactionTemplate transactionTemplate() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
        return transactionTemplate;
    }

    private static String fingerprintFor(String phone, String smsType) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(
                    (phone + ':' + smsType).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
