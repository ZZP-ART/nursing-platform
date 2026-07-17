package com.nursing.feedback.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import com.nursing.common.result.Result;
import com.nursing.feedback.dto.request.SubmitReviewRequest;
import com.nursing.feedback.dto.response.ReviewSubmitResponse;
import com.nursing.feedback.dto.response.ReviewVO;
import com.nursing.feedback.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final String gatewayToken;

    public ReviewController(ReviewService reviewService,
                            @Value("${nursing.gateway.trusted-token:}") String gatewayToken) {
        this.reviewService = reviewService;
        this.gatewayToken = gatewayToken;
    }

    @PostMapping
    public Result<ReviewSubmitResponse> submitReview(@Valid @RequestBody SubmitReviewRequest request,
                                                     @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                     HttpServletRequest servletRequest) {
        Long userId = currentUserId(servletRequest);
        ReviewSubmitResponse response = reviewService.submitReview(request, userId, idempotencyKey);
        Result<ReviewSubmitResponse> result = Result.success(response);
        result.setMessage("评价提交成功");
        return result;
    }

    @GetMapping
    public Result<PageResult<ReviewVO>> pageReviews(@RequestParam("itemId") Long itemId,
                                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                                    @RequestParam(value = "size", defaultValue = "20") int size,
                                                    HttpServletRequest servletRequest) {
        currentUserId(servletRequest);
        return Result.success(reviewService.pageReviews(itemId, page, size));
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
