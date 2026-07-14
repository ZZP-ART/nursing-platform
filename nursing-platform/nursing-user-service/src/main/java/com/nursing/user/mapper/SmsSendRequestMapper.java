package com.nursing.user.mapper;

import com.nursing.user.entity.SmsSendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SmsSendRequestMapper {
    int insert(SmsSendRequest request);

    SmsSendRequest selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    SmsSendRequest selectByIdAndIdempotencyKey(@Param("id") Long id,
                                               @Param("idempotencyKey") String idempotencyKey);

    SmsSendRequest selectById(@Param("id") Long id);

    int deleteExpiredTerminalByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey,
                                              @Param("now") java.time.LocalDateTime now);

    int deleteExpiredTerminal(@Param("now") java.time.LocalDateTime now, @Param("limit") int limit);

    int markProcessing(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);

    int resetPending(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);

    int markSent(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);

    int markFailed(@Param("id") Long id,
                   @Param("failureReason") String failureReason,
                   @Param("updateTime") java.time.LocalDateTime updateTime);

    int markUnknown(@Param("id") Long id,
                    @Param("failureReason") String failureReason,
                    @Param("updateTime") java.time.LocalDateTime updateTime);
}
