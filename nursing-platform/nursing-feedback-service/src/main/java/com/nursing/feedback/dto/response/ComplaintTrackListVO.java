package com.nursing.feedback.dto.response;

import java.util.List;

public class ComplaintTrackListVO {
    private Long complaintId;
    private Integer status;
    private String statusText;
    private List<ComplaintTrackVO> tracks;

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public List<ComplaintTrackVO> getTracks() {
        return tracks;
    }

    public void setTracks(List<ComplaintTrackVO> tracks) {
        this.tracks = tracks;
    }
}
