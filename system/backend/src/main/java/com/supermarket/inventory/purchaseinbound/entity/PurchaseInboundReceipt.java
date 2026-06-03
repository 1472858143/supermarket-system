package com.supermarket.inventory.purchaseinbound.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PurchaseInboundReceipt {

    private Long id;
    private String receiptNo;
    private Long purchaseInboundId;
    private Long operatorUserId;
    private String operatorUsername;
    private Integer totalBaseQuantity;
    private BigDecimal totalAmount;
    private String remark;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public Long getPurchaseInboundId() {
        return purchaseInboundId;
    }

    public void setPurchaseInboundId(Long purchaseInboundId) {
        this.purchaseInboundId = purchaseInboundId;
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

    public Integer getTotalBaseQuantity() {
        return totalBaseQuantity;
    }

    public void setTotalBaseQuantity(Integer totalBaseQuantity) {
        this.totalBaseQuantity = totalBaseQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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
