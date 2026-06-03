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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBatchServiceTest {

    @Mock
    private StockBatchMapper stockBatchMapper;

    @Mock
    private StockMapper stockMapper;

    private StockBatchService stockBatchService;

    @BeforeEach
    void setUp() {
        stockBatchService = new StockBatchService(stockBatchMapper, stockMapper, new StockDomainService());
    }

    @Test
    void createFromPurchaseInboundReceiptBatch_copiesReceiptBatchSourceAndPrices() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(null);
        when(stockBatchMapper.insertBatch(any(StockBatch.class))).thenReturn(10L);

        StockBatch result = stockBatchService.createFromPurchaseInboundReceiptBatch(command());

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getBatchNo()).isEqualTo(todayPrefix + "001");
        assertThat(result.getSkuId()).isEqualTo(20L);
        assertThat(result.getPurchaseInboundReceiptBatchId()).isEqualTo(300L);
        assertThat(result.getInitialQuantity()).isEqualTo(48);
        assertThat(result.getQuantity()).isEqualTo(48);
        assertThat(result.getStatus()).isEqualTo("AVAILABLE");
        assertThat(result.getPurchasePrice()).isEqualByComparingTo("48.000000");
        assertThat(result.getCostPrice()).isEqualByComparingTo("2.00000000");
        assertThat(result.getProductionDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.getShelfLifeDays()).isEqualTo(180);
        assertThat(result.getExpireDate()).isEqualTo(LocalDate.of(2026, 11, 28));

        ArgumentCaptor<StockBatch> batchCaptor = ArgumentCaptor.forClass(StockBatch.class);
        verify(stockBatchMapper).insertBatch(batchCaptor.capture());
        StockBatch savedBatch = batchCaptor.getValue();
        assertThat(savedBatch.getBatchNo()).isEqualTo(result.getBatchNo());
        assertThat(savedBatch.getInitialQuantity()).isEqualTo(48);
        assertThat(savedBatch.getQuantity()).isEqualTo(48);
        assertThat(savedBatch.getStatus()).isEqualTo("AVAILABLE");
        assertThat(savedBatch.getPurchaseInboundReceiptBatchId()).isEqualTo(300L);
        assertThat(savedBatch.getPurchasePrice()).isEqualByComparingTo("48.000000");
        assertThat(savedBatch.getCostPrice()).isEqualByComparingTo("2.00000000");
        assertThat(savedBatch.getExpireDate()).isEqualTo(LocalDate.of(2026, 11, 28));
    }

    @Test
    void createFromPurchaseInboundReceiptBatch_usesNextSequence() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(todayPrefix + "009");
        when(stockBatchMapper.insertBatch(any(StockBatch.class))).thenReturn(11L);

        StockBatch result = stockBatchService.createFromPurchaseInboundReceiptBatch(command());

        assertThat(result.getBatchNo()).isEqualTo(todayPrefix + "010");
    }

    @Test
    void createFromPurchaseInboundReceiptBatch_rejectsInvalidBatchNoSuffix() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(todayPrefix + "ABC");

        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundReceiptBatch(command()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存批次号序号异常");

        verify(stockBatchMapper, never()).insertBatch(any(StockBatch.class));
    }

    @Test
    void createFromPurchaseInboundReceiptBatch_translatesDuplicateBatchNo() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(null);
        when(stockBatchMapper.insertBatch(any(StockBatch.class)))
                .thenThrow(new DuplicateKeyException("duplicate batch_no"));

        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundReceiptBatch(command()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存批次号重复，请重试");
    }

    @Test
    void createFromPurchaseInboundReceiptBatch_rejectsInvalidCommandFields() {
        assertInvalidCommand(null, "库存批次创建参数不能为空");

        StockBatchCreateCommand missingSkuId = command();
        missingSkuId.setSkuId(null);
        assertInvalidCommand(missingSkuId, "SKU ID不能为空");

        StockBatchCreateCommand missingPurchaseInboundReceiptBatchId = command();
        missingPurchaseInboundReceiptBatchId.setPurchaseInboundReceiptBatchId(null);
        assertInvalidCommand(missingPurchaseInboundReceiptBatchId, "采购入库执行批次ID不能为空");

        StockBatchCreateCommand missingBaseQuantity = command();
        missingBaseQuantity.setBaseQuantity(null);
        assertInvalidCommand(missingBaseQuantity, "批次数量必须大于0");

        StockBatchCreateCommand zeroBaseQuantity = command();
        zeroBaseQuantity.setBaseQuantity(0);
        assertInvalidCommand(zeroBaseQuantity, "批次数量必须大于0");

        StockBatchCreateCommand missingPurchasePrice = command();
        missingPurchasePrice.setPurchasePrice(null);
        assertInvalidCommand(missingPurchasePrice, "批次进价不能小于0");

        StockBatchCreateCommand negativePurchasePrice = command();
        negativePurchasePrice.setPurchasePrice(new BigDecimal("-0.01"));
        assertInvalidCommand(negativePurchasePrice, "批次进价不能小于0");

        StockBatchCreateCommand missingCostPrice = command();
        missingCostPrice.setCostPrice(null);
        assertInvalidCommand(missingCostPrice, "批次成本价不能小于0");

        StockBatchCreateCommand negativeCostPrice = command();
        negativeCostPrice.setCostPrice(new BigDecimal("-0.00000001"));
        assertInvalidCommand(negativeCostPrice, "批次成本价不能小于0");

        StockBatchCreateCommand missingProductionDate = command();
        missingProductionDate.setProductionDate(null);
        assertInvalidCommand(missingProductionDate, "生产日期不能为空");

        StockBatchCreateCommand missingShelfLifeDays = command();
        missingShelfLifeDays.setShelfLifeDays(null);
        assertInvalidCommand(missingShelfLifeDays, "保质期天数必须大于0");

        StockBatchCreateCommand zeroShelfLifeDays = command();
        zeroShelfLifeDays.setShelfLifeDays(0);
        assertInvalidCommand(zeroShelfLifeDays, "保质期天数必须大于0");

        verify(stockBatchMapper, never()).insertBatch(any(StockBatch.class));
    }

    @Test
    void createFromPurchaseInboundReceiptBatch_rejectsMissingGeneratedKey() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(null);
        when(stockBatchMapper.insertBatch(any(StockBatch.class))).thenReturn(null);

        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundReceiptBatch(command()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存批次保存失败");
    }

    @Test
    void createFromPurchaseInboundReceiptBatch_rejectsSequenceBeyondDailyLimit() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(todayPrefix + "999");

        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundReceiptBatch(command()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存批次号当日序号已达上限");

        verify(stockBatchMapper, never()).insertBatch(any(StockBatch.class));
    }

    @Test
    void writePurchaseInboundLog_writesBatchLog() {
        StockBatch batch = batch();

        stockBatchService.writePurchaseInboundLog(batch);

        ArgumentCaptor<StockBatchLog> logCaptor = ArgumentCaptor.forClass(StockBatchLog.class);
        verify(stockBatchMapper).insertLog(logCaptor.capture());
        StockBatchLog log = logCaptor.getValue();
        assertThat(log.getStockBatchId()).isEqualTo(10L);
        assertThat(log.getSkuId()).isEqualTo(20L);
        assertThat(log.getChangeType()).isEqualTo("PURCHASE_INBOUND");
        assertThat(log.getChangeQuantity()).isEqualTo(48);
        assertThat(log.getBeforeQuantity()).isEqualTo(0);
        assertThat(log.getAfterQuantity()).isEqualTo(48);
        assertThat(log.getSourceType()).isEqualTo("PURCHASE_INBOUND_RECEIPT_BATCH");
        assertThat(log.getSourceId()).isEqualTo(300L);
    }

    @Test
    void consumeByFefo_consumesEarliestExpiringBatchesAndUpdatesStatus() {
        StockBatch earlyBatch = batch(10L, 20L, 5, LocalDate.of(2026, 7, 1));
        StockBatch laterBatch = batch(11L, 20L, 10, LocalDate.of(2026, 8, 1));
        when(stockBatchMapper.findConsumableBySkuIdForUpdate(20L))
                .thenReturn(List.of(earlyBatch, laterBatch));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 0, "DEPLETED")).thenReturn(1);
        when(stockBatchMapper.updateRemainingQuantityAndStatus(11L, 20L, 7, "AVAILABLE")).thenReturn(1);

        List<StockBatchService.BatchConsumption> consumptions = stockBatchService.consumeByFefo(20L, 8);

        assertThat(consumptions).containsExactly(
                new StockBatchService.BatchConsumption(10L, 20L, 5, 5, 0),
                new StockBatchService.BatchConsumption(11L, 20L, 10, 3, 7)
        );
        var inOrder = inOrder(stockBatchMapper);
        inOrder.verify(stockBatchMapper).updateRemainingQuantityAndStatus(10L, 20L, 0, "DEPLETED");
        inOrder.verify(stockBatchMapper).updateRemainingQuantityAndStatus(11L, 20L, 7, "AVAILABLE");
    }

    @Test
    void consumeByFefo_rejectsInsufficientBatchStockWithoutUpdating() {
        when(stockBatchMapper.findConsumableBySkuIdForUpdate(20L))
                .thenReturn(List.of(batch(10L, 20L, 2, LocalDate.of(2026, 7, 1))));

        assertThatThrownBy(() -> stockBatchService.consumeByFefo(20L, 3))
                .isInstanceOf(BusinessException.class);

        verify(stockBatchMapper, never()).updateRemainingQuantityAndStatus(anyLong(), anyLong(), anyInt(), anyString());
        verify(stockBatchMapper, never()).insertLog(any(StockBatchLog.class));
    }

    @Test
    void writeOutboundLogs_writesNegativeBatchLogsForEachConsumption() {
        List<StockBatchService.BatchConsumption> consumptions = List.of(
                new StockBatchService.BatchConsumption(10L, 20L, 5, 5, 0),
                new StockBatchService.BatchConsumption(11L, 20L, 10, 3, 7)
        );

        stockBatchService.writeOutboundLogs(consumptions, "OUTBOUND_ORDER", 300L);

        ArgumentCaptor<StockBatchLog> logCaptor = ArgumentCaptor.forClass(StockBatchLog.class);
        verify(stockBatchMapper, times(2)).insertLog(logCaptor.capture());
        List<StockBatchLog> logs = logCaptor.getAllValues();
        assertThat(logs.get(0).getStockBatchId()).isEqualTo(10L);
        assertThat(logs.get(0).getSkuId()).isEqualTo(20L);
        assertThat(logs.get(0).getChangeType()).isEqualTo("OUTBOUND");
        assertThat(logs.get(0).getChangeQuantity()).isEqualTo(-5);
        assertThat(logs.get(0).getBeforeQuantity()).isEqualTo(5);
        assertThat(logs.get(0).getAfterQuantity()).isEqualTo(0);
        assertThat(logs.get(0).getSourceType()).isEqualTo("OUTBOUND_ORDER");
        assertThat(logs.get(0).getSourceId()).isEqualTo(300L);
        assertThat(logs.get(1).getStockBatchId()).isEqualTo(11L);
        assertThat(logs.get(1).getChangeQuantity()).isEqualTo(-3);
        assertThat(logs.get(1).getBeforeQuantity()).isEqualTo(10);
        assertThat(logs.get(1).getAfterQuantity()).isEqualTo(7);
    }

    @Test
    void listBySkuId_delegatesToMapper() {
        StockBatchVO batch = new StockBatchVO();
        batch.setId(10L);
        when(stockBatchMapper.findBySkuId(20L)).thenReturn(List.of(batch));

        List<StockBatchVO> result = stockBatchService.listBySkuId(20L);

        assertThat(result).containsExactly(batch);
        verify(stockBatchMapper).findBySkuId(20L);
    }

    @Test
    void lock_changesAvailableBatchToLockedAndWritesStatusLog() {
        StockBatch batch = batch("AVAILABLE", 48);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 60, 50, 6, 4)));
        when(stockBatchMapper.updateStatus(10L, 20L, "LOCKED")).thenReturn(1);

        stockBatchService.lock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "LOCKED");
        verify(stockMapper).updateQuantities(20L, 60, 2, 54, 4);
        StockBatchLog log = captureLog();
        assertStatusLog(log, "BATCH_LOCK", 48);
    }

    @Test
    void lock_changesExpiredBatchToLockedAndWritesStatusLog() {
        StockBatch batch = batch("EXPIRED", 48);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 60, 4, 8, 48)));
        when(stockBatchMapper.updateStatus(10L, 20L, "LOCKED")).thenReturn(1);

        stockBatchService.lock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "LOCKED");
        verify(stockMapper).updateQuantities(20L, 60, 4, 56, 0);
        StockBatchLog log = captureLog();
        assertStatusLog(log, "BATCH_LOCK", 48);
    }

    @Test
    void lock_rejectsIllegalStatus() {
        StockBatch batch = batch("LOCKED", 48);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));

        assertThatThrownBy(() -> stockBatchService.lock(20L, 10L))
                .isInstanceOf(BusinessException.class);

        verify(stockBatchMapper, never()).updateStatus(any(), any(), any());
        verify(stockBatchMapper, never()).insertLog(any());
    }

    @Test
    void unlock_changesLockedUnexpiredBatchToAvailable() {
        StockBatch batch = batch("LOCKED", 48);
        batch.setExpireDate(LocalDate.now().plusDays(1));
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 60, 5, 50, 5)));
        when(stockBatchMapper.updateStatus(10L, 20L, "AVAILABLE")).thenReturn(1);

        stockBatchService.unlock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "AVAILABLE");
        verify(stockMapper).updateQuantities(20L, 60, 53, 2, 5);
        StockBatchLog log = captureLog();
        assertThat(log.getSourceType()).isEqualTo("BATCH_UNLOCK");
        assertThat(log.getChangeType()).isEqualTo("BATCH_STATUS");
        assertThat(log.getBeforeQuantity()).isEqualTo(48);
        assertThat(log.getAfterQuantity()).isEqualTo(48);
    }

    @Test
    void unlock_changesLockedExpiredBatchWithQuantityToExpired() {
        StockBatch batch = batch("LOCKED", 48);
        batch.setExpireDate(LocalDate.now().minusDays(1));
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 60, 5, 50, 5)));
        when(stockBatchMapper.updateStatus(10L, 20L, "EXPIRED")).thenReturn(1);

        stockBatchService.unlock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "EXPIRED");
        verify(stockMapper).updateQuantities(20L, 60, 5, 2, 53);
        StockBatchLog log = captureLog();
        assertThat(log.getSourceType()).isEqualTo("BATCH_UNLOCK");
    }

    @Test
    void unlock_changesLockedExpiredBatchWithoutQuantityToAvailable() {
        StockBatch batch = batch("LOCKED", 0);
        batch.setExpireDate(LocalDate.now().minusDays(1));
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 60, 5, 50, 5)));
        when(stockBatchMapper.updateStatus(10L, 20L, "AVAILABLE")).thenReturn(1);

        stockBatchService.unlock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "AVAILABLE");
        verify(stockMapper).updateQuantities(20L, 60, 5, 50, 5);
        StockBatchLog log = captureLog();
        assertThat(log.getSourceType()).isEqualTo("BATCH_UNLOCK");
        assertThat(log.getBeforeQuantity()).isEqualTo(0);
        assertThat(log.getAfterQuantity()).isEqualTo(0);
    }

    @Test
    void damage_partiallyDamagesLockedBatchKeepsLockedStatusAndWritesDamageLog() {
        StockBatch batch = batch("LOCKED", 48);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(java.util.Optional.of(stock(20L, 60, 8, 48, 4)));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 43, "LOCKED")).thenReturn(1);

        stockBatchService.damage(20L, 10L, damageRequest(5, "破损", "外包装破损 "));

        verify(stockMapper).updateQuantities(20L, 55, 8, 43, 4);
        verify(stockMapper).insertLog(20L, "DAMAGE", -5, 60, 55);
        verify(stockBatchMapper).updateRemainingQuantityAndStatus(10L, 20L, 43, "LOCKED");
        StockBatchLog log = captureLog();
        assertThat(log.getStockBatchId()).isEqualTo(10L);
        assertThat(log.getSkuId()).isEqualTo(20L);
        assertThat(log.getChangeType()).isEqualTo("DAMAGE");
        assertThat(log.getChangeQuantity()).isEqualTo(-5);
        assertThat(log.getBeforeQuantity()).isEqualTo(48);
        assertThat(log.getAfterQuantity()).isEqualTo(43);
        assertThat(log.getSourceType()).isEqualTo("BATCH_DAMAGE");
        assertThat(log.getSourceId()).isEqualTo(10L);
        assertThat(log.getReason()).isEqualTo("破损");
        assertThat(log.getRemark()).isEqualTo("外包装破损");
    }

    @Test
    void damage_fullyDamagesBatchAndSetsDamaged() {
        StockBatch batch = batch("AVAILABLE", 48);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(java.util.Optional.of(stock(20L, 60, 48, 8, 4)));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 0, "DAMAGED")).thenReturn(1);

        stockBatchService.damage(20L, 10L, damageRequest(48, "过期", null));

        verify(stockMapper).updateQuantities(20L, 12, 0, 8, 4);
        verify(stockMapper).insertLog(20L, "DAMAGE", -48, 60, 12);
        verify(stockBatchMapper).updateRemainingQuantityAndStatus(10L, 20L, 0, "DAMAGED");
    }

    @Test
    void damage_partiallyDamagesExpiredBatchDeductsExpiredBucket() {
        StockBatch batch = batch("EXPIRED", 12);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(java.util.Optional.of(stock(20L, 60, 40, 8, 12)));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 7, "EXPIRED")).thenReturn(1);

        stockBatchService.damage(20L, 10L, damageRequest(5, "过期", null));

        verify(stockMapper).updateQuantities(20L, 55, 40, 8, 7);
        verify(stockMapper).insertLog(20L, "DAMAGE", -5, 60, 55);
        verify(stockBatchMapper).updateRemainingQuantityAndStatus(10L, 20L, 7, "EXPIRED");
    }

    @Test
    void damage_rejectsIllegalStatuses() {
        for (String status : List.of("DEPLETED", "DAMAGED", "CLOSED")) {
            org.mockito.Mockito.reset(stockBatchMapper, stockMapper);
            StockBatch batch = batch(status, 48);
            when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 60, 60, 0, 0)));
            when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));

            assertThatThrownBy(() -> stockBatchService.damage(20L, 10L, damageRequest(1, "破损", null)))
                    .isInstanceOf(BusinessException.class);
        }

        verify(stockMapper, never()).updateQuantity(anyLong(), anyInt());
        verify(stockMapper, never()).updateQuantities(anyLong(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(stockMapper, never()).insertLog(anyLong(), anyString(), anyInt(), anyInt(), anyInt());
        verify(stockBatchMapper, never()).updateRemainingQuantityAndStatus(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void damage_rejectsMissingStockWithoutLockingBatch() {
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockBatchService.damage(20L, 10L, damageRequest(1, "破损", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存记录不存在");

        verify(stockBatchMapper, never()).findByIdAndSkuIdForUpdate(anyLong(), anyLong());
        verify(stockMapper, never()).updateQuantity(anyLong(), anyInt());
        verify(stockMapper, never()).updateQuantities(anyLong(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(stockMapper, never()).insertLog(anyLong(), anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void damage_rejectsInsufficientStockBeforeUpdatingBatch() {
        StockBatch batch = batch("AVAILABLE", 48);
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 3, 3, 0, 0)));
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> stockBatchService.damage(20L, 10L, damageRequest(5, "破损", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存不足，无法出库");

        verify(stockMapper, never()).updateQuantity(anyLong(), anyInt());
        verify(stockMapper, never()).updateQuantities(anyLong(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(stockMapper, never()).insertLog(anyLong(), anyString(), anyInt(), anyInt(), anyInt());
        verify(stockBatchMapper, never()).updateRemainingQuantityAndStatus(anyLong(), anyLong(), anyInt(), anyString());
        verify(stockBatchMapper, never()).insertLog(any());
    }

    @Test
    void damage_rejectsBatchUpdateMissAndDoesNotWriteBatchLog() {
        StockBatch batch = batch("AVAILABLE", 48);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(java.util.Optional.of(stock(20L, 60, 48, 8, 4)));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 43, "AVAILABLE")).thenReturn(0);

        assertThatThrownBy(() -> stockBatchService.damage(20L, 10L, damageRequest(5, "破损", null)))
                .isInstanceOf(BusinessException.class);

        verify(stockMapper).updateQuantities(20L, 55, 43, 8, 4);
        verify(stockMapper).insertLog(20L, "DAMAGE", -5, 60, 55);
        verify(stockBatchMapper, never()).insertLog(any());
    }

    @Test
    void damage_rejectsQuantityGreaterThanRemaining() {
        StockBatch batch = batch("AVAILABLE", 3);
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(java.util.Optional.of(stock(20L, 60, 60, 0, 0)));
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));

        assertThatThrownBy(() -> stockBatchService.damage(20L, 10L, damageRequest(4, "破损", null)))
                .isInstanceOf(BusinessException.class);

        verify(stockMapper, never()).updateQuantity(anyLong(), anyInt());
        verify(stockMapper, never()).updateQuantities(anyLong(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(stockMapper, never()).insertLog(anyLong(), anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void close_rejectsBatchWithRemainingQuantity() {
        StockBatch batch = batch("DEPLETED", 1);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));

        assertThatThrownBy(() -> stockBatchService.close(20L, 10L))
                .isInstanceOf(BusinessException.class);

        verify(stockBatchMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void close_rejectsAlreadyClosedEmptyBatch() {
        StockBatch batch = batch("CLOSED", 0);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));

        assertThatThrownBy(() -> stockBatchService.close(20L, 10L))
                .isInstanceOf(BusinessException.class);

        verify(stockBatchMapper, never()).updateStatus(any(), any(), any());
        verify(stockBatchMapper, never()).insertLog(any());
    }

    @Test
    void close_changesAnyEmptyLifecycleBatchToClosed() {
        for (String status : List.of("AVAILABLE", "EXPIRED", "LOCKED", "DEPLETED", "DAMAGED")) {
            org.mockito.Mockito.reset(stockBatchMapper);
            StockBatch batch = batch(status, 0);
            when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
            when(stockBatchMapper.updateStatus(10L, 20L, "CLOSED")).thenReturn(1);

            stockBatchService.close(20L, 10L);

            verify(stockBatchMapper).updateStatus(10L, 20L, "CLOSED");
            StockBatchLog log = captureLog();
            assertStatusLog(log, "BATCH_CLOSE", 0);
        }
    }

    @Test
    void markExpiredBatches_updatesMapperReturnedBatchesWritesLogsAndReturnsCount() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        StockBatch first = batch("AVAILABLE", 5);
        first.setId(10L);
        StockBatch second = batch("AVAILABLE", 8);
        second.setId(11L);
        when(stockBatchMapper.findExpiredAvailableBatchesForUpdate(today)).thenReturn(List.of(first, second));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 60, 40, 8, 12)));
        when(stockBatchMapper.updateStatus(10L, 20L, "EXPIRED")).thenReturn(1);
        when(stockBatchMapper.updateStatus(11L, 20L, "EXPIRED")).thenReturn(1);

        int result = stockBatchService.markExpiredBatches(today);

        assertThat(result).isEqualTo(2);
        verify(stockBatchMapper).updateStatus(10L, 20L, "EXPIRED");
        verify(stockBatchMapper).updateStatus(11L, 20L, "EXPIRED");
        verify(stockMapper).updateQuantities(20L, 60, 27, 8, 25);
        ArgumentCaptor<StockBatchLog> logCaptor = ArgumentCaptor.forClass(StockBatchLog.class);
        verify(stockBatchMapper, org.mockito.Mockito.times(2)).insertLog(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .extracting(StockBatchLog::getSourceType)
                .containsExactly("BATCH_EXPIRE_SCAN", "BATCH_EXPIRE_SCAN");
    }

    @Test
    void consumeByFefoAndWriteOutboundLogs_consumesInMapperFefoOrderAndWritesOutboundLogs() {
        StockBatch first = batch("AVAILABLE", 5);
        first.setId(10L);
        StockBatch second = batch("AVAILABLE", 8);
        second.setId(11L);
        when(stockBatchMapper.findConsumableBySkuIdForUpdate(20L)).thenReturn(List.of(first, second));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 0, "DEPLETED")).thenReturn(1);
        when(stockBatchMapper.updateRemainingQuantityAndStatus(11L, 20L, 3, "AVAILABLE")).thenReturn(1);

        List<StockBatchService.BatchConsumption> consumptions = stockBatchService.consumeByFefo(20L, 10);
        stockBatchService.writeOutboundLogs(consumptions, "OUTBOUND_ORDER", 99L);

        verify(stockBatchMapper).updateRemainingQuantityAndStatus(10L, 20L, 0, "DEPLETED");
        verify(stockBatchMapper).updateRemainingQuantityAndStatus(11L, 20L, 3, "AVAILABLE");
        ArgumentCaptor<StockBatchLog> logCaptor = ArgumentCaptor.forClass(StockBatchLog.class);
        verify(stockBatchMapper, times(2)).insertLog(logCaptor.capture());
        assertThat(logCaptor.getAllValues()).extracting(StockBatchLog::getStockBatchId)
                .containsExactly(10L, 11L);
        assertThat(logCaptor.getAllValues()).extracting(StockBatchLog::getChangeType)
                .containsExactly("OUTBOUND", "OUTBOUND");
        assertThat(logCaptor.getAllValues()).extracting(StockBatchLog::getChangeQuantity)
                .containsExactly(-5, -5);
        assertThat(logCaptor.getAllValues()).extracting(StockBatchLog::getBeforeQuantity)
                .containsExactly(5, 8);
        assertThat(logCaptor.getAllValues()).extracting(StockBatchLog::getAfterQuantity)
                .containsExactly(0, 3);
        assertThat(logCaptor.getAllValues()).extracting(StockBatchLog::getSourceType)
                .containsExactly("OUTBOUND_ORDER", "OUTBOUND_ORDER");
        assertThat(logCaptor.getAllValues()).extracting(StockBatchLog::getSourceId)
                .containsExactly(99L, 99L);
    }

    @Test
    void consumeByFefo_rejectsWhenAvailableBatchesAreInsufficient() {
        StockBatch first = batch("AVAILABLE", 5);
        first.setId(10L);
        when(stockBatchMapper.findConsumableBySkuIdForUpdate(20L)).thenReturn(List.of(first));

        assertThatThrownBy(() -> stockBatchService.consumeByFefo(20L, 10))
                .isInstanceOf(BusinessException.class);

        verify(stockBatchMapper, never()).updateRemainingQuantityAndStatus(anyLong(), anyLong(), anyInt(), anyString());
        verify(stockBatchMapper, never()).insertLog(any());
    }

    @Test
    void consumeByFefo_rejectsBatchUpdateMissAndStopsLogging() {
        StockBatch first = batch("AVAILABLE", 5);
        first.setId(10L);
        when(stockBatchMapper.findConsumableBySkuIdForUpdate(20L)).thenReturn(List.of(first));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 0, "DEPLETED")).thenReturn(0);

        assertThatThrownBy(() -> stockBatchService.consumeByFefo(20L, 5))
                .isInstanceOf(BusinessException.class);

        verify(stockBatchMapper, never()).insertLog(any());
    }

    private void assertInvalidCommand(StockBatchCreateCommand command, String message) {
        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundReceiptBatch(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage(message);
    }

    private StockBatchCreateCommand command() {
        StockBatchCreateCommand command = new StockBatchCreateCommand();
        command.setSkuId(20L);
        command.setPurchaseInboundReceiptBatchId(300L);
        command.setBaseQuantity(48);
        command.setPurchasePrice(new BigDecimal("48.000000"));
        command.setCostPrice(new BigDecimal("2.00000000"));
        command.setProductionDate(LocalDate.of(2026, 6, 1));
        command.setShelfLifeDays(180);
        return command;
    }

    private StockBatch batch() {
        return batch("AVAILABLE", 48);
    }

    private StockBatch batch(String status, int quantity) {
        StockBatch batch = new StockBatch();
        batch.setId(10L);
        batch.setBatchNo("SB20260601001");
        batch.setSkuId(20L);
        batch.setPurchaseInboundReceiptBatchId(300L);
        batch.setInitialQuantity(48);
        batch.setQuantity(quantity);
        batch.setStatus(status);
        batch.setPurchasePrice(new BigDecimal("48.000000"));
        batch.setCostPrice(new BigDecimal("2.00000000"));
        batch.setProductionDate(LocalDate.of(2026, 6, 1));
        batch.setShelfLifeDays(180);
        batch.setExpireDate(LocalDate.of(2026, 11, 28));
        return batch;
    }

    private StockBatchDamageRequest damageRequest(int quantity, String reason, String remark) {
        StockBatchDamageRequest request = new StockBatchDamageRequest();
        request.setQuantity(quantity);
        request.setReason(reason);
        request.setRemark(remark);
        return request;
    }

    private StockBatchLog captureLog() {
        ArgumentCaptor<StockBatchLog> logCaptor = ArgumentCaptor.forClass(StockBatchLog.class);
        verify(stockBatchMapper).insertLog(logCaptor.capture());
        return logCaptor.getValue();
    }

    private void assertStatusLog(StockBatchLog log, String sourceType, int quantity) {
        assertThat(log.getStockBatchId()).isEqualTo(10L);
        assertThat(log.getSkuId()).isEqualTo(20L);
        assertThat(log.getChangeType()).isEqualTo("BATCH_STATUS");
        assertThat(log.getChangeQuantity()).isEqualTo(0);
        assertThat(log.getBeforeQuantity()).isEqualTo(quantity);
        assertThat(log.getAfterQuantity()).isEqualTo(quantity);
        assertThat(log.getSourceType()).isEqualTo(sourceType);
        assertThat(log.getSourceId()).isEqualTo(10L);
    }

    private StockBatch batch(Long id, Long skuId, int quantity, LocalDate expireDate) {
        StockBatch batch = new StockBatch();
        batch.setId(id);
        batch.setSkuId(skuId);
        batch.setStatus("AVAILABLE");
        batch.setQuantity(quantity);
        batch.setExpireDate(expireDate);
        return batch;
    }

    private Stock stock(Long skuId, int quantity) {
        return stock(skuId, quantity, quantity, 0, 0);
    }

    private Stock stock(Long skuId, int totalQuantity, int availableQuantity, int lockedQuantity, int expiredQuantity) {
        Stock stock = new Stock();
        stock.setId(30L);
        stock.setSkuId(skuId);
        stock.setTotalQuantity(totalQuantity);
        stock.setAvailableQuantity(availableQuantity);
        stock.setLockedQuantity(lockedQuantity);
        stock.setExpiredQuantity(expiredQuantity);
        stock.setMinStock(0);
        stock.setMaxStock(100);
        return stock;
    }
}
