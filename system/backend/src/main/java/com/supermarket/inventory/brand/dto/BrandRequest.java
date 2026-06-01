package com.supermarket.inventory.brand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BrandRequest {

    @NotBlank(message = "品牌名称不能为空")
    @Size(max = 100, message = "品牌名称不能超过100个字符")
    private String brandName;

    private Integer status = 1;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
