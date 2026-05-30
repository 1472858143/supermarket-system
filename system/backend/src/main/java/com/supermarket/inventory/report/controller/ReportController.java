package com.supermarket.inventory.report.controller;

import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.report.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/stock")
    public ApiResponse<Map<String, Object>> stock() {
        return ApiResponse.success(reportService.stock());
    }

    @GetMapping("/inbound")
    public ApiResponse<Map<String, Object>> inbound() {
        return ApiResponse.success(reportService.inbound());
    }

    @GetMapping("/outbound")
    public ApiResponse<Map<String, Object>> outbound() {
        return ApiResponse.success(reportService.outbound());
    }

    @GetMapping("/warning")
    public ApiResponse<List<Map<String, Object>>> warning() {
        return ApiResponse.success(reportService.warning());
    }
}
