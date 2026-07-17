package com.nursing.operations.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import com.nursing.operations.dto.request.CaregiverApplicationRequest;
import com.nursing.operations.entity.CaregiverApplication;
import com.nursing.operations.service.CaregiverApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CaregiverApplicationController {
    private final CaregiverApplicationService service;
    private final String gatewayToken;

    public CaregiverApplicationController(CaregiverApplicationService service,
                                          @Value("${nursing.gateway.trusted-token:}") String gatewayToken) {
        this.service = service;
        this.gatewayToken = gatewayToken;
    }

    @PostMapping("/api/v1/caregiver/applications")
    public Result<CaregiverApplication> apply(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                              @RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                              @Valid @RequestBody CaregiverApplicationRequest request) {
        trusted(token);
        return Result.success(service.apply(required(userId), request));
    }

    @GetMapping("/api/v1/caregiver/applications/me")
    public Result<CaregiverApplication> myApplication(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                      @RequestHeader(value = "X-Gateway-Token", required = false) String token) {
        trusted(token);
        return Result.success(service.getByUserId(required(userId)));
    }

    @GetMapping("/api/v1/admin/caregiver-applications")
    public Result<List<CaregiverApplication>> pending(@RequestHeader(value = "X-Gateway-Token", required = false) String token) {
        trusted(token);
        return Result.success(service.pending());
    }

    @PostMapping("/api/v1/admin/caregiver-applications/{userId}/approve")
    public Result<Void> approve(@PathVariable Long userId,
                                @RequestParam(value = "remark", required = false) String remark,
                                @RequestHeader(value = "X-Gateway-Token", required = false) String token) {
        trusted(token);
        service.approve(userId, remark);
        return Result.success();
    }

    @PostMapping("/api/v1/admin/caregiver-applications/{userId}/reject")
    public Result<Void> reject(@PathVariable Long userId,
                               @RequestParam(value = "remark", required = false) String remark,
                               @RequestHeader(value = "X-Gateway-Token", required = false) String token) {
        trusted(token);
        service.reject(userId, remark);
        return Result.success();
    }

    private Long required(Long value) {
        if (value == null || value <= 0) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return value;
    }

    private void trusted(String token) {
        if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(token)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
        }
    }
}
