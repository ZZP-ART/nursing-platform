package com.nursing.order.repository;

import com.nursing.order.entity.OrderHeader;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderHeaderMapper {
    int insert(OrderHeader order);

    OrderHeader selectById(@Param("id") Long id);

    OrderHeader selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    OrderHeader selectByOrderNo(@Param("orderNo") String orderNo);

    List<OrderHeader> selectPage(@Param("userId") Long userId,
                                 @Param("status") Integer status,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    long countPage(@Param("userId") Long userId, @Param("status") Integer status);

    int countUserServiceSlot(@Param("userId") Long userId,
                             @Param("serviceItemId") Long serviceItemId,
                             @Param("serviceDate") java.time.LocalDate serviceDate,
                             @Param("serviceTimeSlot") String serviceTimeSlot);

    int updateStatusByIdVersion(@Param("id") Long id,
                                @Param("fromStatus") Integer fromStatus,
                                @Param("toStatus") Integer toStatus,
                                @Param("version") Integer version,
                                @Param("cancelReason") String cancelReason);

    int updateStatusByOrderNo(@Param("orderNo") String orderNo,
                              @Param("fromStatus") Integer fromStatus,
                              @Param("toStatus") Integer toStatus);
}
