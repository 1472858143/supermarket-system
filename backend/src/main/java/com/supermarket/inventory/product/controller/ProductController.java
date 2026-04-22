package com.supermarket.inventory.product.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.product.dto.ProductRequest;
import com.supermarket.inventory.product.service.ProductService;
import com.supermarket.inventory.product.vo.ProductVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResult<ProductVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(productService.list(keyword, page, pageSize));
    }

    @PostMapping
    @RequireRoles("ADMIN")
    public ApiResponse<ProductVO> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.create(request));
    }

    @PutMapping("/{id}")
    @RequireRoles("ADMIN")
    public ApiResponse<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRoles("ADMIN")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success();
    }
}
