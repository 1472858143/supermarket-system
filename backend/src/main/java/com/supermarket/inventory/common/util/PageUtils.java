package com.supermarket.inventory.common.util;

public final class PageUtils {

    private PageUtils() {
    }

    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    public static int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }
}
