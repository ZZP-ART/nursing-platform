package com.nursing.order.dto.response;

import java.time.LocalDateTime;

public record OrderOperationLogResponse(
        String action,
        Integer fromStatus,
        Integer toStatus,
        String remark,
        LocalDateTime createTime) {
}
