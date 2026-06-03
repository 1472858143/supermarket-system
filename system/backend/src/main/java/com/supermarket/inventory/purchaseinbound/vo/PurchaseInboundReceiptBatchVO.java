package com.supermarket.inventory.purchaseinbound.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PurchaseInboundReceiptBatchVO {

    private Long id;
    private Long receiptId;
    private Long purchaseInboundId;
    private Long purchaseInboundItemId;
    private Long skuId;
    private Integer quantity;
    private Integer baseQuantity;
    private LocalDate productionDate;
    private Integer shelfLifeDays;
    private LocalDate expireDate;
    private BigDecimal purchasePriceSnapshot;
    private BigDecimal costPriceSnapshot;
    private BigDecimal amount;
    private String supplierSkuCodeSnapshot;
    private String supplierSkuNameSnapshot;
    private String supplierSpecSnapshot;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
    }

    public Long getPurchaseInboundId() {
        return purchaseInboundId;
    }

    public void setPurchaseInboundId(Long purchaseInboundId) {
        this.purchaseInboundId = purchaseInboundId;
    }

    public Long getPurchaseInboundItemId() {
        return purchaseInboundItemId;
    }

    public void setPurchaseInboundItemId(Long purchaseInboundItemId) {
        this.purchaseInboundItemId = purchaseInboundItemId;
    }

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

    public Integer getBaseQuantity() {
        return baseQuantity;
    }

    public void setBaseQuantity(Integer baseQuantity) {
        this.baseQuantity = baseQuantity;
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

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    public BigDecimal getPurchasePriceSnapshot() {
        return purchasePriceSnapshot;
    }

    public void setPurchasePriceSnapshot(BigDecimal purchasePriceSnapshot) {
        this.purchasePriceSnapshot = purchasePriceSnapshot;
    }

    public BigDecimal getCostPriceSnapshot() {
        return costPriceSnapshot;
    }

    public void setCostPriceSnapshot(BigDecimal costPriceSnapshot) {
        this.costPriceSnapshot = costPriceSnapshot;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSupplierSkuCodeSnapshot() {
        return supplierSkuCodeSnapshot;
    }

    public void setSupplierSkuCodeSnapshot(String supplierSkuCodeSnapshot) {
        this.supplierSkuCodeSnapshot = supplierSkuCodeSnapshot;
    }

    public String getSupplierSkuNameSnapshot() {
        return supplierSkuNameSnapshot;
    }

    public void setSupplierSkuNameSnapshot(String supplierSkuNameSnapshot) {
        this.supplierSkuNameSnapshot = supplierSkuNameSnapshot;
    }

    public String getSupplierSpecSnapshot() {
        return supplierSpecSnapshot;
    }

    public void setSupplierSpecSnapshot(String supplierSpecSnapshot) {
        this.supplierSpecSnapshot = supplierSpecSnapshot;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
