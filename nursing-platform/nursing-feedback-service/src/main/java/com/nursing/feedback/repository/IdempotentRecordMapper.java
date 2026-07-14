package com.nursing.feedback.repository;

import com.nursing.feedback.entity.IdempotentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdempotentRecordMapper {
    int insert(IdempotentRecord record);

    IdempotentRecord selectByScope(@Param("bizType") String bizType,
                                   @Param("subjectId") Long subjectId,
                                   @Param("idempotentKey") String idempotentKey);

    int updateCompleted(@Param("bizType") String bizType,
                        @Param("subjectId") Long subjectId,
                        @Param("idempotentKey") String idempotentKey,
                        @Param("bizId") Long bizId);

    int deleteExpiredByScope(@Param("bizType") String bizType,
                             @Param("subjectId") Long subjectId,
                             @Param("idempotentKey") String idempotentKey,
                             @Param("now") java.time.LocalDateTime now);

    int deleteExpiredBefore(@Param("now") java.time.LocalDateTime now,
                            @Param("limit") int limit);
}
