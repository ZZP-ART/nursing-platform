package com.nursing.order.service;

import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.dto.request.PayRequest;
import com.nursing.order.dto.response.PayResponse;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.entity.OrderOperationLog;
import com.nursing.order.entity.PaymentRecord;
import com.nursing.order.repository.OrderHeaderMapper;
import com.nursing.order.repository.OrderOperationLogMapper;
import com.nursing.order.repository.PaymentRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class PaymentService {
    private static final int ORDER_NOT_FOUND = 3007;
    private static final int ORDER_FORBIDDEN = 3008;
    private static final int ORDER_PAY_INVALID = 3010;
    private static final int SIGN_INVALID = 3012;
    private static final int PAY_TYPE_ALIPAY = 1;

    private final OrderHeaderMapper orderHeaderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final boolean mockPayment;
    private final String notifyUrl;

    public PaymentService(OrderHeaderMapper orderHeaderMapper,
                          PaymentRecordMapper paymentRecordMapper,
                          OrderOperationLogMapper orderOperationLogMapper,
                          SnowflakeIdWorker snowflakeIdWorker,
                          @Value("${nursing.payment.mock:true}") boolean mockPayment,
                          @Value("${nursing.payment.alipay.notify-url:}") String notifyUrl) {
        this.orderHeaderMapper = orderHeaderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.orderOperationLogMapper = orderOperationLogMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.mockPayment = mockPayment;
        this.notifyUrl = notifyUrl;
    }

    public PayResponse initiatePayment(Long userId, Long orderId, PayRequest request) {
        OrderHeader order = requireOwnedOrder(userId, orderId);
        if (!Integer.valueOf(0).equals(order.getStatus())) {
            throw new BusinessException(ORDER_PAY_INVALID, "当前状态不可支付");
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("payChannel", request.getPayChannel());
        params.put("outTradeNo", order.getOrderNo());
        params.put("totalAmount", order.getTotalAmount().toPlainString());
        params.put("notifyUrl", notifyUrl);
        params.put("mockTradeStatus", "TRADE_SUCCESS");
        return new PayResponse(order.getId(), order.getOrderNo(), request.getPayChannel(),
                order.getTotalAmount(), "READY", mockPayment, params);
    }

    @Transactional
    public String handleAlipayCallback(Map<String, String> params) {
        verifyCallback(params);
        if (!"TRADE_SUCCESS".equals(params.get("trade_status"))) {
            return "success";
        }
        String orderNo = params.get("out_trade_no");
        OrderHeader order = orderHeaderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return "success";
        }
        if (paymentRecordMapper.selectByOrderNoAndPayType(orderNo, PAY_TYPE_ALIPAY) != null) {
            return "success";
        }

        PaymentRecord record = new PaymentRecord();
        record.setId(snowflakeIdWorker.nextId());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setUserId(order.getUserId());
        record.setPayAmount(parseAmount(params.get("total_amount")));
        record.setPayType(PAY_TYPE_ALIPAY);
        record.setPayStatus(1);
        record.setTradeNo(params.get("trade_no"));
        record.setNotifyId(params.get("notify_id"));
        record.setPayTime(LocalDateTime.now());
        record.setVersion(0);
        record.setIsDeleted(0);
        try {
            paymentRecordMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            return "success";
        }

        int updated = orderHeaderMapper.updateStatusByOrderNo(orderNo, 0, 1);
        if (updated > 0) {
            OrderOperationLog log = new OrderOperationLog();
            log.setId(snowflakeIdWorker.nextId());
            log.setOrderId(order.getId());
            log.setOrderNo(order.getOrderNo());
            log.setUserId(order.getUserId());
            log.setOperator("ALIPAY");
            log.setAction("pay");
            log.setFromStatus(0);
            log.setToStatus(1);
            log.setRemark("支付宝支付成功");
            orderOperationLogMapper.insert(log);
        }
        return "success";
    }

    private void verifyCallback(Map<String, String> params) {
        requireParam(params, "notify_id");
        requireParam(params, "trade_no");
        requireParam(params, "out_trade_no");
        requireParam(params, "total_amount");
        requireParam(params, "trade_status");
        requireParam(params, "sign");
        requireParam(params, "sign_type");
        if (!"RSA2".equals(params.get("sign_type"))) {
            throw new BusinessException(SIGN_INVALID, "支付宝验签失败");
        }
        if (!mockPayment) {
            throw new BusinessException(SIGN_INVALID, "支付宝验签失败");
        }
    }

    private void requireParam(Map<String, String> params, String name) {
        if (params.get(name) == null || params.get(name).isBlank()) {
            throw new BusinessException(SIGN_INVALID, "支付宝验签失败");
        }
    }

    private BigDecimal parseAmount(String amount) {
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            throw new BusinessException(SIGN_INVALID, "支付宝验签失败");
        }
    }

    private OrderHeader requireOwnedOrder(Long userId, Long orderId) {
        OrderHeader order = orderHeaderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ORDER_NOT_FOUND, "订单不存在");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ORDER_FORBIDDEN, "无权操作此订单");
        }
        return order;
    }
}
