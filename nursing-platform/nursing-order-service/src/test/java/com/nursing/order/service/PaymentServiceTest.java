package com.nursing.order.service;

import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.entity.PaymentRecord;
import com.nursing.order.event.OrderEventPublisher;
import com.nursing.order.repository.OrderHeaderMapper;
import com.nursing.order.repository.OrderOperationLogMapper;
import com.nursing.order.repository.PaymentRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    private OrderHeaderMapper orderHeaderMapper;
    private PaymentRecordMapper paymentRecordMapper;
    private OrderOperationLogMapper logMapper;
    private OrderEventPublisher eventPublisher;
    private KeyPair keyPair;
    private PaymentService service;

    @BeforeEach
    void setUp() throws Exception {
        orderHeaderMapper = mock(OrderHeaderMapper.class);
        paymentRecordMapper = mock(PaymentRecordMapper.class);
        logMapper = mock(OrderOperationLogMapper.class);
        eventPublisher = mock(OrderEventPublisher.class);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        service = new PaymentService(orderHeaderMapper, paymentRecordMapper, logMapper, eventPublisher,
                new SnowflakeIdWorker(1, 1), false, "http://localhost/callback",
                "app-1", "seller-1", publicKey, new MockEnvironment());
    }

    @Test
    void forgedSignatureIsRejected() {
        Map<String, String> params = callbackParams("150.00");
        params.put("sign", "forged");

        assertThatThrownBy(() -> service.handleAlipayCallback(params))
                .isInstanceOf(BusinessException.class)
                .hasMessage("支付宝验签失败");
        verify(orderHeaderMapper, never()).updateStatusByOrderNo(anyString(), anyInt(), anyInt());
    }

    @Test
    void amountMismatchIsRejected() throws Exception {
        OrderHeader order = pendingOrder();
        when(orderHeaderMapper.selectByOrderNo(order.getOrderNo())).thenReturn(order);
        Map<String, String> params = signedCallbackParams("149.99");

        assertThatThrownBy(() -> service.handleAlipayCallback(params))
                .isInstanceOf(BusinessException.class)
                .hasMessage("支付金额不一致");
        verify(paymentRecordMapper, never()).insert(any());
    }

    @Test
    void duplicateCallbackReturnsSuccessWithoutWritingAgain() throws Exception {
        OrderHeader order = pendingOrder();
        when(orderHeaderMapper.selectByOrderNo(order.getOrderNo())).thenReturn(order);
        when(paymentRecordMapper.selectByOrderNoAndPayType(order.getOrderNo(), 1)).thenReturn(new PaymentRecord());

        String result = service.handleAlipayCallback(signedCallbackParams("150.00"));

        assertThat(result).isEqualTo("success");
        verify(paymentRecordMapper, never()).insert(any());
        verify(orderHeaderMapper, never()).updateStatusByOrderNo(anyString(), anyInt(), anyInt());
    }

    @Test
    void nonPendingOrderCallbackIsRejected() throws Exception {
        OrderHeader order = pendingOrder();
        order.setStatus(1);
        when(orderHeaderMapper.selectByOrderNo(order.getOrderNo())).thenReturn(order);

        assertThatThrownBy(() -> service.handleAlipayCallback(signedCallbackParams("150.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单状态不可支付");
        verify(paymentRecordMapper, never()).insert(any());
    }

    private OrderHeader pendingOrder() {
        OrderHeader order = new OrderHeader();
        order.setId(20001L);
        order.setOrderNo("2026070820001");
        order.setUserId(10001L);
        order.setStatus(0);
        order.setTotalAmount(new BigDecimal("150.00"));
        return order;
    }

    private Map<String, String> signedCallbackParams(String amount) throws Exception {
        Map<String, String> params = callbackParams(amount);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(canonicalContent(params).getBytes(StandardCharsets.UTF_8));
        params.put("sign", Base64.getEncoder().encodeToString(signature.sign()));
        return params;
    }

    private Map<String, String> callbackParams(String amount) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("notify_id", "notify-1");
        params.put("trade_no", "trade-1");
        params.put("out_trade_no", "2026070820001");
        params.put("total_amount", amount);
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("app_id", "app-1");
        params.put("seller_id", "seller-1");
        params.put("sign_type", "RSA2");
        return params;
    }

    private String canonicalContent(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}
