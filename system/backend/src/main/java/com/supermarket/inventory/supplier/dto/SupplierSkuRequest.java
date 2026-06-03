package com.supermarket.inventory.supplier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class SupplierSkuRequest {

    private Long skuId;

    @NotBlank(message = "供应商SKU编码不能为空")
    @Size(max = 80, message = "供应商SKU编码不能超过80个字符")
    private String supplierSkuCode;

    @NotBlank(message = "供应商SKU名称不能为空")
    @Size(max = 120, message = "供应商SKU名称不能超过120个字符")
    private String supplierSkuName;

    @Size(max = 120, message = "供应商规格不能超过120个字符")
    private String supplierSpec;

    @NotNull(message = "默认采购价不能为空")
    @DecimalMin(value = "0.00", message = "默认采购价不能小于0")
    @Digits(integer = 8, fraction = 2, message = "默认采购价最多保留2位小数")
    private BigDecimal defaultPurchasePrice;

    @NotNull(message = "最小采购数量不能为空")
    @Min(value = 1, message = "最小采购数量不能小于1")
    private Integer minPurchaseQuantity = 1;

    @Min(value = 0, message = "状态不正确")
    @Max(value = 1, message = "状态不正确")
    private Integer status = 1;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getSupplierSkuCode() {
        return supplierSkuCode;
    }

    public void setSupplierSkuCode(String supplierSkuCode) {
        this.supplierSkuCode = supplierSkuCode;
    }

    public String getSupplierSkuName() {
        return supplierSkuName;
    }

    public void setSupplierSkuName(String supplierSkuName) {
        this.supplierSkuName = supplierSkuName;
    }

    public String getSupplierSpec() {
        return supplierSpec;
    }

    public void setSupplierSpec(String supplierSpec) {
        this.supplierSpec = supplierSpec;
    }

    public BigDecimal getDefaultPurchasePrice() {
        return defaultPurchasePrice;
    }

    public void setDefaultPurchasePrice(BigDecimal defaultPurchasePrice) {
        this.defaultPurchasePrice = defaultPurchasePrice;
    }

    public Integer getMinPurchaseQuantity() {
        return minPurchaseQuantity;
    }

    public void setMinPurchaseQuantity(Integer minPurchaseQuantity) {
        this.minPurchaseQuantity = minPurchaseQuantity;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
