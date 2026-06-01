package com.supermarket.inventory.stockcheck.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StockCheckItemVO {

    private Long id;
    private Long stockCheckId;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String productCode;
    private String productName;
    private Long stockBatchId;
    private String batchNo;
    private String batchStatus;
    private LocalDate expireDate;
    private Integer systemQuantity;
    private Integer actualQuantity;
    private Integer difference;
    private String status;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStockCheckId() {
        return stockCheckId;
    }

    public void setStockCheckId(Long stockCheckId) {
        this.stockCheckId = stockCheckId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
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

    public Long getStockBatchId() {
        return stockBatchId;
    }

    public void setStockBatchId(Long stockBatchId) {
        this.stockBatchId = stockBatchId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    public Integer getSystemQuantity() {
        return systemQuantity;
    }

    public void setSystemQuantity(Integer systemQuantity) {
        this.systemQuantity = systemQuantity;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public Integer getDifference() {
        return difference;
    }

    public void setDifference(Integer difference) {
        this.difference = difference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
