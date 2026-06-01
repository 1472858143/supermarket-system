package com.supermarket.inventory.stockcheck.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class StockCheckItemsUpdateRequest {

    @Valid
    @NotEmpty(message = "盘点明细不能为空")
    private List<StockCheckItemActualRequest> items = new ArrayList<>();

    public List<StockCheckItemActualRequest> getItems() {
        return items;
    }

    public void setItems(List<StockCheckItemActualRequest> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}
