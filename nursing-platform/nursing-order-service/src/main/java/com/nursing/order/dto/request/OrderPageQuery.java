package com.nursing.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class OrderPageQuery {
    @Min(value = 0, message = "订单状态不合法")
    @Max(value = 5, message = "订单状态不合法")
    private Integer status;

    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 50, message = "每页条数不能超过50")
    private Integer size = 20;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getPage() { return page == null ? 1 : page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size == null ? 20 : size; }
    public void setSize(Integer size) { this.size = size; }
    public int offset() { return (getPage() - 1) * getSize(); }
}
