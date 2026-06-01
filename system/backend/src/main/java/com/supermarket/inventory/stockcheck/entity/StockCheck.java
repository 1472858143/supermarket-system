package com.supermarket.inventory.stockcheck.entity;

import java.time.LocalDateTime;

public class StockCheck {

    private Long id;
    private String checkNo;
    private String name;
    private String scopeType;
    private Long categoryId;
    private String skuSelectType;
    private String status;
    private Integer totalSkuCount;
    private Integer totalBatchCount;
    private Integer totalDifference;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCheckNo() {
        return checkNo;
    }

    public void setCheckNo(String checkNo) {
        this.checkNo = checkNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getSkuSelectType() {
        return skuSelectType;
    }

    public void setSkuSelectType(String skuSelectType) {
        this.skuSelectType = skuSelectType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalSkuCount() {
        return totalSkuCount;
    }

    public void setTotalSkuCount(Integer totalSkuCount) {
        this.totalSkuCount = totalSkuCount;
    }

    public Integer getTotalBatchCount() {
        return totalBatchCount;
    }

    public void setTotalBatchCount(Integer totalBatchCount) {
        this.totalBatchCount = totalBatchCount;
    }

    public Integer getTotalDifference() {
        return totalDifference;
    }

    public void setTotalDifference(Integer totalDifference) {
        this.totalDifference = totalDifference;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }
}
