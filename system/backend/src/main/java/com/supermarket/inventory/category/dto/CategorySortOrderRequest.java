package com.supermarket.inventory.category.dto;

import jakarta.validation.constraints.NotNull;

public class CategorySortOrderRequest {

    @NotNull(message = "分类ID不能为空")
    private Long id;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
