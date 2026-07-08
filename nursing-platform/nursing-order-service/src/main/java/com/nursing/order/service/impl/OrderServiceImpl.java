package com.nursing.order.service.impl;

import com.nursing.common.dto.ServiceItemDTO;
import com.nursing.common.dto.ServiceSpecDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.feign.CatalogServiceFeignClient;
import com.nursing.common.result.Result;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.dto.request.OrderCreateRequest;
import com.nursing.order.dto.response.OrderCreateResponse;
import com.nursing.order.entity.IdempotentRecord;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.entity.OrderOperationLog;
import com.nursing.order.entity.UserAddress;
import com.nursing.order.repository.OrderHeaderMapper;
import com.nursing.order.repository.OrderOperationLogMapper;
import com.nursing.order.repository.OrderSequenceMapper;
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

    private final IdempotentService idempotentService;
    private final CatalogServiceFeignClient catalogServiceFeignClient;
    private final UserAddressMapper userAddressMapper;
    private final OrderHeaderMapper orderHeaderMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final OrderSequenceMapper orderSequenceMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public OrderServiceImpl(IdempotentService idempotentService,
                            CatalogServiceFeignClient catalogServiceFeignClient,
                            UserAddressMapper userAddressMapper,
                            OrderHeaderMapper orderHeaderMapper,
                            OrderOperationLogMapper orderOperationLogMapper,
                            OrderSequenceMapper orderSequenceMapper,
                            SnowflakeIdWorker snowflakeIdWorker) {
        this.idempotentService = idempotentService;
        this.catalogServiceFeignClient = catalogServiceFeignClient;
        this.userAddressMapper = userAddressMapper;
        this.orderHeaderMapper = orderHeaderMapper;
        this.orderOperationLogMapper = orderOperationLogMapper;
        this.orderSequenceMapper = orderSequenceMapper;
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
}
