package com.supermarket.inventory.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupplierRequest {

    @Size(max = 50, message = "供应商编码不能超过50个字符")
    private String supplierCode;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称不能超过100个字符")
    private String supplierName;

    @Size(max = 50, message = "联系人不能超过50个字符")
    private String contactPerson;

    @Size(max = 30, message = "联系电话不能超过30个字符")
    private String contactPhone;

    @Size(max = 200, message = "地址不能超过200个字符")
    private String address;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;

    private Integer status = 1;

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

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
