package com.nursing.catalog.service;

import com.nursing.catalog.dto.vo.CategoryTreeVO;
import com.nursing.catalog.entity.ServiceCategory;
import com.nursing.catalog.repository.ServiceCategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private ServiceCategoryMapper serviceCategoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void buildCategoryTreeReturnsRootsWithChildren() {
        ServiceCategory root = category(101L, 0L, "康复护理", 1);
        ServiceCategory child = category(111L, 101L, "术后康复", 1);
        ServiceCategory secondRoot = category(102L, 0L, "健康体检", 2);
        when(serviceCategoryMapper.selectListVisible()).thenReturn(List.of(root, child, secondRoot));

        List<CategoryTreeVO> result = categoryService.buildCategoryTree();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryId()).isEqualTo(101L);
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getCategoryId()).isEqualTo(111L);
        assertThat(result.get(1).getCategoryId()).isEqualTo(102L);
    }

    private ServiceCategory category(Long id, Long parentId, String name, Integer sortOrder) {
        ServiceCategory category = new ServiceCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setIcon("/assets/default-category-icon.png");
        category.setSortOrder(sortOrder);
        category.setStatus(1);
        return category;
    }
}
