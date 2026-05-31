package com.supermarket.inventory.inbound.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.inbound.dto.InboundRequest;
import com.supermarket.inventory.inbound.mapper.InboundMapper;
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
class InboundServiceTest {

    @Mock
    private InboundMapper inboundMapper;

    @Mock
    private StockService stockService;

    @Mock
    private SkuMapper skuMapper;

    private InboundService inboundService;

    @BeforeEach
    void setUp() {
        inboundService = new InboundService(inboundMapper, stockService, skuMapper);
    }

    @Test
    void create_increasesDefaultSkuStockAndKeepsProductInboundRecord() {
        when(skuMapper.findDefaultByProductId(7L)).thenReturn(Optional.of(defaultSku(20L, 7L)));

        inboundService.create(request(7L, 3, "operator"));

        verify(stockService).increase(20L, 3);
        verify(inboundMapper).insert(7L, 3, "operator");
    }

    @Test
    void create_rejectsProductWithoutDefaultSkuAndDoesNotWriteStockOrInboundRecord() {
        when(skuMapper.findDefaultByProductId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inboundService.create(request(7L, 3, "operator")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该商品无默认SKU");

        verify(stockService, never()).increase(20L, 3);
        verify(inboundMapper, never()).insert(7L, 3, "operator");
    }

    private InboundRequest request(Long productId, Integer quantity, String operator) {
        InboundRequest request = new InboundRequest();
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
