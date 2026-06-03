package com.supermarket.inventory.supplier.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplierSku {

    private Long id;
    private Long supplierId;
    private Long skuId;
    private String supplierSkuCode;
    private String supplierSkuName;
    private String supplierSpec;
    private BigDecimal defaultPurchasePrice;
    private Integer minPurchaseQuantity;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
