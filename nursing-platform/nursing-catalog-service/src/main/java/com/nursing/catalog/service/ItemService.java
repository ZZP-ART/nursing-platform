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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ServiceItemMapper serviceItemMapper;
    private final ServiceSpecMapper serviceSpecMapper;
    private final ServiceCategoryMapper serviceCategoryMapper;

    public CursorPageResponse<ItemListResponse> getItemPage(Long categoryId, String cursor, Integer size) {
        List<Long> categoryIds = resolveVisibleCategoryIds(categoryId);
        int safeSize = normalizeSize(size);
        CursorPosition cursorPosition = decodeCursor(cursor);

        List<ItemListResponse> items = serviceItemMapper.selectPage(categoryIds,
                cursorPosition == null ? null : cursorPosition.sortOrder(),
                cursorPosition == null ? null : cursorPosition.itemId(), safeSize + 1);
        return toCursorPage(items, safeSize);
    }

    public CursorPageResponse<ItemListResponse> searchItems(String keyword, Long categoryId, String cursor, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "关键词不能为空");
        }
        List<Long> categoryIds = resolveVisibleCategoryIds(categoryId);
        int safeSize = normalizeSize(size);
        CursorPosition cursorPosition = decodeCursor(cursor);
        String cleanKeyword = keyword.trim();

        List<ItemListResponse> items = serviceItemMapper.searchPage(cleanKeyword, categoryIds,
                cursorPosition == null ? null : cursorPosition.sortOrder(),
                cursorPosition == null ? null : cursorPosition.itemId(), safeSize + 1);
        return toCursorPage(items, safeSize);
    }

    public ItemDetailResponse getItemDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "服务项目ID无效");
        }

        ItemDetailResponse detail = serviceItemMapper.selectById(id);
        if (detail == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "服务项目不存在");
        }
        detail.setSpecs(serviceSpecMapper.selectByItemId(id));
        if (StringUtils.hasText(detail.getCoverImage()) && detail.getImages().isEmpty()) {
            detail.getImages().add(detail.getCoverImage());
        }
        return detail;
    }

    private void attachSpecs(List<ItemListResponse> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> itemIds = items.stream().map(ItemListResponse::getItemId).toList();
        Map<Long, List<ServiceSpecResponse>> specsByItemId = serviceSpecMapper.selectByItemIds(itemIds).stream()
                .collect(Collectors.groupingBy(ServiceSpecResponse::getServiceItemId));
        for (ItemListResponse item : items) {
            item.setSpecs(specsByItemId.getOrDefault(item.getItemId(), List.of()));
        }
    }

    private CursorPageResponse<ItemListResponse> toCursorPage(List<ItemListResponse> queriedItems, int size) {
        List<ItemListResponse> items = queriedItems == null ? new ArrayList<>() : new ArrayList<>(queriedItems);
        boolean hasNext = items.size() > size;
        if (hasNext) {
            items.removeLast();
        }
        attachSpecs(items);
        String nextCursor = hasNext ? encodeCursor(items.getLast()) : null;
        return new CursorPageResponse<>(items, size, hasNext, nextCursor);
    }

    private CursorPosition decodeCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = value.split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("invalid cursor");
            }
            int sortOrder = Integer.parseInt(parts[0]);
            long itemId = Long.parseLong(parts[1]);
            if (itemId <= 0) {
                throw new IllegalArgumentException("invalid item id");
            }
            return new CursorPosition(sortOrder, itemId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "分页游标无效");
        }
    }

    private String encodeCursor(ItemListResponse item) {
        String value = item.getSortOrder() + ":" + item.getItemId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private List<Long> resolveVisibleCategoryIds(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        if (categoryId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "分类ID无效");
        }
        ServiceCategory category = serviceCategoryMapper.selectVisibleById(categoryId);
        if (category == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "分类不存在");
        }
        List<Long> descendantIds = serviceCategoryMapper.selectVisibleDescendantIds(category.getPath());
        if (descendantIds == null || descendantIds.isEmpty()) {
            throw new BusinessException(ApiCode.NOT_FOUND, "分类不存在");
        }
        return descendantIds;
    }

    private int normalizeSize(Integer size) {
        int value = size == null ? DEFAULT_SIZE : size;
        if (value < 1 || value > MAX_SIZE) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "每页条数必须在1到50之间");
        }
        return value;
    }

    private record CursorPosition(int sortOrder, long itemId) {
    }
}
