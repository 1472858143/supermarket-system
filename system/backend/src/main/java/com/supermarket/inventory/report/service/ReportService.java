package com.supermarket.inventory.report.service;

import com.supermarket.inventory.report.mapper.ReportMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final ReportMapper reportMapper;

    public ReportService(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    public Map<String, Object> stock() {
        Map<String, Object> data = new LinkedHashMap<>(reportMapper.stockSummary());
        data.put("trend", reportMapper.stockTrend());
        return data;
    }

    public Map<String, Object> inbound() {
        return reportMapper.inboundSummary();
    }

    public Map<String, Object> outbound() {
        return reportMapper.outboundSummary();
    }

    public List<Map<String, Object>> warning() {
        return reportMapper.warningStocks();
    }
}
