package com.supermarket.inventory.outbound.service;

import com.supermarket.inventory.outbound.dto.OutboundRequest;
import com.supermarket.inventory.outbound.mapper.OutboundMapper;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.service.SkuUnitResolver;
import com.supermarket.inventory.stock.service.StockService;
import com.supermarket.inventory.stockbatch.service.StockBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundServiceTest {

    @Mock
    private OutboundMapper outboundMapper;

    @Mock
    private StockService stockService;

    @Mock
    private SkuUnitResolver skuUnitResolver;

    @Mock
    private StockBatchService stockBatchService;

    private OutboundService outboundService;

    @BeforeEach
    void setUp() {
        outboundService = new OutboundService(outboundMapper, stockService, skuUnitResolver, stockBatchService);
    }

    @Test
    void create_writesOutboundRecordThenConsumesBatchesAndDecreasesTotalStock() {
        when(skuUnitResolver.resolve(20L, "箱"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "箱", 24));
        when(outboundMapper.insert(20L, 2, "箱", 24, 48, "operator")).thenReturn(99L);

        outboundService.create(request(20L, 2, "箱", 24, "operator"));

        var inOrder = inOrder(outboundMapper, stockBatchService, stockService);
        inOrder.verify(outboundMapper).insert(20L, 2, "箱", 24, 48, "operator");
        inOrder.verify(stockBatchService).consumeAvailableBatches(20L, 48, "OUTBOUND_ORDER", 99L);
        inOrder.verify(stockService).decrease(20L, 48);
    }

    @Test
    void create_usesBaseUnitWhenUnitIsNotProvided() {
        when(skuUnitResolver.resolve(20L, null))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "瓶", 1));
        when(outboundMapper.insert(20L, 10, "瓶", 1, 10, "operator")).thenReturn(99L);

        outboundService.create(request(20L, 10, null, null, "operator"));

        verify(stockBatchService).consumeAvailableBatches(20L, 10, "OUTBOUND_ORDER", 99L);
        verify(stockService).decrease(20L, 10);
        verify(outboundMapper).insert(20L, 10, "瓶", 1, 10, "operator");
    }

    @Test
    void create_usesResolverConversionRateInsteadOfRequestSnapshot() {
        when(skuUnitResolver.resolve(20L, "箱"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "箱", 24));
        when(outboundMapper.insert(20L, 2, "箱", 24, 48, "operator")).thenReturn(99L);

        outboundService.create(request(20L, 2, "箱", 999, "operator"));

        verify(stockBatchService).consumeAvailableBatches(20L, 48, "OUTBOUND_ORDER", 99L);
        verify(stockService).decrease(20L, 48);
        verify(outboundMapper).insert(20L, 2, "箱", 24, 48, "operator");
    }

    @Test
    void create_doesNotDecreaseTotalStockWhenBatchConsumptionFails() {
        when(skuUnitResolver.resolve(20L, "箱"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "箱", 24));
        when(outboundMapper.insert(20L, 2, "箱", 24, 48, "operator")).thenReturn(99L);
        org.mockito.Mockito.doThrow(new BusinessException("可用批次数不足"))
                .when(stockBatchService).consumeAvailableBatches(20L, 48, "OUTBOUND_ORDER", 99L);

        assertThatThrownBy(() -> outboundService.create(request(20L, 2, "箱", 24, "operator")))
                .isInstanceOf(BusinessException.class);

        verify(stockService, never()).decrease(20L, 48);
    }

    @Test
    void create_rejectsBaseQuantityOverflow() {
        when(skuUnitResolver.resolve(20L, "箱"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "箱", 2));

        assertThatThrownBy(() -> outboundService.create(request(20L, Integer.MAX_VALUE, "箱", 2, "operator")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("基础单位数量超出范围");

        verify(stockService, never()).decrease(20L, -2);
        verify(stockBatchService, never()).consumeAvailableBatches(20L, -2, "OUTBOUND_ORDER", null);
        verify(outboundMapper, never()).insert(20L, Integer.MAX_VALUE, "箱", 2, -2, "operator");
    }

    private OutboundRequest request(Long skuId, Integer quantity, String unit, Integer conversionRate, String operator) {
        OutboundRequest request = new OutboundRequest();
        request.setSkuId(skuId);
        request.setQuantity(quantity);
        request.setUnit(unit);
        request.setConversionRate(conversionRate);
        request.setOperator(operator);
        return request;
    }

    private Sku sku(Long id) {
        Sku sku = new Sku();
        sku.setId(id);
        return sku;
    }
}
