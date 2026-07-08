package com.nursing.order.repository;

import com.nursing.order.entity.OrderOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderOperationLogMapper {
    int insert(OrderOperationLog log);

    List<OrderOperationLog> selectByOrderId(@Param("orderId") Long orderId);
}
