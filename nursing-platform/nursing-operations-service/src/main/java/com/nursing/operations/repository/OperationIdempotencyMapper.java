package com.nursing.operations.repository;

import com.nursing.operations.entity.OperationIdempotency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperationIdempotencyMapper {
    OperationIdempotency select(@Param("operation") String operation, @Param("actorUserId") Long actorUserId,
                                @Param("idempotentKey") String idempotentKey);
    int insert(OperationIdempotency record);
    int complete(@Param("id") Long id, @Param("assignmentId") Long assignmentId);
}
