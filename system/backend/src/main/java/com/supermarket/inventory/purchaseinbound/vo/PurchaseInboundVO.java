package com.supermarket.inventory.purchaseinbound.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PurchaseInboundVO {

    private Long id;
    private String orderNo;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private String status;
    private String operator;
    private String remark;
    private LocalDateTime createTime;
    private List<PurchaseInboundItemVO> items;

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

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
