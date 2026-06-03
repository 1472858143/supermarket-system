package com.supermarket.inventory.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UserUpdateRequest {

    private String realName;

    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;

    @NotBlank(message = "联系方式不能为空")
    @Size(max = 30, message = "联系方式不能超过30个字符")
    private String contactPhone;

    private Integer status;

    @NotEmpty(message = "用户角色不能为空")
    private List<Long> roleIds;

    private String newPassword;

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
