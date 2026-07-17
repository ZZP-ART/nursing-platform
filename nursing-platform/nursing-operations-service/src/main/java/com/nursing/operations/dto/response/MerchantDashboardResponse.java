package com.nursing.operations.dto.response;

import java.math.BigDecimal;

public record MerchantDashboardResponse(Long merchantId, String merchantName, long waitingDispatch,
                                        long waitingAccept, long todayServices, long inService,
                                        long totalOrders, long completed, BigDecimal monthRevenue,
                                        long exceptionCount) { }
