package com.nursing.user.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.config.SmsProperties;
import com.nursing.user.entity.SmsOutboxEvent;
import com.nursing.user.entity.SmsSendRequest;
import com.nursing.user.mapper.SmsOutboxEventMapper;
import com.nursing.user.mapper.SmsRecordMapper;
import com.nursing.user.mapper.SmsRecordStatusTransitionMapper;
import com.nursing.user.mapper.SmsSendRequestMapper;
import com.nursing.user.sms.SmsSendResult;
import com.nursing.user.sms.SmsSender;
import com.nursing.user.sms.SmsCodeGenerator;
import com.nursing.user.security.SmsCodeDigest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsOutboxWorkerTest {

    private static final String TEST_JWT_SECRET = "test-jwt-secret-with-at-least-32-bytes";

    @Test
    void sendsClaimedRequestAndCompletesOutboxOnlyAfterProviderSuccess() {
        SmsProperties properties = new SmsProperties();
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsRecordMapper recordMapper = mock(SmsRecordMapper.class);
        SmsRecordStatusTransitionMapper transitionMapper = mock(SmsRecordStatusTransitionMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        SmsSender sender = mock(SmsSender.class);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        TransactionTemplate transactionTemplate = transactionTemplate();

        SmsOutboxEvent event = new SmsOutboxEvent();
        event.setId(10L);
        event.setRequestId(20L);
        SmsSendRequest request = new SmsSendRequest();
        request.setId(20L);
        request.setPhone("13800138000");
        request.setSmsType("register");
        request.setRequestIp("127.0.0.1");
        when(outboxMapper.selectStaleProcessing(any(), eq(20))).thenReturn(List.of());
        when(outboxMapper.selectPending(eq(20), any(java.time.LocalDateTime.class))).thenReturn(List.of(event));
        when(outboxMapper.claim(eq(10L), anyString(), any(), any())).thenReturn(1);
        when(requestMapper.selectById(20L)).thenReturn(request);
        when(requestMapper.markProcessing(eq(20L), any())).thenReturn(1);
        when(sender.send(any())).thenReturn(SmsSendResult.success("provider-id"));
        when(recordMapper.markSent(anyLong(), eq("provider-id"), any())).thenReturn(1);
        when(requestMapper.markSent(eq(20L), any())).thenReturn(1);
        when(outboxMapper.markCompleted(eq(10L), anyString(), any())).thenReturn(1);

        SmsOutboxWorker worker = new SmsOutboxWorker(
                properties,
                outboxMapper,
                requestMapper,
                recordMapper,
                transitionMapper,
                rateLimiter,
                sender,
                redisTemplate,
                new SnowflakeIdWorker(1, 1),
                () -> "123456",
                new SmsCodeDigest(TEST_JWT_SECRET),
                passwordEncoder,
                transactionTemplate);

        worker.dispatchPending();

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("sms:code:register:13800138000"), codeCaptor.capture(), eq(300L), eq(TimeUnit.SECONDS));
        verify(recordMapper).insert(any());
        verify(recordMapper).markSent(anyLong(), eq("provider-id"), any());
        verify(requestMapper).markSent(eq(20L), any());
        verify(outboxMapper).markCompleted(eq(10L), anyString(), any());
        verify(transitionMapper, times(2)).insert(any());
        org.assertj.core.api.Assertions.assertThat(codeCaptor.getValue())
                .matches("^[0-9a-f]{64}$")
                .isNotEqualTo("123456");
    }

    @Test
    void marksUnknownProviderResultWithoutReschedulingIt() {
        SmsProperties properties = configuredProperties();
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsRecordMapper recordMapper = mock(SmsRecordMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        SmsSender sender = mock(SmsSender.class);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        SmsOutboxEvent event = pendingEvent();
        SmsSendRequest request = pendingRequest();
        when(outboxMapper.selectStaleProcessing(any(), eq(20))).thenReturn(List.of());
        when(outboxMapper.selectPending(eq(20), any(java.time.LocalDateTime.class))).thenReturn(List.of(event));
        when(outboxMapper.claim(eq(10L), anyString(), any(), any())).thenReturn(1);
        when(requestMapper.selectById(20L)).thenReturn(request);
        when(requestMapper.markProcessing(eq(20L), any())).thenReturn(1);
        when(sender.send(any())).thenReturn(SmsSendResult.unknown("TIMEOUT", "provider timeout"));
        when(outboxMapper.markUnknown(eq(10L), anyString(), eq("TIMEOUT: provider timeout"), any())).thenReturn(1);
        when(requestMapper.markUnknown(eq(20L), eq("TIMEOUT: provider timeout"), any())).thenReturn(1);
        when(recordMapper.markUnknown(anyLong(), eq("TIMEOUT: provider timeout"), any())).thenReturn(1);

        worker(properties, outboxMapper, requestMapper, recordMapper, rateLimiter, sender,
                redisTemplate, passwordEncoder).dispatchPending();

        verify(requestMapper).markUnknown(eq(20L), eq("TIMEOUT: provider timeout"), any());
        verify(outboxMapper).markUnknown(eq(10L), anyString(), eq("TIMEOUT: provider timeout"), any());
        verify(outboxMapper, never()).reschedule(any(), any(), any(), any(), any());
    }

    @Test
    void reschedulesOnlyPreparationFailuresBeforeCallingProvider() {
        SmsProperties properties = new SmsProperties();
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsRecordMapper recordMapper = mock(SmsRecordMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        SmsSender sender = mock(SmsSender.class);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SmsOutboxEvent event = pendingEvent();
        SmsSendRequest request = pendingRequest();
        when(outboxMapper.selectStaleProcessing(any(), eq(20))).thenReturn(List.of());
        when(outboxMapper.selectPending(eq(20), any(java.time.LocalDateTime.class))).thenReturn(List.of(event));
        when(outboxMapper.claim(eq(10L), anyString(), any(), any())).thenReturn(1);
        when(requestMapper.selectById(20L)).thenReturn(request);
        when(requestMapper.markProcessing(eq(20L), any())).thenReturn(1);
        when(requestMapper.resetPending(eq(20L), any())).thenReturn(1);
        when(outboxMapper.reschedule(eq(10L), anyString(), any(), eq("SMS_PRE_SEND_PREPARATION_FAILED"), any()))
                .thenReturn(1);

        worker(properties, outboxMapper, requestMapper, recordMapper, rateLimiter, sender,
                redisTemplate, passwordEncoder,
                () -> { throw new IllegalStateException("pre-send test failure"); }).dispatchPending();

        verify(requestMapper).resetPending(eq(20L), any());
        verify(outboxMapper).reschedule(eq(10L), anyString(), any(), eq("SMS_PRE_SEND_PREPARATION_FAILED"), any());
        verify(sender, never()).send(any());
    }

    private static SmsProperties configuredProperties() {
        SmsProperties properties = new SmsProperties();
        return properties;
    }

    private static SmsOutboxEvent pendingEvent() {
        SmsOutboxEvent event = new SmsOutboxEvent();
        event.setId(10L);
        event.setRequestId(20L);
        event.setRetryCount(0);
        return event;
    }

    private static SmsSendRequest pendingRequest() {
        SmsSendRequest request = new SmsSendRequest();
        request.setId(20L);
        request.setPhone("13800138000");
        request.setSmsType("register");
        request.setRequestIp("127.0.0.1");
        return request;
    }

    private static SmsOutboxWorker worker(SmsProperties properties,
                                           SmsOutboxEventMapper outboxMapper,
                                           SmsSendRequestMapper requestMapper,
                                           SmsRecordMapper recordMapper,
                                           SmsRateLimiter rateLimiter,
                                           SmsSender sender,
                                           RedisTemplate<String, String> redisTemplate,
                                           PasswordEncoder passwordEncoder) {
        return worker(properties, outboxMapper, requestMapper, recordMapper, rateLimiter, sender,
                redisTemplate, passwordEncoder, () -> "123456");
    }

    private static SmsOutboxWorker worker(SmsProperties properties,
                                           SmsOutboxEventMapper outboxMapper,
                                           SmsSendRequestMapper requestMapper,
                                           SmsRecordMapper recordMapper,
                                           SmsRateLimiter rateLimiter,
                                           SmsSender sender,
                                           RedisTemplate<String, String> redisTemplate,
                                           PasswordEncoder passwordEncoder,
                                           SmsCodeGenerator smsCodeGenerator) {
        return new SmsOutboxWorker(
                properties,
                outboxMapper,
                requestMapper,
                recordMapper,
                mock(SmsRecordStatusTransitionMapper.class),
                rateLimiter,
                sender,
                redisTemplate,
                new SnowflakeIdWorker(1, 1),
                smsCodeGenerator,
                new SmsCodeDigest(TEST_JWT_SECRET),
                passwordEncoder,
                transactionTemplate());
    }

    @Test
    void reschedulesSafePreparationFailureWithoutCallingProvider() {
        SmsProperties properties = new SmsProperties();
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsRecordMapper recordMapper = mock(SmsRecordMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        SmsSender sender = mock(SmsSender.class);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        SmsOutboxEvent event = event(10L, 20L, 0);
        SmsSendRequest request = request(20L);
        when(outboxMapper.selectStaleProcessing(any(), eq(20))).thenReturn(List.of());
        when(outboxMapper.selectPending(eq(20), any(java.time.LocalDateTime.class))).thenReturn(List.of(event));
        when(outboxMapper.claim(eq(10L), anyString(), any(), any())).thenReturn(1);
        when(requestMapper.selectById(20L)).thenReturn(request);
        when(requestMapper.markProcessing(eq(20L), any())).thenReturn(1);
        when(requestMapper.resetPending(eq(20L), any())).thenReturn(1);
        when(outboxMapper.reschedule(eq(10L), anyString(), any(), anyString(), any())).thenReturn(1);

        worker(properties, outboxMapper, requestMapper, recordMapper, rateLimiter, sender,
                redisTemplate, passwordEncoder,
                () -> { throw new IllegalStateException("pre-send test failure"); }).dispatchPending();

        verify(sender, org.mockito.Mockito.never()).send(any());
        verify(requestMapper).resetPending(eq(20L), any());
        verify(outboxMapper).reschedule(eq(10L), anyString(), any(), anyString(), any());
    }

    @Test
    void marksRequestAndOutboxUnknownWhenProviderOutcomeIsUncertain() {
        SmsProperties properties = new SmsProperties();
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsRecordMapper recordMapper = mock(SmsRecordMapper.class);
        SmsRateLimiter rateLimiter = mock(SmsRateLimiter.class);
        SmsSender sender = mock(SmsSender.class);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        SmsOutboxEvent event = event(10L, 20L, 0);
        SmsSendRequest request = request(20L);
        when(outboxMapper.selectStaleProcessing(any(), eq(20))).thenReturn(List.of());
        when(outboxMapper.selectPending(eq(20), any(java.time.LocalDateTime.class))).thenReturn(List.of(event));
        when(outboxMapper.claim(eq(10L), anyString(), any(), any())).thenReturn(1);
        when(requestMapper.selectById(20L)).thenReturn(request);
        when(requestMapper.markProcessing(eq(20L), any())).thenReturn(1);
        when(sender.send(any())).thenThrow(new RuntimeException("provider timeout"));
        when(outboxMapper.markUnknown(eq(10L), anyString(), eq("SMS_PROVIDER_CALL_UNKNOWN"), any())).thenReturn(1);
        when(requestMapper.markUnknown(eq(20L), eq("SMS_PROVIDER_CALL_UNKNOWN"), any())).thenReturn(1);
        when(recordMapper.markUnknown(anyLong(), eq("SMS_PROVIDER_CALL_UNKNOWN"), any())).thenReturn(1);

        worker(properties, outboxMapper, requestMapper, recordMapper, rateLimiter, sender,
                redisTemplate, passwordEncoder).dispatchPending();

        verify(recordMapper).markUnknown(anyLong(), eq("SMS_PROVIDER_CALL_UNKNOWN"), any());
        verify(requestMapper).markUnknown(eq(20L), eq("SMS_PROVIDER_CALL_UNKNOWN"), any());
        verify(outboxMapper).markUnknown(eq(10L), anyString(), eq("SMS_PROVIDER_CALL_UNKNOWN"), any());
    }

    @Test
    void doesNotOverwriteRequestWhenStaleLeaseWasAlreadyReplaced() {
        SmsProperties properties = new SmsProperties();
        SmsOutboxEventMapper outboxMapper = mock(SmsOutboxEventMapper.class);
        SmsSendRequestMapper requestMapper = mock(SmsSendRequestMapper.class);
        SmsRecordMapper recordMapper = mock(SmsRecordMapper.class);
        SmsOutboxEvent staleEvent = event(10L, 20L, 0);
        staleEvent.setLeaseOwner("expired-worker");
        when(outboxMapper.selectStaleProcessing(any(), eq(20))).thenReturn(List.of(staleEvent));
        when(outboxMapper.selectPending(eq(20), any(java.time.LocalDateTime.class))).thenReturn(List.of());
        when(outboxMapper.markUnknown(eq(10L), eq("expired-worker"), anyString(), any())).thenReturn(0);

        worker(properties, outboxMapper, requestMapper, recordMapper, mock(SmsRateLimiter.class),
                mock(SmsSender.class), mock(RedisTemplate.class), mock(PasswordEncoder.class)).dispatchPending();

        verify(requestMapper, never()).markUnknown(anyLong(), anyString(), any());
        verify(recordMapper, never()).markUnknown(anyLong(), anyString(), any());
    }

    private static SmsOutboxEvent event(Long id, Long requestId, int retryCount) {
        SmsOutboxEvent event = new SmsOutboxEvent();
        event.setId(id);
        event.setRequestId(requestId);
        event.setRetryCount(retryCount);
        return event;
    }

    private static SmsSendRequest request(Long id) {
        SmsSendRequest request = new SmsSendRequest();
        request.setId(id);
        request.setPhone("13800138000");
        request.setSmsType("register");
        request.setRequestIp("127.0.0.1");
        return request;
    }

    private static TransactionTemplate transactionTemplate() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
        doAnswer(invocation -> {
            Consumer<org.springframework.transaction.TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        return transactionTemplate;
    }
}
