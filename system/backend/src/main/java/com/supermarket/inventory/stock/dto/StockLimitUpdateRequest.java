package com.supermarket.inventory.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockLimitUpdateRequest {

    @NotNull(message = "库存下限不能为空")
    @Min(value = 0, message = "库存下限不能小于0")
    private Integer minStock;

    @NotNull(message = "库存上限不能为空")
    @Min(value = 0, message = "库存上限不能小于0")
    private Integer maxStock;

    public Integer getMinStock() {
        return minStock;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public Integer getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(Integer maxStock) {
        this.maxStock = maxStock;
    }
}
