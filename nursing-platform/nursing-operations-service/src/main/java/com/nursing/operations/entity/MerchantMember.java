package com.nursing.operations.entity;

public class MerchantMember {
    private Long merchantId;
    private Long userId;
    private String position;
    private Integer status;

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
