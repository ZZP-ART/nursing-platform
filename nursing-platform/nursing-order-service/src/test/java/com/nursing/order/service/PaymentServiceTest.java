package com.nursing.order.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.entity.PaymentRecord;
import com.nursing.order.repository.OrderHeaderMapper;
import com.nursing.order.repository.OrderOperationLogMapper;
import com.nursing.order.repository.PaymentRecordMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Test
    void duplicateCallbackReturnsSuccessWithoutWritingAgain() {
        OrderHeaderMapper orderHeaderMapper = mock(OrderHeaderMapper.class);
        PaymentRecordMapper paymentRecordMapper = mock(PaymentRecordMapper.class);
        OrderOperationLogMapper logMapper = mock(OrderOperationLogMapper.class);
        PaymentService service = new PaymentService(orderHeaderMapper, paymentRecordMapper, logMapper,
                mock(com.nursing.order.event.OrderEventPublisher.class), new SnowflakeIdWorker(1, 1),
                true, "http://localhost/callback");
        OrderHeader order = new OrderHeader();
        order.setId(20001L);
        order.setOrderNo("2026070820001");
        order.setUserId(10001L);
        when(orderHeaderMapper.selectByOrderNo("2026070820001")).thenReturn(order);
        when(paymentRecordMapper.selectByOrderNoAndPayType("2026070820001", 1)).thenReturn(new PaymentRecord());

        String result = service.handleAlipayCallback(Map.of(
                "notify_id", "notify-1",
                "trade_no", "trade-1",
                "out_trade_no", "2026070820001",
                "total_amount", "150.00",
                "trade_status", "TRADE_SUCCESS",
                "sign", "mock-sign",
                "sign_type", "RSA2"));

        assertThat(result).isEqualTo("success");
        verify(paymentRecordMapper, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(orderHeaderMapper, never()).updateStatusByOrderNo(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }
}
