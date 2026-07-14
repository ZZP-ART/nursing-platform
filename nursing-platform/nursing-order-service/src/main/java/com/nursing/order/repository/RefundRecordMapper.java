package com.nursing.order.repository;
import com.nursing.order.entity.RefundRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper public interface RefundRecordMapper {
    RefundRecord selectByOrderId(@Param("orderId") Long orderId);
    int insert(RefundRecord record);
    List<RefundRecord> selectPending(@Param("limit") int limit, @Param("now") LocalDateTime now);
    int claim(@Param("id") Long id, @Param("owner") String owner, @Param("until") LocalDateTime until, @Param("now") LocalDateTime now);
    int markSucceeded(@Param("id") Long id, @Param("owner") String owner, @Param("providerRefundNo") String providerRefundNo);
    int retry(@Param("id") Long id, @Param("owner") String owner, @Param("next") LocalDateTime next, @Param("error") String error);
    int markManual(@Param("id") Long id, @Param("owner") String owner, @Param("error") String error);
}
