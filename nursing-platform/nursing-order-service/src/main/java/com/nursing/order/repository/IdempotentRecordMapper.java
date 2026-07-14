package com.nursing.order.repository;

import com.nursing.order.entity.IdempotentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdempotentRecordMapper {
    int insert(IdempotentRecord record);

    IdempotentRecord selectByKey(@Param("idempotentKey") String idempotentKey);

    IdempotentRecord selectByKeyForUpdate(@Param("idempotentKey") String idempotentKey);

    int bindRequest(@Param("idempotentKey") String idempotentKey,
                    @Param("userId") Long userId,
                    @Param("requestFingerprint") String requestFingerprint);

    int complete(@Param("idempotentKey") String idempotentKey,
                 @Param("userId") Long userId,
                 @Param("requestFingerprint") String requestFingerprint,
                 @Param("bizId") Long bizId);
}
