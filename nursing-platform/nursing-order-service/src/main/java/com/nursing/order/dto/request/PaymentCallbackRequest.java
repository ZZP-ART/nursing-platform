package com.nursing.order.dto.request;

import jakarta.validation.constraints.NotBlank;

public class PaymentCallbackRequest {
    @NotBlank(message = "notify_id不能为空")
    private String notifyId;
    @NotBlank(message = "trade_no不能为空")
    private String tradeNo;
    @NotBlank(message = "out_trade_no不能为空")
    private String outTradeNo;
    @NotBlank(message = "total_amount不能为空")
    private String totalAmount;
    @NotBlank(message = "trade_status不能为空")
    private String tradeStatus;
    @NotBlank(message = "sign不能为空")
    private String sign;
    @NotBlank(message = "sign_type不能为空")
    private String signType;

    public String getNotifyId() { return notifyId; }
    public void setNotifyId(String notifyId) { this.notifyId = notifyId; }
    public String getTradeNo() { return tradeNo; }
    public void setTradeNo(String tradeNo) { this.tradeNo = tradeNo; }
    public String getOutTradeNo() { return outTradeNo; }
    public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public String getTradeStatus() { return tradeStatus; }
    public void setTradeStatus(String tradeStatus) { this.tradeStatus = tradeStatus; }
    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }
    public String getSignType() { return signType; }
    public void setSignType(String signType) { this.signType = signType; }
}
