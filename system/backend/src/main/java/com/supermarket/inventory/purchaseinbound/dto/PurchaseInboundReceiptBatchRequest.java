package com.supermarket.inventory.purchaseinbound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class PurchaseInboundReceiptBatchRequest {

    @NotNull(message = "实际入库数量不能为空")
    @Min(value = 1, message = "实际入库数量必须大于0")
    private Integer quantity;

    @NotNull(message = "生产日期不能为空")
    private LocalDate productionDate;

    @NotNull(message = "保质期天数不能为空")
    @Min(value = 1, message = "保质期天数必须大于0")
    private Integer shelfLifeDays;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }
}
