package com.nursing.catalog.service;

import com.nursing.catalog.dto.response.CategoryTreeResponse;
import com.nursing.catalog.entity.ServiceCategory;
import com.nursing.catalog.repository.ServiceCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private static final long ROOT_PARENT_ID = 0L;

    private final ServiceCategoryMapper serviceCategoryMapper;

    public List<CategoryTreeResponse> buildCategoryTree() {
        List<ServiceCategory> categories = serviceCategoryMapper.selectListVisible();
        Map<Long, CategoryTreeResponse> nodeMap = new LinkedHashMap<>();

        for (ServiceCategory category : categories) {
            nodeMap.put(category.getId(), toTreeNode(category));
        }

        List<CategoryTreeResponse> roots = new ArrayList<>();
        for (ServiceCategory category : categories) {
            CategoryTreeResponse node = nodeMap.get(category.getId());
            Long parentId = category.getParentId();
            if (parentId == null || parentId == ROOT_PARENT_ID) {
                roots.add(node);
                continue;
            }

            CategoryTreeResponse parent = nodeMap.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private CategoryTreeResponse toTreeNode(ServiceCategory category) {
        CategoryTreeResponse response = new CategoryTreeResponse();
        response.setCategoryId(category.getId());
        response.setParentId(category.getParentId());
        response.setName(category.getName());
        response.setIcon(category.getIcon());
        response.setSortOrder(category.getSortOrder());
        response.setStatus(category.getStatus());
        return response;
    }
}
