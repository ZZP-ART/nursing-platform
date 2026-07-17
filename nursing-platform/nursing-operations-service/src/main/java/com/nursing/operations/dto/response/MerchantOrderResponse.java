package com.nursing.operations.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MerchantOrderResponse(Long orderId, String orderNo, Integer orderStatus, Integer assignmentStatus,
                                    String serviceItemName, String specName, BigDecimal totalAmount,
                                    LocalDate serviceDate, String serviceTimeSlot, String receiverName,
                                    String receiverPhone, String addressDetail,
                                    AssignmentResponse currentAssignment, List<AssignmentResponse> assignments,
                                    List<ServiceActionResponse> serviceRecords, LocalDateTime createTime) { }
