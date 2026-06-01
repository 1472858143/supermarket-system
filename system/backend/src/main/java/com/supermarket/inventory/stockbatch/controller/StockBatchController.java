package com.supermarket.inventory.stockbatch.controller;

import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.stockbatch.service.StockBatchService;
import com.supermarket.inventory.stockbatch.vo.StockBatchVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skus/{skuId}/stock-batches")
public class StockBatchController {

    private final StockBatchService stockBatchService;

    public StockBatchController(StockBatchService stockBatchService) {
        this.stockBatchService = stockBatchService;
    }

    @GetMapping
    public ApiResponse<List<StockBatchVO>> listBySkuId(@PathVariable Long skuId) {
        return ApiResponse.success(stockBatchService.listBySkuId(skuId));
    }
}
