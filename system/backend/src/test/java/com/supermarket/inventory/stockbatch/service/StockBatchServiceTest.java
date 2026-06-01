package com.supermarket.inventory.stockbatch.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.stock.service.StockService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBatchServiceTest {

    @Mock
    private StockBatchMapper stockBatchMapper;

    @Mock
    private StockService stockService;

    private StockBatchService stockBatchService;

    @BeforeEach
    void setUp() {
        stockBatchService = new StockBatchService(stockBatchMapper, stockService);
    }

    @Test
    void createFromPurchaseInboundItem_generatesBatchAndCalculatesExpireDate() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(null);
        when(stockBatchMapper.insertBatch(any(StockBatch.class))).thenReturn(10L);

        StockBatch result = stockBatchService.createFromPurchaseInboundItem(command());

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getBatchNo()).isEqualTo(todayPrefix + "001");
        assertThat(result.getSkuId()).isEqualTo(20L);
        assertThat(result.getPurchaseInboundItemId()).isEqualTo(7L);
        assertThat(result.getInitialQuantity()).isEqualTo(48);
        assertThat(result.getQuantity()).isEqualTo(48);
        assertThat(result.getStatus()).isEqualTo("AVAILABLE");
        assertThat(result.getPurchasePrice()).isEqualByComparingTo("48.00");
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
        assertThat(savedBatch.getExpireDate()).isEqualTo(LocalDate.of(2026, 11, 28));
    }

    @Test
    void createFromPurchaseInboundItem_usesNextSequence() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(todayPrefix + "009");
        when(stockBatchMapper.insertBatch(any(StockBatch.class))).thenReturn(11L);

        StockBatch result = stockBatchService.createFromPurchaseInboundItem(command());

        assertThat(result.getBatchNo()).isEqualTo(todayPrefix + "010");
    }

    @Test
    void createFromPurchaseInboundItem_rejectsInvalidBatchNoSuffix() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(todayPrefix + "ABC");

        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundItem(command()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存批次号序号异常");

        verify(stockBatchMapper, never()).insertBatch(any(StockBatch.class));
    }

    @Test
    void createFromPurchaseInboundItem_translatesDuplicateBatchNo() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(null);
        when(stockBatchMapper.insertBatch(any(StockBatch.class)))
                .thenThrow(new DuplicateKeyException("duplicate batch_no"));

        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundItem(command()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存批次号重复，请重试");
    }

    @Test
    void createFromPurchaseInboundItem_rejectsInvalidCommandFields() {
        assertInvalidCommand(null, "库存批次创建参数不能为空");

        StockBatchCreateCommand missingSkuId = command();
        missingSkuId.setSkuId(null);
        assertInvalidCommand(missingSkuId, "SKU ID不能为空");

        StockBatchCreateCommand missingPurchaseInboundItemId = command();
        missingPurchaseInboundItemId.setPurchaseInboundItemId(null);
        assertInvalidCommand(missingPurchaseInboundItemId, "采购入库明细ID不能为空");

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
    void createFromPurchaseInboundItem_rejectsMissingGeneratedKey() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(null);
        when(stockBatchMapper.insertBatch(any(StockBatch.class))).thenReturn(null);

        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundItem(command()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("库存批次保存失败");
    }

    @Test
    void createFromPurchaseInboundItem_rejectsSequenceBeyondDailyLimit() {
        String todayPrefix = "SB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(stockBatchMapper.findMaxBatchNo(todayPrefix + "%")).thenReturn(todayPrefix + "999");

        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundItem(command()))
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
        assertThat(log.getSourceType()).isEqualTo("PURCHASE_INBOUND_ITEM");
        assertThat(log.getSourceId()).isEqualTo(7L);
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

        stockBatchService.lock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "LOCKED");
        StockBatchLog log = captureLog();
        assertStatusLog(log, "BATCH_LOCK", 48);
    }

    @Test
    void lock_changesExpiredBatchToLockedAndWritesStatusLog() {
        StockBatch batch = batch("EXPIRED", 48);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));

        stockBatchService.lock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "LOCKED");
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

        stockBatchService.unlock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "AVAILABLE");
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

        stockBatchService.unlock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "EXPIRED");
        StockBatchLog log = captureLog();
        assertThat(log.getSourceType()).isEqualTo("BATCH_UNLOCK");
    }

    @Test
    void unlock_changesLockedExpiredBatchWithoutQuantityToAvailable() {
        StockBatch batch = batch("LOCKED", 0);
        batch.setExpireDate(LocalDate.now().minusDays(1));
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));

        stockBatchService.unlock(20L, 10L);

        verify(stockBatchMapper).updateStatus(10L, 20L, "AVAILABLE");
        StockBatchLog log = captureLog();
        assertThat(log.getSourceType()).isEqualTo("BATCH_UNLOCK");
        assertThat(log.getBeforeQuantity()).isEqualTo(0);
        assertThat(log.getAfterQuantity()).isEqualTo(0);
    }

    @Test
    void damage_partiallyDamagesLockedBatchKeepsLockedStatusAndWritesDamageLog() {
        StockBatch batch = batch("LOCKED", 48);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 43, "LOCKED")).thenReturn(1);

        stockBatchService.damage(20L, 10L, damageRequest(5, "破损", "外包装破损 "));

        verify(stockService).damage(20L, 5);
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
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 0, "DAMAGED")).thenReturn(1);

        stockBatchService.damage(20L, 10L, damageRequest(48, "过期", null));

        verify(stockService).damage(20L, 48);
        verify(stockBatchMapper).updateRemainingQuantityAndStatus(10L, 20L, 0, "DAMAGED");
    }

    @Test
    void damage_rejectsIllegalStatuses() {
        for (String status : List.of("DEPLETED", "DAMAGED", "CLOSED")) {
            StockBatch batch = batch(status, 48);
            when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));

            assertThatThrownBy(() -> stockBatchService.damage(20L, 10L, damageRequest(1, "破损", null)))
                    .isInstanceOf(BusinessException.class);
        }

        verify(stockService, never()).damage(any(), any(Integer.class));
        verify(stockBatchMapper, never()).updateRemainingQuantityAndStatus(any(), any(), any(Integer.class), any());
    }

    @Test
    void damage_rejectsBatchUpdateMissAndDoesNotWriteBatchLog() {
        StockBatch batch = batch("AVAILABLE", 48);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 43, "AVAILABLE")).thenReturn(0);

        assertThatThrownBy(() -> stockBatchService.damage(20L, 10L, damageRequest(5, "鐮存崯", null)))
                .isInstanceOf(BusinessException.class);

        verify(stockService).damage(20L, 5);
        verify(stockBatchMapper, never()).insertLog(any());
    }

    @Test
    void damage_rejectsQuantityGreaterThanRemaining() {
        StockBatch batch = batch("AVAILABLE", 3);
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L)).thenReturn(java.util.Optional.of(batch));

        assertThatThrownBy(() -> stockBatchService.damage(20L, 10L, damageRequest(4, "破损", null)))
                .isInstanceOf(BusinessException.class);

        verify(stockService, never()).damage(any(), any(Integer.class));
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

        int result = stockBatchService.markExpiredBatches(today);

        assertThat(result).isEqualTo(2);
        verify(stockBatchMapper).updateStatus(10L, 20L, "EXPIRED");
        verify(stockBatchMapper).updateStatus(11L, 20L, "EXPIRED");
        ArgumentCaptor<StockBatchLog> logCaptor = ArgumentCaptor.forClass(StockBatchLog.class);
        verify(stockBatchMapper, org.mockito.Mockito.times(2)).insertLog(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .extracting(StockBatchLog::getSourceType)
                .containsExactly("BATCH_EXPIRE_SCAN", "BATCH_EXPIRE_SCAN");
        verify(stockService, never()).damage(any(), any(Integer.class));
    }

    @Test
    void consumeAvailableBatches_consumesInMapperFefoOrderAndWritesOutboundLogs() {
        StockBatch first = batch("AVAILABLE", 5);
        first.setId(10L);
        StockBatch second = batch("AVAILABLE", 8);
        second.setId(11L);
        when(stockBatchMapper.findAvailableBatchesForConsumption(20L)).thenReturn(List.of(first, second));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 0, "DEPLETED")).thenReturn(1);
        when(stockBatchMapper.updateRemainingQuantityAndStatus(11L, 20L, 3, "AVAILABLE")).thenReturn(1);

        stockBatchService.consumeAvailableBatches(20L, 10, "OUTBOUND_ORDER", 99L);

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
    void consumeAvailableBatches_rejectsWhenAvailableBatchesAreInsufficient() {
        StockBatch first = batch("AVAILABLE", 5);
        first.setId(10L);
        when(stockBatchMapper.findAvailableBatchesForConsumption(20L)).thenReturn(List.of(first));

        assertThatThrownBy(() -> stockBatchService.consumeAvailableBatches(20L, 10, "OUTBOUND_ORDER", 99L))
                .isInstanceOf(BusinessException.class);

        verify(stockBatchMapper, never()).updateRemainingQuantityAndStatus(any(), any(), any(Integer.class), any());
        verify(stockBatchMapper, never()).insertLog(any());
    }

    @Test
    void consumeAvailableBatches_rejectsBatchUpdateMissAndStopsLogging() {
        StockBatch first = batch("AVAILABLE", 5);
        first.setId(10L);
        when(stockBatchMapper.findAvailableBatchesForConsumption(20L)).thenReturn(List.of(first));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 0, "DEPLETED")).thenReturn(0);

        assertThatThrownBy(() -> stockBatchService.consumeAvailableBatches(20L, 5, "OUTBOUND_ORDER", 99L))
                .isInstanceOf(BusinessException.class);

        verify(stockBatchMapper, never()).insertLog(any());
    }

    private void assertInvalidCommand(StockBatchCreateCommand command, String message) {
        assertThatThrownBy(() -> stockBatchService.createFromPurchaseInboundItem(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage(message);
    }

    private StockBatchCreateCommand command() {
        StockBatchCreateCommand command = new StockBatchCreateCommand();
        command.setSkuId(20L);
        command.setPurchaseInboundItemId(7L);
        command.setBaseQuantity(48);
        command.setPurchasePrice(new BigDecimal("48.00"));
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
        batch.setPurchaseInboundItemId(7L);
        batch.setInitialQuantity(48);
        batch.setQuantity(quantity);
        batch.setStatus(status);
        batch.setPurchasePrice(new BigDecimal("48.00"));
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
}
