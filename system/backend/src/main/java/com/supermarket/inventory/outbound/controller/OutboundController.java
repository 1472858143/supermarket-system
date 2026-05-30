package com.supermarket.inventory.outbound.controller;

import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.outbound.dto.OutboundRequest;
import com.supermarket.inventory.outbound.service.OutboundService;
import com.supermarket.inventory.outbound.vo.OutboundVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outbounds")
public class OutboundController {

    private final OutboundService outboundService;

    public OutboundController(OutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @GetMapping
    public ApiResponse<PageResult<OutboundVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(outboundService.list(keyword, page, pageSize));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody OutboundRequest request) {
        outboundService.create(request);
        return ApiResponse.success();
    }
}
