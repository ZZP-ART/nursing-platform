package com.nursing.operations.repository;

import com.nursing.operations.entity.CaregiverProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CaregiverProfileMapper {
    CaregiverProfile selectByUserId(@Param("userId") Long userId);
}
