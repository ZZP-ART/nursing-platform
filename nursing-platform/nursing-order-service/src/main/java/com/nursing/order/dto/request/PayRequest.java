package com.nursing.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PayRequest {
    @NotBlank(message = "支付渠道不能为空")
    @Pattern(regexp = "alipay", message = "支付渠道仅支持alipay")
    private String payChannel;

    public String getPayChannel() { return payChannel; }
    public void setPayChannel(String payChannel) { this.payChannel = payChannel; }
}
