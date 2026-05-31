package com.supermarket.inventory.sku.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SkuUsageMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SkuUsageMapper skuUsageMapper;

    @BeforeEach
    void setUp() {
        skuUsageMapper = new SkuUsageMapper(jdbcTemplate);
    }

    @Test
    void countBusinessReferencesByProductId_queriesStockLogThroughSkuOnly() {
        Long productId = 42L;

        skuUsageMapper.countBusinessReferencesByProductId(productId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), argsCaptor.capture());

        String sql = sqlCaptor.getValue();
        Object[] args = argsCaptor.getValue();

        assertThat(sql).doesNotContain("stock_log l where l.product_id");
        assertThat(sql).doesNotContain("l.product_id = ?");
        assertThat(sql).contains("from stock_log l where exists");
        assertThat(sql).contains("s.id = l.sku_id");
        assertThat(args).containsOnly(productId);
        assertThat(args).hasSize(countPlaceholders(sql));
    }

    private int countPlaceholders(String sql) {
        return (int) sql.chars()
                .filter(ch -> ch == '?')
                .count();
    }
}
