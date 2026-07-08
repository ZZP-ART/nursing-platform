package com.nursing.feedback.service;

import com.nursing.common.result.PageResult;
import com.nursing.feedback.dto.request.SubmitComplaintRequest;
import com.nursing.feedback.dto.response.ComplaintSubmitResponse;
import com.nursing.feedback.dto.response.ComplaintTrackListVO;
import com.nursing.feedback.dto.response.ComplaintVO;

public interface ComplaintService {
    ComplaintSubmitResponse submitComplaint(SubmitComplaintRequest request, Long userId, String idempotentKey);

    PageResult<ComplaintVO> pageComplaints(Long userId, int page, int size);

    ComplaintTrackListVO getComplaintTracks(Long complaintId, Long userId);
}
