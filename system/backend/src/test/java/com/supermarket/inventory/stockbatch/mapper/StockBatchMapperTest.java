package com.supermarket.inventory.stockbatch.mapper;

import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.entity.StockBatchLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBatchMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private StockBatchMapper stockBatchMapper;

    @BeforeEach
    void setUp() {
        stockBatchMapper = new StockBatchMapper(jdbcTemplate);
    }

    @Test
    void findMaxBatchNo_searchesByPattern() {
        when(jdbcTemplate.queryForObject(
                eq("select max(batch_no) from stock_batch where batch_no like ?"),
                eq(String.class),
                eq("SB20260601%")
        )).thenReturn("SB20260601003");

        String result = stockBatchMapper.findMaxBatchNo("SB20260601%");

        assertThat(result).isEqualTo("SB20260601003");
    }

    @Test
    void insertBatch_usesAllBatchColumns() {
        StockBatch batch = batch();

        stockBatchMapper.insertBatch(batch);

        ArgumentCaptor<org.springframework.jdbc.core.PreparedStatementCreator> captor =
                ArgumentCaptor.forClass(org.springframework.jdbc.core.PreparedStatementCreator.class);
        verify(jdbcTemplate).update(captor.capture(), any(KeyHolder.class));
    }

    @Test
    void insertLog_writesBatchLogColumns() {
        StockBatchLog log = new StockBatchLog();
        log.setStockBatchId(10L);
        log.setSkuId(20L);
        log.setChangeType("PURCHASE_INBOUND");
        log.setChangeQuantity(48);
        log.setBeforeQuantity(0);
        log.setAfterQuantity(48);
        log.setSourceType("PURCHASE_INBOUND_ITEM");
        log.setSourceId(7L);

        stockBatchMapper.insertLog(log);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                sqlCaptor.capture(),
                eq(10L),
                eq(20L),
                eq("PURCHASE_INBOUND"),
                eq(48),
                eq(0),
                eq(48),
                eq("PURCHASE_INBOUND_ITEM"),
                eq(7L)
        );

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("insert into stock_batch_log");
        assertThat(sql).contains("stock_batch_id, sku_id, change_type, change_quantity");
        assertThat(sql).contains("before_quantity, after_quantity, source_type, source_id");
    }

    @Test
    void findBySkuId_ordersByExpireDateAndId() {
        stockBatchMapper.findBySkuId(20L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(20L)
        );

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("from stock_batch b");
        assertThat(sql).contains("inner join sku k on k.id = b.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("where b.sku_id = ?");
        assertThat(sql).contains("order by b.expire_date asc, b.id asc");
    }

    private StockBatch batch() {
        StockBatch batch = new StockBatch();
        batch.setBatchNo("SB20260601001");
        batch.setSkuId(20L);
        batch.setPurchaseInboundItemId(7L);
        batch.setInitialQuantity(48);
        batch.setQuantity(48);
        batch.setPurchasePrice(new BigDecimal("48.00"));
        batch.setProductionDate(LocalDate.of(2026, 6, 1));
        batch.setShelfLifeDays(180);
        batch.setExpireDate(LocalDate.of(2026, 11, 28));
        return batch;
    }
}
