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
    void countBusinessReferences_includesPurchaseInboundItems() {
        Long skuId = 20L;

        skuUsageMapper.countBusinessReferences(skuId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), argsCaptor.capture());

        String sql = sqlCaptor.getValue();
        Object[] args = argsCaptor.getValue();

        assertThat(sql).contains("select count(*) from purchase_inbound_item where sku_id = ?");
        assertThat(sql).doesNotContain("inbound_order");
        assertThat(args).containsOnly(skuId);
        assertThat(args).hasSize(countPlaceholders(sql));
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

        assertThat(sql).doesNotContain("inbound_order o where o.product_id");
        assertThat(sql).doesNotContain("outbound_order o where o.product_id");
        assertThat(sql).doesNotContain("stock_check c where c.product_id");
        assertThat(sql).doesNotContain("o.product_id = ?");
        assertThat(sql).doesNotContain("c.product_id = ?");
        assertThat(sql).doesNotContain("from inbound_order");
        assertThat(sql).contains("from outbound_order o where exists");
        assertThat(sql).contains("s.id = o.sku_id");
        assertThat(sql).contains("from stock_check c where exists");
        assertThat(sql).contains("s.id = c.sku_id");
        assertThat(sql).doesNotContain("stock_log l where l.product_id");
        assertThat(sql).doesNotContain("l.product_id = ?");
        assertThat(sql).contains("from stock_log l where exists");
        assertThat(sql).contains("s.id = l.sku_id");
        assertThat(sql).contains("from purchase_inbound_item pi where exists");
        assertThat(sql).contains("s.id = pi.sku_id");
        assertThat(args).containsOnly(productId);
        assertThat(args).hasSize(countPlaceholders(sql));
    }

    private int countPlaceholders(String sql) {
        return (int) sql.chars()
                .filter(ch -> ch == '?')
                .count();
    }
}
