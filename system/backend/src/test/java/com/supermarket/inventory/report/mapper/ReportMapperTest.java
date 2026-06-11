package com.supermarket.inventory.report.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ReportMapper reportMapper;

    @BeforeEach
    void setUp() {
        reportMapper = new ReportMapper(jdbcTemplate);
    }

    @Test
    void warningStocks_queriesSkuWarningFieldsThroughSkuJoin() {
        reportMapper.warningStocks();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture());

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("inner join sku k on k.id = s.sku_id");
        assertThat(sql).contains("k.sku_code as skuCode");
        assertThat(sql).contains("k.sku_name as skuName");
        assertThat(sql).contains("p.product_code as productCode");
        assertThat(sql).doesNotContain("p.id = s." + "product_id");
    }

    @Test
    void inboundSummary_sumsPurchaseInboundTotalQuantity() {
        reportMapper.inboundSummary();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForMap(sqlCaptor.capture());

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("from purchase_inbound");
        assertThat(sql).contains("sum(inbound_total_quantity)");
        assertThat(sql).doesNotContain("inbound_order");
        assertThat(sql).doesNotContain("sum(quantity)");
    }

    @Test
    void outboundSummary_sumsBaseQuantity() {
        reportMapper.outboundSummary();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForMap(sqlCaptor.capture());

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("sum(base_quantity)");
        assertThat(sql).doesNotContain("sum(quantity)");
    }
}
