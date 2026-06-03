package com.supermarket.inventory.category.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.category.dto.CategoryRequest;
import com.supermarket.inventory.category.dto.CategorySortOrderRequest;
import com.supermarket.inventory.category.service.CategoryService;
import com.supermarket.inventory.category.vo.CategoryVO;
import com.supermarket.inventory.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryVO>> getCategoryTree() {
        return ApiResponse.success(categoryService.getCategoryTree());
    }

    @PostMapping
    @RequireRoles("ADMIN")
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @RequireRoles("ADMIN")
    public ApiResponse<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.update(id, request));
    }

    @PutMapping("/sort-order")
    @RequireRoles("ADMIN")
    public ApiResponse<Void> updateSortOrders(@Valid @RequestBody List<CategorySortOrderRequest> requests) {
        categoryService.updateSortOrders(requests);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @RequireRoles("ADMIN")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success();
    }
}
