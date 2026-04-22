package com.supermarket.inventory.outbound.service;

import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.outbound.dto.OutboundRequest;
import com.supermarket.inventory.outbound.mapper.OutboundMapper;
import com.supermarket.inventory.outbound.vo.OutboundVO;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboundService {

    private final OutboundMapper outboundMapper;
    private final StockService stockService;

    public OutboundService(OutboundMapper outboundMapper, StockService stockService) {
        this.outboundMapper = outboundMapper;
        this.stockService = stockService;
    }

    public PageResult<OutboundVO> list(String keyword, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        return new PageResult<>(
                outboundMapper.findPage(keyword, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize),
                outboundMapper.count(keyword),
                normalizedPage,
                normalizedPageSize
        );
    }

    @Transactional
    public void create(OutboundRequest request) {
        String operator = resolveOperator(request.getOperator());
        stockService.decrease(request.getProductId(), request.getQuantity());
        outboundMapper.insert(request.getProductId(), request.getQuantity(), operator);
    }

    private String resolveOperator(String operator) {
        if (operator != null && !operator.isBlank()) {
            return operator.trim();
        }
        return CurrentUserContext.get().getUsername();
    }
}
