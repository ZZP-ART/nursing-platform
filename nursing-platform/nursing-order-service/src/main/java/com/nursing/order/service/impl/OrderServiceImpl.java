package com.nursing.order.service.impl;

import com.nursing.common.dto.ServiceItemDTO;
import com.nursing.common.dto.ServiceSpecDTO;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.feign.CatalogServiceFeignClient;
import com.nursing.common.result.PageResult;
import com.nursing.common.result.Result;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.dto.request.CancelOrderRequest;
import com.nursing.order.dto.request.OrderCreateRequest;
import com.nursing.order.dto.request.OrderPageQuery;
import com.nursing.order.dto.response.CancelResponse;
import com.nursing.order.dto.response.OrderCreateResponse;
import com.nursing.order.dto.response.OrderDetailResponse;
import com.nursing.order.dto.response.OrderListResponse;
import com.nursing.order.dto.response.OrderOperationLogResponse;
import com.nursing.order.entity.IdempotentRecord;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.entity.OrderOperationLog;
import com.nursing.order.entity.PaymentRecord;
import com.nursing.order.entity.UserAddress;
import com.nursing.order.repository.OrderHeaderMapper;
import com.nursing.order.repository.OrderOperationLogMapper;
import com.nursing.order.repository.OrderSequenceMapper;
import com.nursing.order.repository.PaymentRecordMapper;
import com.nursing.order.repository.UserAddressMapper;
import com.nursing.order.service.IOrderService;
import com.nursing.order.service.IdempotentService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class OrderServiceImpl implements IOrderService {
    private static final int IDEMPOTENT_INVALID = 3001;
    private static final int SLOT_OCCUPIED = 3002;
    private static final int ITEM_UNAVAILABLE = 3003;
    private static final int SPEC_UNAVAILABLE = 3004;
    private static final int ADDRESS_INVALID = 3006;
    private static final int ORDER_NOT_FOUND = 3007;
    private static final int ORDER_FORBIDDEN = 3008;
    private static final int ORDER_CANCEL_INVALID = 3009;
    private static final int ORDER_PAY_INVALID = 3010;

    private final IdempotentService idempotentService;
    private final CatalogServiceFeignClient catalogServiceFeignClient;
    private final UserAddressMapper userAddressMapper;
    private final OrderHeaderMapper orderHeaderMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final OrderSequenceMapper orderSequenceMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public OrderServiceImpl(IdempotentService idempotentService,
                            CatalogServiceFeignClient catalogServiceFeignClient,
                            UserAddressMapper userAddressMapper,
                            OrderHeaderMapper orderHeaderMapper,
                            OrderOperationLogMapper orderOperationLogMapper,
                            OrderSequenceMapper orderSequenceMapper,
                            PaymentRecordMapper paymentRecordMapper,
                            SnowflakeIdWorker snowflakeIdWorker) {
        this.idempotentService = idempotentService;
        this.catalogServiceFeignClient = catalogServiceFeignClient;
        this.userAddressMapper = userAddressMapper;
        this.orderHeaderMapper = orderHeaderMapper;
        this.orderOperationLogMapper = orderOperationLogMapper;
        this.orderSequenceMapper = orderSequenceMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    @Override
    @Transactional
    public OrderCreateResponse createOrder(Long userId, String idempotentKey, OrderCreateRequest request) {
        IdempotentRecord idempotent = lockUsableToken(idempotentKey);
        if (Integer.valueOf(1).equals(idempotent.getStatus()) && idempotent.getBizId() != null) {
            OrderHeader existing = orderHeaderMapper.selectById(idempotent.getBizId());
            return new OrderCreateResponse(idempotent.getBizId(), existing == null ? null : existing.getOrderNo());
        }

        validateServiceDate(request.getServiceDate());
        UserAddress address = userAddressMapper.selectByIdAndUserId(request.getAddressId(), userId);
        if (address == null) {
            throw new BusinessException(ADDRESS_INVALID, "地址不存在或已被删除");
        }
        if (orderHeaderMapper.countUserServiceSlot(userId, request.getServiceItemId(),
                request.getServiceDate(), request.getServiceTimeSlot()) > 0) {
            throw new BusinessException(SLOT_OCCUPIED, "该时段已被预约");
        }

        ServiceItemDTO item = fetchServiceItem(request.getServiceItemId());
        ServiceSpecDTO spec = findAvailableSpec(item, request.getServiceSpecId());
        OrderHeader order = buildOrder(userId, request, address, item, spec);
        try {
            orderHeaderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            if (e.getMessage() != null && e.getMessage().contains("uk_user_service_slot")) {
                throw new BusinessException(SLOT_OCCUPIED, "该时段已被预约");
            }
            throw e;
        }
        orderOperationLogMapper.insert(buildCreateLog(order));
        idempotentService.complete(idempotentKey, order.getId());
        return new OrderCreateResponse(order.getId(), order.getOrderNo());
    }

    @Override
    public PageResult<OrderListResponse> listOrders(Long userId, OrderPageQuery query) {
        var orders = orderHeaderMapper.selectPage(userId, query.getStatus(), query.offset(), query.getSize())
                .stream()
                .map(this::toListResponse)
                .toList();
        long total = orderHeaderMapper.countPage(userId, query.getStatus());
        return PageResult.of(orders, total, query.getPage(), query.getSize());
    }

    @Override
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        OrderHeader order = requireOwnedOrder(userId, orderId);
        PaymentRecord payment = paymentRecordMapper.selectByOrderId(orderId);
        var logs = orderOperationLogMapper.selectByOrderId(orderId).stream()
                .map(log -> new OrderOperationLogResponse(log.getAction(), log.getFromStatus(),
                        log.getToStatus(), log.getRemark(), log.getCreateTime()))
                .toList();
        return new OrderDetailResponse(order.getId(), order.getOrderNo(), order.getServiceItemName(),
                order.getSpecName(), order.getSpecPrice(), order.getTotalAmount(), order.getStatus(),
                order.getReceiverName(), order.getReceiverPhone(), order.getAddressDetail(),
                order.getServiceDate(), order.getServiceTimeSlot(), payment == null ? null : payment.getPayStatus(),
                logs, order.getCreateTime());
    }

    @Override
    public OrderDTO getInternalOrder(Long orderId) {
        OrderHeader order = orderHeaderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ORDER_NOT_FOUND, "订单不存在");
        }
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setServiceItemId(order.getServiceItemId());
        dto.setServiceItemName(order.getServiceItemName());
        dto.setSpecName(order.getSpecName());
        dto.setTotalAmount(order.getTotalAmount());
        return dto;
    }

    @Override
    @Transactional
    public CancelResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request) {
        OrderHeader order = requireOwnedOrder(userId, orderId);
        if (Integer.valueOf(3).equals(order.getStatus())) {
            return new CancelResponse(order.getId(), order.getStatus(), "NO_REFUND");
        }
        if (!Integer.valueOf(0).equals(order.getStatus()) && !Integer.valueOf(1).equals(order.getStatus())) {
            throw new BusinessException(ORDER_CANCEL_INVALID, "当前状态不可取消");
        }
        String reason = request == null ? null : request.getCancelReason();
        int toStatus = Integer.valueOf(1).equals(order.getStatus()) ? 4 : 3;
        int updated = orderHeaderMapper.updateStatusByIdVersion(order.getId(), order.getStatus(), toStatus,
                order.getVersion(), reason);
        if (updated == 0) {
            throw new BusinessException(ORDER_CANCEL_INVALID, "当前状态不可取消");
        }
        OrderOperationLog log = new OrderOperationLog();
        log.setId(snowflakeIdWorker.nextId());
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setUserId(userId);
        log.setOperator("USER");
        log.setAction(Integer.valueOf(1).equals(order.getStatus()) ? "refund" : "cancel");
        log.setFromStatus(order.getStatus());
        log.setToStatus(toStatus);
        log.setRemark(reason);
        orderOperationLogMapper.insert(log);
        if (Integer.valueOf(1).equals(order.getStatus())) {
            paymentRecordMapper.markRefundedByOrderId(order.getId());
        }
        return new CancelResponse(order.getId(), toStatus, Integer.valueOf(1).equals(order.getStatus()) ? "REFUNDING" : "NO_REFUND");
    }

    @Override
    @Transactional
    public OrderDTO completeOrder(Long userId, Long orderId) {
        OrderHeader order = requireOwnedOrder(userId, orderId);
        if (!Integer.valueOf(1).equals(order.getStatus())) {
            throw new BusinessException(ORDER_PAY_INVALID, "当前状态不可完成");
        }
        int updated = orderHeaderMapper.updateStatusByIdVersion(order.getId(), 1, 2, order.getVersion(), null);
        if (updated == 0) {
            throw new BusinessException(ORDER_PAY_INVALID, "当前状态不可完成");
        }
        OrderOperationLog log = new OrderOperationLog();
        log.setId(snowflakeIdWorker.nextId());
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setUserId(userId);
        log.setOperator("USER");
        log.setAction("complete");
        log.setFromStatus(1);
        log.setToStatus(2);
        log.setRemark("服务完成，可提交评价");
        orderOperationLogMapper.insert(log);
        order.setStatus(2);
        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public int cancelExpiredPendingPaymentOrders(int timeoutMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(Math.max(timeoutMinutes, 1));
        int cancelled = 0;
        for (OrderHeader order : orderHeaderMapper.selectExpiredPendingPayment(deadline, 100)) {
            int updated = orderHeaderMapper.updateStatusByIdVersion(order.getId(), 0, 3, order.getVersion(), "支付超时自动取消");
            if (updated > 0) {
                OrderOperationLog log = new OrderOperationLog();
                log.setId(snowflakeIdWorker.nextId());
                log.setOrderId(order.getId());
                log.setOrderNo(order.getOrderNo());
                log.setUserId(order.getUserId());
                log.setOperator("SYSTEM");
                log.setAction("timeout_cancel");
                log.setFromStatus(0);
                log.setToStatus(3);
                log.setRemark("支付超时自动取消");
                orderOperationLogMapper.insert(log);
                cancelled++;
            }
        }
        return cancelled;
    }

    private IdempotentRecord lockUsableToken(String idempotentKey) {
        if (!StringUtils.hasText(idempotentKey)) {
            throw new BusinessException(IDEMPOTENT_INVALID, "幂等令牌不存在或已过期");
        }
        IdempotentRecord record = idempotentService.selectByKeyForUpdate(idempotentKey);
        if (record == null
                || !IdempotentService.BIZ_TYPE_CREATE_ORDER.equals(record.getBizType())
                || record.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(IDEMPOTENT_INVALID, "幂等令牌不存在或已过期");
        }
        return record;
    }

    private void validateServiceDate(LocalDate serviceDate) {
        if (serviceDate == null || !serviceDate.isAfter(LocalDate.now())) {
            throw new BusinessException(com.nursing.common.constant.ApiCode.PARAM_ERROR, "预约日期不得早于明天");
        }
    }

    private ServiceItemDTO fetchServiceItem(Long serviceItemId) {
        Result<ServiceItemDTO> result = catalogServiceFeignClient.getItemDetail(serviceItemId);
        if (result == null || result.getCode() != 0 || result.getData() == null) {
            throw new BusinessException(ITEM_UNAVAILABLE, "服务项目已下架");
        }
        ServiceItemDTO item = result.getData();
        if (!Integer.valueOf(1).equals(item.getStatus())) {
            throw new BusinessException(ITEM_UNAVAILABLE, "服务项目已下架");
        }
        return item;
    }

    private ServiceSpecDTO findAvailableSpec(ServiceItemDTO item, Long serviceSpecId) {
        if (item.getSpecs() == null) {
            throw new BusinessException(SPEC_UNAVAILABLE, "规格已下架");
        }
        return item.getSpecs().stream()
                .filter(spec -> Objects.equals(spec.getId(), serviceSpecId))
                .filter(spec -> Integer.valueOf(1).equals(spec.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(SPEC_UNAVAILABLE, "规格已下架"));
    }

    private OrderHeader buildOrder(Long userId, OrderCreateRequest request, UserAddress address,
                                   ServiceItemDTO item, ServiceSpecDTO spec) {
        OrderHeader order = new OrderHeader();
        order.setId(snowflakeIdWorker.nextId());
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setSource(0);
        order.setVersion(0);
        order.setServiceItemId(request.getServiceItemId());
        order.setServiceSpecId(request.getServiceSpecId());
        order.setServiceItemName(item.getName());
        order.setSpecName(spec.getName());
        order.setSpecPrice(spec.getPrice());
        order.setSpecDuration(spec.getDuration());
        order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setAddressDetail(joinAddress(address));
        order.setServiceDate(request.getServiceDate());
        order.setServiceTimeSlot(request.getServiceTimeSlot());
        order.setTotalAmount(spec.getPrice());
        order.setStatus(0);
        order.setRemark(request.getRemark());
        order.setIsDeleted(0);
        return order;
    }

    private String nextOrderNo() {
        orderSequenceMapper.nextSequence();
        Long sequence = orderSequenceMapper.lastInsertId();
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + sequence;
    }

    private String joinAddress(UserAddress address) {
        return String.join("", nullToEmpty(address.getProvince()), nullToEmpty(address.getCity()),
                nullToEmpty(address.getDistrict()), nullToEmpty(address.getDetailAddress()));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private OrderOperationLog buildCreateLog(OrderHeader order) {
        OrderOperationLog log = new OrderOperationLog();
        log.setId(snowflakeIdWorker.nextId());
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setUserId(order.getUserId());
        log.setOperator("USER");
        log.setAction("create");
        log.setToStatus(0);
        log.setRemark("用户创建订单");
        return log;
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

    private OrderDTO toOrderDTO(OrderHeader order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setServiceItemId(order.getServiceItemId());
        dto.setServiceItemName(order.getServiceItemName());
        dto.setSpecName(order.getSpecName());
        dto.setTotalAmount(order.getTotalAmount());
        return dto;
    }

    private OrderListResponse toListResponse(OrderHeader order) {
        return new OrderListResponse(order.getId(), order.getOrderNo(), order.getServiceItemName(),
                order.getSpecName(), order.getSpecPrice(), order.getTotalAmount(), order.getStatus(),
                order.getServiceDate(), order.getServiceTimeSlot(), order.getReceiverName(),
                order.getReceiverPhone(), order.getAddressDetail(), order.getCreateTime());
    }
}
