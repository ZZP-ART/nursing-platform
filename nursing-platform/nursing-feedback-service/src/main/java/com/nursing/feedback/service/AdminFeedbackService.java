package com.nursing.feedback.service;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.dto.request.HandleComplaintRequest;
import com.nursing.feedback.dto.request.ReviewModerationRequest;
import com.nursing.feedback.entity.Complaint;
import com.nursing.feedback.entity.ComplaintTrack;
import com.nursing.feedback.entity.Review;
import com.nursing.feedback.repository.ComplaintMapper;
import com.nursing.feedback.repository.ComplaintTrackMapper;
import com.nursing.feedback.repository.ReviewMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminFeedbackService {
    private static final int REVIEW_APPROVED = 2;
    private static final int REVIEW_REJECTED = 3;
    private static final int COMPLAINT_PROCESSING = 1;
    private static final int COMPLAINT_RESOLVED = 2;
    private static final int COMPLAINT_CLOSED = 3;
    private static final int NOT_DELETED = 0;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewMapper reviewMapper;
    private final ComplaintMapper complaintMapper;
    private final ComplaintTrackMapper complaintTrackMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public AdminFeedbackService(ReviewMapper reviewMapper,
                                ComplaintMapper complaintMapper,
                                ComplaintTrackMapper complaintTrackMapper,
                                SnowflakeIdWorker snowflakeIdWorker) {
        this.reviewMapper = reviewMapper;
        this.complaintMapper = complaintMapper;
        this.complaintTrackMapper = complaintTrackMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    public PageResult<Review> pageReviews(Integer status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;
        List<Review> reviews = reviewMapper.selectAdminPage(status, offset, safeSize);
        long total = reviewMapper.countAdmin(status);
        return PageResult.of(reviews, total, safePage, safeSize);
    }

    @Transactional
    public void moderateReview(Long reviewId, ReviewModerationRequest request) {
        if (reviewId == null || reviewId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "reviewId is required");
        }
        int status = normalizeReviewStatus(request.getStatus());
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "Review not found");
        }
        if (reviewMapper.updateStatus(reviewId, status) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Review status update failed");
        }
    }

    public PageResult<Complaint> pageComplaints(Integer status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;
        List<Complaint> complaints = complaintMapper.selectAdminPage(status, offset, safeSize);
        long total = complaintMapper.countAdmin(status);
        return PageResult.of(complaints, total, safePage, safeSize);
    }

    @Transactional
    public void handleComplaint(Long complaintId, HandleComplaintRequest request, Long operatorId) {
        if (complaintId == null || complaintId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "complaintId is required");
        }
        int status = normalizeComplaintStatus(request.getStatus());
        Complaint complaint = complaintMapper.selectById(complaintId);
        if (complaint == null) {
            throw new BusinessException(ApiCode.COMPLAINT_NOT_FOUND, "Complaint not found");
        }
        if (complaintMapper.updateStatus(complaintId, status) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Complaint status update failed");
        }
        ComplaintTrack track = new ComplaintTrack();
        LocalDateTime now = LocalDateTime.now();
        track.setId(snowflakeIdWorker.nextId());
        track.setComplaintId(complaintId);
        track.setOperator(operatorId == null ? "admin" : "admin:" + operatorId);
        track.setContent(request.getContent().trim());
        track.setIsDeleted(NOT_DELETED);
        track.setCreateTime(now);
        track.setUpdateTime(now);
        complaintTrackMapper.insert(track);
    }

    private int normalizeReviewStatus(Integer status) {
        if (status == null || (status != REVIEW_APPROVED && status != REVIEW_REJECTED)) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "review status must be 2 or 3");
        }
        return status;
    }

    private int normalizeComplaintStatus(Integer status) {
        if (status == null
                || (status != COMPLAINT_PROCESSING && status != COMPLAINT_RESOLVED && status != COMPLAINT_CLOSED)) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "complaint status must be 1, 2 or 3");
        }
        return status;
    }
}
