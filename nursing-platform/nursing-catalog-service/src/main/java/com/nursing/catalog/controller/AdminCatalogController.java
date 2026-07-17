package com.nursing.catalog.controller;

import com.nursing.catalog.dto.request.AdminCategoryRequest;
import com.nursing.catalog.dto.request.AdminItemRequest;
import com.nursing.catalog.dto.request.AdminSpecRequest;
import com.nursing.catalog.dto.response.AdminCategoryResponse;
import com.nursing.catalog.dto.response.AdminItemResponse;
import com.nursing.catalog.dto.response.AdminSpecResponse;
import com.nursing.catalog.entity.ServiceCategory;
import com.nursing.catalog.entity.ServiceItem;
import com.nursing.catalog.entity.ServiceSpec;
import com.nursing.catalog.service.AdminCatalogService;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final AdminCatalogService adminCatalogService;

    @Value("${nursing.gateway.trusted-token:}")
    private String gatewayToken;

    @PostMapping("/categories")
    public Result<AdminCategoryResponse> createCategory(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                                  @Valid @RequestBody AdminCategoryRequest request) {
        trusted(token);
        return Result.success(AdminCategoryResponse.from(adminCatalogService.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    public Result<AdminCategoryResponse> updateCategory(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody AdminCategoryRequest request) {
        trusted(token);
        return Result.success(AdminCategoryResponse.from(adminCatalogService.updateCategory(id, request)));
    }

    @PostMapping("/categories/{id}/publish")
    public Result<Void> publishCategory(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                        @PathVariable Long id) {
        trusted(token);
        adminCatalogService.publishCategory(id);
        return Result.success();
    }

    @PostMapping("/categories/{id}/unpublish")
    public Result<Void> unpublishCategory(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                          @PathVariable Long id) {
        trusted(token);
        adminCatalogService.unpublishCategory(id);
        return Result.success();
    }

    @PostMapping("/items")
    public Result<AdminItemResponse> createItem(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                          @Valid @RequestBody AdminItemRequest request) {
        trusted(token);
        return Result.success(AdminItemResponse.from(adminCatalogService.createItem(request)));
    }

    @PutMapping("/items/{id}")
    public Result<AdminItemResponse> updateItem(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                          @PathVariable Long id,
                                          @Valid @RequestBody AdminItemRequest request) {
        trusted(token);
        return Result.success(AdminItemResponse.from(adminCatalogService.updateItem(id, request)));
    }

    @PostMapping("/items/{id}/publish")
    public Result<Void> publishItem(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                    @PathVariable Long id) {
        trusted(token);
        adminCatalogService.publishItem(id);
        return Result.success();
    }

    @PostMapping("/items/{id}/unpublish")
    public Result<Void> unpublishItem(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                      @PathVariable Long id) {
        trusted(token);
        adminCatalogService.unpublishItem(id);
        return Result.success();
    }

    @PostMapping("/specs")
    public Result<AdminSpecResponse> createSpec(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                          @Valid @RequestBody AdminSpecRequest request) {
        trusted(token);
        return Result.success(AdminSpecResponse.from(adminCatalogService.createSpec(request)));
    }

    @PutMapping("/specs/{id}")
    public Result<AdminSpecResponse> updateSpec(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                          @PathVariable Long id,
                                          @Valid @RequestBody AdminSpecRequest request) {
        trusted(token);
        return Result.success(AdminSpecResponse.from(adminCatalogService.updateSpec(id, request)));
    }

    @PostMapping("/specs/{id}/publish")
    public Result<Void> publishSpec(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                    @PathVariable Long id) {
        trusted(token);
        adminCatalogService.publishSpec(id);
        return Result.success();
    }

    @PostMapping("/specs/{id}/unpublish")
    public Result<Void> unpublishSpec(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                      @PathVariable Long id) {
        trusted(token);
        adminCatalogService.unpublishSpec(id);
        return Result.success();
    }

    private void trusted(String token) {
        if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(token)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
        }
    }
}
