package com.supermarket.inventory.purchaseinbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PurchaseInboundReceiptRequest {

    @NotEmpty(message = "实际入库明细不能为空")
    @Valid
    private List<@NotNull(message = "实际入库明细不能为空") @Valid PurchaseInboundReceiptItemRequest> items;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;

    public List<PurchaseInboundReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PurchaseInboundReceiptItemRequest> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
