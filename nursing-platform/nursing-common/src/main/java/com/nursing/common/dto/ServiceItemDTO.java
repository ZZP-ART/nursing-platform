package com.nursing.common.dto;

import java.io.Serializable;
import java.util.List;

public class ServiceItemDTO implements Serializable {
    private Long id;
    private String name;
    private Long categoryId;
    private Integer status;
    private List<ServiceSpecDTO> specs;

    public ServiceItemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<ServiceSpecDTO> getSpecs() { return specs; }
    public void setSpecs(List<ServiceSpecDTO> specs) { this.specs = specs; }
}
