package com.supermarket.inventory.stockbatch.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.stock.domain.StockDomainService;
import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.mapper.StockMapper;
import com.supermarket.inventory.stockbatch.dto.StockBatchCreateCommand;
import com.supermarket.inventory.stockbatch.dto.StockBatchDamageRequest;
import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.entity.StockBatchLog;
import com.supermarket.inventory.stockbatch.mapper.StockBatchMapper;
import com.supermarket.inventory.stockbatch.vo.StockBatchVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StockBatchService {

    private static final String CHANGE_TYPE_PURCHASE_INBOUND = "PURCHASE_INBOUND";
    private static final String CHANGE_TYPE_BATCH_STATUS = "BATCH_STATUS";
    private static final String CHANGE_TYPE_DAMAGE = "DAMAGE";
    private static final String CHANGE_TYPE_OUTBOUND = "OUTBOUND";
    private static final String SOURCE_TYPE_PURCHASE_INBOUND_RECEIPT_BATCH = "PURCHASE_INBOUND_RECEIPT_BATCH";
    private static final String SOURCE_TYPE_BATCH_LOCK = "BATCH_LOCK";
    private static final String SOURCE_TYPE_BATCH_UNLOCK = "BATCH_UNLOCK";
    private static final String SOURCE_TYPE_BATCH_DAMAGE = "BATCH_DAMAGE";
    private static final String SOURCE_TYPE_BATCH_CLOSE = "BATCH_CLOSE";
    private static final String SOURCE_TYPE_BATCH_EXPIRE_SCAN = "BATCH_EXPIRE_SCAN";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_LOCKED = "LOCKED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_DEPLETED = "DEPLETED";
    private static final String STATUS_DAMAGED = "DAMAGED";
    private static final String STATUS_CLOSED = "CLOSED";

    private final StockBatchMapper stockBatchMapper;
    private final StockMapper stockMapper;
    private final StockDomainService stockDomainService;

    public StockBatchService(StockBatchMapper stockBatchMapper, StockMapper stockMapper,
                             StockDomainService stockDomainService) {
        this.stockBatchMapper = stockBatchMapper;
        this.stockMapper = stockMapper;
        this.stockDomainService = stockDomainService;
    }

    @Transactional
    public StockBatch createFromPurchaseInboundItem(StockBatchCreateCommand command) {
        return createFromPurchaseInboundReceiptBatch(command);
    }

    @Transactional
    public StockBatch createFromPurchaseInboundReceiptBatch(StockBatchCreateCommand command) {
        validateCreateCommand(command);
        StockBatch batch = new StockBatch();
        batch.setBatchNo(nextBatchNo());
        batch.setSkuId(command.getSkuId());
        batch.setPurchaseInboundReceiptBatchId(command.getPurchaseInboundReceiptBatchId());
        batch.setInitialQuantity(command.getBaseQuantity());
        batch.setQuantity(command.getBaseQuantity());
        batch.setStatus(STATUS_AVAILABLE);
        batch.setPurchasePrice(command.getPurchasePrice());
        batch.setCostPrice(command.getCostPrice());
        batch.setProductionDate(command.getProductionDate());
        batch.setShelfLifeDays(command.getShelfLifeDays());
        batch.setExpireDate(command.getProductionDate().plusDays(command.getShelfLifeDays()));
        try {
            Long id = stockBatchMapper.insertBatch(batch);
            if (id == null) {
                throw new BusinessException("库存批次保存失败");
            }
            batch.setId(id);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("库存批次号重复，请重试");
        }
        return batch;
    }

    @Transactional
    public void writePurchaseInboundLog(StockBatch batch) {
        writePurchaseInboundLog(
                batch,
                SOURCE_TYPE_PURCHASE_INBOUND_RECEIPT_BATCH,
                batch.getPurchaseInboundReceiptBatchId()
        );
    }

    public void writePurchaseInboundLog(StockBatch batch, String sourceType, Long sourceId) {
        StockBatchLog log = new StockBatchLog();
        log.setStockBatchId(batch.getId());
        log.setSkuId(batch.getSkuId());
        log.setChangeType(CHANGE_TYPE_PURCHASE_INBOUND);
        log.setChangeQuantity(batch.getQuantity());
        log.setBeforeQuantity(0);
        log.setAfterQuantity(batch.getQuantity());
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        stockBatchMapper.insertLog(log);
    }

    public List<BatchConsumption> consumeByFefo(Long skuId, int quantity) {
        validateConsumption(skuId, quantity);
        List<StockBatch> batches = stockBatchMapper.findConsumableBySkuIdForUpdate(skuId);
        int availableQuantity = batches.stream()
                .map(StockBatch::getQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        if (availableQuantity < quantity) {
            throw new BusinessException("库存批次数量不足，无法出库");
        }

        int remainingToConsume = quantity;
        List<BatchConsumption> consumptions = new ArrayList<>();
        for (StockBatch batch : batches) {
            if (remainingToConsume == 0) {
                break;
            }
            int beforeQuantity = batch.getQuantity();
            int consumedQuantity = Math.min(beforeQuantity, remainingToConsume);
            int afterQuantity = beforeQuantity - consumedQuantity;
            String status = afterQuantity == 0 ? STATUS_DEPLETED : STATUS_AVAILABLE;
            ensureBatchUpdated(stockBatchMapper.updateRemainingQuantityAndStatus(
                    batch.getId(), batch.getSkuId(), afterQuantity, status));
            consumptions.add(new BatchConsumption(
                    batch.getId(),
                    batch.getSkuId(),
                    beforeQuantity,
                    consumedQuantity,
                    afterQuantity
            ));
            remainingToConsume -= consumedQuantity;
        }
        return consumptions;
    }

    public void writeOutboundLogs(List<BatchConsumption> consumptions, String sourceType, Long sourceId) {
        if (consumptions == null || consumptions.isEmpty()) {
            return;
        }
        validateSource(sourceType, sourceId);
        for (BatchConsumption consumption : consumptions) {
            StockBatchLog log = new StockBatchLog();
            log.setStockBatchId(consumption.stockBatchId());
            log.setSkuId(consumption.skuId());
            log.setChangeType(CHANGE_TYPE_OUTBOUND);
            log.setChangeQuantity(-consumption.consumedQuantity());
            log.setBeforeQuantity(consumption.beforeQuantity());
            log.setAfterQuantity(consumption.afterQuantity());
            log.setSourceType(sourceType);
            log.setSourceId(sourceId);
            stockBatchMapper.insertLog(log);
        }
    }

    public List<StockBatchVO> listBySkuId(Long skuId) {
        return stockBatchMapper.findBySkuId(skuId);
    }

    @Transactional
    public void lock(Long skuId, Long batchId) {
        StockBatch batch = getBatchForUpdate(skuId, batchId);
        if (!STATUS_AVAILABLE.equals(batch.getStatus()) && !STATUS_EXPIRED.equals(batch.getStatus())) {
            throw new BusinessException("当前批次状态不允许锁定");
        }
        Stock stock = getStockForUpdate(skuId);
        ensureBatchUpdated(stockBatchMapper.updateStatus(batchId, skuId, STATUS_LOCKED));
        moveStockBucket(stock, skuId, batch.getStatus(), STATUS_LOCKED, safeQuantity(batch));
        writeStatusLog(batch, SOURCE_TYPE_BATCH_LOCK);
    }

    @Transactional
    public void unlock(Long skuId, Long batchId) {
        StockBatch batch = getBatchForUpdate(skuId, batchId);
        if (!STATUS_LOCKED.equals(batch.getStatus())) {
            throw new BusinessException("当前批次状态不允许解锁");
        }
        String targetStatus = isExpiredWithQuantity(batch, LocalDate.now()) ? STATUS_EXPIRED : STATUS_AVAILABLE;
        Stock stock = getStockForUpdate(skuId);
        ensureBatchUpdated(stockBatchMapper.updateStatus(batchId, skuId, targetStatus));
        moveStockBucket(stock, skuId, STATUS_LOCKED, targetStatus, safeQuantity(batch));
        writeStatusLog(batch, SOURCE_TYPE_BATCH_UNLOCK);
    }

    @Transactional
    public void damage(Long skuId, Long batchId, StockBatchDamageRequest request) {
        validateDamageInput(skuId, batchId, request);
        Stock stock = getStockForUpdate(skuId);
        StockBatch batch = getBatchForUpdate(skuId, batchId);
        validateDamageRequest(batch, request);

        int beforeQuantity = batch.getQuantity();
        int damageQuantity = request.getQuantity();
        int afterQuantity = beforeQuantity - damageQuantity;
        String afterStatus = afterQuantity == 0 ? STATUS_DAMAGED : batch.getStatus();

        decreaseStockForDamage(stock, skuId, damageQuantity, batch.getStatus());
        ensureBatchUpdated(stockBatchMapper.updateRemainingQuantityAndStatus(batchId, skuId, afterQuantity, afterStatus));
        writeQuantityLog(batch, -damageQuantity, afterQuantity, SOURCE_TYPE_BATCH_DAMAGE,
                request.getReason().trim(), normalizeRemark(request.getRemark()));
    }

    @Transactional
    public void close(Long skuId, Long batchId) {
        StockBatch batch = getBatchForUpdate(skuId, batchId);
        if (STATUS_CLOSED.equals(batch.getStatus())) {
            throw new BusinessException("当前批次已关闭");
        }
        if (batch.getQuantity() == null || batch.getQuantity() != 0) {
            throw new BusinessException("仅剩余数量为0的批次允许关闭");
        }
        ensureBatchUpdated(stockBatchMapper.updateStatus(batchId, skuId, STATUS_CLOSED));
        writeStatusLog(batch, SOURCE_TYPE_BATCH_CLOSE);
    }

    @Transactional
    public int markExpiredBatches(LocalDate today) {
        if (today == null) {
            throw new BusinessException("过期扫描日期不能为空");
        }
        List<StockBatch> batches = stockBatchMapper.findExpiredAvailableBatchesForUpdate(today);
        Map<Long, Integer> expiredQuantityBySkuId = new LinkedHashMap<>();
        for (StockBatch batch : batches) {
            ensureBatchUpdated(stockBatchMapper.updateStatus(batch.getId(), batch.getSkuId(), STATUS_EXPIRED));
            expiredQuantityBySkuId.merge(batch.getSkuId(), safeQuantity(batch), Integer::sum);
            writeStatusLog(batch, SOURCE_TYPE_BATCH_EXPIRE_SCAN);
        }
        for (Map.Entry<Long, Integer> entry : expiredQuantityBySkuId.entrySet()) {
            Stock stock = getStockForUpdate(entry.getKey());
            moveStockBucket(stock, entry.getKey(), STATUS_AVAILABLE, STATUS_EXPIRED, entry.getValue());
        }
        return batches.size();
    }

    private Stock getStockForUpdate(Long skuId) {
        return stockMapper.findBySkuIdForUpdate(skuId)
                .orElseThrow(() -> new BusinessException(404, "库存记录不存在"));
    }

    private void decreaseStockForDamage(Stock stock, Long skuId, int quantity, String batchStatus) {
        int beforeTotal = stock.getTotalQuantity();
        int afterTotal = stockDomainService.decrease(beforeTotal, quantity);
        int afterAvailable = stock.getAvailableQuantity();
        int afterLocked = stock.getLockedQuantity();
        int afterExpired = stock.getExpiredQuantity();
        if (STATUS_AVAILABLE.equals(batchStatus)) {
            afterAvailable = stockDomainService.decrease(afterAvailable, quantity);
        } else if (STATUS_LOCKED.equals(batchStatus)) {
            afterLocked = stockDomainService.decrease(afterLocked, quantity);
        } else if (STATUS_EXPIRED.equals(batchStatus)) {
            afterExpired = stockDomainService.decrease(afterExpired, quantity);
        }
        stockMapper.updateQuantities(skuId, afterTotal, afterAvailable, afterLocked, afterExpired);
        stockMapper.insertLog(skuId, CHANGE_TYPE_DAMAGE, -quantity, beforeTotal, afterTotal);
    }

    private void moveStockBucket(Stock stock, Long skuId, String fromStatus, String toStatus, int quantity) {
        int available = stock.getAvailableQuantity();
        int locked = stock.getLockedQuantity();
        int expired = stock.getExpiredQuantity();
        if (quantity > 0) {
            if (STATUS_AVAILABLE.equals(fromStatus)) {
                available = stockDomainService.decrease(available, quantity);
            } else if (STATUS_LOCKED.equals(fromStatus)) {
                locked = stockDomainService.decrease(locked, quantity);
            } else if (STATUS_EXPIRED.equals(fromStatus)) {
                expired = stockDomainService.decrease(expired, quantity);
            }

            if (STATUS_AVAILABLE.equals(toStatus)) {
                available = stockDomainService.increase(available, quantity);
            } else if (STATUS_LOCKED.equals(toStatus)) {
                locked = stockDomainService.increase(locked, quantity);
            } else if (STATUS_EXPIRED.equals(toStatus)) {
                expired = stockDomainService.increase(expired, quantity);
            }
        }
        stockMapper.updateQuantities(skuId, stock.getTotalQuantity(), available, locked, expired);
    }

    private int safeQuantity(StockBatch batch) {
        return batch.getQuantity() == null ? 0 : batch.getQuantity();
    }

    private StockBatch getBatchForUpdate(Long skuId, Long batchId) {
        if (skuId == null) {
            throw new BusinessException("SKU ID不能为空");
        }
        if (batchId == null) {
            throw new BusinessException("库存批次ID不能为空");
        }
        return stockBatchMapper.findByIdAndSkuIdForUpdate(batchId, skuId)
                .orElseThrow(() -> new BusinessException(404, "库存批次不存在"));
    }

    private void writeStatusLog(StockBatch batch, String sourceType) {
        int quantity = batch.getQuantity() == null ? 0 : batch.getQuantity();
        StockBatchLog log = baseLog(batch, CHANGE_TYPE_BATCH_STATUS, sourceType);
        log.setChangeQuantity(0);
        log.setBeforeQuantity(quantity);
        log.setAfterQuantity(quantity);
        stockBatchMapper.insertLog(log);
    }

    private void writeQuantityLog(StockBatch batch, int changeQuantity, int afterQuantity,
                                  String sourceType, String reason, String remark) {
        StockBatchLog log = baseLog(batch, CHANGE_TYPE_DAMAGE, sourceType);
        log.setChangeQuantity(changeQuantity);
        log.setBeforeQuantity(batch.getQuantity());
        log.setAfterQuantity(afterQuantity);
        log.setReason(reason);
        log.setRemark(remark);
        stockBatchMapper.insertLog(log);
    }

    private StockBatchLog baseLog(StockBatch batch, String changeType, String sourceType) {
        StockBatchLog log = new StockBatchLog();
        log.setStockBatchId(batch.getId());
        log.setSkuId(batch.getSkuId());
        log.setChangeType(changeType);
        log.setSourceType(sourceType);
        log.setSourceId(batch.getId());
        return log;
    }

    private void validateDamageInput(Long skuId, Long batchId, StockBatchDamageRequest request) {
        if (skuId == null) {
            throw new BusinessException("SKU ID不能为空");
        }
        if (batchId == null) {
            throw new BusinessException("库存批次ID不能为空");
        }
        if (request == null) {
            throw new BusinessException("报损参数不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("报损数量必须大于0");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException("报损原因不能为空");
        }
    }

    private void validateDamageRequest(StockBatch batch, StockBatchDamageRequest request) {
        if (!STATUS_AVAILABLE.equals(batch.getStatus())
                && !STATUS_EXPIRED.equals(batch.getStatus())
                && !STATUS_LOCKED.equals(batch.getStatus())) {
            throw new BusinessException("当前批次状态不允许报损");
        }
        if (batch.getQuantity() == null || request.getQuantity() > batch.getQuantity()) {
            throw new BusinessException("报损数量不能超过批次剩余数量");
        }
    }

    private void validateConsumption(Long skuId, int quantity) {
        if (skuId == null) {
            throw new BusinessException("SKU ID不能为空");
        }
        if (quantity <= 0) {
            throw new BusinessException("库存变化数量必须大于0");
        }
    }

    private void validateSource(String sourceType, Long sourceId) {
        if (sourceType == null || sourceType.isBlank()) {
            throw new BusinessException("库存来源类型不能为空");
        }
        if (sourceId == null) {
            throw new BusinessException("库存来源ID不能为空");
        }
    }

    private void ensureBatchUpdated(int updatedRows) {
        if (updatedRows != 1) {
            throw new BusinessException("库存批次扣减失败");
        }
    }

    private String normalizeRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String normalized = remark.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isExpiredWithQuantity(StockBatch batch, LocalDate today) {
        return batch.getExpireDate() != null
                && batch.getExpireDate().isBefore(today)
                && batch.getQuantity() != null
                && batch.getQuantity() > 0;
    }

    private void validateCreateCommand(StockBatchCreateCommand command) {
        if (command == null) {
            throw new BusinessException("库存批次创建参数不能为空");
        }
        if (command.getSkuId() == null) {
            throw new BusinessException("SKU ID不能为空");
        }
        if (command.getPurchaseInboundReceiptBatchId() == null) {
            throw new BusinessException("采购入库执行批次ID不能为空");
        }
        if (command.getBaseQuantity() == null || command.getBaseQuantity() <= 0) {
            throw new BusinessException("批次数量必须大于0");
        }
        if (command.getPurchasePrice() == null || command.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("批次进价不能小于0");
        }
        if (command.getCostPrice() == null || command.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("批次成本价不能小于0");
        }
        if (command.getProductionDate() == null) {
            throw new BusinessException("生产日期不能为空");
        }
        if (command.getShelfLifeDays() == null || command.getShelfLifeDays() <= 0) {
            throw new BusinessException("保质期天数必须大于0");
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
                throw new BusinessException("库存批次号序号异常");
            }
        }
        if (sequence > 999) {
            throw new BusinessException("库存批次号当日序号已达上限");
        }
        return prefix + String.format("%03d", sequence);
    }

    public record BatchConsumption(
            Long stockBatchId,
            Long skuId,
            int beforeQuantity,
            int consumedQuantity,
            int afterQuantity
    ) {
    }
}
