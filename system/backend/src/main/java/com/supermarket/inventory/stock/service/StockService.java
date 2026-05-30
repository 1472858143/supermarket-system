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

    public StockVO getByProductId(Long productId) {
        return stockMapper.findVOByProductId(productId)
                .orElseThrow(() -> new BusinessException(404, "库存记录不存在"));
    }

    @Transactional
    public void initializeStock(Long productId) {
        // 商品创建后立即补齐库存行，后续入库、出库和盘点都依赖这条记录。
        if (stockMapper.findByProductIdForUpdate(productId).isEmpty()) {
            stockMapper.insertInitialStock(productId);
        }
    }

    @Transactional
    public StockVO updateLimit(Long productId, StockLimitUpdateRequest request) {
        stockDomainService.validateLimit(request.getMinStock(), request.getMaxStock());
        stockMapper.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(404, "库存记录不存在"));
        stockMapper.updateLimit(productId, request.getMinStock(), request.getMaxStock());
        return getByProductId(productId);
    }

    @Transactional
    public StockVO increase(Long productId, int quantity) {
        Stock stock = lockStock(productId);
        int afterQuantity = stockDomainService.increase(stock.getQuantity(), quantity);
        stockMapper.updateQuantity(productId, afterQuantity);
        // 库存日志只记录变化事实，入库单本身负责记录业务原因和操作人。
        stockMapper.insertLog(productId, "INBOUND", quantity, stock.getQuantity(), afterQuantity);
        return getByProductId(productId);
    }

    @Transactional
    public StockVO decrease(Long productId, int quantity) {
        Stock stock = lockStock(productId);
        int afterQuantity = stockDomainService.decrease(stock.getQuantity(), quantity);
        stockMapper.updateQuantity(productId, afterQuantity);
        // 出库日志使用负数变化量，便于报表按方向聚合库存变化。
        stockMapper.insertLog(productId, "OUTBOUND", -quantity, stock.getQuantity(), afterQuantity);
        return getByProductId(productId);
    }

    @Transactional
    public StockVO adjustTo(Long productId, int actualQuantity) {
        Stock stock = lockStock(productId);
        int afterQuantity = stockDomainService.adjustTo(actualQuantity);
        int difference = afterQuantity - stock.getQuantity();
        stockMapper.updateQuantity(productId, afterQuantity);
        stockMapper.insertLog(productId, "CHECK", difference, stock.getQuantity(), afterQuantity);
        return getByProductId(productId);
    }

    public Stock lockStock(Long productId) {
        // 使用 FOR UPDATE 锁定单个商品库存，保证并发入库、出库、盘点时数量一致。
        return stockMapper.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(404, "库存记录不存在"));
    }

    @Transactional
    public void deleteStockByProductId(Long productId) {
        stockMapper.deleteByProductId(productId);
    }
}
