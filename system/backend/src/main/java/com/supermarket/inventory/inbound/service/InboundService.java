package com.supermarket.inventory.inbound.service;

import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.inbound.dto.InboundRequest;
import com.supermarket.inventory.inbound.mapper.InboundMapper;
import com.supermarket.inventory.inbound.vo.InboundVO;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.mapper.SkuMapper;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboundService {

    private final InboundMapper inboundMapper;
    private final StockService stockService;
    private final SkuMapper skuMapper;

    public InboundService(InboundMapper inboundMapper, StockService stockService, SkuMapper skuMapper) {
        this.inboundMapper = inboundMapper;
        this.stockService = stockService;
        this.skuMapper = skuMapper;
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
        Sku defaultSku = skuMapper.findDefaultByProductId(request.getProductId())
                .orElseThrow(() -> new BusinessException("该商品无默认SKU"));
        inboundMapper.insert(request.getProductId(), request.getQuantity(), operator);
        // 入库服务只记录库存增加原因，实际库存变更统一交给 StockService。
        stockService.increase(defaultSku.getId(), request.getQuantity());
    }

    private String resolveOperator(String operator) {
        if (operator != null && !operator.isBlank()) {
            return operator.trim();
        }
        return CurrentUserContext.get().getUsername();
    }
}
