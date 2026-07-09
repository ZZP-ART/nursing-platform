package com.nursing.feedback.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.constant.OrderStatus;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.dto.request.SubmitReviewRequest;
import com.nursing.feedback.dto.response.ReviewSubmitResponse;
import com.nursing.feedback.entity.IdempotentRecord;
import com.nursing.feedback.integration.OrderQueryService;
import com.nursing.feedback.repository.IdempotentRecordMapper;
import com.nursing.feedback.repository.ReviewImageMapper;
import com.nursing.feedback.repository.ReviewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewImageMapper reviewImageMapper;
    @Mock
    private IdempotentRecordMapper idempotentRecordMapper;
    @Mock
    private OrderQueryService orderQueryService;
    @Mock
    private SnowflakeIdWorker snowflakeIdWorker;

    @Test
    void submitReviewReturnsExistingReviewForCompletedIdempotentKey() {
        IdempotentRecord record = new IdempotentRecord();
        record.setStatus(1);
        record.setBizId(30001L);
        when(idempotentRecordMapper.selectByKey("idem-key")).thenReturn(record);

        ReviewServiceImpl service = new ReviewServiceImpl(
                reviewMapper, reviewImageMapper, idempotentRecordMapper, orderQueryService, snowflakeIdWorker);

        ReviewSubmitResponse response = service.submitReview(new SubmitReviewRequest(), 10001L, "idem-key");

        assertEquals(30001L, response.getReviewId());
        verifyNoInteractions(reviewMapper, reviewImageMapper, orderQueryService, snowflakeIdWorker);
    }

    @Test
    void ownerCanReviewCompletedOrder() {
        when(idempotentRecordMapper.selectByKey("idem-key")).thenReturn(null);
        when(snowflakeIdWorker.nextId()).thenReturn(1L, 30001L);
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(20001L);
        request.setRating(5);
        OrderDTO order = order(10001L, OrderStatus.COMPLETED.getValue());
        when(orderQueryService.getOrder(20001L)).thenReturn(order);

        ReviewServiceImpl service = new ReviewServiceImpl(
                reviewMapper, reviewImageMapper, idempotentRecordMapper, orderQueryService, snowflakeIdWorker);

        ReviewSubmitResponse response = service.submitReview(request, 10001L, "idem-key");

        assertEquals(30001L, response.getReviewId());
        verify(reviewMapper).insert(any());
        verify(idempotentRecordMapper).updateCompleted("idem-key", 30001L);
    }

    @Test
    void nonOwnerCannotReviewOrder() {
        when(idempotentRecordMapper.selectByKey("idem-key")).thenReturn(null);
        when(snowflakeIdWorker.nextId()).thenReturn(1L);
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(20001L);
        request.setRating(5);
        when(orderQueryService.getOrder(20001L)).thenReturn(order(20002L, OrderStatus.COMPLETED.getValue()));

        ReviewServiceImpl service = new ReviewServiceImpl(
                reviewMapper, reviewImageMapper, idempotentRecordMapper, orderQueryService, snowflakeIdWorker);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitReview(request, 10001L, "idem-key"));

        assertEquals(ApiCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void nonCompletedOrderCannotBeReviewed() {
        when(idempotentRecordMapper.selectByKey("idem-key")).thenReturn(null);
        when(snowflakeIdWorker.nextId()).thenReturn(1L);
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(20001L);
        request.setRating(5);
        when(orderQueryService.getOrder(20001L)).thenReturn(order(10001L, OrderStatus.WAITING_SERVICE.getValue()));

        ReviewServiceImpl service = new ReviewServiceImpl(
                reviewMapper, reviewImageMapper, idempotentRecordMapper, orderQueryService, snowflakeIdWorker);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitReview(request, 10001L, "idem-key"));

        assertEquals(ApiCode.REVIEW_ORDER_STATUS_INVALID, ex.getCode());
    }

    private OrderDTO order(Long userId, int status) {
        OrderDTO order = new OrderDTO();
        order.setOrderId(20001L);
        order.setUserId(userId);
        order.setStatus(status);
        order.setServiceItemId(201L);
        return order;
    }
}
