package com.nursing.feedback.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.entity.IdempotentRecord;
import com.nursing.feedback.event.OrderEvent;
import com.nursing.feedback.event.OrderEventType;
import com.nursing.feedback.repository.IdempotentRecordMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final String BIZ_TYPE_ORDER_PAID = "ORDER_PAID";
    private static final int IDEMPOTENT_COMPLETED = 1;

    private final ObjectMapper objectMapper;
    private final IdempotentRecordMapper idempotentRecordMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public OrderEventConsumer(ObjectMapper objectMapper,
                              IdempotentRecordMapper idempotentRecordMapper,
                              SnowflakeIdWorker snowflakeIdWorker) {
        this.objectMapper = objectMapper;
        this.idempotentRecordMapper = idempotentRecordMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    @Transactional(rollbackFor = Exception.class)
    @KafkaListener(topics = "${nursing.kafka.order-topic:order_events}", groupId = "${spring.kafka.consumer.group-id:feedback-group}")
    public void consume(String payload) {
        OrderEvent event = parse(payload);
        if (event == null || !OrderEventType.ORDER_PAID.equals(event.getEventType())) {
            return;
        }
        if (event.getOrderId() == null) {
            log.warn("Skip ORDER_PAID event without orderId: payload={}", payload);
            return;
        }
        String eventKey = "order_paid_" + event.getOrderId();
        if (idempotentRecordMapper.selectByKey(eventKey) != null) {
            log.info("Skip duplicated ORDER_PAID event: eventKey={}", eventKey);
            return;
        }
        IdempotentRecord record = new IdempotentRecord();
        record.setId(snowflakeIdWorker.nextId());
        record.setIdempotentKey(eventKey);
        record.setBizType(BIZ_TYPE_ORDER_PAID);
        record.setBizId(event.getOrderId());
        record.setStatus(IDEMPOTENT_COMPLETED);
        record.setExpireTime(LocalDateTime.now().plusDays(30));
        record.setCreateTime(LocalDateTime.now());
        try {
            idempotentRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            log.info("ORDER_PAID event already consumed concurrently: eventKey={}", eventKey);
        }
    }

    private OrderEvent parse(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, OrderEvent.class);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse order event payload: payload={}", payload, ex);
            return null;
        }
    }
}
