package com.supermarket.inventory.purchaseinbound.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundRequest;
import com.supermarket.inventory.purchaseinbound.service.PurchaseInboundService;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-inbounds")
public class PurchaseInboundController {

    private final PurchaseInboundService purchaseInboundService;

    public PurchaseInboundController(PurchaseInboundService purchaseInboundService) {
        this.purchaseInboundService = purchaseInboundService;
    }

    @GetMapping
    public ApiResponse<PageResult<PurchaseInboundVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(purchaseInboundService.list(keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseInboundVO> getById(@PathVariable Long id) {
        return ApiResponse.success(purchaseInboundService.getById(id));
    }

    @PostMapping
    @RequireRoles({"ADMIN"})
    public ApiResponse<PurchaseInboundVO> create(@Valid @RequestBody PurchaseInboundRequest request) {
        return ApiResponse.success(purchaseInboundService.create(request));
    }
}
