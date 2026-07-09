package com.nursing.feedback.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import com.nursing.common.result.Result;
import com.nursing.feedback.dto.request.SubmitComplaintRequest;
import com.nursing.feedback.dto.response.ComplaintSubmitResponse;
import com.nursing.feedback.dto.response.ComplaintTrackListVO;
import com.nursing.feedback.dto.response.ComplaintVO;
import com.nursing.feedback.service.ComplaintService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/complaints")
public class ComplaintController {
    private final ComplaintService complaintService;
    private final String gatewayToken;

    public ComplaintController(ComplaintService complaintService,
                               @Value("${nursing.gateway.trusted-token:}") String gatewayToken) {
        this.complaintService = complaintService;
        this.gatewayToken = gatewayToken;
    }

    @PostMapping
    public Result<ComplaintSubmitResponse> submitComplaint(@Valid @RequestBody SubmitComplaintRequest request,
                                                           @RequestHeader(value = "Idempotent-Key", required = false) String idempotentKey,
                                                           HttpServletRequest servletRequest) {
        Long userId = currentUserId(servletRequest);
        ComplaintSubmitResponse response = complaintService.submitComplaint(request, userId, idempotentKey);
        Result<ComplaintSubmitResponse> result = Result.success(response);
        result.setMessage("投诉提交成功，我们将在24小时内处理");
        return result;
    }

    @GetMapping
    public Result<PageResult<ComplaintVO>> pageComplaints(@RequestParam(value = "page", defaultValue = "1") int page,
                                                          @RequestParam(value = "size", defaultValue = "20") int size,
                                                          HttpServletRequest servletRequest) {
        Long userId = currentUserId(servletRequest);
        return Result.success(complaintService.pageComplaints(userId, page, size));
    }

    @GetMapping("/{id}/tracks")
    public Result<ComplaintTrackListVO> getComplaintTracks(@PathVariable("id") Long complaintId,
                                                           HttpServletRequest servletRequest) {
        Long userId = currentUserId(servletRequest);
        return Result.success(complaintService.getComplaintTracks(complaintId, userId));
    }

    private Long currentUserId(HttpServletRequest request) {
        requireTrustedGateway(request.getHeader("X-Gateway-Token"));
        String userId = firstTextHeader(request, "X-User-Id");
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "未获取到登录用户");
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "登录用户无效");
        }
    }

    private void requireTrustedGateway(String trustedToken) {
        if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(trustedToken)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
        }
    }

    private String firstTextHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
