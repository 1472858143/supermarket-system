package com.supermarket.inventory.sku.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UnitConversionRequest {

    @NotBlank(message = "单位名称不能为空")
    private String unitName;

    @NotNull(message = "换算比例不能为空")
    @Min(value = 1, message = "换算比例不能小于1")
    private Integer conversionRate;

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public Integer getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Integer conversionRate) {
        this.conversionRate = conversionRate;
    }
}
