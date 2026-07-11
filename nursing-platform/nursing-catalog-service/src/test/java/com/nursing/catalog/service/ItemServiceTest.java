package com.nursing.catalog.service;

import com.nursing.catalog.dto.response.CursorPageResponse;
import com.nursing.catalog.dto.response.ItemDetailResponse;
import com.nursing.catalog.dto.response.ItemListResponse;
import com.nursing.catalog.dto.response.ServiceSpecResponse;
import com.nursing.catalog.entity.ServiceCategory;
import com.nursing.catalog.repository.ServiceCategoryMapper;
import com.nursing.catalog.repository.ServiceItemMapper;
import com.nursing.catalog.repository.ServiceSpecMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {
    @Mock
    private ServiceItemMapper serviceItemMapper;
    @Mock
    private ServiceSpecMapper serviceSpecMapper;
    @Mock
    private ServiceCategoryMapper serviceCategoryMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void getItemPageReturnsFirstCursorPageWithSpecs() {
        ItemListResponse item = new ItemListResponse();
        item.setItemId(201L);
        item.setCategoryId(101L);
        item.setName("上门康复推拿");
        ServiceCategory category = new ServiceCategory();
        category.setId(101L);
        category.setPath("/101/");
        List<Long> categoryIds = List.of(101L, 111L, 112L, 113L);
        when(serviceCategoryMapper.selectVisibleById(101L)).thenReturn(category);
        when(serviceCategoryMapper.selectVisibleDescendantIds("/101/")).thenReturn(categoryIds);
        when(serviceItemMapper.selectPage(categoryIds, null, null, 21)).thenReturn(List.of(item));
        when(serviceSpecMapper.selectByItemIds(List.of(201L))).thenReturn(List.of(spec(201L, 301L)));

        CursorPageResponse<ItemListResponse> result = itemService.getItemPage(101L, null, 20);

        assertThat(result.list()).hasSize(1);
        assertThat(result.list().get(0).getSpecs()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        verify(serviceItemMapper).selectPage(categoryIds, null, null, 21);
        verify(serviceCategoryMapper).selectVisibleDescendantIds("/101/");
        verify(serviceSpecMapper).selectByItemIds(List.of(201L));
        verify(serviceSpecMapper, never()).selectByItemId(201L);
    }

    @Test
    void searchItemsRejectsBlankKeyword() {
        assertThatThrownBy(() -> itemService.searchItems("   ", null, null, 20))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(ApiCode.PARAM_ERROR);
                    assertThat(businessException.getMessage()).isEqualTo("关键词不能为空");
                });
    }

    @Test
    void getItemPageReturnsNextCursorFromLastReturnedItem() {
        ItemListResponse first = pageItem(201L, 1);
        ItemListResponse lookahead = pageItem(202L, 2);
        when(serviceItemMapper.selectPage(null, null, null, 2)).thenReturn(List.of(first, lookahead));
        when(serviceSpecMapper.selectByItemIds(List.of(201L))).thenReturn(List.of());

        CursorPageResponse<ItemListResponse> result = itemService.getItemPage(null, null, 1);

        assertThat(result.list()).containsExactly(first);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("MToyMDE");
        verify(serviceItemMapper).selectPage(null, null, null, 2);
        verify(serviceSpecMapper).selectByItemIds(List.of(201L));

        when(serviceItemMapper.selectPage(null, 1, 201L, 2)).thenReturn(List.of(lookahead));
        when(serviceSpecMapper.selectByItemIds(List.of(202L))).thenReturn(List.of());

        CursorPageResponse<ItemListResponse> nextPage = itemService.getItemPage(null, result.nextCursor(), 1);

        assertThat(nextPage.list()).containsExactly(lookahead);
        assertThat(nextPage.hasNext()).isFalse();
        assertThat(nextPage.nextCursor()).isNull();
        verify(serviceItemMapper).selectPage(null, 1, 201L, 2);
    }

    @Test
    void getItemPageRejectsMalformedCursor() {
        assertThatThrownBy(() -> itemService.getItemPage(null, "not-a-cursor", 20))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(ApiCode.PARAM_ERROR);
                    assertThat(businessException.getMessage()).isEqualTo("分页游标无效");
                });
    }

    @Test
    void getItemDetailRejectsMissingItem() {
        when(serviceItemMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> itemService.getItemDetail(999L))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(ApiCode.NOT_FOUND);
                    assertThat(businessException.getMessage()).isEqualTo("服务项目不存在");
                });
    }

    @Test
    void getItemDetailReturnsSpecsAndCoverImage() {
        ItemDetailResponse detail = new ItemDetailResponse();
        detail.setItemId(201L);
        detail.setName("上门康复推拿");
        detail.setCoverImage("/assets/default-service-cover.png");
        when(serviceItemMapper.selectById(201L)).thenReturn(detail);
        when(serviceSpecMapper.selectByItemId(201L)).thenReturn(List.of(spec(201L, 301L)));

        ItemDetailResponse result = itemService.getItemDetail(201L);

        assertThat(result.getSpecs()).hasSize(1);
        assertThat(result.getImages()).containsExactly("/assets/default-service-cover.png");
    }

    private ServiceSpecResponse spec(Long itemId, Long id) {
        ServiceSpecResponse spec = new ServiceSpecResponse();
        spec.setSpecId(id);
        spec.setServiceItemId(itemId);
        spec.setName("单次体验");
        spec.setPrice(BigDecimal.valueOf(198.00));
        spec.setOriginalPrice(BigDecimal.valueOf(298.00));
        spec.setDuration(60);
        spec.setStatus(1);
        return spec;
    }

    private ItemListResponse pageItem(Long itemId, int sortOrder) {
        ItemListResponse item = new ItemListResponse();
        item.setItemId(itemId);
        item.setSortOrder(sortOrder);
        return item;
    }
}
