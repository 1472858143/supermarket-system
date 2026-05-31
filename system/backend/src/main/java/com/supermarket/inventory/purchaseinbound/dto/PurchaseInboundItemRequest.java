package com.supermarket.inventory.purchaseinbound.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PurchaseInboundItemRequest {

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "采购入库数量不能为空")
    @Min(value = 1, message = "采购入库数量必须大于0")
    private Integer quantity;

    private String unit;

    @NotNull(message = "采购单价不能为空")
    @DecimalMin(value = "0.00", message = "采购单价不能小于0")
    @Digits(integer = 8, fraction = 2, message = "采购单价最多8位整数和2位小数")
    private BigDecimal purchasePrice;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }
}
