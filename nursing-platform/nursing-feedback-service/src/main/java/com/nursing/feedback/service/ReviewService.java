package com.nursing.feedback.service;

import com.nursing.common.result.PageResult;
import com.nursing.feedback.dto.request.SubmitReviewRequest;
import com.nursing.feedback.dto.response.ReviewSubmitResponse;
import com.nursing.feedback.dto.response.ReviewVO;

public interface ReviewService {
    ReviewSubmitResponse submitReview(SubmitReviewRequest request, Long userId, String idempotentKey);

    PageResult<ReviewVO> pageReviews(Long itemId, int page, int size);
}
