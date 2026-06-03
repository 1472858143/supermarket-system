package com.supermarket.inventory.purchaseinbound.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PurchaseInboundItemVO {

    private Long id;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String productCode;
    private String productName;
    private Long supplierSkuId;
    private String supplierSkuCodeSnapshot;
    private String supplierSkuNameSnapshot;
    private String supplierSpecSnapshot;
    private Integer plannedQuantity;
    private String unit;
    private Integer conversionRate;
    private Integer plannedBaseQuantity;
    private Integer inboundedBaseQuantity;
    private BigDecimal inboundedAmount;
    private BigDecimal purchasePrice;
    private BigDecimal costPrice;
    private BigDecimal plannedAmount;
    private LocalDateTime createTime;
    private List<PurchaseInboundReceiptBatchVO> receiptBatches;
    private String batchNo;
    private LocalDate productionDate;
    private Integer shelfLifeDays;
    private LocalDate expireDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getSupplierSkuId() {
        return supplierSkuId;
    }

    public void setSupplierSkuId(Long supplierSkuId) {
        this.supplierSkuId = supplierSkuId;
    }

    public String getSupplierSkuCodeSnapshot() {
        return supplierSkuCodeSnapshot;
    }

    public void setSupplierSkuCodeSnapshot(String supplierSkuCodeSnapshot) {
        this.supplierSkuCodeSnapshot = supplierSkuCodeSnapshot;
    }

    public String getSupplierSkuNameSnapshot() {
        return supplierSkuNameSnapshot;
    }

    public void setSupplierSkuNameSnapshot(String supplierSkuNameSnapshot) {
        this.supplierSkuNameSnapshot = supplierSkuNameSnapshot;
    }

    public String getSupplierSpecSnapshot() {
        return supplierSpecSnapshot;
    }

    public void setSupplierSpecSnapshot(String supplierSpecSnapshot) {
        this.supplierSpecSnapshot = supplierSpecSnapshot;
    }

    public Integer getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(Integer plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Integer conversionRate) {
        this.conversionRate = conversionRate;
    }

    public Integer getPlannedBaseQuantity() {
        return plannedBaseQuantity;
    }

    public void setPlannedBaseQuantity(Integer plannedBaseQuantity) {
        this.plannedBaseQuantity = plannedBaseQuantity;
    }

    public Integer getInboundedBaseQuantity() {
        return inboundedBaseQuantity;
    }

    public void setInboundedBaseQuantity(Integer inboundedBaseQuantity) {
        this.inboundedBaseQuantity = inboundedBaseQuantity;
    }

    public BigDecimal getInboundedAmount() {
        return inboundedAmount;
    }

    public void setInboundedAmount(BigDecimal inboundedAmount) {
        this.inboundedAmount = inboundedAmount;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<PurchaseInboundReceiptBatchVO> getReceiptBatches() {
        return receiptBatches;
    }

    public void setReceiptBatches(List<PurchaseInboundReceiptBatchVO> receiptBatches) {
        this.receiptBatches = receiptBatches;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    public Integer getQuantity() {
        return plannedQuantity;
    }

    public void setQuantity(Integer quantity) {
        this.plannedQuantity = quantity;
    }

    public Integer getBaseQuantity() {
        return plannedBaseQuantity;
    }

    public void setBaseQuantity(Integer baseQuantity) {
        this.plannedBaseQuantity = baseQuantity;
    }

    public BigDecimal getAmount() {
        return plannedAmount;
    }

    public void setAmount(BigDecimal amount) {
        this.plannedAmount = amount;
    }
}
