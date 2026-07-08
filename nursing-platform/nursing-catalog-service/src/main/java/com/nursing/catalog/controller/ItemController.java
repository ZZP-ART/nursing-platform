package com.nursing.catalog.controller;

import com.nursing.catalog.dto.vo.ItemDetailVO;
import com.nursing.catalog.dto.vo.ItemPageVO;
import com.nursing.catalog.service.ItemService;
import com.nursing.common.result.PageResult;
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
    public Result<PageResult<ItemPageVO>> listOrSearchItems(@RequestParam(required = false) Long categoryId,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer size) {
        if (keyword != null) {
            return Result.success(itemService.searchItems(keyword, categoryId, page, size));
        }
        return Result.success(itemService.getItemPage(categoryId, page, size));
    }

    @GetMapping("/{id}")
    public Result<ItemDetailVO> getItemDetail(@PathVariable Long id) {
        return Result.success(itemService.getItemDetail(id));
    }
}
