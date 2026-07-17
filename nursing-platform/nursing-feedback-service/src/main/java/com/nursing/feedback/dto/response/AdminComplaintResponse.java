package com.nursing.feedback.dto.response;

import com.nursing.feedback.entity.Complaint;
import java.time.LocalDateTime;

public record AdminComplaintResponse(Long id, Long orderId, Long userId, Integer type,
                                     String content, String images, Integer status,
                                     LocalDateTime createTime, LocalDateTime updateTime) {
    public static AdminComplaintResponse from(Complaint complaint) {
        return new AdminComplaintResponse(complaint.getId(), complaint.getOrderId(), complaint.getUserId(),
                complaint.getType(), complaint.getContent(), complaint.getImages(), complaint.getStatus(),
                complaint.getCreateTime(), complaint.getUpdateTime());
    }
}
