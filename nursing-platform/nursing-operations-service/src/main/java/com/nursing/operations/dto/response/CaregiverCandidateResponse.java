package com.nursing.operations.dto.response;

import java.util.List;

public record CaregiverCandidateResponse(Long caregiverUserId, Long caregiverId, String name, Double rating,
                                         Integer completedOrders, Double distanceKm, Integer dailyTaskCount,
                                         Integer maxDailyOrders, List<String> serviceAreas, List<String> skills,
                                         boolean eligible, List<String> conflictReasons) { }
