package com.nursing.operations.repository;

import com.nursing.operations.entity.CaregiverApplication;
import com.nursing.operations.dto.request.CaregiverApplicationRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CaregiverApplicationMapper {
    int insert(CaregiverApplication application);
    CaregiverApplication selectByUserId(@Param("userId") Long userId);
    List<CaregiverApplication> selectPending();
    int approve(@Param("userId") Long userId, @Param("remark") String remark);
    int reject(@Param("userId") Long userId, @Param("remark") String remark);
    int resubmitRejected(@Param("userId") Long userId, @Param("request") CaregiverApplicationRequest request);
}
