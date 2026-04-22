package com.supermarket.inventory.system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemService {

    private final String activeProfile;

    public SystemService(@Value("${spring.profiles.active:dev}") String activeProfile) {
        this.activeProfile = activeProfile;
    }

    public Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("systemName", "超市库存管理系统");
        info.put("version", "1.0.0");
        info.put("backend", "Spring Boot");
        info.put("database", "MySQL 8.0.44");
        info.put("profile", activeProfile);
        info.put("serverTime", LocalDateTime.now());
        info.put("description", "system 模块保持轻量化，仅展示系统基础信息和运行说明。");
        return info;
    }
}
