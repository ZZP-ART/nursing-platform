package com.nursing.order.service.impl;

import com.nursing.common.dto.ServiceItemDTO;
import com.nursing.common.dto.ServiceSpecDTO;
import com.nursing.common.feign.CatalogServiceFeignClient;
import com.nursing.common.result.Result;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.dto.request.OrderCreateRequest;
import com.nursing.order.dto.response.OrderCreateResponse;
import com.nursing.order.entity.IdempotentRecord;
import com.nursing.order.entity.UserAddress;
import com.nursing.order.repository.OrderHeaderMapper;
import com.nursing.order.repository.OrderOperationLogMapper;
import com.nursing.order.repository.OrderSequenceMapper;
import com.nursing.order.repository.PaymentRecordMapper;
import com.nursing.order.repository.UserAddressMapper;
import com.nursing.order.service.IdempotentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {
    @Test
    void createOrderMatchesCatalogItem201Spec301() {
        IdempotentService idempotentService = mock(IdempotentService.class);
        CatalogServiceFeignClient catalogClient = mock(CatalogServiceFeignClient.class);
        UserAddressMapper addressMapper = mock(UserAddressMapper.class);
        OrderHeaderMapper orderMapper = mock(OrderHeaderMapper.class);
        OrderOperationLogMapper logMapper = mock(OrderOperationLogMapper.class);
        OrderSequenceMapper sequenceMapper = mock(OrderSequenceMapper.class);
        PaymentRecordMapper paymentMapper = mock(PaymentRecordMapper.class);
        SnowflakeIdWorker snowflake = mock(SnowflakeIdWorker.class);

        IdempotentRecord token = new IdempotentRecord();
        token.setStatus(0);
        token.setBizType(IdempotentService.BIZ_TYPE_CREATE_ORDER);
        token.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(idempotentService.selectByKeyForUpdate("pt-ok")).thenReturn(token);
        when(addressMapper.selectByIdAndUserId(501L, 10001L)).thenReturn(address());
        when(orderMapper.countUserServiceSlot(any(), any(), any(), any())).thenReturn(0);
        when(catalogClient.getItemDetail(201L)).thenReturn(Result.success(catalogItem()));
        when(sequenceMapper.lastInsertId()).thenReturn(1L);
        when(snowflake.nextId()).thenReturn(90001L, 90002L);

        OrderServiceImpl service = new OrderServiceImpl(idempotentService, catalogClient, addressMapper,
                orderMapper, logMapper, sequenceMapper, paymentMapper, snowflake);

        OrderCreateResponse response = service.createOrder(10001L, "pt-ok", request());

        assertThat(response.orderId()).isEqualTo(90001L);
        ArgumentCaptor<com.nursing.order.entity.OrderHeader> orderCaptor =
                ArgumentCaptor.forClass(com.nursing.order.entity.OrderHeader.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getCategoryName()).isEqualTo("康复护理");
        assertThat(orderCaptor.getValue().getQuantity()).isEqualTo(1);
        assertThat(orderCaptor.getValue().getCatalogSnapshotVersion()).isEqualTo(1);
        verify(logMapper).insert(any());
        verify(idempotentService).complete("pt-ok", 90001L);
    }

    private OrderCreateRequest request() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setServiceItemId(201L);
        request.setServiceSpecId(301L);
        request.setAddressId(501L);
        request.setServiceDate(LocalDate.now().plusDays(1));
        request.setServiceTimeSlot("MORNING");
        return request;
    }

    private UserAddress address() {
        UserAddress address = new UserAddress();
        address.setId(501L);
        address.setReceiverName("张三");
        address.setReceiverPhone("13812345678");
        address.setProvince("北京市");
        address.setCity("北京市");
        address.setDistrict("朝阳区");
        address.setDetailAddress("建国路88号");
        return address;
    }

    private ServiceItemDTO catalogItem() {
        ServiceSpecDTO spec = new ServiceSpecDTO();
        spec.setId(301L);
        spec.setName("单次服务");
        spec.setPrice(new BigDecimal("150.00"));
        spec.setDuration(60);
        spec.setStatus(1);
        ServiceItemDTO item = new ServiceItemDTO();
        item.setId(201L);
        item.setCategoryId(101L);
        item.setCategoryName("康复护理");
        item.setName("上门护理");
        item.setStatus(1);
        item.setSpecs(List.of(spec));
        return item;
    }
}
