package com.supermarket.inventory.stock.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.stock.dto.StockLimitUpdateRequest;
import com.supermarket.inventory.stock.service.StockService;
import com.supermarket.inventory.stock.vo.StockVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ApiResponse<PageResult<StockVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(stockService.list(keyword, page, pageSize));
    }

    @GetMapping("/{productId}")
    public ApiResponse<StockVO> getByProductId(@PathVariable Long productId) {
        return ApiResponse.success(stockService.getByProductId(productId));
    }

    @PutMapping("/{productId}/limit")
    @RequireRoles("ADMIN")
    public ApiResponse<StockVO> updateLimit(
            @PathVariable Long productId,
            @Valid @RequestBody StockLimitUpdateRequest request
    ) {
        return ApiResponse.success(stockService.updateLimit(productId, request));
    }
}
