package com.supermarket.inventory.stockbatch.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StockBatchDamageRequest {

    @NotNull(message = "报损数量不能为空")
    @Min(value = 1, message = "报损数量必须大于0")
    private Integer quantity;

    @NotBlank(message = "报损原因不能为空")
    @Size(max = 50, message = "报损原因不能超过50个字符")
    private String reason;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
