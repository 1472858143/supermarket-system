package com.supermarket.inventory.supplier.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.supplier.dto.SupplierSkuRequest;
import com.supermarket.inventory.supplier.service.SupplierSkuService;
import com.supermarket.inventory.supplier.vo.SupplierSkuVO;
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
@RequestMapping("/api/suppliers/{supplierId}/skus")
public class SupplierSkuController {

    private final SupplierSkuService supplierSkuService;

    public SupplierSkuController(SupplierSkuService supplierSkuService) {
        this.supplierSkuService = supplierSkuService;
    }

    @GetMapping
    public ApiResponse<List<SupplierSkuVO>> list(@PathVariable Long supplierId) {
        return ApiResponse.success(supplierSkuService.list(supplierId));
    }

    @GetMapping("/enabled")
    public ApiResponse<List<SupplierSkuVO>> listEnabled(@PathVariable Long supplierId) {
        return ApiResponse.success(supplierSkuService.listEnabled(supplierId));
    }

    @PostMapping
    @RequireRoles("ADMIN")
    public ApiResponse<SupplierSkuVO> create(
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierSkuRequest request
    ) {
        return ApiResponse.success(supplierSkuService.create(supplierId, request));
    }

    @PutMapping("/{bindingId}")
    @RequireRoles("ADMIN")
    public ApiResponse<SupplierSkuVO> update(
            @PathVariable Long supplierId,
            @PathVariable Long bindingId,
            @Valid @RequestBody SupplierSkuRequest request
    ) {
        return ApiResponse.success(supplierSkuService.update(supplierId, bindingId, request));
    }

    @DeleteMapping("/{bindingId}")
    @RequireRoles("ADMIN")
    public ApiResponse<Void> delete(@PathVariable Long supplierId, @PathVariable Long bindingId) {
        supplierSkuService.delete(supplierId, bindingId);
        return ApiResponse.success();
    }
}
