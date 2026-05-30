package com.supermarket.inventory.auth.security;

import java.util.List;

public class CurrentUser {

    private Long userId;
    private String username;
    private List<String> roles;

    public CurrentUser() {
    }

    public CurrentUser(Long userId, String username, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
