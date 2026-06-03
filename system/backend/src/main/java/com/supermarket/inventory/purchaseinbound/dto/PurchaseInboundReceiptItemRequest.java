package com.supermarket.inventory.purchaseinbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PurchaseInboundReceiptItemRequest {

    @NotNull(message = "采购计划明细ID不能为空")
    private Long purchaseInboundItemId;

    @NotEmpty(message = "实际入库批次不能为空")
    @Valid
    private List<@NotNull(message = "实际入库批次不能为空") @Valid PurchaseInboundReceiptBatchRequest> batches;

    public Long getPurchaseInboundItemId() {
        return purchaseInboundItemId;
    }

    public void setPurchaseInboundItemId(Long purchaseInboundItemId) {
        this.purchaseInboundItemId = purchaseInboundItemId;
    }

    public List<PurchaseInboundReceiptBatchRequest> getBatches() {
        return batches;
    }

    public void setBatches(List<PurchaseInboundReceiptBatchRequest> batches) {
        this.batches = batches;
    }
}
