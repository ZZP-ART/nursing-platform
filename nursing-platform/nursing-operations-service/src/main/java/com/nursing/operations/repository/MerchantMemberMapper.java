package com.nursing.operations.repository;

import com.nursing.operations.entity.MerchantMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MerchantMemberMapper {
    MerchantMember selectEnabledByUserId(@Param("userId") Long userId);
    boolean existsActiveCaregiver(@Param("merchantId") Long merchantId, @Param("caregiverUserId") Long caregiverUserId);
    List<Long> selectActiveCaregiverUserIds(@Param("merchantId") Long merchantId);
}
