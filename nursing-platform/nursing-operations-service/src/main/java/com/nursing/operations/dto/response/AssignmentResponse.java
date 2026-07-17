package com.nursing.operations.dto.response;

import java.time.LocalDateTime;

public record AssignmentResponse(Long assignmentId, Long orderId, Long merchantId, Long caregiverUserId,
                                 Integer status, LocalDateTime acceptedTime, LocalDateTime rejectedTime) { }
