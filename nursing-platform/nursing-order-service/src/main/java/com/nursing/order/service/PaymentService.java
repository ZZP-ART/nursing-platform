package com.nursing.order.service;

import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.dto.request.PayRequest;
import com.nursing.order.dto.response.PayResponse;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.entity.OrderOperationLog;
import com.nursing.order.entity.PaymentRecord;
import com.nursing.order.event.OrderEventPublisher;
import com.nursing.order.repository.OrderHeaderMapper;
import com.nursing.order.repository.OrderOperationLogMapper;
import com.nursing.order.repository.PaymentRecordMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final OrderEventPublisher orderEventPublisher;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final boolean mockPayment;
    private final String notifyUrl;
    private final String appId;
    private final String sellerId;
    private final String alipayPublicKey;
    private final Environment environment;

    public PaymentService(OrderHeaderMapper orderHeaderMapper,
                          PaymentRecordMapper paymentRecordMapper,
                          OrderOperationLogMapper orderOperationLogMapper,
                          OrderEventPublisher orderEventPublisher,
                          SnowflakeIdWorker snowflakeIdWorker,
                          @Value("${nursing.payment.mock:true}") boolean mockPayment,
                          @Value("${nursing.payment.alipay.notify-url:}") String notifyUrl,
                          @Value("${nursing.payment.alipay.app-id:}") String appId,
                          @Value("${nursing.payment.alipay.seller-id:}") String sellerId,
                          @Value("${nursing.payment.alipay.public-key:}") String alipayPublicKey,
                          Environment environment) {
        this.orderHeaderMapper = orderHeaderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.orderOperationLogMapper = orderOperationLogMapper;
        this.orderEventPublisher = orderEventPublisher;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.mockPayment = mockPayment;
        this.notifyUrl = notifyUrl;
        this.appId = appId;
        this.sellerId = sellerId;
        this.alipayPublicKey = alipayPublicKey;
        this.environment = environment;
    }

    @PostConstruct
    void validatePaymentConfig() {
        boolean devOrTest = environment != null && java.util.Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equals(profile) || "test".equals(profile));
        if (mockPayment && !devOrTest) {
            throw new IllegalStateException("Mock payment is only allowed in dev/test profiles");
        }
        if (!mockPayment && !StringUtils.hasText(alipayPublicKey)) {
            throw new IllegalStateException("Alipay public key is required when mock payment is disabled");
        }
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
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success";
        }
        String orderNo = params.get("out_trade_no");
        OrderHeader order = orderHeaderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ORDER_NOT_FOUND, "订单不存在");
        }
        if (paymentRecordMapper.selectByOrderNoAndPayType(orderNo, PAY_TYPE_ALIPAY) != null) {
            return "success";
        }
        if (!Integer.valueOf(0).equals(order.getStatus())) {
            throw new BusinessException(ORDER_PAY_INVALID, "订单状态不可支付");
        }
        BigDecimal callbackAmount = parseAmount(params.get("total_amount"));
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(callbackAmount) != 0) {
            throw new BusinessException(ORDER_PAY_INVALID, "支付金额不一致");
        }

        PaymentRecord record = new PaymentRecord();
        record.setId(snowflakeIdWorker.nextId());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setUserId(order.getUserId());
        record.setPayAmount(callbackAmount);
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
            orderEventPublisher.saveOrderPaidEvent(order, record.getPayTime());
        }
        return "success";
    }

    private void verifyCallback(Map<String, String> params) {
        requireParam(params, "notify_id");
        requireParam(params, "trade_no");
        requireParam(params, "out_trade_no");
        requireParam(params, "total_amount");
        requireParam(params, "trade_status");
        requireParam(params, "app_id");
        if (StringUtils.hasText(sellerId)) {
            requireParam(params, "seller_id");
        }
        requireParam(params, "sign");
        requireParam(params, "sign_type");
        if (!"RSA2".equals(params.get("sign_type"))) {
            throw new BusinessException(SIGN_INVALID, "支付宝验签失败");
        }
        if (StringUtils.hasText(appId) && !appId.equals(params.get("app_id"))) {
            throw new BusinessException(SIGN_INVALID, "支付宝验签失败");
        }
        if (StringUtils.hasText(sellerId) && !sellerId.equals(params.get("seller_id"))) {
            throw new BusinessException(SIGN_INVALID, "支付宝验签失败");
        }
        if (!mockPayment) {
            verifyRsa2Signature(params);
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

    private void verifyRsa2Signature(Map<String, String> params) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(parsePublicKey(alipayPublicKey));
            signature.update(canonicalContent(params).getBytes(StandardCharsets.UTF_8));
            if (!signature.verify(Base64.getDecoder().decode(params.get("sign")))) {
                throw new BusinessException(SIGN_INVALID, "支付宝验签失败");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(SIGN_INVALID, "支付宝验签失败");
        }
    }

    private PublicKey parsePublicKey(String configuredKey) throws Exception {
        String key = configuredKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] bytes = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private String canonicalContent(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
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
