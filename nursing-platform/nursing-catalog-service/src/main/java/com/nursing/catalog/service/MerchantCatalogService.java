package com.nursing.catalog.service;

import com.nursing.catalog.dto.request.MerchantServiceRequest;
import com.nursing.catalog.dto.response.MerchantServiceListResponse;
import com.nursing.catalog.dto.response.MerchantServiceResponse;
import com.nursing.catalog.dto.response.ServiceSpecResponse;
import com.nursing.catalog.entity.ServiceCategory;
import com.nursing.catalog.entity.ServiceItem;
import com.nursing.catalog.entity.ServiceSpec;
import com.nursing.catalog.repository.ServiceCategoryMapper;
import com.nursing.catalog.repository.ServiceItemMapper;
import com.nursing.catalog.repository.ServiceSpecMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantCatalogService {
    private static final int OFFLINE = 0;
    private static final int ONLINE = 1;
    private static final int NOT_DELETED = 0;
    private static final String DRAFT = "DRAFT";
    private static final String APPROVED = "APPROVED";
    private static final String OFFLINE_PUBLISH = "OFFLINE";
    private static final String PUBLISHED = "PUBLISHED";

    private final ServiceCategoryMapper categoryMapper;
    private final ServiceItemMapper itemMapper;
    private final ServiceSpecMapper specMapper;
    private final SnowflakeIdWorker idWorker;

    @Transactional(readOnly = true)
    public MerchantServiceListResponse list(Long ownerUserId) {
        List<MerchantServiceResponse> services = itemMapper.selectByOwnerUserId(ownerUserId).stream()
                .map(this::response).toList();
        int draft = count(services, DRAFT);
        int approved = count(services, APPROVED);
        int published = (int) services.stream().filter(item -> PUBLISHED.equals(item.getPublishStatus())).count();
        return new MerchantServiceListResponse(services,
                new MerchantServiceListResponse.Summary(services.size(), draft, 0, approved, 0, published));
    }

    @Transactional(readOnly = true)
    public MerchantServiceResponse detail(Long ownerUserId, Long itemId) {
        return response(requireOwnedItem(ownerUserId, itemId));
    }

    @Transactional
    public MerchantServiceResponse create(Long ownerUserId, MerchantServiceRequest request) {
        requireCategory(request.getCategoryId());
        ServiceItem item = new ServiceItem();
        item.setId(idWorker.nextId());
        item.setOwnerUserId(ownerUserId);
        item.setIsDeleted(NOT_DELETED);
        item.setStatus(OFFLINE);
        item.setAuditStatus(DRAFT);
        item.setPublishStatus(OFFLINE_PUBLISH);
        item.setVersion(1);
        item.setSortOrder(0);
        LocalDateTime now = LocalDateTime.now();
        item.setCreateTime(now);
        item.setUpdateTime(now);
        fill(item, request);
        itemMapper.insert(item);
        replaceSpecs(item.getId(), request.getSpecs());
        return detail(ownerUserId, item.getId());
    }

    @Transactional
    public MerchantServiceResponse update(Long ownerUserId, Long itemId, MerchantServiceRequest request) {
        ServiceItem item = requireOwnedItem(ownerUserId, itemId);
        if (!DRAFT.equals(item.getAuditStatus())) {
            throw new BusinessException(ApiCode.BIZ_ERROR, "Only draft services can be edited");
        }
        requireCategory(request.getCategoryId());
        fill(item, request);
        item.setVersion(item.getVersion() == null ? 1 : item.getVersion() + 1);
        if (itemMapper.update(item) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Service update failed");
        }
        replaceSpecs(itemId, request.getSpecs());
        return detail(ownerUserId, itemId);
    }

    @Transactional
    public MerchantServiceResponse submit(Long ownerUserId, Long itemId) {
        ServiceItem item = requireOwnedItem(ownerUserId, itemId);
        if (!DRAFT.equals(item.getAuditStatus())) {
            throw new BusinessException(ApiCode.BIZ_ERROR, "Service cannot be submitted in its current state");
        }
        if (specMapper.selectAdminByItemId(itemId).isEmpty()) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "At least one service specification is required");
        }
        // The current merchant workflow has no separate reviewer endpoint. Submission is
        // therefore approved atomically, making the verified service available for publishing.
        item.setAuditStatus(APPROVED);
        item.setVersion(item.getVersion() == null ? 1 : item.getVersion() + 1);
        itemMapper.update(item);
        return detail(ownerUserId, itemId);
    }

    @Transactional
    public MerchantServiceResponse publish(Long ownerUserId, Long itemId) {
        ServiceItem item = requireOwnedItem(ownerUserId, itemId);
        if (!APPROVED.equals(item.getAuditStatus())) {
            throw new BusinessException(ApiCode.BIZ_ERROR, "Service has not been approved");
        }
        item.setStatus(ONLINE);
        item.setPublishStatus(PUBLISHED);
        itemMapper.update(item);
        return detail(ownerUserId, itemId);
    }

    @Transactional
    public MerchantServiceResponse offline(Long ownerUserId, Long itemId) {
        ServiceItem item = requireOwnedItem(ownerUserId, itemId);
        item.setStatus(OFFLINE);
        item.setPublishStatus(OFFLINE_PUBLISH);
        itemMapper.update(item);
        return detail(ownerUserId, itemId);
    }

    private void fill(ServiceItem item, MerchantServiceRequest request) {
        item.setCategoryId(request.getCategoryId());
        item.setName(request.getName().trim());
        item.setDescription(request.getDescription().trim());
        item.setCoverImage(trimToNull(request.getCoverImage()));
    }

    private void replaceSpecs(Long itemId, List<MerchantServiceRequest.SpecRequest> requests) {
        specMapper.markDeletedByItemId(itemId);
        LocalDateTime now = LocalDateTime.now();
        for (MerchantServiceRequest.SpecRequest request : requests) {
            ServiceSpec spec = new ServiceSpec();
            spec.setId(idWorker.nextId());
            spec.setServiceItemId(itemId);
            spec.setName(request.getName().trim());
            spec.setPrice(request.getPrice());
            spec.setOriginalPrice(request.getOriginalPrice() == null ? request.getPrice() : request.getOriginalPrice());
            spec.setDuration(request.getDuration());
            spec.setStatus(ONLINE);
            spec.setIsDeleted(NOT_DELETED);
            spec.setCreateTime(now);
            spec.setUpdateTime(now);
            specMapper.insert(spec);
        }
    }

    private MerchantServiceResponse response(ServiceItem item) {
        ServiceCategory category = requireCategory(item.getCategoryId());
        List<ServiceSpecResponse> specs = specMapper.selectAdminByItemId(item.getId()).stream().map(spec -> {
            ServiceSpecResponse response = new ServiceSpecResponse();
            response.setSpecId(spec.getId());
            response.setServiceItemId(item.getId());
            response.setName(spec.getName());
            response.setPrice(spec.getPrice());
            response.setOriginalPrice(spec.getOriginalPrice());
            response.setDuration(spec.getDuration());
            response.setStatus(spec.getStatus());
            return response;
        }).toList();
        MerchantServiceResponse response = new MerchantServiceResponse();
        response.setItemId(item.getId());
        response.setCategoryId(item.getCategoryId());
        response.setCategoryName(category.getName());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setCoverImage(item.getCoverImage());
        response.setAuditStatus(item.getAuditStatus());
        response.setPublishStatus(item.getPublishStatus());
        response.setVersion(item.getVersion());
        response.setSpecs(specs);
        response.setMinPrice(specs.stream().map(ServiceSpecResponse::getPrice).min(BigDecimal::compareTo).orElse(null));
        response.setCreateTime(item.getCreateTime());
        response.setUpdateTime(item.getUpdateTime());
        return response;
    }

    private ServiceItem requireOwnedItem(Long ownerUserId, Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "itemId is required");
        }
        ServiceItem item = itemMapper.selectByOwnerUserIdAndId(ownerUserId, itemId);
        if (item == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "Service not found");
        }
        return item;
    }

    private ServiceCategory requireCategory(Long categoryId) {
        ServiceCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !Integer.valueOf(ONLINE).equals(category.getStatus())) {
            throw new BusinessException(ApiCode.NOT_FOUND, "Service category not found");
        }
        return category;
    }

    private int count(List<MerchantServiceResponse> services, String status) {
        return (int) services.stream().filter(item -> status.equals(item.getAuditStatus())).count();
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
