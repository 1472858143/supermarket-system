package com.supermarket.inventory.stockbatch.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StockBatch {

    private Long id;
    private String batchNo;
    private Long skuId;
    private Long purchaseInboundItemId;
    private Long purchaseInboundReceiptBatchId;
    private Integer initialQuantity;
    private Integer quantity;
    private String status = "AVAILABLE";
    private BigDecimal purchasePrice;
    private BigDecimal costPrice;
    private LocalDate productionDate;
    private Integer shelfLifeDays;
    private LocalDate expireDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
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

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
