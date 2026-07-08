package com.nursing.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class ItemPriceDTO implements Serializable {
    private Long specId;
    private String specName;
    private BigDecimal price;
    private Integer status;

    public ItemPriceDTO() {}

    public Long getSpecId() { return specId; }
    public void setSpecId(Long specId) { this.specId = specId; }
    public String getSpecName() { return specName; }
    public void setSpecName(String specName) { this.specName = specName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
