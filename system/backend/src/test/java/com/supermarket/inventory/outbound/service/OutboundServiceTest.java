package com.supermarket.inventory.outbound.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.outbound.dto.OutboundRequest;
import com.supermarket.inventory.outbound.mapper.OutboundMapper;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.mapper.SkuMapper;
import com.supermarket.inventory.stock.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private SkuMapper skuMapper;

    private OutboundService outboundService;

    @BeforeEach
    void setUp() {
        outboundService = new OutboundService(outboundMapper, stockService, skuMapper);
    }

    @Test
    void create_decreasesDefaultSkuStockAndKeepsProductOutboundRecord() {
        when(skuMapper.findDefaultByProductId(7L)).thenReturn(Optional.of(defaultSku(20L, 7L)));

        outboundService.create(request(7L, 3, "operator"));

        verify(stockService).decrease(20L, 3);
        verify(outboundMapper).insert(7L, 3, "operator");
    }

    @Test
    void create_rejectsProductWithoutDefaultSkuAndDoesNotDecreaseStockOrInsertRecord() {
        when(skuMapper.findDefaultByProductId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outboundService.create(request(7L, 3, "operator")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该商品无默认SKU");

        verify(stockService, never()).decrease(20L, 3);
        verify(outboundMapper, never()).insert(7L, 3, "operator");
    }

    private OutboundRequest request(Long productId, Integer quantity, String operator) {
        OutboundRequest request = new OutboundRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        request.setOperator(operator);
        return request;
    }

    private Sku defaultSku(Long id, Long productId) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setIsDefault(1);
        return sku;
    }
}
