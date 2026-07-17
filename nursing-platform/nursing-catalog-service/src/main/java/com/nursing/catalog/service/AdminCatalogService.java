package com.nursing.catalog.service;

import com.nursing.catalog.dto.request.AdminCategoryRequest;
import com.nursing.catalog.dto.request.AdminItemRequest;
import com.nursing.catalog.dto.request.AdminSpecRequest;
import com.nursing.catalog.entity.ServiceCategory;
import com.nursing.catalog.entity.ServiceItem;
import com.nursing.catalog.entity.ServiceSpec;
import com.nursing.catalog.repository.ServiceCategoryMapper;
import com.nursing.catalog.repository.ServiceItemMapper;
import com.nursing.catalog.repository.ServiceSpecMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private static final long ROOT_PARENT_ID = 0L;
    private static final int STATUS_OFFLINE = 0;
    private static final int STATUS_ONLINE = 1;
    private static final int NOT_DELETED = 0;

    private final ServiceCategoryMapper categoryMapper;
    private final ServiceItemMapper itemMapper;
    private final ServiceSpecMapper specMapper;
    private final SnowflakeIdWorker idWorker;

    @Transactional
    public ServiceCategory createCategory(AdminCategoryRequest request) {
        Long categoryId = idWorker.nextId();
        ServiceCategory parent = resolveParent(request.getParentId());
        ServiceCategory category = new ServiceCategory();
        category.setId(categoryId);
        category.setParentId(parent == null ? ROOT_PARENT_ID : parent.getId());
        category.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        category.setPath(parent == null ? "/" + categoryId + "/" : parent.getPath() + categoryId + "/");
        category.setName(request.getName().trim());
        category.setIcon(trimToNull(request.getIcon()));
        category.setSortOrder(defaultSort(request.getSortOrder()));
        category.setStatus(normalizeStatus(request.getStatus(), STATUS_OFFLINE));
        category.setIsDeleted(NOT_DELETED);
        LocalDateTime now = LocalDateTime.now();
        category.setCreateTime(now);
        category.setUpdateTime(now);
        categoryMapper.insert(category);
        return categoryMapper.selectById(categoryId);
    }

    @Transactional
    public ServiceCategory updateCategory(Long id, AdminCategoryRequest request) {
        ServiceCategory category = requireCategory(id);
        category.setName(request.getName().trim());
        category.setIcon(trimToNull(request.getIcon()));
        category.setSortOrder(defaultSort(request.getSortOrder()));
        category.setStatus(normalizeStatus(request.getStatus(), category.getStatus()));
        if (categoryMapper.update(category) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Category update failed");
        }
        return categoryMapper.selectById(id);
    }

    @Transactional
    public void publishCategory(Long id) {
        updateCategoryStatus(id, STATUS_ONLINE);
    }

    @Transactional
    public void unpublishCategory(Long id) {
        updateCategoryStatus(id, STATUS_OFFLINE);
    }

    @Transactional
    public ServiceItem createItem(AdminItemRequest request) {
        requireCategory(request.getCategoryId());
        ServiceItem item = new ServiceItem();
        Long itemId = idWorker.nextId();
        item.setId(itemId);
        fillItem(item, request, normalizeStatus(request.getStatus(), STATUS_OFFLINE));
        item.setAuditStatus("APPROVED");
        item.setPublishStatus(item.getStatus() == STATUS_ONLINE ? "PUBLISHED" : "OFFLINE");
        item.setVersion(1);
        item.setIsDeleted(NOT_DELETED);
        LocalDateTime now = LocalDateTime.now();
        item.setCreateTime(now);
        item.setUpdateTime(now);
        itemMapper.insert(item);
        return itemMapper.selectAdminById(itemId);
    }

    @Transactional
    public ServiceItem updateItem(Long id, AdminItemRequest request) {
        ServiceItem item = requireItem(id);
        requireCategory(request.getCategoryId());
        fillItem(item, request, normalizeStatus(request.getStatus(), item.getStatus()));
        if (itemMapper.update(item) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Item update failed");
        }
        return itemMapper.selectAdminById(id);
    }

    @Transactional
    public void publishItem(Long id) {
        updateItemStatus(id, STATUS_ONLINE);
    }

    @Transactional
    public void unpublishItem(Long id) {
        updateItemStatus(id, STATUS_OFFLINE);
    }

    @Transactional
    public ServiceSpec createSpec(AdminSpecRequest request) {
        requireItem(request.getServiceItemId());
        ServiceSpec spec = new ServiceSpec();
        Long specId = idWorker.nextId();
        spec.setId(specId);
        fillSpec(spec, request, normalizeStatus(request.getStatus(), STATUS_OFFLINE));
        spec.setIsDeleted(NOT_DELETED);
        LocalDateTime now = LocalDateTime.now();
        spec.setCreateTime(now);
        spec.setUpdateTime(now);
        specMapper.insert(spec);
        return specMapper.selectAdminById(specId);
    }

    @Transactional
    public ServiceSpec updateSpec(Long id, AdminSpecRequest request) {
        ServiceSpec spec = requireSpec(id);
        requireItem(request.getServiceItemId());
        fillSpec(spec, request, normalizeStatus(request.getStatus(), spec.getStatus()));
        if (specMapper.update(spec) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Spec update failed");
        }
        return specMapper.selectAdminById(id);
    }

    @Transactional
    public void publishSpec(Long id) {
        updateSpecStatus(id, STATUS_ONLINE);
    }

    @Transactional
    public void unpublishSpec(Long id) {
        updateSpecStatus(id, STATUS_OFFLINE);
    }

    private void updateCategoryStatus(Long id, int status) {
        requireCategory(id);
        if (categoryMapper.updateStatus(id, status) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Category status update failed");
        }
    }

    private void updateItemStatus(Long id, int status) {
        requireItem(id);
        if (itemMapper.updateStatus(id, status) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Item status update failed");
        }
    }

    private void updateSpecStatus(Long id, int status) {
        requireSpec(id);
        if (specMapper.updateStatus(id, status) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Spec status update failed");
        }
    }

    private void fillItem(ServiceItem item, AdminItemRequest request, int status) {
        item.setCategoryId(request.getCategoryId());
        item.setName(request.getName().trim());
        item.setDescription(trimToNull(request.getDescription()));
        item.setCoverImage(trimToNull(request.getCoverImage()));
        item.setSortOrder(defaultSort(request.getSortOrder()));
        item.setStatus(status);
    }

    private void fillSpec(ServiceSpec spec, AdminSpecRequest request, int status) {
        spec.setServiceItemId(request.getServiceItemId());
        spec.setName(request.getName().trim());
        spec.setPrice(request.getPrice());
        spec.setOriginalPrice(request.getOriginalPrice());
        spec.setDuration(request.getDuration());
        spec.setStatus(status);
    }

    private ServiceCategory resolveParent(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return null;
        }
        return requireCategory(parentId);
    }

    private ServiceCategory requireCategory(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "categoryId is required");
        }
        ServiceCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "Category not found");
        }
        return category;
    }

    private ServiceItem requireItem(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "itemId is required");
        }
        ServiceItem item = itemMapper.selectAdminById(id);
        if (item == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "Item not found");
        }
        return item;
    }

    private ServiceSpec requireSpec(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "specId is required");
        }
        ServiceSpec spec = specMapper.selectAdminById(id);
        if (spec == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "Spec not found");
        }
        return spec;
    }

    private int normalizeStatus(Integer status, Integer defaultStatus) {
        int value = status == null ? defaultStatus : status;
        if (value != STATUS_OFFLINE && value != STATUS_ONLINE) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "status must be 0 or 1");
        }
        return value;
    }

    private int defaultSort(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
