package com.nursing.operations.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import com.nursing.operations.dto.request.AssignmentRemarkRequest;
import com.nursing.operations.dto.request.DispatchRequest;
import com.nursing.operations.dto.response.AssignmentResponse;
import com.nursing.operations.service.ServiceAssignmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class ServiceAssignmentController {
    private final ServiceAssignmentService service;
    private final String gatewayToken;

    public ServiceAssignmentController(ServiceAssignmentService service,
                                       @Value("${nursing.gateway.trusted-token:}") String gatewayToken) {
        this.service = service;
        this.gatewayToken = gatewayToken;
    }

    @PostMapping({"/api/v1/merchants/orders/{orderId}/dispatch", "/api/v1/merchants/orders/{orderId}/redispatch"})
    public Result<AssignmentResponse> dispatch(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                               @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                               @RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                               @RequestHeader("Idempotency-Key") String idempotentKey,
                                               @PathVariable @Positive Long orderId,
                                               @Valid @RequestBody DispatchRequest request) {
        trusted(token); role(roles, "MERCHANT_MEMBER"); requireIdempotentKey(idempotentKey);
        return Result.success(service.dispatch(requiredUser(userId), orderId, request, idempotentKey));
    }

    @PostMapping("/api/v1/caregivers/assignments/{assignmentId}/accept")
    public Result<AssignmentResponse> accept(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                             @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                             @RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                             @RequestHeader("Idempotency-Key") String idempotentKey,
                                             @PathVariable @Positive Long assignmentId) {
        trusted(token); role(roles, "CAREGIVER"); requireIdempotentKey(idempotentKey);
        return Result.success(service.accept(requiredUser(userId), assignmentId, idempotentKey));
    }

    @PostMapping("/api/v1/caregivers/assignments/{assignmentId}/reject")
    public Result<AssignmentResponse> reject(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                             @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                             @RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                             @RequestHeader("Idempotency-Key") String idempotentKey,
                                             @PathVariable @Positive Long assignmentId,
                                             @Valid @RequestBody(required = false) AssignmentRemarkRequest request) {
        trusted(token); role(roles, "CAREGIVER"); requireIdempotentKey(idempotentKey);
        return Result.success(service.reject(requiredUser(userId), assignmentId, request, idempotentKey));
    }

    @PostMapping("/api/v1/caregivers/orders/{orderId}/{action:depart|check-in|start|finish}")
    public Result<AssignmentResponse> action(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                             @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                             @RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                             @RequestHeader("Idempotency-Key") String idempotentKey,
                                             @PathVariable @Positive Long orderId, @PathVariable String action,
                                             @Valid @RequestBody(required = false) AssignmentRemarkRequest request) {
        trusted(token); role(roles, "CAREGIVER"); requireIdempotentKey(idempotentKey);
        return Result.success(service.recordAction(requiredUser(userId), orderId, action, request, idempotentKey));
    }

    @GetMapping("/api/v1/merchants/assignments")
    public Result<java.util.List<AssignmentResponse>> merchantAssignments(@RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles, @RequestHeader(value = "X-Gateway-Token", required = false) String token) {
        trusted(token); role(roles, "MERCHANT_MEMBER"); return Result.success(service.merchantAssignments(requiredUser(userId)));
    }

    @GetMapping("/api/v1/caregivers/tasks")
    public Result<java.util.List<com.nursing.operations.dto.response.CaregiverTaskResponse>> caregiverTasks(@RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles, @RequestHeader(value = "X-Gateway-Token", required = false) String token) {
        trusted(token); role(roles, "CAREGIVER"); return Result.success(service.caregiverTasks(requiredUser(userId)));
    }

    @GetMapping("/api/v1/caregivers/tasks/{orderId}")
    public Result<com.nursing.operations.dto.response.CaregiverTaskResponse> caregiverTask(@RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles, @RequestHeader(value = "X-Gateway-Token", required = false) String token,
            @PathVariable @Positive Long orderId) {
        trusted(token); role(roles, "CAREGIVER"); return Result.success(service.caregiverTask(requiredUser(userId), orderId));
    }

    @GetMapping("/api/v1/merchants/dashboard")
    public Result<com.nursing.operations.dto.response.MerchantDashboardResponse> merchantDashboard(@RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles, @RequestHeader(value = "X-Gateway-Token", required = false) String token) {
        trusted(token); role(roles, "MERCHANT_MEMBER"); return Result.success(service.merchantDashboard(requiredUser(userId)));
    }

    @GetMapping("/api/v1/merchants/orders")
    public Result<java.util.List<com.nursing.operations.dto.response.MerchantOrderResponse>> merchantOrders(@RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles, @RequestHeader(value = "X-Gateway-Token", required = false) String token,
            @RequestParam(required = false) Integer status) {
        trusted(token); role(roles, "MERCHANT_MEMBER"); return Result.success(service.merchantOrders(requiredUser(userId), status));
    }

    @GetMapping("/api/v1/merchants/orders/{orderId}")
    public Result<com.nursing.operations.dto.response.MerchantOrderResponse> merchantOrder(@RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles, @RequestHeader(value = "X-Gateway-Token", required = false) String token,
            @PathVariable @Positive Long orderId) {
        trusted(token); role(roles, "MERCHANT_MEMBER"); return Result.success(service.merchantOrder(requiredUser(userId), orderId));
    }

    @GetMapping("/api/v1/merchants/orders/{orderId}/assignments")
    public Result<java.util.List<AssignmentResponse>> merchantOrderAssignments(@RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles, @RequestHeader(value = "X-Gateway-Token", required = false) String token,
            @PathVariable @Positive Long orderId) {
        trusted(token); role(roles, "MERCHANT_MEMBER"); return Result.success(service.merchantOrderAssignments(requiredUser(userId), orderId));
    }

    @GetMapping("/api/v1/merchants/orders/{orderId}/candidates")
    public Result<java.util.List<com.nursing.operations.dto.response.CaregiverCandidateResponse>> merchantCandidates(@RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles, @RequestHeader(value = "X-Gateway-Token", required = false) String token,
            @PathVariable @Positive Long orderId) {
        trusted(token); role(roles, "MERCHANT_MEMBER"); return Result.success(service.merchantCandidates(requiredUser(userId), orderId));
    }

    private Long requiredUser(Long userId) {
        if (userId == null || userId <= 0) throw new BusinessException(ApiCode.UNAUTHORIZED, "Unauthorized", HttpStatus.UNAUTHORIZED);
        return userId;
    }

    private void trusted(String token) {
        if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(token)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
        }
    }

    private void role(String header, String expected) {
        Set<String> roles = header == null ? Set.of() : Set.copyOf(Arrays.asList(header.split(",")));
        if (!roles.contains(expected)) throw new BusinessException(ApiCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
    }
    private void requireIdempotentKey(String key) {
        if (!StringUtils.hasText(key) || key.length() > 128) throw new BusinessException(ApiCode.PARAM_ERROR, "Idempotency-Key is required");
    }
}
