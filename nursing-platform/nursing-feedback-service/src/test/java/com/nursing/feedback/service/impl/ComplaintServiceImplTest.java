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
import com.nursing.feedback.support.RequestFingerprint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

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
    void replaysACompletedComplaintForTheSameUserAndPayload() {
        SubmitComplaintRequest request = complaintRequest();
        Complaint complaint = existingComplaint(request, 40001L);
        when(complaintMapper.selectByUserAndIdempotentKey(10001L, "idem-key")).thenReturn(complaint);

        assertEquals(40001L, service().submitComplaint(request, 10001L, "idem-key").getComplaintId());
    }

    @Test
    void rejectsSameComplaintKeyWithDifferentPayload() {
        SubmitComplaintRequest request = complaintRequest();
        Complaint complaint = existingComplaint(request, 40001L);
        complaint.setRequestHash("different-request");
        when(complaintMapper.selectByUserAndIdempotentKey(10001L, "idem-key")).thenReturn(complaint);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().submitComplaint(request, 10001L, "idem-key"));

        assertEquals(ApiCode.CONFLICT, ex.getCode());
    }

    @Test
    void replaysAfterConcurrentComplaintUniqueKeyConflict() {
        SubmitComplaintRequest request = complaintRequest();
        Complaint complaint = existingComplaint(request, 40001L);
        when(complaintMapper.selectByUserAndIdempotentKey(10001L, "idem-key"))
                .thenReturn(null, complaint);
        when(orderQueryService.getOrder(20001L)).thenReturn(order(10001L, OrderStatus.WAITING_SERVICE.getValue()));
        when(snowflakeIdWorker.nextId()).thenReturn(40001L);
        when(complaintMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));

        assertEquals(40001L, service().submitComplaint(request, 10001L, "idem-key").getComplaintId());
    }

    @Test
    void sameKeyFromAnotherUserDoesNotReadTheFirstUsersComplaint() {
        SubmitComplaintRequest request = complaintRequest();
        when(complaintMapper.selectByUserAndIdempotentKey(20002L, "idem-key")).thenReturn(null);
        when(orderQueryService.getOrder(20001L)).thenReturn(order(20002L, OrderStatus.WAITING_SERVICE.getValue()));
        when(snowflakeIdWorker.nextId()).thenReturn(40001L, 50001L);

        assertEquals(40001L, service().submitComplaint(request, 20002L, "idem-key").getComplaintId());
        verify(complaintMapper).insert(any());
    }

    @Test
    void rejectsBlankComplaintContentInServiceLayer() {
        SubmitComplaintRequest request = complaintRequest();
        request.setContent(" ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().submitComplaint(request, 10001L, "idem-key"));

        assertEquals(ApiCode.PARAM_ERROR, ex.getCode());
    }

    private ComplaintServiceImpl service() {
        return new ComplaintServiceImpl(
                complaintMapper, complaintTrackMapper, orderQueryService, snowflakeIdWorker, new ObjectMapper());
    }

    private SubmitComplaintRequest complaintRequest() {
        SubmitComplaintRequest request = new SubmitComplaintRequest();
        request.setOrderId(20001L);
        request.setType(1);
        request.setContent("Service quality problem");
        return request;
    }

    private Complaint existingComplaint(SubmitComplaintRequest request, Long complaintId) {
        Complaint complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setRequestHash(RequestFingerprint.complaint(request));
        return complaint;
    }

    private OrderDTO order(Long userId, int status) {
        OrderDTO order = new OrderDTO();
        order.setOrderId(20001L);
        order.setUserId(userId);
        order.setStatus(status);
        return order;
    }
}
