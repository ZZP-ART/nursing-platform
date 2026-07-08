package com.nursing.feedback.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
}
