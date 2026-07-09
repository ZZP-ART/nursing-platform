package com.nursing.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;
import java.math.BigDecimal;

public class ServiceSpecDTO implements Serializable {
    @JsonAlias("specId")
    private Long id;
    private Long serviceItemId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer duration;
    private Integer status;

    public ServiceSpecDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(Long serviceItemId) { this.serviceItemId = serviceItemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
