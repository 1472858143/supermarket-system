package com.supermarket.inventory.stockcheck.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.stockcheck.dto.StockCheckRequest;
import com.supermarket.inventory.stockcheck.service.StockCheckService;
import com.supermarket.inventory.stockcheck.vo.StockCheckVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ApiResponse<Void> create(@Valid @RequestBody StockCheckRequest request) {
        stockCheckService.create(request);
        return ApiResponse.success();
    }
}
