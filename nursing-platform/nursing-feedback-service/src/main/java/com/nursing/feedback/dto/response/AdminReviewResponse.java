package com.nursing.feedback.dto.response;

import com.nursing.feedback.entity.Review;
import java.time.LocalDateTime;

public record AdminReviewResponse(Long id, Long orderId, Long userId, Long serviceItemId,
                                  String serviceItemName, String specName, Integer rating,
                                  String content, Integer status, LocalDateTime createTime,
                                  LocalDateTime updateTime) {
    public static AdminReviewResponse from(Review review) {
        return new AdminReviewResponse(review.getId(), review.getOrderId(), review.getUserId(), review.getServiceItemId(),
                review.getServiceItemName(), review.getSpecName(), review.getRating(), review.getContent(),
                review.getStatus(), review.getCreateTime(), review.getUpdateTime());
    }
}
