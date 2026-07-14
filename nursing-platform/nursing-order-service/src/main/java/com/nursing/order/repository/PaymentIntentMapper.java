package com.nursing.order.repository;
import com.nursing.order.entity.PaymentIntent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper public interface PaymentIntentMapper {
    PaymentIntent selectByUserKey(@Param("userId") Long userId, @Param("key") String key);
    PaymentIntent selectByOrderId(@Param("orderId") Long orderId);
    int insert(PaymentIntent intent);
    int markPaid(@Param("orderId") Long orderId, @Param("paymentRecordId") Long paymentRecordId, @Param("snapshot") String snapshot);
}
