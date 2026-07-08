package com.nursing.feedback.dto.response;

public class ComplaintSubmitResponse {
    private Long complaintId;

    public ComplaintSubmitResponse() {
    }

    public ComplaintSubmitResponse(Long complaintId) {
        this.complaintId = complaintId;
    }

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }
}
