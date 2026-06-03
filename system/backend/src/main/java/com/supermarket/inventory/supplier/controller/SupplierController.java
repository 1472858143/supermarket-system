package com.supermarket.inventory.supplier.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.supplier.dto.SupplierRequest;
import com.supermarket.inventory.supplier.service.SupplierService;
import com.supermarket.inventory.supplier.vo.SupplierVO;
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
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ApiResponse<PageResult<SupplierVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(supplierService.list(keyword, page, pageSize));
    }

    @PostMapping
    @RequireRoles("ADMIN")
    public ApiResponse<SupplierVO> create(@Valid @RequestBody SupplierRequest request) {
        return ApiResponse.success(supplierService.create(request));
    }

    @PutMapping("/{id}")
    @RequireRoles("ADMIN")
    public ApiResponse<SupplierVO> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return ApiResponse.success(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRoles("ADMIN")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.success();
    }
}
