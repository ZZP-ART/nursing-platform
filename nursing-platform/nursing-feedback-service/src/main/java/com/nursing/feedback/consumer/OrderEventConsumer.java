package com.nursing.feedback.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.common.event.OrderPaidEvent;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.entity.IdempotentRecord;
import com.nursing.feedback.entity.ReviewEligibility;
import com.nursing.feedback.repository.IdempotentRecordMapper;
import com.nursing.feedback.repository.ReviewEligibilityMapper;
import com.nursing.feedback.support.RequestFingerprint;
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
    private final ReviewEligibilityMapper reviewEligibilityMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public OrderEventConsumer(ObjectMapper objectMapper,
                              IdempotentRecordMapper idempotentRecordMapper,
                              ReviewEligibilityMapper reviewEligibilityMapper,
                              SnowflakeIdWorker snowflakeIdWorker) {
        this.objectMapper = objectMapper;
        this.idempotentRecordMapper = idempotentRecordMapper;
        this.reviewEligibilityMapper = reviewEligibilityMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    @Transactional(rollbackFor = Exception.class)
    @KafkaListener(topics = "${nursing.events.order-paid.topic:order_events}", groupId = "${spring.kafka.consumer.group-id:feedback-group}")
    public void consume(String payload) {
        OrderPaidEvent event = parse(payload);
        if (event == null || !OrderPaidEvent.TYPE.equals(event.eventType())) {
            return;
        }
        if (event.orderId() == null || event.userId() == null || event.serviceItemId() == null || event.paidAt() == null) {
            log.warn("Skip ORDER_PAID event without orderId");
            return;
        }
        IdempotentRecord record = new IdempotentRecord();
        record.setId(snowflakeIdWorker.nextId());
        record.setBizType(BIZ_TYPE_ORDER_PAID);
        record.setSubjectId(event.orderId());
        record.setIdempotentKey("order_paid");
        record.setRequestHash(RequestFingerprint.orderPaid(event));
        record.setBizId(event.orderId());
        record.setStatus(IDEMPOTENT_COMPLETED);
        record.setExpireTime(LocalDateTime.now().plusDays(30));
        record.setCreateTime(LocalDateTime.now());
        try {
            idempotentRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            log.info("ORDER_PAID event already consumed: orderId={}", event.orderId());
            return;
        }

        ReviewEligibility eligibility = new ReviewEligibility();
        eligibility.setOrderId(event.orderId());
        eligibility.setUserId(event.userId());
        eligibility.setServiceItemId(event.serviceItemId());
        eligibility.setPaidTime(event.paidAt());
        eligibility.setCreateTime(LocalDateTime.now());
        try {
            reviewEligibilityMapper.insert(eligibility);
        } catch (DuplicateKeyException ex) {
            log.info("Review eligibility already exists: orderId={}", event.orderId());
        }
    }

    private OrderPaidEvent parse(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, OrderPaidEvent.class);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse order event payload", ex);
            return null;
        }
    }
}
