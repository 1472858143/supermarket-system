package com.supermarket.inventory.user.vo;

import java.util.List;

public class PermissionModuleVO {

    private String moduleCode;
    private String moduleName;
    private List<PermissionVO> permissions;

    public PermissionModuleVO() {
    }

    public PermissionModuleVO(String moduleCode, String moduleName, List<PermissionVO> permissions) {
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.permissions = permissions;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public List<PermissionVO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionVO> permissions) {
        this.permissions = permissions;
    }
}
