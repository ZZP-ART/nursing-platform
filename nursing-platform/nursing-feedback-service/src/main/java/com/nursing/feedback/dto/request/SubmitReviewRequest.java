package com.nursing.feedback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SubmitReviewRequest {
    @NotNull(message = "orderId不能为空")
    private Long orderId;

    @NotNull(message = "rating不能为空")
    @Min(value = 1, message = "rating最小为1")
    @Max(value = 5, message = "rating最大为5")
    private Integer rating;

    @Size(max = 500, message = "评价内容最多500字符")
    private String content;

    @Size(max = 6, message = "评价图片最多6张")
    private List<String> images;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
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
