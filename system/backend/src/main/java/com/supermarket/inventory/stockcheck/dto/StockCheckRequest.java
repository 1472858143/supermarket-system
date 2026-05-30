package com.supermarket.inventory.stockcheck.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockCheckRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "实际库存不能为空")
    @Min(value = 0, message = "实际库存不能小于0")
    private Integer actualQuantity;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }
}
