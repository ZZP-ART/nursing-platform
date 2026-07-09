package com.nursing.order.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import com.nursing.order.dto.request.AddressRequest;
import com.nursing.order.dto.response.AddressResponse;
import com.nursing.order.service.IAddressService;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {
    private final IAddressService addressService;
    private final String gatewayToken;

    public AddressController(IAddressService addressService,
                             @Value("${nursing.gateway.trusted-token:}") String gatewayToken) {
        this.addressService = addressService;
        this.gatewayToken = gatewayToken;
    }

    @GetMapping
    public Result<List<AddressResponse>> list(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                              @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken) {
        requireTrustedGateway(trustedToken);
        return Result.success(addressService.listAddresses(requireUserId(userId)));
    }

    @PostMapping
    public Result<Map<String, Long>> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @Validated(AddressRequest.Create.class) @RequestBody AddressRequest request) {
        requireTrustedGateway(trustedToken);
        Long addressId = addressService.createAddress(requireUserId(userId), request);
        return Result.success(Map.of("addressId", addressId));
    }

    @PatchMapping("/{id}")
    public Result<Void> update(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @PathVariable("id") @Positive(message = "地址ID必须大于0") Long id,
            @Validated(AddressRequest.Update.class) @RequestBody AddressRequest request) {
        requireTrustedGateway(trustedToken);
        addressService.updateAddress(requireUserId(userId), id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @PathVariable("id") @Positive(message = "地址ID必须大于0") Long id) {
        requireTrustedGateway(trustedToken);
        addressService.deleteAddress(requireUserId(userId), id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @PathVariable("id") @Positive(message = "地址ID必须大于0") Long id) {
        requireTrustedGateway(trustedToken);
        addressService.setDefaultAddress(requireUserId(userId), id);
        return Result.success();
    }

    private Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }

    private void requireTrustedGateway(String trustedToken) {
        if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(trustedToken)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
        }
    }
}
