package com.nursing.order.repository;

import com.nursing.order.entity.EventMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EventMessageMapper {
    int insert(EventMessage message);

    List<EventMessage> selectPending(@Param("limit") int limit, @Param("now") LocalDateTime now);

    int markSent(@Param("id") Long id);

    int markRetry(@Param("id") Long id,
                  @Param("nextExecuteTime") LocalDateTime nextExecuteTime,
                  @Param("lastError") String lastError);
}
