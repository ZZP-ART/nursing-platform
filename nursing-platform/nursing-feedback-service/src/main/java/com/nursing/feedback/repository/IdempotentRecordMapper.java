package com.nursing.feedback.repository;

import com.nursing.feedback.entity.IdempotentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdempotentRecordMapper {
    int insert(IdempotentRecord record);

    IdempotentRecord selectByKey(@Param("idempotentKey") String idempotentKey);

    int updateCompleted(@Param("idempotentKey") String idempotentKey,
                        @Param("bizId") Long bizId);

    int updateStatus(@Param("idempotentKey") String idempotentKey,
                     @Param("status") Integer status);
}
