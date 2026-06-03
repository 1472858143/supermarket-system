package com.supermarket.inventory.purchaseinbound.entity;

import java.time.LocalDateTime;

public class PurchaseInboundApprovalLog {

    private Long id;
    private Long purchaseInboundId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private Long operatorUserId;
    private String operatorUsername;
    private String reason;
    private String remark;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPurchaseInboundId() {
        return purchaseInboundId;
    }

    public void setPurchaseInboundId(Long purchaseInboundId) {
        this.purchaseInboundId = purchaseInboundId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
