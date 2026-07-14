package com.nursing.feedback.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.common.event.OrderPaidEvent;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.entity.IdempotentRecord;
import com.nursing.feedback.entity.ReviewEligibility;
import com.nursing.feedback.repository.IdempotentRecordMapper;
import com.nursing.feedback.repository.ReviewEligibilityMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {
    @Mock
    private IdempotentRecordMapper idempotentRecordMapper;
    @Mock
    private ReviewEligibilityMapper reviewEligibilityMapper;
    @Mock
    private SnowflakeIdWorker snowflakeIdWorker;

    @Test
    void persistsEligibilityFromTheSharedProducerEvent() throws Exception {
        when(snowflakeIdWorker.nextId()).thenReturn(1L);
        OrderPaidEvent event = paidEvent();

        consumer().consume(objectMapper().writeValueAsString(event));

        ArgumentCaptor<IdempotentRecord> record = ArgumentCaptor.forClass(IdempotentRecord.class);
        ArgumentCaptor<ReviewEligibility> eligibility = ArgumentCaptor.forClass(ReviewEligibility.class);
        verify(idempotentRecordMapper).insert(record.capture());
        verify(reviewEligibilityMapper).insert(eligibility.capture());
        assertEquals(OrderPaidEvent.TYPE, record.getValue().getBizType());
        assertEquals(20001L, record.getValue().getSubjectId());
        assertEquals(201L, eligibility.getValue().getServiceItemId());
        assertEquals(event.paidAt(), eligibility.getValue().getPaidTime());
    }

    @Test
    void duplicateEventDoesNotWriteEligibilityAgain() throws Exception {
        when(snowflakeIdWorker.nextId()).thenReturn(1L);
        when(idempotentRecordMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));

        consumer().consume(objectMapper().writeValueAsString(paidEvent()));

        verifyNoInteractions(reviewEligibilityMapper);
    }

    @Test
    void eligibilityFailurePropagatesSoTheTransactionCanRollback() throws Exception {
        when(snowflakeIdWorker.nextId()).thenReturn(1L);
        when(reviewEligibilityMapper.insert(any())).thenThrow(new DataIntegrityViolationException("write failed"));

        assertThrows(DataIntegrityViolationException.class,
                () -> consumer().consume(objectMapper().writeValueAsString(paidEvent())));
    }

    private OrderEventConsumer consumer() {
        return new OrderEventConsumer(objectMapper(), idempotentRecordMapper, reviewEligibilityMapper, snowflakeIdWorker);
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private OrderPaidEvent paidEvent() {
        return OrderPaidEvent.of(
                20001L, "NO-20001", 10001L, 201L, LocalDateTime.of(2026, 7, 13, 10, 0));
    }
}
