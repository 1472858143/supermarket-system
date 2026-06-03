package com.supermarket.inventory.purchaseinbound.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PurchaseInboundVO {

    private Long id;
    private String orderNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private Integer plannedTotalQuantity;
    private Integer inboundTotalQuantity;
    private BigDecimal plannedTotalAmount;
    private BigDecimal inboundTotalAmount;
    private String status;
    private Long creatorUserId;
    private String creatorUsername;
    private Long submitterUserId;
    private String submitterUsername;
    private LocalDateTime submitTime;
    private Long approverUserId;
    private String approverUsername;
    private LocalDateTime approveTime;
    private Long cancelUserId;
    private String cancelUsername;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private Long closeUserId;
    private String closeUsername;
    private LocalDateTime closeTime;
    private String closeReason;
    private String operator;
    private String remark;
    private LocalDateTime createTime;
    private List<PurchaseInboundItemVO> items;
    private List<PurchaseInboundApprovalLogVO> approvalLogs;
    private List<PurchaseInboundReceiptVO> receipts;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Integer getPlannedTotalQuantity() {
        return plannedTotalQuantity;
    }

    public void setPlannedTotalQuantity(Integer plannedTotalQuantity) {
        this.plannedTotalQuantity = plannedTotalQuantity;
    }

    public Integer getInboundTotalQuantity() {
        return inboundTotalQuantity;
    }

    public void setInboundTotalQuantity(Integer inboundTotalQuantity) {
        this.inboundTotalQuantity = inboundTotalQuantity;
    }

    public BigDecimal getPlannedTotalAmount() {
        return plannedTotalAmount;
    }

    public void setPlannedTotalAmount(BigDecimal plannedTotalAmount) {
        this.plannedTotalAmount = plannedTotalAmount;
    }

    public BigDecimal getInboundTotalAmount() {
        return inboundTotalAmount;
    }

    public void setInboundTotalAmount(BigDecimal inboundTotalAmount) {
        this.inboundTotalAmount = inboundTotalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(Long creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public Long getSubmitterUserId() {
        return submitterUserId;
    }

    public void setSubmitterUserId(Long submitterUserId) {
        this.submitterUserId = submitterUserId;
    }

    public String getSubmitterUsername() {
        return submitterUsername;
    }

    public void setSubmitterUsername(String submitterUsername) {
        this.submitterUsername = submitterUsername;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public Long getApproverUserId() {
        return approverUserId;
    }

    public void setApproverUserId(Long approverUserId) {
        this.approverUserId = approverUserId;
    }

    public String getApproverUsername() {
        return approverUsername;
    }

    public void setApproverUsername(String approverUsername) {
        this.approverUsername = approverUsername;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public Long getCancelUserId() {
        return cancelUserId;
    }

    public void setCancelUserId(Long cancelUserId) {
        this.cancelUserId = cancelUserId;
    }

    public String getCancelUsername() {
        return cancelUsername;
    }

    public void setCancelUsername(String cancelUsername) {
        this.cancelUsername = cancelUsername;
    }

    public LocalDateTime getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(LocalDateTime cancelTime) {
        this.cancelTime = cancelTime;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Long getCloseUserId() {
        return closeUserId;
    }

    public void setCloseUserId(Long closeUserId) {
        this.closeUserId = closeUserId;
    }

    public String getCloseUsername() {
        return closeUsername;
    }

    public void setCloseUsername(String closeUsername) {
        this.closeUsername = closeUsername;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
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

    public List<PurchaseInboundItemVO> getItems() {
        return items;
    }

    public void setItems(List<PurchaseInboundItemVO> items) {
        this.items = items;
    }

    public List<PurchaseInboundApprovalLogVO> getApprovalLogs() {
        return approvalLogs;
    }

    public void setApprovalLogs(List<PurchaseInboundApprovalLogVO> approvalLogs) {
        this.approvalLogs = approvalLogs;
    }

    public List<PurchaseInboundReceiptVO> getReceipts() {
        return receipts;
    }

    public void setReceipts(List<PurchaseInboundReceiptVO> receipts) {
        this.receipts = receipts;
    }

    public Integer getTotalQuantity() {
        return plannedTotalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.plannedTotalQuantity = totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return plannedTotalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.plannedTotalAmount = totalAmount;
    }
}
