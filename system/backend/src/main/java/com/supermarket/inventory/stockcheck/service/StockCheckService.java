package com.supermarket.inventory.stockcheck.service;

import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.service.StockService;
import com.supermarket.inventory.stockcheck.dto.StockCheckRequest;
import com.supermarket.inventory.stockcheck.mapper.StockCheckMapper;
import com.supermarket.inventory.stockcheck.vo.StockCheckVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockCheckService {

    private final StockCheckMapper stockCheckMapper;
    private final StockService stockService;

    public StockCheckService(StockCheckMapper stockCheckMapper, StockService stockService) {
        this.stockCheckMapper = stockCheckMapper;
        this.stockService = stockService;
    }

    public PageResult<StockCheckVO> list(String keyword, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        return new PageResult<>(
                stockCheckMapper.findPage(keyword, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize),
                stockCheckMapper.count(keyword),
                normalizedPage,
                normalizedPageSize
        );
    }

    @Transactional
    public void create(StockCheckRequest request) {
        Stock stock = stockService.lockStock(request.getSkuId());
        int systemQuantity = stock.getQuantity();
        int difference = request.getActualQuantity() - systemQuantity;
        stockCheckMapper.insert(request.getSkuId(), systemQuantity, request.getActualQuantity(), difference);
        stockService.adjustTo(request.getSkuId(), request.getActualQuantity());
    }
}
