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
        // 盘点先读取锁定后的系统库存，再记录差异，最后通过库存模块调整当前库存。
        Stock stock = stockService.lockStock(request.getProductId());
        int systemQuantity = stock.getQuantity();
        int difference = request.getActualQuantity() - systemQuantity;
        stockCheckMapper.insert(request.getProductId(), systemQuantity, request.getActualQuantity(), difference);
        stockService.adjustTo(request.getProductId(), request.getActualQuantity());
    }
}
