package com.nursing.feedback.repository;

import com.nursing.feedback.entity.ReviewEligibility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewEligibilityMapper {
    int insert(ReviewEligibility eligibility);

    ReviewEligibility selectByOrderIdAndUserId(@Param("orderId") Long orderId,
                                                @Param("userId") Long userId);
}
