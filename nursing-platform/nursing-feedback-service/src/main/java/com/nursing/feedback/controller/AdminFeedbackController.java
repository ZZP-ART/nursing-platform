package com.nursing.feedback.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import com.nursing.common.result.Result;
import com.nursing.feedback.dto.request.HandleComplaintRequest;
import com.nursing.feedback.dto.request.ReviewModerationRequest;
import com.nursing.feedback.dto.response.AdminComplaintResponse;
import com.nursing.feedback.dto.response.AdminReviewResponse;
import com.nursing.feedback.service.AdminFeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/feedback")
public class AdminFeedbackController {
    private final AdminFeedbackService adminFeedbackService;
    private final String gatewayToken;

    public AdminFeedbackController(AdminFeedbackService adminFeedbackService,
                                   @Value("${nursing.gateway.trusted-token:}") String gatewayToken) {
        this.adminFeedbackService = adminFeedbackService;
        this.gatewayToken = gatewayToken;
    }

    @GetMapping("/reviews")
    public Result<PageResult<AdminReviewResponse>> pageReviews(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                                  @RequestParam(value = "status", required = false) Integer status,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "size", defaultValue = "20") int size) {
        trusted(token);
        PageResult<com.nursing.feedback.entity.Review> result = adminFeedbackService.pageReviews(status, page, size);
        return Result.success(PageResult.of(result.getList().stream().map(AdminReviewResponse::from).toList(),
                result.getTotal(), result.getPage(), result.getSize()));
    }

    @PostMapping("/reviews/{id}/moderate")
    public Result<Void> moderateReview(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                       @PathVariable("id") Long reviewId,
                                       @Valid @RequestBody ReviewModerationRequest request) {
        trusted(token);
        adminFeedbackService.moderateReview(reviewId, request);
        return Result.success();
    }

    @GetMapping("/complaints")
    public Result<PageResult<AdminComplaintResponse>> pageComplaints(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                                        @RequestParam(value = "status", required = false) Integer status,
                                                        @RequestParam(value = "page", defaultValue = "1") int page,
                                                        @RequestParam(value = "size", defaultValue = "20") int size) {
        trusted(token);
        PageResult<com.nursing.feedback.entity.Complaint> result = adminFeedbackService.pageComplaints(status, page, size);
        return Result.success(PageResult.of(result.getList().stream().map(AdminComplaintResponse::from).toList(),
                result.getTotal(), result.getPage(), result.getSize()));
    }

    @PostMapping("/complaints/{id}/handle")
    public Result<Void> handleComplaint(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                        @RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                                        @PathVariable("id") Long complaintId,
                                        @Valid @RequestBody HandleComplaintRequest request) {
        trusted(token);
        adminFeedbackService.handleComplaint(complaintId, request, operatorId);
        return Result.success();
    }

    private void trusted(String token) {
        if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(token)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
        }
    }
}
