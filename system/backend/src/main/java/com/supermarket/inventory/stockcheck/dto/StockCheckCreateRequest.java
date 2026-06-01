package com.supermarket.inventory.stockcheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StockCheckCreateRequest {

    @NotBlank(message = "盘点名称不能为空")
    private String name;

    @NotNull(message = "盘点范围不能为空")
    private String scopeType;

    private Long categoryId;

    private String skuSelectType = "ALL";

    private List<Long> skuIds = new ArrayList<>();

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

    public List<Long> getSkuIds() {
        return skuIds;
    }

    public void setSkuIds(List<Long> skuIds) {
        this.skuIds = skuIds == null ? new ArrayList<>() : skuIds;
    }
}
