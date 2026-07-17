package com.nursing.order.dto.request;

import jakarta.validation.constraints.NotNull;

public class InternalOrderTransitionRequest {
    @NotNull private Integer fromStatus;
    @NotNull private Integer toStatus;
    private String reason;
    public Integer getFromStatus(){return fromStatus;} public void setFromStatus(Integer v){fromStatus=v;}
    public Integer getToStatus(){return toStatus;} public void setToStatus(Integer v){toStatus=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
}
