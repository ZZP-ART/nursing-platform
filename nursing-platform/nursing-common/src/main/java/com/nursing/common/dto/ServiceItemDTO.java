package com.nursing.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;
import java.util.List;

public class ServiceItemDTO implements Serializable {
    @JsonAlias("itemId")
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Integer status;
    private List<ServiceSpecDTO> specs;

    public ServiceItemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<ServiceSpecDTO> getSpecs() { return specs; }
    public void setSpecs(List<ServiceSpecDTO> specs) { this.specs = specs; }
}
