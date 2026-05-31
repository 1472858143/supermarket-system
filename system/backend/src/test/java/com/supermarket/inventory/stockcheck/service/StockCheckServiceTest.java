package com.supermarket.inventory.stockcheck.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.mapper.SkuMapper;
import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.service.StockService;
import com.supermarket.inventory.stockcheck.dto.StockCheckRequest;
import com.supermarket.inventory.stockcheck.mapper.StockCheckMapper;
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
class StockCheckServiceTest {

    @Mock
    private StockCheckMapper stockCheckMapper;

    @Mock
    private StockService stockService;

    @Mock
    private SkuMapper skuMapper;

    private StockCheckService stockCheckService;

    @BeforeEach
    void setUp() {
        stockCheckService = new StockCheckService(stockCheckMapper, stockService, skuMapper);
    }

    @Test
    void create_locksAndAdjustsDefaultSkuStockAndKeepsProductCheckRecord() {
        when(skuMapper.findDefaultByProductId(7L)).thenReturn(Optional.of(defaultSku(20L, 7L)));
        when(stockService.lockStock(20L)).thenReturn(stock(20L, 5));

        stockCheckService.create(request(7L, 8));

        verify(stockService).lockStock(20L);
        verify(stockCheckMapper).insert(7L, 5, 8, 3);
        verify(stockService).adjustTo(20L, 8);
    }

    @Test
    void create_rejectsProductWithoutDefaultSkuAndDoesNotLockStockOrInsertRecord() {
        when(skuMapper.findDefaultByProductId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockCheckService.create(request(7L, 8)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该商品无默认SKU");

        verify(stockService, never()).lockStock(20L);
        verify(stockCheckMapper, never()).insert(7L, 5, 8, 3);
        verify(stockService, never()).adjustTo(20L, 8);
    }

    private StockCheckRequest request(Long productId, Integer actualQuantity) {
        StockCheckRequest request = new StockCheckRequest();
        request.setProductId(productId);
        request.setActualQuantity(actualQuantity);
        return request;
    }

    private Sku defaultSku(Long id, Long productId) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setIsDefault(1);
        return sku;
    }

    private Stock stock(Long skuId, Integer quantity) {
        Stock stock = new Stock();
        stock.setSkuId(skuId);
        stock.setQuantity(quantity);
        return stock;
    }
}
