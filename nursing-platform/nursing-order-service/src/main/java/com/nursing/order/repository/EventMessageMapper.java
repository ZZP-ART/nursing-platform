package com.nursing.order.repository;

import com.nursing.order.entity.EventMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventMessageMapper {
    int insert(EventMessage message);

    List<EventMessage> selectPending(@Param("limit") int limit);

    int markSent(@Param("id") Long id);

    int markFailed(@Param("id") Long id);
}
