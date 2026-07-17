package com.nursing.catalog.controller;

import com.nursing.catalog.dto.request.MerchantServiceRequest;
import com.nursing.catalog.dto.response.MerchantServiceListResponse;
import com.nursing.catalog.dto.response.MerchantServiceResponse;
import com.nursing.catalog.service.MerchantCatalogService;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchants/services")
@RequiredArgsConstructor
public class MerchantCatalogController {
    private static final String MERCHANT_MEMBER = "MERCHANT_MEMBER";

    private final MerchantCatalogService merchantCatalogService;

    @Value("${nursing.gateway.trusted-token:}")
    private String gatewayToken;

    @GetMapping
    public Result<MerchantServiceListResponse> list(@RequestHeader("X-Gateway-Token") String token,
                                                     @RequestHeader("X-User-Id") Long userId,
                                                     @RequestHeader("X-User-Roles") String roles) {
        requireMerchant(token, userId, roles);
        return Result.success(merchantCatalogService.list(userId));
    }

    @GetMapping("/{itemId}")
    public Result<MerchantServiceResponse> detail(@RequestHeader("X-Gateway-Token") String token,
                                                   @RequestHeader("X-User-Id") Long userId,
                                                   @RequestHeader("X-User-Roles") String roles,
                                                   @PathVariable Long itemId) {
        requireMerchant(token, userId, roles);
        return Result.success(merchantCatalogService.detail(userId, itemId));
    }

    @PostMapping
    public Result<MerchantServiceResponse> create(@RequestHeader("X-Gateway-Token") String token,
                                                   @RequestHeader("X-User-Id") Long userId,
                                                   @RequestHeader("X-User-Roles") String roles,
                                                   @Valid @RequestBody MerchantServiceRequest request) {
        requireMerchant(token, userId, roles);
        return Result.success(merchantCatalogService.create(userId, request));
    }

    @PutMapping("/{itemId}")
    public Result<MerchantServiceResponse> update(@RequestHeader("X-Gateway-Token") String token,
                                                   @RequestHeader("X-User-Id") Long userId,
                                                   @RequestHeader("X-User-Roles") String roles,
                                                   @PathVariable Long itemId,
                                                   @Valid @RequestBody MerchantServiceRequest request) {
        requireMerchant(token, userId, roles);
        return Result.success(merchantCatalogService.update(userId, itemId, request));
    }

    @PostMapping("/{itemId}/submit")
    public Result<MerchantServiceResponse> submit(@RequestHeader("X-Gateway-Token") String token,
                                                   @RequestHeader("X-User-Id") Long userId,
                                                   @RequestHeader("X-User-Roles") String roles,
                                                   @PathVariable Long itemId) {
        requireMerchant(token, userId, roles);
        return Result.success(merchantCatalogService.submit(userId, itemId));
    }

    @PostMapping("/{itemId}/publish")
    public Result<MerchantServiceResponse> publish(@RequestHeader("X-Gateway-Token") String token,
                                                    @RequestHeader("X-User-Id") Long userId,
                                                    @RequestHeader("X-User-Roles") String roles,
                                                    @PathVariable Long itemId) {
        requireMerchant(token, userId, roles);
        return Result.success(merchantCatalogService.publish(userId, itemId));
    }

    @PostMapping("/{itemId}/offline")
    public Result<MerchantServiceResponse> offline(@RequestHeader("X-Gateway-Token") String token,
                                                    @RequestHeader("X-User-Id") Long userId,
                                                    @RequestHeader("X-User-Roles") String roles,
                                                    @PathVariable Long itemId) {
        requireMerchant(token, userId, roles);
        return Result.success(merchantCatalogService.offline(userId, itemId));
    }

    private void requireMerchant(String token, Long userId, String roles) {
        if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(token)
                || userId == null || userId <= 0
                || !StringUtils.commaDelimitedListToSet(roles).contains(MERCHANT_MEMBER)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Merchant permission is required");
        }
    }
}
