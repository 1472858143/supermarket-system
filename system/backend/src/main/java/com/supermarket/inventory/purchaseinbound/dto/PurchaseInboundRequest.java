package com.supermarket.inventory.purchaseinbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PurchaseInboundRequest {

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotEmpty(message = "采购计划明细不能为空")
    @Valid
    private List<PurchaseInboundItemRequest> items;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public List<PurchaseInboundItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PurchaseInboundItemRequest> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
