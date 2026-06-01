package com.supermarket.inventory.stockbatch.controller;

import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.stockbatch.dto.StockBatchDamageRequest;
import com.supermarket.inventory.stockbatch.service.StockBatchService;
import com.supermarket.inventory.stockbatch.vo.StockBatchVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PutMapping("/{batchId}/lock")
    public ApiResponse<Void> lock(@PathVariable Long skuId, @PathVariable Long batchId) {
        stockBatchService.lock(skuId, batchId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{batchId}/unlock")
    public ApiResponse<Void> unlock(@PathVariable Long skuId, @PathVariable Long batchId) {
        stockBatchService.unlock(skuId, batchId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{batchId}/damage")
    public ApiResponse<Void> damage(
            @PathVariable Long skuId,
            @PathVariable Long batchId,
            @Valid @RequestBody StockBatchDamageRequest request) {
        stockBatchService.damage(skuId, batchId, request);
        return ApiResponse.success(null);
    }

    @PutMapping("/{batchId}/close")
    public ApiResponse<Void> close(@PathVariable Long skuId, @PathVariable Long batchId) {
        stockBatchService.close(skuId, batchId);
        return ApiResponse.success(null);
    }
}
