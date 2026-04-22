package com.supermarket.inventory.inbound.controller;

import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.inbound.dto.InboundRequest;
import com.supermarket.inventory.inbound.service.InboundService;
import com.supermarket.inventory.inbound.vo.InboundVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inbounds")
public class InboundController {

    private final InboundService inboundService;

    public InboundController(InboundService inboundService) {
        this.inboundService = inboundService;
    }

    @GetMapping
    public ApiResponse<PageResult<InboundVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(inboundService.list(keyword, page, pageSize));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody InboundRequest request) {
        inboundService.create(request);
        return ApiResponse.success();
    }
}
