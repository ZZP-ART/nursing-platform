package com.nursing.catalog.service;

import com.nursing.catalog.dto.vo.ItemDetailVO;
import com.nursing.catalog.dto.vo.ItemPageVO;
import com.nursing.catalog.dto.vo.SpecVO;
import com.nursing.catalog.entity.ServiceCategory;
import com.nursing.catalog.repository.ServiceCategoryMapper;
import com.nursing.catalog.repository.ServiceItemMapper;
import com.nursing.catalog.repository.ServiceSpecMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    void getItemPageReturnsPagedItemsWithSpecs() {
        ItemPageVO item = new ItemPageVO();
        item.setItemId(201L);
        item.setCategoryId(101L);
        item.setName("上门康复推拿");
        when(serviceCategoryMapper.selectVisibleById(101L)).thenReturn(new ServiceCategory());
        when(serviceItemMapper.selectPage(101L, 0, 20)).thenReturn(List.of(item));
        when(serviceItemMapper.count(101L)).thenReturn(1L);
        when(serviceSpecMapper.selectByItemIds(List.of(201L))).thenReturn(List.of(spec(201L, 301L)));

        PageResult<ItemPageVO> result = itemService.getItemPage(101L, 1, 20);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getSpecs()).hasSize(1);
        verify(serviceItemMapper).selectPage(101L, 0, 20);
        verify(serviceSpecMapper).selectByItemIds(List.of(201L));
        verify(serviceSpecMapper, never()).selectByItemId(201L);
    }

    @Test
    void searchItemsRejectsBlankKeyword() {
        assertThatThrownBy(() -> itemService.searchItems("   ", null, 1, 20))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(ApiCode.PARAM_ERROR);
                    assertThat(businessException.getMessage()).isEqualTo("关键词不能为空");
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
        ItemDetailVO detail = new ItemDetailVO();
        detail.setItemId(201L);
        detail.setName("上门康复推拿");
        detail.setCoverImage("/assets/default-service-cover.png");
        when(serviceItemMapper.selectById(201L)).thenReturn(detail);
        when(serviceSpecMapper.selectByItemId(201L)).thenReturn(List.of(spec(201L, 301L)));

        ItemDetailVO result = itemService.getItemDetail(201L);

        assertThat(result.getSpecs()).hasSize(1);
        assertThat(result.getImages()).containsExactly("/assets/default-service-cover.png");
    }

    private SpecVO spec(Long itemId, Long id) {
        SpecVO spec = new SpecVO();
        spec.setSpecId(id);
        spec.setServiceItemId(itemId);
        spec.setName("单次体验");
        spec.setPrice(BigDecimal.valueOf(198.00));
        spec.setOriginalPrice(BigDecimal.valueOf(298.00));
        spec.setDuration(60);
        spec.setStatus(1);
        return spec;
    }
}
