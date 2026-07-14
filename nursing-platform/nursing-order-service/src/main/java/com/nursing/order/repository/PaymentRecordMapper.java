package com.nursing.order.repository;

import com.nursing.order.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentRecordMapper {
    int insert(PaymentRecord record);

    PaymentRecord selectByOrderNoAndPayType(@Param("orderNo") String orderNo, @Param("payType") Integer payType);

    PaymentRecord selectByOrderNoAndPayTypeForUpdate(@Param("orderNo") String orderNo,
                                                      @Param("payType") Integer payType);

    PaymentRecord selectByNotifyId(@Param("notifyId") String notifyId);

    PaymentRecord selectByTradeNo(@Param("tradeNo") String tradeNo);

    PaymentRecord selectByOrderId(@Param("orderId") Long orderId);

    int markRefunded(@Param("id") Long id, @Param("refundTime") java.time.LocalDateTime refundTime);

}
