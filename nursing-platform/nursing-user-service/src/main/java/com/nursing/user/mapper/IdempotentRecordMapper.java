package com.nursing.user.mapper;

import com.nursing.user.entity.IdempotentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdempotentRecordMapper {
    int insert(IdempotentRecord record);

    IdempotentRecord selectByKeyForUpdate(@Param("idempotentKey") String idempotentKey);

    int complete(@Param("idempotentKey") String idempotentKey, @Param("bizId") Long bizId);
}
