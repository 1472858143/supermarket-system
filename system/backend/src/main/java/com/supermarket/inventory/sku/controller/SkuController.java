package com.supermarket.inventory.sku.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.sku.dto.SkuRequest;
import com.supermarket.inventory.sku.dto.UnitConversionRequest;
import com.supermarket.inventory.sku.service.SkuService;
import com.supermarket.inventory.sku.vo.SkuVO;
import com.supermarket.inventory.sku.vo.UnitConversionVO;
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
@RequestMapping("/api/products/{productId}/skus")
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    @GetMapping({"", "/"})
    public ApiResponse<List<SkuVO>> list(@PathVariable Long productId) {
        return ApiResponse.success(skuService.listByProductId(productId));
    }

    @PostMapping({"", "/"})
    @RequireRoles("ADMIN")
    public ApiResponse<SkuVO> create(@PathVariable Long productId, @Valid @RequestBody SkuRequest request) {
        return ApiResponse.success(skuService.create(productId, request));
    }

    @PutMapping("/{skuId}")
    @RequireRoles("ADMIN")
    public ApiResponse<SkuVO> update(
            @PathVariable Long productId,
            @PathVariable Long skuId,
            @Valid @RequestBody SkuRequest request
    ) {
        return ApiResponse.success(skuService.update(productId, skuId, request));
    }

    @DeleteMapping("/{skuId}")
    @RequireRoles("ADMIN")
    public ApiResponse<Void> delete(@PathVariable Long productId, @PathVariable Long skuId) {
        skuService.delete(productId, skuId);
        return ApiResponse.success();
    }

    @GetMapping("/{skuId}/units")
    public ApiResponse<List<UnitConversionVO>> listUnits(@PathVariable Long skuId) {
        return ApiResponse.success(skuService.listUnits(skuId));
    }

    @PostMapping("/{skuId}/units")
    @RequireRoles("ADMIN")
    public ApiResponse<UnitConversionVO> createUnit(
            @PathVariable Long skuId,
            @Valid @RequestBody UnitConversionRequest request
    ) {
        return ApiResponse.success(skuService.createUnit(skuId, request));
    }

    @PutMapping("/{skuId}/units/{unitId}")
    @RequireRoles("ADMIN")
    public ApiResponse<UnitConversionVO> updateUnit(
            @PathVariable Long skuId,
            @PathVariable Long unitId,
            @Valid @RequestBody UnitConversionRequest request
    ) {
        return ApiResponse.success(skuService.updateUnit(skuId, unitId, request));
    }

    @DeleteMapping("/{skuId}/units/{unitId}")
    @RequireRoles("ADMIN")
    public ApiResponse<Void> deleteUnit(@PathVariable Long skuId, @PathVariable Long unitId) {
        skuService.deleteUnit(skuId, unitId);
        return ApiResponse.success();
    }
}
