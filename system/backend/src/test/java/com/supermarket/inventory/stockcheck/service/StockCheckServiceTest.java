package com.supermarket.inventory.stockcheck.service;

import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.mapper.StockMapper;
import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.entity.StockBatchLog;
import com.supermarket.inventory.stockbatch.mapper.StockBatchMapper;
import com.supermarket.inventory.stockcheck.dto.StockCheckCreateRequest;
import com.supermarket.inventory.stockcheck.entity.StockCheck;
import com.supermarket.inventory.stockcheck.entity.StockCheckItem;
import com.supermarket.inventory.stockcheck.mapper.StockCheckMapper;
import com.supermarket.inventory.stockcheck.vo.StockCheckVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockCheckServiceTest {

    @Mock
    private StockCheckMapper stockCheckMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockBatchMapper stockBatchMapper;

    private StockCheckService stockCheckService;

    @BeforeEach
    void setUp() {
        stockCheckService = new StockCheckService(stockCheckMapper, stockMapper, stockBatchMapper);
    }

    @Test
    void create_resolvesSkuRangeAndExpandsCheckableBatches() {
        when(stockCheckMapper.findMaxCheckNo(anyString())).thenReturn(null);
        when(stockCheckMapper.filterActiveSkuIds(List.of(20L))).thenReturn(List.of(20L));
        when(stockBatchMapper.findCheckableBySkuIds(List.of(20L))).thenReturn(List.of(batch(100L, 20L, 12)));
        when(stockCheckMapper.insert(any(StockCheck.class))).thenReturn(8L);
        when(stockCheckMapper.findVOById(8L)).thenReturn(Optional.of(new StockCheckVO()));

        stockCheckService.create(createRequest());

        ArgumentCaptor<StockCheck> stockCheckCaptor = ArgumentCaptor.forClass(StockCheck.class);
        verify(stockCheckMapper).insert(stockCheckCaptor.capture());
        StockCheck stockCheck = stockCheckCaptor.getValue();
        assertThat(stockCheck.getName()).isEqualTo("6月盘点");
        assertThat(stockCheck.getScopeType()).isEqualTo("SKU");
        assertThat(stockCheck.getSkuSelectType()).isEqualTo("SINGLE");
        assertThat(stockCheck.getStatus()).isEqualTo("DRAFT");
        assertThat(stockCheck.getTotalSkuCount()).isEqualTo(1);
        assertThat(stockCheck.getTotalBatchCount()).isEqualTo(1);

        ArgumentCaptor<StockCheckItem> itemCaptor = ArgumentCaptor.forClass(StockCheckItem.class);
        verify(stockCheckMapper).insertItem(itemCaptor.capture());
        StockCheckItem item = itemCaptor.getValue();
        assertThat(item.getStockCheckId()).isEqualTo(8L);
        assertThat(item.getSkuId()).isEqualTo(20L);
        assertThat(item.getStockBatchId()).isEqualTo(100L);
        assertThat(item.getSystemQuantity()).isEqualTo(12);
        assertThat(item.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void complete_updatesBatchSyncsSkuStockAndWritesBothLogs() {
        StockCheck stockCheck = draftStockCheck(8L);
        StockCheckItem item = countedItem(8L, 9L, 20L, 100L, 18);
        StockBatch batch = batch(100L, 20L, 12);

        when(stockCheckMapper.findByIdForUpdate(8L)).thenReturn(Optional.of(stockCheck));
        when(stockCheckMapper.findItemsByCheckIdForUpdate(8L)).thenReturn(List.of(item));
        when(stockMapper.findBySkuIdForUpdate(20L)).thenReturn(Optional.of(stock(20L, 12)));
        when(stockBatchMapper.findByIdAndSkuIdForUpdate(100L, 20L)).thenReturn(Optional.of(batch));
        when(stockBatchMapper.updateRemainingQuantityAndStatus(100L, 20L, 18, "AVAILABLE")).thenReturn(1);
        when(stockBatchMapper.sumQuantitiesByStatus(20L))
                .thenReturn(new StockBatchMapper.StockQuantitySummary(18, 18, 0, 0));
        when(stockCheckMapper.findVOById(8L)).thenReturn(Optional.of(new StockCheckVO()));

        stockCheckService.complete(8L);

        verify(stockBatchMapper).updateRemainingQuantityAndStatus(100L, 20L, 18, "AVAILABLE");
        verify(stockMapper).updateQuantities(20L, 18, 18, 0, 0);
        verify(stockMapper).insertLog(20L, "CHECK", 6, 12, 18);
        verify(stockCheckMapper).complete(8L, 6);

        ArgumentCaptor<StockBatchLog> logCaptor = ArgumentCaptor.forClass(StockBatchLog.class);
        verify(stockBatchMapper).insertLog(logCaptor.capture());
        StockBatchLog log = logCaptor.getValue();
        assertThat(log.getStockBatchId()).isEqualTo(100L);
        assertThat(log.getSkuId()).isEqualTo(20L);
        assertThat(log.getChangeType()).isEqualTo("CHECK");
        assertThat(log.getChangeQuantity()).isEqualTo(6);
        assertThat(log.getBeforeQuantity()).isEqualTo(12);
        assertThat(log.getAfterQuantity()).isEqualTo(18);
        assertThat(log.getSourceType()).isEqualTo("STOCK_CHECK");
        assertThat(log.getSourceId()).isEqualTo(8L);
    }

    @Test
    void complete_recalculatesStockAggregateFromBatchFacts() {
        when(stockBatchMapper.sumQuantitiesByStatus(20L))
                .thenReturn(new StockBatchMapper.StockQuantitySummary(10, 6, 3, 1));

        stockCheckService.recalculateStockSummary(20L);

        verify(stockMapper).updateQuantities(20L, 10, 6, 3, 1);
    }

    private StockCheckCreateRequest createRequest() {
        StockCheckCreateRequest request = new StockCheckCreateRequest();
        request.setName("6月盘点");
        request.setScopeType("SKU");
        request.setSkuSelectType("SINGLE");
        request.setSkuIds(List.of(20L));
        return request;
    }

    private StockCheck draftStockCheck(Long id) {
        StockCheck stockCheck = new StockCheck();
        stockCheck.setId(id);
        stockCheck.setStatus("DRAFT");
        return stockCheck;
    }

    private StockCheckItem countedItem(Long stockCheckId, Long itemId, Long skuId, Long batchId, Integer actualQuantity) {
        StockCheckItem item = new StockCheckItem();
        item.setId(itemId);
        item.setStockCheckId(stockCheckId);
        item.setSkuId(skuId);
        item.setStockBatchId(batchId);
        item.setActualQuantity(actualQuantity);
        return item;
    }

    private StockBatch batch(Long id, Long skuId, Integer quantity) {
        StockBatch batch = new StockBatch();
        batch.setId(id);
        batch.setSkuId(skuId);
        batch.setBatchNo("SB20260602001");
        batch.setQuantity(quantity);
        batch.setStatus("AVAILABLE");
        batch.setExpireDate(LocalDate.now().plusDays(30));
        return batch;
    }

    private Stock stock(Long skuId, Integer quantity) {
        Stock stock = new Stock();
        stock.setSkuId(skuId);
        stock.setQuantity(quantity);
        return stock;
    }
}
