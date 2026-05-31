package com.supermarket.inventory.stockcheck.mapper;

import com.supermarket.inventory.stockcheck.vo.StockCheckVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

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
    void findPage_queriesStockChecksThroughSkuAndProduct() {
        stockCheckMapper.findPage("cola", 20, 10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq(10),
                eq(20)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("select c.*, k.sku_code, k.sku_name, p.product_code, p.product_name");
        assertThat(sql).contains("from stock_check c");
        assertThat(sql).contains("inner join sku k on k.id = c.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("p.product_code like ?");
        assertThat(sql).contains("p.product_name like ?");
        assertThat(sql).contains("k.sku_code like ?");
        assertThat(sql).contains("k.sku_name like ?");
        assertThat(sql).doesNotContain("c.product_id");
        assertThat(sql).doesNotContain("findDefaultByProductId");
    }

    @Test
    void count_queriesStockChecksThroughSkuAndProduct() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object[].class))).thenReturn(0L);

        stockCheckMapper.count("cola");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                eq(Long.class),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%")
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from stock_check c");
        assertThat(sql).contains("inner join sku k on k.id = c.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("p.product_code like ?");
        assertThat(sql).contains("p.product_name like ?");
        assertThat(sql).contains("k.sku_code like ?");
        assertThat(sql).contains("k.sku_name like ?");
        assertThat(sql).doesNotContain("c.product_id");
        assertThat(sql).doesNotContain("findDefaultByProductId");
    }

    @Test
    void insert_writesSkuIdOnly() {
        stockCheckMapper.insert(20L, 50, 40, -10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(20L), eq(50), eq(40), eq(-10));

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("insert into stock_check(sku_id, system_quantity, actual_quantity, difference)");
        assertThat(sql).doesNotContain("product_id");
    }
}
