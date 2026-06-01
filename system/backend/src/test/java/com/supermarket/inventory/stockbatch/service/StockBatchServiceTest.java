package com.supermarket.inventory.stockbatch.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.stockbatch.dto.StockBatchCreateCommand;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBatchServiceTest {

    @Mock
    private StockBatchMapper stockBatchMapper;

    private StockBatchService stockBatchService;

    @BeforeEach
    void setUp() {
        stockBatchService = new StockBatchService(stockBatchMapper);
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
        StockBatch batch = new StockBatch();
        batch.setId(10L);
        batch.setBatchNo("SB20260601001");
        batch.setSkuId(20L);
        batch.setPurchaseInboundItemId(7L);
        batch.setInitialQuantity(48);
        batch.setQuantity(48);
        batch.setPurchasePrice(new BigDecimal("48.00"));
        batch.setProductionDate(LocalDate.of(2026, 6, 1));
        batch.setShelfLifeDays(180);
        batch.setExpireDate(LocalDate.of(2026, 11, 28));
        return batch;
    }
}
