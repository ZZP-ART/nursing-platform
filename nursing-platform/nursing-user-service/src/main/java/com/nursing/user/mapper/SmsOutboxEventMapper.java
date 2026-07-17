package com.nursing.user.mapper;

import com.nursing.user.entity.SmsOutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SmsOutboxEventMapper {
    int insert(SmsOutboxEvent event);

    List<SmsOutboxEvent> selectPending(@Param("limit") int limit, @Param("now") LocalDateTime now);

    List<SmsOutboxEvent> selectStaleProcessing(@Param("before") LocalDateTime before,
                                                @Param("limit") int limit);

    int claim(@Param("id") Long id,
              @Param("leaseOwner") String leaseOwner,
              @Param("leaseExpireTime") LocalDateTime leaseExpireTime,
              @Param("processingTime") LocalDateTime processingTime);

    int reschedule(@Param("id") Long id,
                   @Param("leaseOwner") String leaseOwner,
                   @Param("nextExecuteTime") LocalDateTime nextExecuteTime,
                   @Param("failureReason") String failureReason,
                   @Param("updateTime") LocalDateTime updateTime);

    int markCompleted(@Param("id") Long id,
                      @Param("leaseOwner") String leaseOwner,
                      @Param("updateTime") LocalDateTime updateTime);

    int markUnknown(@Param("id") Long id,
                    @Param("leaseOwner") String leaseOwner,
                    @Param("failureReason") String failureReason,
                    @Param("updateTime") LocalDateTime updateTime);

    int markDeadLetter(@Param("id") Long id,
                       @Param("leaseOwner") String leaseOwner,
                       @Param("failureReason") String failureReason,
                       @Param("updateTime") LocalDateTime updateTime);
}
