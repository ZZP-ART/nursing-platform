package com.nursing.feedback.dto.response;

public class ReviewSubmitResponse {
    private Long reviewId;

    public ReviewSubmitResponse() {
    }

    public ReviewSubmitResponse(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }
}
