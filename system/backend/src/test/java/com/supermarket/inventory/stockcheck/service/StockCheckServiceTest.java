package com.supermarket.inventory.stockcheck.service;

import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.service.StockService;
import com.supermarket.inventory.stockcheck.dto.StockCheckRequest;
import com.supermarket.inventory.stockcheck.mapper.StockCheckMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockCheckServiceTest {

    @Mock
    private StockCheckMapper stockCheckMapper;

    @Mock
    private StockService stockService;

    private StockCheckService stockCheckService;

    @BeforeEach
    void setUp() {
        stockCheckService = new StockCheckService(stockCheckMapper, stockService);
    }

    @Test
    void create_locksSkuStockWritesSkuRecordAndAdjustsSkuStock() {
        when(stockService.lockStock(20L)).thenReturn(stock(20L, 5));

        stockCheckService.create(request(20L, 8));

        verify(stockService).lockStock(20L);
        verify(stockCheckMapper).insert(20L, 5, 8, 3);
        verify(stockService).adjustTo(20L, 8);
    }

    private StockCheckRequest request(Long skuId, Integer actualQuantity) {
        StockCheckRequest request = new StockCheckRequest();
        request.setSkuId(skuId);
        request.setActualQuantity(actualQuantity);
        return request;
    }

    private Stock stock(Long skuId, Integer quantity) {
        Stock stock = new Stock();
        stock.setSkuId(skuId);
        stock.setQuantity(quantity);
        return stock;
    }
}
