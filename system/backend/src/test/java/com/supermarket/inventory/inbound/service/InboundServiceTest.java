package com.supermarket.inventory.inbound.service;

import com.supermarket.inventory.inbound.dto.InboundRequest;
import com.supermarket.inventory.inbound.mapper.InboundMapper;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.service.SkuUnitResolver;
import com.supermarket.inventory.stock.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundServiceTest {

    @Mock
    private InboundMapper inboundMapper;

    @Mock
    private StockService stockService;

    @Mock
    private SkuUnitResolver skuUnitResolver;

    private InboundService inboundService;

    @BeforeEach
    void setUp() {
        inboundService = new InboundService(inboundMapper, stockService, skuUnitResolver);
    }

    @Test
    void create_writesSkuInboundRecordAndIncreasesBaseQuantityStock() {
        when(skuUnitResolver.resolve(20L, "箱"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "箱", 24));

        inboundService.create(request(20L, 2, "箱", 24, "operator"));

        verify(inboundMapper).insert(20L, 2, "箱", 24, 48, "operator");
        verify(stockService).increase(20L, 48);
    }

    @Test
    void create_usesBaseUnitWhenUnitIsNotProvided() {
        when(skuUnitResolver.resolve(20L, null))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "瓶", 1));

        inboundService.create(request(20L, 10, null, null, "operator"));

        verify(inboundMapper).insert(20L, 10, "瓶", 1, 10, "operator");
        verify(stockService).increase(20L, 10);
    }

    @Test
    void create_usesResolverConversionRateInsteadOfRequestSnapshot() {
        when(skuUnitResolver.resolve(20L, "箱"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "箱", 24));

        inboundService.create(request(20L, 2, "箱", 999, "operator"));

        verify(inboundMapper).insert(20L, 2, "箱", 24, 48, "operator");
        verify(stockService).increase(20L, 48);
    }

    @Test
    void create_rejectsBaseQuantityOverflow() {
        when(skuUnitResolver.resolve(20L, "箱"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "箱", 2));

        assertThatThrownBy(() -> inboundService.create(request(20L, Integer.MAX_VALUE, "箱", 2, "operator")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("基础单位数量超出范围");

        verify(inboundMapper, never()).insert(20L, Integer.MAX_VALUE, "箱", 2, -2, "operator");
        verify(stockService, never()).increase(20L, -2);
    }

    private InboundRequest request(Long skuId, Integer quantity, String unit, Integer conversionRate, String operator) {
        InboundRequest request = new InboundRequest();
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
