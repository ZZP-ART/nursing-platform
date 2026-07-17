package com.nursing.feedback.dto.request;

import jakarta.validation.constraints.NotNull;

public class ReviewModerationRequest {
    @NotNull
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
