package com.nursing.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.entity.EventMessage;
import com.nursing.order.repository.EventMessageMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {
    @Mock
    private EventMessageMapper eventMessageMapper;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private SnowflakeIdWorker snowflakeIdWorker;

    @Test
    void kafkaFailureIsScheduledForRetryInsteadOfMarkedTerminal() {
        EventMessage message = new EventMessage();
        message.setId(10001L);
        message.setTopic("order_events");
        message.setEventKey("order_paid:20001");
        message.setPayload("{}");
        message.setRetryCount(2);
        when(eventMessageMapper.selectPending(anyInt(), any(LocalDateTime.class))).thenReturn(List.of(message));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")));

        publisher().publishPendingEvents();

        ArgumentCaptor<LocalDateTime> nextExecuteTime = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(eventMessageMapper).markRetry(org.mockito.ArgumentMatchers.eq(10001L), nextExecuteTime.capture(),
                org.mockito.ArgumentMatchers.contains("Kafka unavailable"));
        assertThat(nextExecuteTime.getValue()).isAfter(LocalDateTime.now().plusSeconds(3));
    }

    private OrderEventPublisher publisher() {
        return new OrderEventPublisher(eventMessageMapper, kafkaTemplate, snowflakeIdWorker,
                new ObjectMapper(), "order_events", 20);
    }
}
