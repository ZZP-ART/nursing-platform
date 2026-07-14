package com.nursing.order.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.common.event.OrderPaidEvent;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.entity.EventMessage;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.repository.EventMessageMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final int MAX_RETRY_DELAY_SECONDS = 300;
    private final EventMessageMapper eventMessageMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final ObjectMapper objectMapper;
    private final String orderEventsTopic;
    private final int batchSize;

    public OrderEventPublisher(EventMessageMapper eventMessageMapper,
                               KafkaTemplate<String, String> kafkaTemplate,
                               SnowflakeIdWorker snowflakeIdWorker,
                               ObjectMapper objectMapper,
                               @Value("${nursing.events.order-paid.topic:order_events}") String orderEventsTopic,
                               @Value("${nursing.kafka.outbox-batch-size:50}") int batchSize) {
        this.eventMessageMapper = eventMessageMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.objectMapper = objectMapper;
        this.orderEventsTopic = orderEventsTopic;
        this.batchSize = batchSize;
    }

    public void saveOrderPaidEvent(OrderHeader order, LocalDateTime paidAt) {
        OrderPaidEvent event = OrderPaidEvent.of(
                order.getId(), order.getOrderNo(), order.getUserId(), order.getServiceItemId(), paidAt);
        EventMessage message = new EventMessage();
        message.setId(snowflakeIdWorker.nextId());
        message.setTopic(orderEventsTopic);
        message.setEventKey("order_paid:" + order.getId());
        message.setPayload(toJson(event));
        message.setStatus(0);
        message.setRetryCount(0);
        message.setNextExecuteTime(LocalDateTime.now());
        try {
            eventMessageMapper.insert(message);
        } catch (DuplicateKeyException ignored) {
            // Outbox idempotency: the same business event was already recorded.
        }
    }

    @Scheduled(fixedDelayString = "${nursing.kafka.outbox-fixed-delay:5000}")
    public void publishPendingEvents() {
        eventMessageMapper.selectPending(batchSize, LocalDateTime.now()).forEach(message -> {
            try {
                kafkaTemplate.send(message.getTopic(), message.getEventKey(), message.getPayload())
                        .get(5, TimeUnit.SECONDS);
                eventMessageMapper.markSent(message.getId());
            } catch (Exception e) {
                int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
                LocalDateTime nextExecuteTime = LocalDateTime.now().plusSeconds(retryDelaySeconds(retryCount));
                eventMessageMapper.markRetry(message.getId(), nextExecuteTime, errorMessage(e));
                log.error("Order outbox publish failed: eventId={}, retryCount={}, nextExecuteTime={}",
                        message.getId(), retryCount + 1, nextExecuteTime, e);
            }
        });
    }

    private long retryDelaySeconds(int retryCount) {
        return Math.min(1L << Math.min(retryCount, 8), MAX_RETRY_DELAY_SECONDS);
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order event", e);
        }
    }
}
