package com.nursing.operations.repository;

import com.nursing.operations.entity.ServiceAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.nursing.operations.entity.ServiceAction;

@Mapper
public interface ServiceAssignmentMapper {
    int insert(ServiceAssignment assignment);
    ServiceAssignment selectById(@Param("id") Long id);
    ServiceAssignment selectActiveByOrderId(@Param("orderId") Long orderId);
    ServiceAssignment selectActiveByOrderIdAndMerchantId(@Param("orderId") Long orderId, @Param("merchantId") Long merchantId);
    int accept(@Param("id") Long id, @Param("caregiverUserId") Long caregiverUserId);
    int reject(@Param("id") Long id, @Param("caregiverUserId") Long caregiverUserId, @Param("remark") String remark);
    int countActions(@Param("assignmentId") Long assignmentId);
    int insertAction(@Param("id") Long id, @Param("assignmentId") Long assignmentId, @Param("action") String action,
                     @Param("operatorUserId") Long operatorUserId, @Param("remark") String remark);
    boolean hasAction(@Param("assignmentId") Long assignmentId, @Param("action") String action);
    List<ServiceAction> selectActionsByAssignmentId(@Param("assignmentId") Long assignmentId);
    List<ServiceAssignment> selectByMerchantId(@Param("merchantId") Long merchantId);
    List<ServiceAssignment> selectByCaregiverUserId(@Param("caregiverUserId") Long caregiverUserId);
}
