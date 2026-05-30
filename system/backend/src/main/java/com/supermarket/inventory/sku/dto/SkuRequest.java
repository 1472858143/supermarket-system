package com.supermarket.inventory.sku.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SkuRequest {

    @NotBlank(message = "SKU名称不能为空")
    private String skuName;

    @NotBlank(message = "规格不能为空")
    private String spec;

    private String barcode;

    @NotBlank(message = "基础单位不能为空")
    private String baseUnit;

    @NotNull(message = "进价不能为空")
    @DecimalMin(value = "0.00", message = "进价不能小于0")
    private BigDecimal purchasePrice;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.00", message = "售价不能小于0")
    private BigDecimal salePrice;

    private Integer status = 1;

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBaseUnit() {
        return baseUnit;
    }

    public void setBaseUnit(String baseUnit) {
        this.baseUnit = baseUnit;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
