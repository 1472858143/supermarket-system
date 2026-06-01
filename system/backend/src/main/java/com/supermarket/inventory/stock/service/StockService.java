package com.supermarket.inventory.stock.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.stock.domain.StockDomainService;
import com.supermarket.inventory.stock.dto.StockIncreaseCommand;
import com.supermarket.inventory.stock.dto.StockLimitUpdateRequest;
import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.mapper.StockMapper;
import com.supermarket.inventory.stock.vo.StockVO;
import com.supermarket.inventory.stockbatch.dto.StockBatchCreateCommand;
import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.service.StockBatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

@Service
public class StockService {

    private static final String CHANGE_TYPE_PURCHASE_INBOUND = "PURCHASE_INBOUND";
    private static final String CHANGE_TYPE_OUTBOUND = "OUTBOUND";
    private static final String SOURCE_TYPE_STOCK_DECREASE = "STOCK_DECREASE";

    private final StockMapper stockMapper;
    private final StockDomainService stockDomainService;
    private final StockBatchService stockBatchService;

    public StockService(
            StockMapper stockMapper,
            StockDomainService stockDomainService,
            StockBatchService stockBatchService
    ) {
        this.stockMapper = stockMapper;
        this.stockDomainService = stockDomainService;
        this.stockBatchService = stockBatchService;
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
    public StockVO increase(Long skuId, int quantity, String changeType) {
        Stock stock = lockStock(skuId);
        int afterQuantity = stockDomainService.increase(stock.getQuantity(), quantity);
        stockMapper.updateQuantity(skuId, afterQuantity);
        stockMapper.insertLog(skuId, changeType, quantity, stock.getQuantity(), afterQuantity);
        return getBySkuId(skuId);
    }

    @Transactional
    public StockVO increase(StockIncreaseCommand command) {
        validateIncreaseCommand(command);
        Stock stock = lockStock(command.getSkuId());
        int afterQuantity = stockDomainService.increase(stock.getQuantity(), command.getQuantity());
        stockMapper.updateQuantity(command.getSkuId(), afterQuantity);
        stockMapper.insertLog(
                command.getSkuId(),
                CHANGE_TYPE_PURCHASE_INBOUND,
                command.getQuantity(),
                stock.getQuantity(),
                afterQuantity
        );
        StockBatch batch = stockBatchService.createFromPurchaseInboundItem(toStockBatchCreateCommand(command));
        stockBatchService.writePurchaseInboundLog(batch, command.getSourceType(), command.getSourceId());
        return getBySkuId(command.getSkuId());
    }

    @Transactional
    public StockVO decrease(Long skuId, int quantity) {
        return decrease(skuId, quantity, SOURCE_TYPE_STOCK_DECREASE, skuId);
    }

    @Transactional
    public StockVO decrease(Long skuId, int quantity, String sourceType, Long sourceId) {
        Stock stock = lockStock(skuId);
        int afterQuantity = stockDomainService.decrease(stock.getQuantity(), quantity);
        var consumptions = stockBatchService.consumeByFefo(skuId, quantity);
        stockMapper.updateQuantity(skuId, afterQuantity);
        stockMapper.insertLog(skuId, CHANGE_TYPE_OUTBOUND, -quantity, stock.getQuantity(), afterQuantity);
        stockBatchService.writeOutboundLogs(consumptions, sourceType, sourceId);
        return getBySkuId(skuId);
    }

    @Transactional
    public StockVO damage(Long skuId, int quantity) {
        Stock stock = lockStock(skuId);
        int afterQuantity = stockDomainService.decrease(stock.getQuantity(), quantity);
        stockMapper.updateQuantity(skuId, afterQuantity);
        stockMapper.insertLog(skuId, "DAMAGE", -quantity, stock.getQuantity(), afterQuantity);
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

    private StockBatchCreateCommand toStockBatchCreateCommand(StockIncreaseCommand command) {
        StockBatchCreateCommand batchCommand = new StockBatchCreateCommand();
        batchCommand.setSkuId(command.getSkuId());
        batchCommand.setPurchaseInboundItemId(command.getPurchaseInboundItemId());
        batchCommand.setBaseQuantity(command.getQuantity());
        batchCommand.setPurchasePrice(command.getPurchasePrice());
        batchCommand.setProductionDate(command.getProductionDate());
        batchCommand.setShelfLifeDays((int) ChronoUnit.DAYS.between(command.getProductionDate(), command.getExpiryDate()));
        return batchCommand;
    }

    private void validateIncreaseCommand(StockIncreaseCommand command) {
        if (command == null) {
            throw new BusinessException("库存增加参数不能为空");
        }
        if (command.getSkuId() == null) {
            throw new BusinessException("SKU ID不能为空");
        }
        if (command.getQuantity() == null || command.getQuantity() <= 0) {
            throw new BusinessException("库存变化数量必须大于0");
        }
        if (command.getPurchasePrice() == null) {
            throw new BusinessException("采购单价不能为空");
        }
        if (command.getProductionDate() == null) {
            throw new BusinessException("生产日期不能为空");
        }
        if (command.getExpiryDate() == null || !command.getExpiryDate().isAfter(command.getProductionDate())) {
            throw new BusinessException("到期日期必须晚于生产日期");
        }
        if (command.getPurchaseInboundId() == null) {
            throw new BusinessException("采购入库单ID不能为空");
        }
        if (command.getPurchaseInboundItemId() == null) {
            throw new BusinessException("采购入库明细ID不能为空");
        }
        if (command.getSourceType() == null || command.getSourceType().isBlank()) {
            throw new BusinessException("库存来源类型不能为空");
        }
        if (command.getSourceId() == null) {
            throw new BusinessException("库存来源ID不能为空");
        }
    }
}
