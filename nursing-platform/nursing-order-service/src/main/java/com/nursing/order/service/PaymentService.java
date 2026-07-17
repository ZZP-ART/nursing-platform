package com.nursing.order.service;

import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.dto.request.PayRequest;
import com.nursing.order.dto.response.PayResponse;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.entity.OrderOperationLog;
import com.nursing.order.entity.PaymentRecord;
import com.nursing.order.entity.PaymentIntent;
import com.nursing.order.event.OrderEventPublisher;
import com.nursing.order.repository.OrderHeaderMapper;
import com.nursing.order.repository.OrderOperationLogMapper;
import com.nursing.order.repository.PaymentRecordMapper;
import com.nursing.order.repository.PaymentIntentMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.security.MessageDigest;
import java.util.HexFormat;

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
    @Autowired(required = false)
    private PaymentIntentMapper paymentIntentMapper;
    @Autowired(required = false)
    private RefundService refundService;

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
        if (!mockPayment && (!StringUtils.hasText(alipayPublicKey)
                || !StringUtils.hasText(appId)
                || !StringUtils.hasText(sellerId))) {
            throw new IllegalStateException("Alipay app ID, seller ID, and public key are required when mock payment is disabled");
        }
    }

    @Transactional
    public PayResponse initiatePayment(Long userId, Long orderId, PayRequest request) {
        OrderHeader order = requireOwnedOrder(userId, orderId);
        if (!Integer.valueOf(0).equals(order.getStatus())) {
            throw new BusinessException(ORDER_PAY_INVALID, "当前状态不可支付");
        }
        if (mockPayment) {
            completeMockPayment(order);
            return new PayResponse(order.getId(), order.getOrderNo(), request.getPayChannel(),
                    order.getTotalAmount(), "SUCCESS", true, Map.of());
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
        if (mockPayment) {
            throw new BusinessException(SIGN_INVALID, "Mock payment callbacks are disabled");
        }
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
        BigDecimal callbackAmount = parseAmount(params.get("total_amount"));
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(callbackAmount) != 0) {
            throw new BusinessException(ORDER_PAY_INVALID, "支付金额不一致");
        }

        PaymentRecord byNotifyId = paymentRecordMapper.selectByNotifyId(params.get("notify_id"));
        PaymentRecord byTradeNo = paymentRecordMapper.selectByTradeNo(params.get("trade_no"));
        if ((byNotifyId != null && !isConsistentCallback(byNotifyId, order, callbackAmount, params))
                || (byTradeNo != null && !isConsistentCallback(byTradeNo, order, callbackAmount, params))) {
            return rejectCallback(order, "callback identifier conflicts with an existing payment");
        }

        int paid = orderHeaderMapper.updateStatusByOrderNo(orderNo, 0, 1);
        if (paid == 0) {
            PaymentRecord existing = paymentRecordMapper.selectByOrderNoAndPayTypeForUpdate(orderNo, PAY_TYPE_ALIPAY);
            if (existing != null) {
                return isConsistentCallback(existing, order, callbackAmount, params)
                        ? "success" : rejectCallback(order, "callback payload conflicts with the order payment");
            }
            OrderHeader latest = orderHeaderMapper.selectByOrderNoForUpdate(orderNo);
            if (latest == null) {
                throw new BusinessException(ORDER_NOT_FOUND, "订单不存在");
            }
            if (Integer.valueOf(3).equals(latest.getStatus())
                    && orderHeaderMapper.updateStatusByOrderNo(orderNo, 3, 4) == 1) {
                PaymentRecord lateRecord = createPaymentRecord(latest, callbackAmount, params);
                try {
                    paymentRecordMapper.insert(lateRecord);
                } catch (DuplicateKeyException ignored) {
                    return "success";
                }
                savePaymentLog(latest, 3, 4, "late_payment_refund", "支付超时后到账，等待退款处理");
                if (refundService != null) refundService.requestRefund(latest);
                return "success";
            }
            existing = paymentRecordMapper.selectByOrderNoAndPayTypeForUpdate(orderNo, PAY_TYPE_ALIPAY);
            if (existing != null) {
                return isConsistentCallback(existing, order, callbackAmount, params)
                        ? "success" : rejectCallback(order, "callback payload conflicts with the order payment");
            }
            throw new BusinessException(ORDER_PAY_INVALID, "订单状态不可支付");
        }

        PaymentRecord record = createPaymentRecord(order, callbackAmount, params);
        try {
            paymentRecordMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            PaymentRecord existing = paymentRecordMapper.selectByOrderNoAndPayTypeForUpdate(orderNo, PAY_TYPE_ALIPAY);
            return existing != null && isConsistentCallback(existing, order, callbackAmount, params)
                    ? "success" : rejectCallback(order, "callback identifier conflicts with an existing payment");
        }
        savePaymentLog(order, 0, 1, "pay", "支付宝支付成功");
        orderEventPublisher.saveOrderPaidEvent(order, record.getPayTime());
        markIntentPaid(order, record);
        return "success";
    }

    @Transactional
    public PayResponse initiatePayment(Long userId, Long orderId, PayRequest request, String idempotentKey) {
        if (!StringUtils.hasText(idempotentKey) || idempotentKey.length() > 128) {
            throw new BusinessException(ORDER_PAY_INVALID, "Idempotency-Key is required");
        }
        if (paymentIntentMapper == null) return initiatePayment(userId, orderId, request);
        String hash = fingerprint(request.getPayChannel());
        OrderHeader order = requireOwnedOrder(userId, orderId);
        PaymentIntent existing = paymentIntentMapper.selectByUserKey(userId, idempotentKey);
        if (existing != null) {
            if (!Objects.equals(existing.getOrderId(), orderId) || !Objects.equals(existing.getRequestHash(), hash)) {
                throw new BusinessException(com.nursing.common.constant.ApiCode.CONFLICT, "Idempotency-Key was reused with a different payment request");
            }
            return replayIntent(order, request, existing);
        }
        if (paymentIntentMapper.selectByOrderId(orderId) != null) {
            throw new BusinessException(com.nursing.common.constant.ApiCode.CONFLICT, "Order already has a payment intent");
        }
        PaymentIntent intent = new PaymentIntent();
        intent.setId(snowflakeIdWorker.nextId()); intent.setOrderId(orderId); intent.setUserId(userId); intent.setIdempotentKey(idempotentKey);
        intent.setRequestHash(hash); intent.setPayChannel(request.getPayChannel()); intent.setStatus(0); intent.setResponseSnapshot("{}");
        try { paymentIntentMapper.insert(intent); } catch (DuplicateKeyException ex) { return replayIntent(order, request, paymentIntentMapper.selectByUserKey(userId, idempotentKey)); }
        PayResponse response = initiatePayment(userId, orderId, request);
        if (Integer.valueOf(1).equals(order.getStatus())) {
            PaymentRecord record = paymentRecordMapper.selectByOrderId(orderId);
            paymentIntentMapper.markPaid(orderId, record == null ? null : record.getId(), "{}");
        }
        return response;
    }

    private PaymentRecord createPaymentRecord(OrderHeader order, BigDecimal callbackAmount, Map<String, String> params) {
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
        return record;
    }

    private void savePaymentLog(OrderHeader order, int fromStatus, int toStatus, String action, String remark) {
        OrderOperationLog log = new OrderOperationLog();
        log.setId(snowflakeIdWorker.nextId());
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setUserId(order.getUserId());
        log.setOperator("ALIPAY");
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        orderOperationLogMapper.insert(log);
    }

    private boolean isConsistentCallback(PaymentRecord record, OrderHeader order, BigDecimal amount,
                                         Map<String, String> params) {
        return Objects.equals(record.getOrderId(), order.getId())
                && Objects.equals(record.getPayType(), PAY_TYPE_ALIPAY)
                && record.getPayAmount() != null && record.getPayAmount().compareTo(amount) == 0
                && Objects.equals(record.getNotifyId(), params.get("notify_id"))
                && Objects.equals(record.getTradeNo(), params.get("trade_no"));
    }

    private String rejectCallback(OrderHeader order, String reason) {
        // Returning failure commits the security audit while asking the provider not to acknowledge the forged callback.
        savePaymentLog(order, order.getStatus(), order.getStatus(), "payment_callback_conflict", reason);
        return "failure";
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
        verifyRsa2Signature(params);
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

    private void completeMockPayment(OrderHeader order) {
        if (orderHeaderMapper.updateStatusByOrderNo(order.getOrderNo(), 0, 1) != 1) {
            PaymentRecord existing = paymentRecordMapper.selectByOrderNoAndPayTypeForUpdate(
                    order.getOrderNo(), PAY_TYPE_ALIPAY);
            if (existing != null) {
                return;
            }
            throw new BusinessException(ORDER_PAY_INVALID, "当前状态不可支付");
        }

        Map<String, String> params = Map.of(
                "trade_no", "mock-" + order.getOrderNo(),
                "notify_id", "mock-" + order.getOrderNo());
        PaymentRecord record = createPaymentRecord(order, order.getTotalAmount(), params);
        try {
            paymentRecordMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            return;
        }
        savePaymentLog(order, 0, 1, "pay", "Mock payment completed");
        orderEventPublisher.saveOrderPaidEvent(order, record.getPayTime());
        markIntentPaid(order, record);
    }

    private PayResponse replayIntent(OrderHeader order, PayRequest request, PaymentIntent intent) {
        boolean paid = Integer.valueOf(1).equals(intent.getStatus()) || Integer.valueOf(1).equals(order.getStatus());
        Map<String, String> params = new LinkedHashMap<>();
        if (!paid) { params.put("payChannel", request.getPayChannel()); params.put("outTradeNo", order.getOrderNo()); params.put("totalAmount", order.getTotalAmount().toPlainString()); params.put("notifyUrl", notifyUrl); }
        return new PayResponse(order.getId(), order.getOrderNo(), request.getPayChannel(), order.getTotalAmount(), paid ? "SUCCESS" : "READY", mockPayment, params);
    }

    private void markIntentPaid(OrderHeader order, PaymentRecord record) {
        if (paymentIntentMapper == null) return;
        PaymentIntent intent = paymentIntentMapper.selectByOrderId(order.getId());
        if (intent == null) {
            intent = new PaymentIntent(); intent.setId(snowflakeIdWorker.nextId()); intent.setOrderId(order.getId()); intent.setUserId(order.getUserId());
            intent.setIdempotentKey("callback:" + record.getNotifyId()); intent.setRequestHash(fingerprint("callback:" + record.getTradeNo())); intent.setPayChannel("alipay"); intent.setStatus(0); intent.setResponseSnapshot("{}");
            try { paymentIntentMapper.insert(intent); } catch (DuplicateKeyException ignored) { }
        }
        paymentIntentMapper.markPaid(order.getId(), record.getId(), "{}");
    }

    private String fingerprint(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("SHA-256 is unavailable", ex); }
    }
}
