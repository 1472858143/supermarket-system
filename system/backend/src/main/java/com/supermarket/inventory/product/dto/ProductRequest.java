package com.supermarket.inventory.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProductRequest {

    @NotBlank(message = "商品编号不能为空")
    private String productCode;

    @NotBlank(message = "商品名称不能为空")
    private String productName;

    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    @NotNull(message = "商品品牌不能为空")
    private Long brandId;

    private Integer status = 1;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
