package com.supermarket.inventory.brand.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.brand.dto.BrandRequest;
import com.supermarket.inventory.brand.service.BrandService;
import com.supermarket.inventory.brand.vo.BrandVO;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
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

import java.util.List;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public ApiResponse<PageResult<BrandVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(brandService.list(keyword, status, page, pageSize));
    }

    @GetMapping("/options")
    public ApiResponse<List<BrandVO>> options() {
        return ApiResponse.success(brandService.options());
    }

    @PostMapping
    @RequireRoles("ADMIN")
    public ApiResponse<BrandVO> create(@Valid @RequestBody BrandRequest request) {
        return ApiResponse.success(brandService.create(request));
    }

    @PutMapping("/{id}")
    @RequireRoles("ADMIN")
    public ApiResponse<BrandVO> update(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return ApiResponse.success(brandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRoles("ADMIN")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ApiResponse.success();
    }
}
