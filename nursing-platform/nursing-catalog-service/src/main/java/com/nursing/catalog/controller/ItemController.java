package com.nursing.catalog.controller;

import com.nursing.catalog.dto.response.CursorPageResponse;
import com.nursing.catalog.dto.response.ItemDetailResponse;
import com.nursing.catalog.dto.response.ItemListResponse;
import com.nursing.catalog.service.ItemService;
import com.nursing.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping
    public Result<CursorPageResponse<ItemListResponse>> listOrSearchItems(@RequestParam(required = false) Long categoryId,
                                                                  @RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) String cursor,
                                                                  @RequestParam(required = false) Integer size) {
        if (keyword != null) {
            return Result.success(itemService.searchItems(keyword, categoryId, cursor, size));
        }
        return Result.success(itemService.getItemPage(categoryId, cursor, size));
    }

    @GetMapping("/{id}")
    public Result<ItemDetailResponse> getItemDetail(@PathVariable Long id) {
        return Result.success(itemService.getItemDetail(id));
    }
}
