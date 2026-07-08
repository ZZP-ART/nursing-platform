package com.nursing.feedback.repository;

import com.nursing.feedback.entity.EventMessage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EventMessageMapper {
    int insert(EventMessage eventMessage);

    List<EventMessage> selectPendingEvents(@Param("limit") int limit);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("retryCount") Integer retryCount);
}
