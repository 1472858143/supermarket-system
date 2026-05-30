package com.supermarket.inventory.user.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class UserUpdateRequest {

    private String realName;

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
