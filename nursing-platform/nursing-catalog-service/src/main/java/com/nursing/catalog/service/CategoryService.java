package com.nursing.catalog.service;

import com.nursing.catalog.dto.vo.CategoryTreeVO;
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

    public List<CategoryTreeVO> buildCategoryTree() {
        List<ServiceCategory> categories = serviceCategoryMapper.selectListVisible();
        Map<Long, CategoryTreeVO> nodeMap = new LinkedHashMap<>();

        for (ServiceCategory category : categories) {
            nodeMap.put(category.getId(), toTreeNode(category));
        }

        List<CategoryTreeVO> roots = new ArrayList<>();
        for (ServiceCategory category : categories) {
            CategoryTreeVO node = nodeMap.get(category.getId());
            Long parentId = category.getParentId();
            if (parentId == null || parentId == ROOT_PARENT_ID) {
                roots.add(node);
                continue;
            }

            CategoryTreeVO parent = nodeMap.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private CategoryTreeVO toTreeNode(ServiceCategory category) {
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setCategoryId(category.getId());
        vo.setParentId(category.getParentId());
        vo.setName(category.getName());
        vo.setIcon(category.getIcon());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        return vo;
    }
}
