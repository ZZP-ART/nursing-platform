package com.nursing.catalog.controller;

import com.nursing.catalog.dto.response.CategoryTreeResponse;
import com.nursing.catalog.service.CategoryService;
import com.nursing.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryTreeResponse>> listCategories() {
        return Result.success(categoryService.buildCategoryTree());
    }
}
