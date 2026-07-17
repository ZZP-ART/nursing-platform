package com.nursing.operations.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nursing.common.constant.OrderStatus;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.result.Result;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.operations.dto.request.DispatchRequest;
import com.nursing.operations.entity.MerchantMember;
import com.nursing.operations.feign.OrderServiceFeignClient;
import com.nursing.operations.repository.MerchantMemberMapper;
import com.nursing.operations.repository.OperationIdempotencyMapper;
import com.nursing.operations.repository.ServiceAssignmentMapper;
import org.junit.jupiter.api.Test;

class ServiceAssignmentServiceTest {
    @Test
    void dispatchRejectsCaregiverOutsideMerchantRelationship() {
        MerchantMemberMapper members = mock(MerchantMemberMapper.class);
        ServiceAssignmentMapper assignments = mock(ServiceAssignmentMapper.class);
        OperationIdempotencyMapper idempotencies = mock(OperationIdempotencyMapper.class);
        OrderServiceFeignClient orders = mock(OrderServiceFeignClient.class);
        SnowflakeIdWorker ids = mock(SnowflakeIdWorker.class);
        MerchantMember member = new MerchantMember();
        member.setMerchantId(20001L);
        member.setUserId(10003L);
        when(members.selectEnabledByUserId(10003L)).thenReturn(member);
        when(members.existsActiveCaregiver(20001L, 10002L)).thenReturn(false);
        ServiceAssignmentService service = new ServiceAssignmentService(members, assignments, idempotencies, orders, ids, "internal");
        DispatchRequest request = new DispatchRequest();
        request.setCaregiverUserId(10002L);

        assertThatThrownBy(() -> service.dispatch(10003L, 90001L, request, "dispatch-1"))
                .isInstanceOf(com.nursing.common.exception.BusinessException.class);
    }

    @Test
    void dispatchTransitionsOnlyPendingDispatchOrder() {
        MerchantMemberMapper members = mock(MerchantMemberMapper.class);
        ServiceAssignmentMapper assignments = mock(ServiceAssignmentMapper.class);
        OperationIdempotencyMapper idempotencies = mock(OperationIdempotencyMapper.class);
        OrderServiceFeignClient orders = mock(OrderServiceFeignClient.class);
        SnowflakeIdWorker ids = mock(SnowflakeIdWorker.class);
        MerchantMember member = new MerchantMember();
        member.setMerchantId(20001L);
        when(members.selectEnabledByUserId(10003L)).thenReturn(member);
        when(members.existsActiveCaregiver(20001L, 10002L)).thenReturn(true);
        OrderDTO order = new OrderDTO();
        order.setStatus(OrderStatus.PENDING_DISPATCH.getValue());
        order.setMerchantId(20001L);
        when(orders.getOrder(90001L, "internal")).thenReturn(Result.success(order));
        when(ids.nextId()).thenReturn(80001L, 70001L);
        when(idempotencies.complete(anyLong(), anyLong())).thenReturn(1);
        when(orders.transition(eq(90001L), any(), eq("internal"))).thenReturn(Result.success(order));
        com.nursing.operations.entity.ServiceAssignment assignment = new com.nursing.operations.entity.ServiceAssignment();
        assignment.setId(70001L); assignment.setOrderId(90001L); assignment.setMerchantId(20001L); assignment.setCaregiverUserId(10002L); assignment.setStatus(0);
        when(assignments.selectById(70001L)).thenReturn(assignment);
        ServiceAssignmentService service = new ServiceAssignmentService(members, assignments, idempotencies, orders, ids, "internal");
        DispatchRequest request = new DispatchRequest(); request.setCaregiverUserId(10002L);

        service.dispatch(10003L, 90001L, request, "dispatch-2");

        verify(assignments).insert(any());
        verify(idempotencies).insert(any());
        verify(idempotencies).complete(eq(80001L), eq(70001L));
        verify(orders).transition(eq(90001L), any(), eq("internal"));
    }

    @Test
    void dispatchRejectsOrderOwnedByAnotherMerchant() {
        MerchantMemberMapper members = mock(MerchantMemberMapper.class);
        ServiceAssignmentMapper assignments = mock(ServiceAssignmentMapper.class);
        OperationIdempotencyMapper idempotencies = mock(OperationIdempotencyMapper.class);
        OrderServiceFeignClient orders = mock(OrderServiceFeignClient.class);
        SnowflakeIdWorker ids = mock(SnowflakeIdWorker.class);
        MerchantMember member = new MerchantMember();
        member.setMerchantId(20001L);
        when(members.selectEnabledByUserId(10003L)).thenReturn(member);
        when(members.existsActiveCaregiver(20001L, 10002L)).thenReturn(true);
        OrderDTO order = new OrderDTO();
        order.setMerchantId(20002L);
        order.setStatus(OrderStatus.PENDING_DISPATCH.getValue());
        when(orders.getOrder(90001L, "internal")).thenReturn(Result.success(order));
        when(ids.nextId()).thenReturn(80001L);
        ServiceAssignmentService service = new ServiceAssignmentService(members, assignments, idempotencies, orders, ids, "internal");
        DispatchRequest request = new DispatchRequest();
        request.setCaregiverUserId(10002L);

        assertThatThrownBy(() -> service.dispatch(10003L, 90001L, request, "dispatch-other-merchant"))
                .isInstanceOf(com.nursing.common.exception.BusinessException.class);
    }
}
