package com.nursing.feedback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SubmitComplaintRequest {
    @NotNull(message = "orderId不能为空")
    private Long orderId;

    @NotNull(message = "type不能为空")
    @Min(value = 1, message = "投诉类型无效")
    @Max(value = 4, message = "投诉类型无效")
    private Integer type;

    @NotBlank(message = "投诉内容不能为空")
    @Size(max = 1000, message = "投诉内容最多1000字符")
    private String content;

    @Size(max = 6, message = "投诉图片最多6张")
    private List<String> images;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
