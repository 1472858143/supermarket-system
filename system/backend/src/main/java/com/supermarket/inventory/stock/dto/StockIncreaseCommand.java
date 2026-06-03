package com.supermarket.inventory.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StockIncreaseCommand {

    private Long skuId;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal costPrice;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private Long purchaseInboundId;
    private Long purchaseInboundItemId;
    private Long purchaseInboundReceiptBatchId;
    private String sourceType;
    private Long sourceId;

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

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
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

    public Long getPurchaseInboundReceiptBatchId() {
        return purchaseInboundReceiptBatchId;
    }

    public void setPurchaseInboundReceiptBatchId(Long purchaseInboundReceiptBatchId) {
        this.purchaseInboundReceiptBatchId = purchaseInboundReceiptBatchId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }
}
