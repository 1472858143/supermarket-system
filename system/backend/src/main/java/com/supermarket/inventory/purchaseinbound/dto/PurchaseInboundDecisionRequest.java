package com.supermarket.inventory.purchaseinbound.dto;

import jakarta.validation.constraints.Size;

public class PurchaseInboundDecisionRequest {

    @Size(max = 200, message = "原因不能超过200个字符")
    private String reason;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

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
