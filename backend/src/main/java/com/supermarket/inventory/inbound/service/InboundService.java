package com.supermarket.inventory.inbound.service;

import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.inbound.dto.InboundRequest;
import com.supermarket.inventory.inbound.mapper.InboundMapper;
import com.supermarket.inventory.inbound.vo.InboundVO;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboundService {

    private final InboundMapper inboundMapper;
    private final StockService stockService;

    public InboundService(InboundMapper inboundMapper, StockService stockService) {
        this.inboundMapper = inboundMapper;
        this.stockService = stockService;
    }

    public PageResult<InboundVO> list(String keyword, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        return new PageResult<>(
                inboundMapper.findPage(keyword, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize),
                inboundMapper.count(keyword),
                normalizedPage,
                normalizedPageSize
        );
    }

    @Transactional
    public void create(InboundRequest request) {
        String operator = resolveOperator(request.getOperator());
        inboundMapper.insert(request.getProductId(), request.getQuantity(), operator);
        stockService.increase(request.getProductId(), request.getQuantity());
    }

    private String resolveOperator(String operator) {
        if (operator != null && !operator.isBlank()) {
            return operator.trim();
        }
        return CurrentUserContext.get().getUsername();
    }
}
