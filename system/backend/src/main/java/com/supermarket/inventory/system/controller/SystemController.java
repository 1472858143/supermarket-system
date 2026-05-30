package com.supermarket.inventory.system.controller;

import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.system.service.SystemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        return ApiResponse.success(systemService.info());
    }
}
