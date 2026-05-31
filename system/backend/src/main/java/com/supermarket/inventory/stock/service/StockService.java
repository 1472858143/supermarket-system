package com.supermarket.inventory.stock.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.stock.domain.StockDomainService;
import com.supermarket.inventory.stock.dto.StockLimitUpdateRequest;
import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.mapper.StockMapper;
import com.supermarket.inventory.stock.vo.StockVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockMapper stockMapper;
    private final StockDomainService stockDomainService;

    public StockService(StockMapper stockMapper, StockDomainService stockDomainService) {
        this.stockMapper = stockMapper;
        this.stockDomainService = stockDomainService;
    }

    public PageResult<StockVO> list(String keyword, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        long total = stockMapper.count(keyword);
        return new PageResult<>(
                stockMapper.findPage(keyword, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize),
                total,
                normalizedPage,
                normalizedPageSize
        );
    }

    public StockVO getBySkuId(Long skuId) {
        return stockMapper.findVOBySkuId(skuId)
                .orElseThrow(() -> new BusinessException(404, "库存记录不存在"));
    }

    @Transactional
    public void initializeStock(Long skuId) {
        if (stockMapper.findBySkuIdForUpdate(skuId).isEmpty()) {
            stockMapper.insertInitialStock(skuId);
        }
    }

    @Transactional
    public StockVO updateLimit(Long skuId, StockLimitUpdateRequest request) {
        stockDomainService.validateLimit(request.getMinStock(), request.getMaxStock());
        stockMapper.findBySkuIdForUpdate(skuId)
                .orElseThrow(() -> new BusinessException(404, "库存记录不存在"));
        stockMapper.updateLimit(skuId, request.getMinStock(), request.getMaxStock());
        return getBySkuId(skuId);
    }

    @Transactional
    public StockVO increase(Long skuId, int quantity) {
        Stock stock = lockStock(skuId);
        int afterQuantity = stockDomainService.increase(stock.getQuantity(), quantity);
        stockMapper.updateQuantity(skuId, afterQuantity);
        stockMapper.insertLog(skuId, "INBOUND", quantity, stock.getQuantity(), afterQuantity);
        return getBySkuId(skuId);
    }

    @Transactional
    public StockVO decrease(Long skuId, int quantity) {
        Stock stock = lockStock(skuId);
        int afterQuantity = stockDomainService.decrease(stock.getQuantity(), quantity);
        stockMapper.updateQuantity(skuId, afterQuantity);
        stockMapper.insertLog(skuId, "OUTBOUND", -quantity, stock.getQuantity(), afterQuantity);
        return getBySkuId(skuId);
    }

    @Transactional
    public StockVO adjustTo(Long skuId, int actualQuantity) {
        Stock stock = lockStock(skuId);
        int afterQuantity = stockDomainService.adjustTo(actualQuantity);
        int difference = afterQuantity - stock.getQuantity();
        stockMapper.updateQuantity(skuId, afterQuantity);
        stockMapper.insertLog(skuId, "CHECK", difference, stock.getQuantity(), afterQuantity);
        return getBySkuId(skuId);
    }

    public Stock lockStock(Long skuId) {
        return stockMapper.findBySkuIdForUpdate(skuId)
                .orElseThrow(() -> new BusinessException(404, "库存记录不存在"));
    }

    @Transactional
    public void deleteStockBySkuId(Long skuId) {
        stockMapper.deleteBySkuId(skuId);
    }
}
