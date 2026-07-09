package com.nursing.feedback.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.constant.OrderStatus;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.dto.request.SubmitComplaintRequest;
import com.nursing.feedback.entity.Complaint;
import com.nursing.feedback.integration.OrderQueryService;
import com.nursing.feedback.repository.ComplaintMapper;
import com.nursing.feedback.repository.ComplaintTrackMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceImplTest {
    @Mock
    private ComplaintMapper complaintMapper;
    @Mock
    private ComplaintTrackMapper complaintTrackMapper;
    @Mock
    private OrderQueryService orderQueryService;
    @Mock
    private SnowflakeIdWorker snowflakeIdWorker;

    @Test
    void getComplaintTracksRejectsNonOwner() {
        Complaint complaint = new Complaint();
        complaint.setId(40001L);
        complaint.setUserId(10001L);
        when(complaintMapper.selectById(40001L)).thenReturn(complaint);

        ComplaintServiceImpl service = new ComplaintServiceImpl(
                complaintMapper, complaintTrackMapper, orderQueryService, snowflakeIdWorker, new ObjectMapper());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getComplaintTracks(40001L, 20002L));

        assertEquals(ApiCode.COMPLAINT_NO_PERMISSION, ex.getCode());
    }

    @Test
    void waitingServiceOrderCanBeComplained() {
        SubmitComplaintRequest request = complaintRequest();
        when(complaintMapper.selectByIdempotentKey("idem-key")).thenReturn(null);
        when(orderQueryService.getOrder(20001L)).thenReturn(order(10001L, OrderStatus.WAITING_SERVICE.getValue()));
        when(snowflakeIdWorker.nextId()).thenReturn(40001L, 50001L);
        ComplaintServiceImpl service = new ComplaintServiceImpl(
                complaintMapper, complaintTrackMapper, orderQueryService, snowflakeIdWorker, new ObjectMapper());

        assertEquals(40001L, service.submitComplaint(request, 10001L, "idem-key").getComplaintId());
        verify(complaintMapper).insert(any());
        verify(complaintTrackMapper).insert(any());
    }

    @Test
    void completedOrderCanBeComplained() {
        SubmitComplaintRequest request = complaintRequest();
        when(complaintMapper.selectByIdempotentKey("idem-key")).thenReturn(null);
        when(orderQueryService.getOrder(20001L)).thenReturn(order(10001L, OrderStatus.COMPLETED.getValue()));
        when(snowflakeIdWorker.nextId()).thenReturn(40001L, 50001L);
        ComplaintServiceImpl service = new ComplaintServiceImpl(
                complaintMapper, complaintTrackMapper, orderQueryService, snowflakeIdWorker, new ObjectMapper());

        assertEquals(40001L, service.submitComplaint(request, 10001L, "idem-key").getComplaintId());
    }

    @Test
    void nonOwnerCannotComplainOrder() {
        SubmitComplaintRequest request = complaintRequest();
        when(complaintMapper.selectByIdempotentKey("idem-key")).thenReturn(null);
        when(orderQueryService.getOrder(20001L)).thenReturn(order(20002L, OrderStatus.WAITING_SERVICE.getValue()));
        ComplaintServiceImpl service = new ComplaintServiceImpl(
                complaintMapper, complaintTrackMapper, orderQueryService, snowflakeIdWorker, new ObjectMapper());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitComplaint(request, 10001L, "idem-key"));

        assertEquals(ApiCode.FORBIDDEN, ex.getCode());
    }

    private SubmitComplaintRequest complaintRequest() {
        SubmitComplaintRequest request = new SubmitComplaintRequest();
        request.setOrderId(20001L);
        request.setType(1);
        request.setContent("服务质量问题");
        return request;
    }

    private OrderDTO order(Long userId, int status) {
        OrderDTO order = new OrderDTO();
        order.setOrderId(20001L);
        order.setUserId(userId);
        order.setStatus(status);
        return order;
    }
}
