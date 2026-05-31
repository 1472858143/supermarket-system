package com.supermarket.inventory.inbound.service;

import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.inbound.dto.InboundRequest;
import com.supermarket.inventory.inbound.mapper.InboundMapper;
import com.supermarket.inventory.inbound.vo.InboundVO;
import com.supermarket.inventory.sku.service.SkuUnitResolver;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboundService {

    private final InboundMapper inboundMapper;
    private final StockService stockService;
    private final SkuUnitResolver skuUnitResolver;

    public InboundService(InboundMapper inboundMapper, StockService stockService, SkuUnitResolver skuUnitResolver) {
        this.inboundMapper = inboundMapper;
        this.stockService = stockService;
        this.skuUnitResolver = skuUnitResolver;
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
        SkuUnitResolver.ResolvedUnit resolvedUnit = skuUnitResolver.resolve(request.getSkuId(), request.getUnit());
        int baseQuantity = calculateBaseQuantity(request.getQuantity(), resolvedUnit.conversionRate());
        inboundMapper.insert(
                resolvedUnit.sku().getId(),
                request.getQuantity(),
                resolvedUnit.unit(),
                resolvedUnit.conversionRate(),
                baseQuantity,
                operator
        );
        stockService.increase(resolvedUnit.sku().getId(), baseQuantity);
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
