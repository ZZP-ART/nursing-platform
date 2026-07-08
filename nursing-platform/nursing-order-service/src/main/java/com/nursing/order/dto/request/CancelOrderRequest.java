package com.nursing.order.dto.request;

import jakarta.validation.constraints.Size;

public class CancelOrderRequest {
    @Size(max = 200, message = "取消原因最多200个字符")
    private String cancelReason;

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
