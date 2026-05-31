package com.supermarket.inventory.purchaseinbound.mapper;

import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundItemVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
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
class PurchaseInboundMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PurchaseInboundMapper purchaseInboundMapper;

    @BeforeEach
    void setUp() {
        purchaseInboundMapper = new PurchaseInboundMapper(jdbcTemplate);
    }

    @Test
    void findPage_searchesOrderOperatorRemarkAndSkuProductThroughItems() {
        purchaseInboundMapper.findPage("cola", 20, 10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq(10),
                eq(20)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from purchase_inbound pi");
        assertThat(sql).contains("exists (");
        assertThat(sql).contains("from purchase_inbound_item item");
        assertThat(sql).contains("inner join sku k on k.id = item.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("item.purchase_inbound_id = pi.id");
        assertThat(sql).contains("pi.order_no like ?");
        assertThat(sql).contains("pi.operator like ?");
        assertThat(sql).contains("pi.remark like ?");
        assertThat(sql).contains("k.sku_code like ?");
        assertThat(sql).contains("k.sku_name like ?");
        assertThat(sql).contains("p.product_code like ?");
        assertThat(sql).contains("p.product_name like ?");
        assertThat(sql).contains("order by pi.id desc");
        assertThat(sql).contains("limit ? offset ?");
    }

    @Test
    void findItemsByInboundId_joinsSkuAndProduct() {
        purchaseInboundMapper.findItemsByInboundId(7L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(7L)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("select item.*, k.sku_code, k.sku_name, p.product_code, p.product_name");
        assertThat(sql).contains("from purchase_inbound_item item");
        assertThat(sql).contains("inner join sku k on k.id = item.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("where item.purchase_inbound_id = ?");
        assertThat(sql).contains("order by item.id asc");
    }

    @Test
    void count_usesSameSearchScopeAsFindPage() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object[].class))).thenReturn(0L);

        purchaseInboundMapper.count("cola");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                eq(Long.class),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%")
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from purchase_inbound pi");
        assertThat(sql).contains("exists (");
        assertThat(sql).contains("from purchase_inbound_item item");
        assertThat(sql).contains("inner join sku k on k.id = item.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("item.purchase_inbound_id = pi.id");
        assertThat(sql).contains("pi.order_no like ?");
        assertThat(sql).contains("pi.operator like ?");
        assertThat(sql).contains("pi.remark like ?");
        assertThat(sql).contains("k.sku_code like ?");
        assertThat(sql).contains("k.sku_name like ?");
        assertThat(sql).contains("p.product_code like ?");
        assertThat(sql).contains("p.product_name like ?");
    }
}
