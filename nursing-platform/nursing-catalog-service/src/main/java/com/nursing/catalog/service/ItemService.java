package com.nursing.catalog.service;

import com.nursing.catalog.dto.vo.ItemDetailVO;
import com.nursing.catalog.dto.vo.ItemPageVO;
import com.nursing.catalog.dto.vo.SpecVO;
import com.nursing.catalog.repository.ServiceCategoryMapper;
import com.nursing.catalog.repository.ServiceItemMapper;
import com.nursing.catalog.repository.ServiceSpecMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ServiceItemMapper serviceItemMapper;
    private final ServiceSpecMapper serviceSpecMapper;
    private final ServiceCategoryMapper serviceCategoryMapper;

    public PageResult<ItemPageVO> getItemPage(Long categoryId, Integer page, Integer size) {
        validateCategory(categoryId);
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        int offset = (safePage - 1) * safeSize;

        List<ItemPageVO> items = serviceItemMapper.selectPage(categoryId, offset, safeSize);
        attachSpecs(items);
        long total = serviceItemMapper.count(categoryId);
        return PageResult.of(items, total, safePage, safeSize);
    }

    public PageResult<ItemPageVO> searchItems(String keyword, Long categoryId, Integer page, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "关键词不能为空");
        }
        validateCategory(categoryId);
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        int offset = (safePage - 1) * safeSize;
        String cleanKeyword = keyword.trim();

        List<ItemPageVO> items = serviceItemMapper.searchPage(cleanKeyword, categoryId, offset, safeSize);
        attachSpecs(items);
        long total = serviceItemMapper.searchCount(cleanKeyword, categoryId);
        return PageResult.of(items, total, safePage, safeSize);
    }

    public ItemDetailVO getItemDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "服务项目ID无效");
        }

        ItemDetailVO detail = serviceItemMapper.selectById(id);
        if (detail == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "服务项目不存在");
        }
        detail.setSpecs(serviceSpecMapper.selectByItemId(id));
        if (StringUtils.hasText(detail.getCoverImage()) && detail.getImages().isEmpty()) {
            detail.getImages().add(detail.getCoverImage());
        }
        return detail;
    }

    private void attachSpecs(List<ItemPageVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> itemIds = items.stream().map(ItemPageVO::getItemId).toList();
        Map<Long, List<SpecVO>> specsByItemId = serviceSpecMapper.selectByItemIds(itemIds).stream()
                .collect(Collectors.groupingBy(SpecVO::getServiceItemId));
        for (ItemPageVO item : items) {
            item.setSpecs(specsByItemId.getOrDefault(item.getItemId(), List.of()));
        }
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (categoryId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "分类ID无效");
        }
        if (serviceCategoryMapper.selectVisibleById(categoryId) == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "分类不存在");
        }
    }

    private int normalizePage(Integer page) {
        int value = page == null ? DEFAULT_PAGE : page;
        if (value < 1) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "页码必须大于等于1");
        }
        return value;
    }

    private int normalizeSize(Integer size) {
        int value = size == null ? DEFAULT_SIZE : size;
        if (value < 1 || value > MAX_SIZE) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "每页条数必须在1到50之间");
        }
        return value;
    }
}
