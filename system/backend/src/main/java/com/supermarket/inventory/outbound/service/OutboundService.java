package com.supermarket.inventory.outbound.service;

import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.outbound.dto.OutboundRequest;
import com.supermarket.inventory.outbound.mapper.OutboundMapper;
import com.supermarket.inventory.outbound.vo.OutboundVO;
import com.supermarket.inventory.sku.service.SkuUnitResolver;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboundService {

    private final OutboundMapper outboundMapper;
    private final StockService stockService;
    private final SkuUnitResolver skuUnitResolver;

    public OutboundService(OutboundMapper outboundMapper, StockService stockService, SkuUnitResolver skuUnitResolver) {
        this.outboundMapper = outboundMapper;
        this.stockService = stockService;
        this.skuUnitResolver = skuUnitResolver;
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
        SkuUnitResolver.ResolvedUnit resolvedUnit = skuUnitResolver.resolve(request.getSkuId(), request.getUnit());
        int baseQuantity = calculateBaseQuantity(request.getQuantity(), resolvedUnit.conversionRate());
        stockService.decrease(resolvedUnit.sku().getId(), baseQuantity);
        outboundMapper.insert(
                resolvedUnit.sku().getId(),
                request.getQuantity(),
                resolvedUnit.unit(),
                resolvedUnit.conversionRate(),
                baseQuantity,
                operator
        );
    }

    private int calculateBaseQuantity(int quantity, int conversionRate) {
        try {
            return Math.multiplyExact(quantity, conversionRate);
        } catch (ArithmeticException ex) {
            throw new BusinessException("基础单位数量超出范围");
        }
    }

    private String resolveOperator(String operator) {
        if (operator != null && !operator.isBlank()) {
            return operator.trim();
        }
        return CurrentUserContext.get().getUsername();
    }
}
