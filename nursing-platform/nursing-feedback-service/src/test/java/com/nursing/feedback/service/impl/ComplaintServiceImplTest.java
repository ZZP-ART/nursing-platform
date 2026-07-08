package com.nursing.feedback.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
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
}
