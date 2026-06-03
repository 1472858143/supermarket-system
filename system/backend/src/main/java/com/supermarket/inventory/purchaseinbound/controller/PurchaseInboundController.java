package com.supermarket.inventory.purchaseinbound.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundDecisionRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundReceiptRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundRequest;
import com.supermarket.inventory.purchaseinbound.service.PurchaseInboundReceiptService;
import com.supermarket.inventory.purchaseinbound.service.PurchaseInboundService;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-inbounds")
public class PurchaseInboundController {

    private final PurchaseInboundService purchaseInboundService;
    private final PurchaseInboundReceiptService purchaseInboundReceiptService;

    public PurchaseInboundController(
            PurchaseInboundService purchaseInboundService,
            PurchaseInboundReceiptService purchaseInboundReceiptService
    ) {
        this.purchaseInboundService = purchaseInboundService;
        this.purchaseInboundReceiptService = purchaseInboundReceiptService;
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

    @PostMapping("/drafts")
    @RequireRoles({"ADMIN"})
    public ApiResponse<PurchaseInboundVO> createDraft(@Valid @RequestBody PurchaseInboundRequest request) {
        return ApiResponse.success(purchaseInboundService.createDraft(request));
    }

    @PutMapping("/{id}/plan")
    @RequireRoles({"ADMIN"})
    public ApiResponse<PurchaseInboundVO> updatePlan(@PathVariable Long id, @Valid @RequestBody PurchaseInboundRequest request) {
        return ApiResponse.success(purchaseInboundService.updatePlan(id, request));
    }

    @PostMapping("/{id}/submit")
    @RequireRoles({"ADMIN"})
    public ApiResponse<PurchaseInboundVO> submit(@PathVariable Long id) {
        return ApiResponse.success(purchaseInboundService.submit(id));
    }

    @PostMapping("/{id}/approve")
    @RequireRoles({"ADMIN"})
    public ApiResponse<PurchaseInboundVO> approve(@PathVariable Long id) {
        return ApiResponse.success(purchaseInboundService.approve(id));
    }

    @PostMapping("/{id}/return")
    @RequireRoles({"ADMIN"})
    public ApiResponse<PurchaseInboundVO> returnForModification(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseInboundDecisionRequest request
    ) {
        return ApiResponse.success(purchaseInboundService.returnForModification(id, request));
    }

    @PostMapping("/{id}/cancel")
    @RequireRoles({"ADMIN"})
    public ApiResponse<PurchaseInboundVO> cancel(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseInboundDecisionRequest request
    ) {
        return ApiResponse.success(purchaseInboundService.cancel(id, request));
    }

    @PostMapping("/{id}/close")
    @RequireRoles({"ADMIN"})
    public ApiResponse<PurchaseInboundVO> close(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseInboundDecisionRequest request
    ) {
        return ApiResponse.success(purchaseInboundService.close(id, request));
    }

    @PostMapping("/{id}/receipts")
    @RequireRoles({"ADMIN"})
    public ApiResponse<PurchaseInboundVO> receive(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseInboundReceiptRequest request
    ) {
        purchaseInboundReceiptService.receive(id, request);
        return ApiResponse.success(purchaseInboundService.getById(id));
    }
}
