package com.supermarket.inventory.stockcheck.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.stockcheck.dto.StockCheckCreateRequest;
import com.supermarket.inventory.stockcheck.dto.StockCheckItemsUpdateRequest;
import com.supermarket.inventory.stockcheck.service.StockCheckService;
import com.supermarket.inventory.stockcheck.vo.StockCheckVO;
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
@RequestMapping("/api/stockchecks")
public class StockCheckController {

    private final StockCheckService stockCheckService;

    public StockCheckController(StockCheckService stockCheckService) {
        this.stockCheckService = stockCheckService;
    }

    @GetMapping
    public ApiResponse<PageResult<StockCheckVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(stockCheckService.list(keyword, page, pageSize));
    }

    @PostMapping
    @RequireRoles("ADMIN")
    public ApiResponse<StockCheckVO> create(@Valid @RequestBody StockCheckCreateRequest request) {
        return ApiResponse.success(stockCheckService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<StockCheckVO> detail(@PathVariable Long id) {
        return ApiResponse.success(stockCheckService.detail(id));
    }

    @PutMapping("/{id}/items")
    @RequireRoles("ADMIN")
    public ApiResponse<StockCheckVO> updateItems(
            @PathVariable Long id,
            @Valid @RequestBody StockCheckItemsUpdateRequest request
    ) {
        return ApiResponse.success(stockCheckService.updateItems(id, request));
    }

    @PostMapping("/{id}/complete")
    @RequireRoles("ADMIN")
    public ApiResponse<StockCheckVO> complete(@PathVariable Long id) {
        return ApiResponse.success(stockCheckService.complete(id));
    }
}
