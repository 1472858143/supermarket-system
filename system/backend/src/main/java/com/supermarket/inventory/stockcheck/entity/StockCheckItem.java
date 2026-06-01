package com.supermarket.inventory.stockcheck.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StockCheckItem {

    private Long id;
    private Long stockCheckId;
    private Long skuId;
    private Long stockBatchId;
    private String batchNo;
    private Integer systemQuantity;
    private Integer actualQuantity;
    private Integer difference;
    private String status;
    private LocalDate expireDate;
    private LocalDateTime createTime;
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
