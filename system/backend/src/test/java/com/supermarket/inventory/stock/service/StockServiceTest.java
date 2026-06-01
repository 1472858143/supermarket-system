package com.supermarket.inventory.stock.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.stock.domain.StockDomainService;
import com.supermarket.inventory.stock.dto.StockIncreaseCommand;
import com.supermarket.inventory.stock.dto.StockLimitUpdateRequest;
import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.mapper.StockMapper;
import com.supermarket.inventory.stock.vo.StockVO;
import com.supermarket.inventory.stockbatch.dto.StockBatchCreateCommand;
import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.service.StockBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockBatchService stockBatchService;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(stockMapper, new StockDomainService(), stockBatchService);
    }

    @Test
    void initializeStock_insertsMissingSkuStock() {
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.empty());

        stockService.initializeStock(20L);

        verify(stockMapper).insertInitialStock(20L);
    }

    @Test
    void initializeStock_doesNotInsertExistingSkuStock() {
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 5)));

        stockService.initializeStock(20L);

        verify(stockMapper, never()).insertInitialStock(20L);
    }

    @Test
    void increase_updatesSkuQuantityAndWritesProvidedInboundLogType() {
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 5)));
        when(stockMapper.findVOBySkuId(20L)).thenReturn(Optional.of(stockVO(20L, 8)));

        StockVO vo = stockService.increase(20L, 3, "PURCHASE_INBOUND");

        assertThat(vo.getSkuId()).isEqualTo(20L);
        assertThat(vo.getQuantity()).isEqualTo(8);
        verify(stockMapper).updateQuantity(20L, 8);
        verify(stockMapper).insertLog(20L, "PURCHASE_INBOUND", 3, 5, 8);
    }

    @Test
    void increaseWithCommand_updatesStockAndCreatesPurchaseInboundBatchAndBatchLog() {
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 5)));
        when(stockMapper.findVOBySkuId(20L)).thenReturn(Optional.of(stockVO(20L, 53)));
        StockBatch batch = batch(2000L, 20L, 1000L, 48);
        when(stockBatchService.createFromPurchaseInboundItem(any(StockBatchCreateCommand.class))).thenReturn(batch);

        StockVO vo = stockService.increase(stockIncreaseCommand());

        assertThat(vo.getSkuId()).isEqualTo(20L);
        assertThat(vo.getQuantity()).isEqualTo(53);
        verify(stockMapper).updateQuantity(20L, 53);
        verify(stockMapper).insertLog(20L, "PURCHASE_INBOUND", 48, 5, 53);

        ArgumentCaptor<StockBatchCreateCommand> commandCaptor = ArgumentCaptor.forClass(StockBatchCreateCommand.class);
        verify(stockBatchService).createFromPurchaseInboundItem(commandCaptor.capture());
        StockBatchCreateCommand batchCommand = commandCaptor.getValue();
        assertThat(batchCommand.getSkuId()).isEqualTo(20L);
        assertThat(batchCommand.getPurchaseInboundItemId()).isEqualTo(1000L);
        assertThat(batchCommand.getBaseQuantity()).isEqualTo(48);
        assertThat(batchCommand.getPurchasePrice()).isEqualByComparingTo("48.00");
        assertThat(batchCommand.getProductionDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(batchCommand.getShelfLifeDays()).isEqualTo(180);
        verify(stockBatchService).writePurchaseInboundLog(batch, "PURCHASE_INBOUND_ITEM", 1000L);
    }

    @Test
    void decrease_rejectsInsufficientSkuStock() {
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 2)));

        assertThatThrownBy(() -> stockService.decrease(20L, 3))
                .isInstanceOf(BusinessException.class);

        verify(stockBatchService, never()).consumeByFefo(20L, 3);
        verify(stockMapper, never()).updateQuantity(20L, -1);
        verify(stockMapper, never()).insertLog(20L, "OUTBOUND", -3, 2, -1);
    }

    @Test
    void decreaseWithSource_consumesBatchesThenWritesStockAndBatchLogs() {
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 5)));
        when(stockMapper.findVOBySkuId(20L)).thenReturn(Optional.of(stockVO(20L, 2)));
        List<StockBatchService.BatchConsumption> consumptions = List.of(
                new StockBatchService.BatchConsumption(2000L, 20L, 5, 3, 2)
        );
        when(stockBatchService.consumeByFefo(20L, 3)).thenReturn(consumptions);

        StockVO vo = stockService.decrease(20L, 3, "OUTBOUND_ORDER", 300L);

        assertThat(vo.getQuantity()).isEqualTo(2);
        var inOrder = inOrder(stockBatchService, stockMapper);
        inOrder.verify(stockBatchService).consumeByFefo(20L, 3);
        inOrder.verify(stockMapper).updateQuantity(20L, 2);
        inOrder.verify(stockMapper).insertLog(20L, "OUTBOUND", -3, 5, 2);
        inOrder.verify(stockBatchService).writeOutboundLogs(consumptions, "OUTBOUND_ORDER", 300L);
    }

    @Test
    void adjustTo_updatesSkuQuantityAndWritesCheckLog() {
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 5)));
        when(stockMapper.findVOBySkuId(20L)).thenReturn(Optional.of(stockVO(20L, 8)));

        StockVO vo = stockService.adjustTo(20L, 8);

        assertThat(vo.getQuantity()).isEqualTo(8);
        verify(stockMapper).updateQuantity(20L, 8);
        verify(stockMapper).insertLog(20L, "CHECK", 3, 5, 8);
    }

    @Test
    void damage_decreasesSkuQuantityAndWritesDamageLog() {
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 8)));
        when(stockMapper.findVOBySkuId(20L)).thenReturn(Optional.of(stockVO(20L, 5)));

        StockVO vo = stockService.damage(20L, 3);

        assertThat(vo.getQuantity()).isEqualTo(5);
        verify(stockMapper).updateQuantity(20L, 5);
        verify(stockMapper).insertLog(20L, "DAMAGE", -3, 8, 5);
    }

    @Test
    void updateLimit_updatesSkuLimit() {
        StockLimitUpdateRequest request = new StockLimitUpdateRequest();
        request.setMinStock(2);
        request.setMaxStock(50);
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 5)));
        when(stockMapper.findVOBySkuId(20L)).thenReturn(Optional.of(stockVO(20L, 5)));

        StockVO vo = stockService.updateLimit(20L, request);

        assertThat(vo.getSkuId()).isEqualTo(20L);
        verify(stockMapper).updateLimit(20L, 2, 50);
    }

    @Test
    void deleteStockBySkuId_deletesSkuStock() {
        stockService.deleteStockBySkuId(20L);

        verify(stockMapper).deleteBySkuId(20L);
    }

    private Stock stock(Long skuId, int quantity) {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setSkuId(skuId);
        stock.setQuantity(quantity);
        stock.setMinStock(0);
        stock.setMaxStock(100);
        stock.setUpdateTime(LocalDateTime.of(2026, 5, 31, 0, 0));
        return stock;
    }

    private StockVO stockVO(Long skuId, int quantity) {
        StockVO vo = new StockVO();
        vo.setId(1L);
        vo.setSkuId(skuId);
        vo.setSkuCode("P001-001");
        vo.setSkuName("500ml");
        vo.setSpec("500ml");
        vo.setBaseUnit("bottle");
        vo.setProductCode("P001");
        vo.setProductName("Test product");
        vo.setCategory("Drink");
        vo.setQuantity(quantity);
        vo.setMinStock(0);
        vo.setMaxStock(100);
        vo.setWarningStatus("NORMAL");
        vo.setUpdateTime(LocalDateTime.of(2026, 5, 31, 0, 0));
        return vo;
    }

    private StockIncreaseCommand stockIncreaseCommand() {
        StockIncreaseCommand command = new StockIncreaseCommand();
        command.setSkuId(20L);
        command.setQuantity(48);
        command.setPurchasePrice(new BigDecimal("48.00"));
        command.setProductionDate(LocalDate.of(2026, 6, 1));
        command.setExpiryDate(LocalDate.of(2026, 11, 28));
        command.setPurchaseInboundId(100L);
        command.setPurchaseInboundItemId(1000L);
        command.setSourceType("PURCHASE_INBOUND_ITEM");
        command.setSourceId(1000L);
        return command;
    }

    private StockBatch batch(Long id, Long skuId, Long purchaseInboundItemId, int quantity) {
        StockBatch batch = new StockBatch();
        batch.setId(id);
        batch.setSkuId(skuId);
        batch.setPurchaseInboundItemId(purchaseInboundItemId);
        batch.setQuantity(quantity);
        return batch;
    }
}
