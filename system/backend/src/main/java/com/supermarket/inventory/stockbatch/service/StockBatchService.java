package com.supermarket.inventory.stockbatch.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.stockbatch.dto.StockBatchCreateCommand;
import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.entity.StockBatchLog;
import com.supermarket.inventory.stockbatch.mapper.StockBatchMapper;
import com.supermarket.inventory.stockbatch.vo.StockBatchVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

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
            batch.setId(stockBatchMapper.insertBatch(batch));
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
        return prefix + String.format("%03d", sequence);
    }
}
