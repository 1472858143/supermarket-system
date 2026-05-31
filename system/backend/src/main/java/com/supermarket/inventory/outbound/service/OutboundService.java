package com.supermarket.inventory.outbound.service;

import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.outbound.dto.OutboundRequest;
import com.supermarket.inventory.outbound.mapper.OutboundMapper;
import com.supermarket.inventory.outbound.vo.OutboundVO;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.mapper.SkuMapper;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboundService {

    private final OutboundMapper outboundMapper;
    private final StockService stockService;
    private final SkuMapper skuMapper;

    public OutboundService(OutboundMapper outboundMapper, StockService stockService, SkuMapper skuMapper) {
        this.outboundMapper = outboundMapper;
        this.stockService = stockService;
        this.skuMapper = skuMapper;
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
        Sku defaultSku = skuMapper.findDefaultByProductId(request.getProductId())
                .orElseThrow(() -> new BusinessException("该商品无默认SKU"));
        // 先扣减库存再写出库单，库存不足时事务回滚且不会留下出库记录。
        stockService.decrease(defaultSku.getId(), request.getQuantity());
        outboundMapper.insert(request.getProductId(), request.getQuantity(), operator);
    }

    private String resolveOperator(String operator) {
        if (operator != null && !operator.isBlank()) {
            return operator.trim();
        }
        return CurrentUserContext.get().getUsername();
    }
}
