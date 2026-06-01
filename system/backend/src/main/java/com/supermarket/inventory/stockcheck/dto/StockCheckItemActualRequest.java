package com.supermarket.inventory.stockcheck.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockCheckItemActualRequest {

    @NotNull(message = "盘点明细ID不能为空")
    private Long itemId;

    @NotNull(message = "实际数量不能为空")
    @Min(value = 0, message = "实际数量不能小于0")
    private Integer actualQuantity;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }
}
