package com.supermarket.inventory.stock.entity;

import java.time.LocalDateTime;

public class Stock {

    private Long id;
    private Long skuId;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer lockedQuantity;
    private Integer expiredQuantity;
    private Integer minStock;
    private Integer maxStock;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return totalQuantity;
    }

    public void setQuantity(Integer quantity) {
        this.totalQuantity = quantity;
        if (this.availableQuantity == null) {
            this.availableQuantity = quantity;
        }
        if (this.lockedQuantity == null) {
            this.lockedQuantity = 0;
        }
        if (this.expiredQuantity == null) {
            this.expiredQuantity = 0;
        }
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getLockedQuantity() {
        return lockedQuantity;
    }

    public void setLockedQuantity(Integer lockedQuantity) {
        this.lockedQuantity = lockedQuantity;
    }

    public Integer getExpiredQuantity() {
        return expiredQuantity;
    }

    public void setExpiredQuantity(Integer expiredQuantity) {
        this.expiredQuantity = expiredQuantity;
    }

    public Integer getMinStock() {
        return minStock;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public Integer getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(Integer maxStock) {
        this.maxStock = maxStock;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
