package com.supermarket.inventory.auth.security;

import com.supermarket.inventory.common.exception.BusinessException;

public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser currentUser) {
        HOLDER.set(currentUser);
    }

    public static CurrentUser get() {
        CurrentUser currentUser = HOLDER.get();
        if (currentUser == null) {
            throw new BusinessException(401, "未登录或认证失败");
        }
        return currentUser;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
