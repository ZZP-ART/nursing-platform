package com.nursing.feedback.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.constant.OrderStatus;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.dto.request.SubmitReviewRequest;
import com.nursing.feedback.entity.IdempotentRecord;
import com.nursing.feedback.entity.ReviewEligibility;
import com.nursing.feedback.integration.OrderQueryService;
import com.nursing.feedback.repository.IdempotentRecordMapper;
import com.nursing.feedback.repository.ReviewImageMapper;
import com.nursing.feedback.repository.ReviewEligibilityMapper;
import com.nursing.feedback.repository.ReviewMapper;
import com.nursing.feedback.support.RequestFingerprint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewImageMapper reviewImageMapper;
    @Mock
    private ReviewEligibilityMapper reviewEligibilityMapper;
    @Mock
    private IdempotentRecordMapper idempotentRecordMapper;
    @Mock
    private OrderQueryService orderQueryService;
    @Mock
    private SnowflakeIdWorker snowflakeIdWorker;

    @Test
    void replaysCompletedRequestOnlyWithinTheSameUserScope() {
        SubmitReviewRequest request = reviewRequest();
        IdempotentRecord record = completedRecord(request, 30001L);
        when(idempotentRecordMapper.selectByScope("REVIEW", 10001L, "idem-key")).thenReturn(record);

        long reviewId = service().submitReview(request, 10001L, "idem-key").getReviewId();

        assertEquals(30001L, reviewId);
        verifyNoInteractions(reviewMapper, reviewImageMapper, orderQueryService, snowflakeIdWorker);
    }

    @Test
    void rejectsSameKeyWithDifferentPayload() {
        SubmitReviewRequest request = reviewRequest();
        IdempotentRecord record = completedRecord(request, 30001L);
        record.setRequestHash("different-request");
        when(idempotentRecordMapper.selectByScope("REVIEW", 10001L, "idem-key")).thenReturn(record);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().submitReview(request, 10001L, "idem-key"));

        assertEquals(ApiCode.CONFLICT, ex.getCode());
        verifyNoInteractions(reviewMapper, reviewImageMapper, orderQueryService, snowflakeIdWorker);
    }

    @Test
    void replaysTheCompletedRecordAfterConcurrentUniqueKeyConflict() {
        SubmitReviewRequest request = reviewRequest();
        IdempotentRecord record = completedRecord(request, 30001L);
        when(idempotentRecordMapper.selectByScope("REVIEW", 10001L, "idem-key"))
                .thenReturn(null, record);
        when(idempotentRecordMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));
        when(snowflakeIdWorker.nextId()).thenReturn(1L);

        long reviewId = service().submitReview(request, 10001L, "idem-key").getReviewId();

        assertEquals(30001L, reviewId);
        verifyNoInteractions(reviewMapper, reviewImageMapper, orderQueryService);
    }

    @Test
    void createsARecordScopedToTheAuthenticatedUser() {
        SubmitReviewRequest request = reviewRequest();
        when(idempotentRecordMapper.selectByScope("REVIEW", 10001L, "idem-key")).thenReturn(null);
        when(snowflakeIdWorker.nextId()).thenReturn(1L, 30001L);
        when(orderQueryService.getOrder(20001L)).thenReturn(order(10001L, OrderStatus.COMPLETED.getValue()));
        when(reviewEligibilityMapper.selectByOrderIdAndUserId(20001L, 10001L)).thenReturn(eligibility(10001L, 201L));

        assertEquals(30001L, service().submitReview(request, 10001L, "idem-key").getReviewId());

        ArgumentCaptor<IdempotentRecord> record = ArgumentCaptor.forClass(IdempotentRecord.class);
        verify(idempotentRecordMapper).insert(record.capture());
        assertEquals("REVIEW", record.getValue().getBizType());
        assertEquals(10001L, record.getValue().getSubjectId());
        assertEquals(RequestFingerprint.review(request), record.getValue().getRequestHash());
        verify(idempotentRecordMapper).updateCompleted("REVIEW", 10001L, "idem-key", 30001L);
    }

    @Test
    void rejectsCompletedOrderWithoutPaymentEligibility() {
        SubmitReviewRequest request = reviewRequest();
        when(idempotentRecordMapper.selectByScope("REVIEW", 10001L, "idem-key")).thenReturn(null);
        when(snowflakeIdWorker.nextId()).thenReturn(1L);
        when(orderQueryService.getOrder(20001L)).thenReturn(order(10001L, OrderStatus.COMPLETED.getValue()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().submitReview(request, 10001L, "idem-key"));

        assertEquals(ApiCode.REVIEW_ORDER_STATUS_INVALID, ex.getCode());
        verify(reviewEligibilityMapper).selectByOrderIdAndUserId(20001L, 10001L);
    }

    @Test
    void rejectsBlankReviewContentInServiceLayer() {
        SubmitReviewRequest request = reviewRequest();
        request.setContent("  ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().submitReview(request, 10001L, "idem-key"));

        assertEquals(ApiCode.PARAM_ERROR, ex.getCode());
    }

    private ReviewServiceImpl service() {
        return new ReviewServiceImpl(
                reviewMapper, reviewImageMapper, reviewEligibilityMapper, idempotentRecordMapper, orderQueryService, snowflakeIdWorker);
    }

    private SubmitReviewRequest reviewRequest() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(20001L);
        request.setRating(5);
        request.setContent("Reliable service");
        return request;
    }

    private IdempotentRecord completedRecord(SubmitReviewRequest request, Long reviewId) {
        IdempotentRecord record = new IdempotentRecord();
        record.setStatus(1);
        record.setBizId(reviewId);
        record.setRequestHash(RequestFingerprint.review(request));
        return record;
    }

    private OrderDTO order(Long userId, int status) {
        OrderDTO order = new OrderDTO();
        order.setOrderId(20001L);
        order.setUserId(userId);
        order.setStatus(status);
        order.setServiceItemId(201L);
        return order;
    }

    private ReviewEligibility eligibility(Long userId, Long serviceItemId) {
        ReviewEligibility eligibility = new ReviewEligibility();
        eligibility.setOrderId(20001L);
        eligibility.setUserId(userId);
        eligibility.setServiceItemId(serviceItemId);
        return eligibility;
    }
}
