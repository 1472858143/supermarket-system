package com.supermarket.inventory.stockcheck.mapper;

import com.supermarket.inventory.stockcheck.entity.StockCheckItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockCheckMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private StockCheckMapper stockCheckMapper;

    @BeforeEach
    void setUp() {
        stockCheckMapper = new StockCheckMapper(jdbcTemplate);
    }

    @Test
    void findPage_queriesStockCheckOrders() {
        stockCheckMapper.findPage("6月", 20, 10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq("%6月%"),
                eq("%6月%"),
                eq("%6月%"),
                eq("%6月%"),
                eq("%6月%"),
                eq(10),
                eq(20)
        );

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("from stock_check c");
        assertThat(sql).contains("left join category cat on cat.id = c.category_id");
        assertThat(sql).contains("c.check_no like ?");
        assertThat(sql).contains("c.name like ?");
        assertThat(sql).doesNotContain("c.sku_id");
    }

    @Test
    void count_queriesStockCheckOrders() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object[].class))).thenReturn(0L);

        stockCheckMapper.count("6月");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                eq(Long.class),
                eq("%6月%"),
                eq("%6月%"),
                eq("%6月%"),
                eq("%6月%"),
                eq("%6月%")
        );

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("from stock_check c");
        assertThat(sql).contains("left join category cat on cat.id = c.category_id");
        assertThat(sql).doesNotContain("inner join sku");
    }

    @Test
    void insertItem_writesBatchSnapshot() {
        StockCheckItem item = new StockCheckItem();
        item.setStockCheckId(8L);
        item.setSkuId(20L);
        item.setStockBatchId(100L);
        item.setBatchNo("SB20260602001");
        item.setSystemQuantity(12);
        item.setStatus("PENDING");
        item.setExpireDate(LocalDate.of(2026, 7, 1));

        stockCheckMapper.insertItem(item);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                sqlCaptor.capture(),
                eq(8L),
                eq(20L),
                eq(100L),
                eq("SB20260602001"),
                eq(12),
                eq(null),
                eq(null),
                eq("PENDING"),
                eq(java.sql.Date.valueOf(LocalDate.of(2026, 7, 1)))
        );

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("insert into stock_check_item");
        assertThat(sql).contains("stock_batch_id");
        assertThat(sql).contains("system_quantity");
    }
}
