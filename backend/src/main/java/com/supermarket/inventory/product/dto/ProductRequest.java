package com.supermarket.inventory.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProductRequest {

    @NotBlank(message = "商品编号不能为空")
    private String productCode;

    @NotBlank(message = "商品名称不能为空")
    private String productName;

    @NotBlank(message = "商品分类不能为空")
    private String category;

    @NotNull(message = "进价不能为空")
    @DecimalMin(value = "0.00", message = "进价不能小于0")
    private BigDecimal purchasePrice;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.00", message = "售价不能小于0")
    private BigDecimal salePrice;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
