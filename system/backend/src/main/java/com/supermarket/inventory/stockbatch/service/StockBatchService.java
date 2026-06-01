package com.supermarket.inventory.stockbatch.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.stockbatch.dto.StockBatchCreateCommand;
import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.entity.StockBatchLog;
import com.supermarket.inventory.stockbatch.mapper.StockBatchMapper;
import com.supermarket.inventory.stockbatch.vo.StockBatchVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockBatchService {

    private static final String CHANGE_TYPE_PURCHASE_INBOUND = "PURCHASE_INBOUND";
    private static final String SOURCE_TYPE_PURCHASE_INBOUND_ITEM = "PURCHASE_INBOUND_ITEM";

    private final StockBatchMapper stockBatchMapper;

    public StockBatchService(StockBatchMapper stockBatchMapper) {
        this.stockBatchMapper = stockBatchMapper;
    }

    public StockBatch createFromPurchaseInboundItem(StockBatchCreateCommand command) {
        validateCreateCommand(command);
        StockBatch batch = new StockBatch();
        batch.setBatchNo(nextBatchNo());
        batch.setSkuId(command.getSkuId());
        batch.setPurchaseInboundItemId(command.getPurchaseInboundItemId());
        batch.setInitialQuantity(command.getBaseQuantity());
        batch.setQuantity(command.getBaseQuantity());
        batch.setPurchasePrice(command.getPurchasePrice());
        batch.setProductionDate(command.getProductionDate());
        batch.setShelfLifeDays(command.getShelfLifeDays());
        batch.setExpireDate(command.getProductionDate().plusDays(command.getShelfLifeDays()));
        try {
            Long id = stockBatchMapper.insertBatch(batch);
            if (id == null) {
                throw new BusinessException("\u5e93\u5b58\u6279\u6b21\u4fdd\u5b58\u5931\u8d25");
            }
            batch.setId(id);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("\u5e93\u5b58\u6279\u6b21\u53f7\u91cd\u590d\uff0c\u8bf7\u91cd\u8bd5");
        }
        return batch;
    }

    public void writePurchaseInboundLog(StockBatch batch) {
        StockBatchLog log = new StockBatchLog();
        log.setStockBatchId(batch.getId());
        log.setSkuId(batch.getSkuId());
        log.setChangeType(CHANGE_TYPE_PURCHASE_INBOUND);
        log.setChangeQuantity(batch.getQuantity());
        log.setBeforeQuantity(0);
        log.setAfterQuantity(batch.getQuantity());
        log.setSourceType(SOURCE_TYPE_PURCHASE_INBOUND_ITEM);
        log.setSourceId(batch.getPurchaseInboundItemId());
        stockBatchMapper.insertLog(log);
    }

    public List<StockBatchVO> listBySkuId(Long skuId) {
        return stockBatchMapper.findBySkuId(skuId);
    }

    private void validateCreateCommand(StockBatchCreateCommand command) {
        if (command == null) {
            throw new BusinessException("\u5e93\u5b58\u6279\u6b21\u521b\u5efa\u53c2\u6570\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (command.getSkuId() == null) {
            throw new BusinessException("SKU ID\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (command.getPurchaseInboundItemId() == null) {
            throw new BusinessException("\u91c7\u8d2d\u5165\u5e93\u660e\u7ec6ID\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (command.getBaseQuantity() == null || command.getBaseQuantity() <= 0) {
            throw new BusinessException("\u6279\u6b21\u6570\u91cf\u5fc5\u987b\u5927\u4e8e0");
        }
        if (command.getPurchasePrice() == null || command.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("\u6279\u6b21\u8fdb\u4ef7\u4e0d\u80fd\u5c0f\u4e8e0");
        }
        if (command.getProductionDate() == null) {
            throw new BusinessException("\u751f\u4ea7\u65e5\u671f\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (command.getShelfLifeDays() == null || command.getShelfLifeDays() <= 0) {
            throw new BusinessException("\u4fdd\u8d28\u671f\u5929\u6570\u5fc5\u987b\u5927\u4e8e0");
        }
    }

    private String nextBatchNo() {
        String prefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String maxBatchNo = stockBatchMapper.findMaxBatchNo(prefix + "%");
        int sequence = 1;
        if (maxBatchNo != null && maxBatchNo.length() >= prefix.length() + 3) {
            try {
                sequence = Integer.parseInt(maxBatchNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException exception) {
                throw new BusinessException("\u5e93\u5b58\u6279\u6b21\u53f7\u5e8f\u53f7\u5f02\u5e38");
            }
        }
        if (sequence > 999) {
            throw new BusinessException("\u5e93\u5b58\u6279\u6b21\u53f7\u5f53\u65e5\u5e8f\u53f7\u5df2\u8fbe\u4e0a\u9650");
        }
        return prefix + String.format("%03d", sequence);
    }
}
