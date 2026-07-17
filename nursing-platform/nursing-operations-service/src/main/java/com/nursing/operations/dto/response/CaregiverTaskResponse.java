package com.nursing.operations.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CaregiverTaskResponse(Long assignmentId, Long orderId, String orderNo, Long merchantId,
                                    Long caregiverUserId, Integer assignmentStatus, Integer orderStatus,
                                    Long serviceItemId, String serviceItemName, String specName,
                                    LocalDate serviceDate, String serviceTimeSlot, String receiverName,
                                    String receiverPhone, String addressDetail, String remark,
                                    BigDecimal totalAmount, LocalDateTime createTime,
                                    List<ServiceActionResponse> serviceRecords,
                                    List<String> availableActions) { }
