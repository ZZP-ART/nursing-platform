package com.nursing.user.mapper;

import com.nursing.user.entity.SmsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SmsRecordMapper {

    int insert(SmsRecord smsRecord);

    int markSent(@Param("id") Long id,
                 @Param("providerRequestId") String providerRequestId,
                 @Param("updateTime") java.time.LocalDateTime updateTime);

    int markFailed(@Param("id") Long id,
                   @Param("failureReason") String failureReason,
                   @Param("updateTime") java.time.LocalDateTime updateTime);

    int markUnknown(@Param("id") Long id,
                    @Param("failureReason") String failureReason,
                    @Param("updateTime") java.time.LocalDateTime updateTime);

    int markLatestVerified(@Param("phone") String phone,
                           @Param("smsType") String smsType,
                           @Param("verifyTime") java.time.LocalDateTime verifyTime);
}
