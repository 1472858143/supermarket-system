package com.supermarket.inventory.supplier.vo;

import com.supermarket.inventory.sku.vo.UnitConversionVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SupplierSkuVO {

    private Long id;
    private Long supplierId;
    private Long skuId;
    private Long productId;
    private String skuCode;
    private String skuName;
    private String productCode;
    private String productName;
    private String spec;
    private String baseUnit;
    private String supplierSkuCode;
    private String supplierSkuName;
    private String supplierSpec;
    private BigDecimal defaultPurchasePrice;
    private Integer minPurchaseQuantity;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<UnitConversionVO> units;

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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getBaseUnit() {
        return baseUnit;
    }

    public void setBaseUnit(String baseUnit) {
        this.baseUnit = baseUnit;
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

    public List<UnitConversionVO> getUnits() {
        return units;
    }

    public void setUnits(List<UnitConversionVO> units) {
        this.units = units;
    }
}
