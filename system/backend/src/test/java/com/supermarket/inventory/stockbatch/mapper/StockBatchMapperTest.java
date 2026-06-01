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
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
    void insertBatch_usesAllBatchColumnsIncludingStatus() throws Exception {
        StockBatch batch = batch();
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        doAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            KeyHolder keyHolder = invocation.getArgument(1);
            creator.createPreparedStatement(connection);
            keyHolder.getKeyList().add(Map.of("GENERATED_KEY", 10L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        Long result = stockBatchMapper.insertBatch(batch);

        assertThat(result).isEqualTo(10L);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("insert into stock_batch");
        assertThat(sql).contains("batch_no, sku_id, purchase_inbound_item_id, initial_quantity, quantity");
        assertThat(sql).contains("status, purchase_price, production_date, shelf_life_days, expire_date");
        verify(preparedStatement).setString(1, "SB20260601001");
        verify(preparedStatement).setLong(2, 20L);
        verify(preparedStatement).setLong(3, 7L);
        verify(preparedStatement).setInt(4, 48);
        verify(preparedStatement).setInt(5, 48);
        verify(preparedStatement).setString(6, "AVAILABLE");
        verify(preparedStatement).setBigDecimal(7, new BigDecimal("48.00"));
        verify(preparedStatement).setDate(8, Date.valueOf(LocalDate.of(2026, 6, 1)));
        verify(preparedStatement).setInt(9, 180);
        verify(preparedStatement).setDate(10, Date.valueOf(LocalDate.of(2026, 11, 28)));
    }

    @Test
    void insertLog_writesBatchLogColumnsIncludingReasonAndRemark() {
        StockBatchLog log = new StockBatchLog();
        log.setStockBatchId(10L);
        log.setSkuId(20L);
        log.setChangeType("PURCHASE_INBOUND");
        log.setChangeQuantity(48);
        log.setBeforeQuantity(0);
        log.setAfterQuantity(48);
        log.setSourceType("PURCHASE_INBOUND_ITEM");
        log.setSourceId(7L);
        log.setReason("BROKEN");
        log.setRemark("Box crushed");

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
                eq(7L),
                eq("BROKEN"),
                eq("Box crushed")
        );

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("insert into stock_batch_log");
        assertThat(sql).contains("stock_batch_id, sku_id, change_type, change_quantity");
        assertThat(sql).contains("before_quantity, after_quantity, source_type, source_id");
        assertThat(sql).contains("reason, remark");
    }

    @Test
    void findByIdAndSkuIdForUpdate_locksBatchByIdAndSkuId() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(10L), eq(20L)))
                .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        Optional<StockBatch> result = stockBatchMapper.findByIdAndSkuIdForUpdate(10L, 20L);

        assertThat(result).isEmpty();
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), any(RowMapper.class), eq(10L), eq(20L));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("select * from stock_batch");
        assertThat(sql).contains("where id = ? and sku_id = ?");
        assertThat(sql).contains("for update");
    }

    @Test
    void updateStatus_updatesBatchStatusByIdAndSkuId() {
        stockBatchMapper.updateStatus(10L, 20L, "EXPIRED");

        verify(jdbcTemplate).update(
                "update stock_batch set status = ? where id = ? and sku_id = ?",
                "EXPIRED",
                10L,
                20L
        );
    }

    @Test
    void findExpiredAvailableBatchesForUpdate_locksExpiredAvailableBatches() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(Date.valueOf(LocalDate.of(2026, 6, 1)))))
                .thenReturn(List.of());

        List<StockBatch> result = stockBatchMapper.findExpiredAvailableBatchesForUpdate(LocalDate.of(2026, 6, 1));

        assertThat(result).isEmpty();
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(Date.valueOf(LocalDate.of(2026, 6, 1))));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("select * from stock_batch");
        assertThat(sql).contains("status = 'AVAILABLE'");
        assertThat(sql).contains("quantity > 0");
        assertThat(sql).contains("expire_date < ?");
        assertThat(sql).contains("order by expire_date asc, id asc");
        assertThat(sql).contains("for update");
    }

    @Test
    void findConsumableBySkuIdForUpdate_locksOnlyAvailablePositiveBatchesInFefoOrder() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(20L))).thenReturn(List.of());

        List<StockBatch> result = stockBatchMapper.findConsumableBySkuIdForUpdate(20L);

        assertThat(result).isEmpty();
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(20L));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("select * from stock_batch");
        assertThat(sql).contains("sku_id = ?");
        assertThat(sql).contains("status = 'AVAILABLE'");
        assertThat(sql).contains("quantity > 0");
        assertThat(sql).contains("order by expire_date asc, id asc");
        assertThat(sql).contains("for update");
    }

    @Test
    void updateRemainingQuantityAndStatus_updatesQuantityAndStatusByIdAndSkuId() {
        when(jdbcTemplate.update(anyString(), eq(5), eq("AVAILABLE"), eq(10L), eq(20L))).thenReturn(1);

        int updated = stockBatchMapper.updateRemainingQuantityAndStatus(10L, 20L, 5, "AVAILABLE");

        assertThat(updated).isEqualTo(1);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(5), eq("AVAILABLE"), eq(10L), eq(20L));
        assertThat(sqlCaptor.getValue()).contains("update stock_batch set quantity = ?, status = ? where id = ? and sku_id = ?");
    }

    @Test
    void findBySkuId_ordersByExpireDateAndId() {
        stockBatchMapper.findBySkuId(20L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(20L));

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("from stock_batch b");
        assertThat(sql).contains("inner join sku k on k.id = b.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("where b.sku_id = ?");
        assertThat(sql).contains("order by b.expire_date asc, b.id asc");
    }

    @Test
    void findBySkuId_rowMapperReadsStatus() throws Exception {
        ArgumentCaptor<RowMapper> rowMapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), rowMapperCaptor.capture(), eq(20L))).thenReturn(List.of());

        stockBatchMapper.findBySkuId(20L);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(10L);
        when(resultSet.getString("batch_no")).thenReturn("SB20260601001");
        when(resultSet.getLong("sku_id")).thenReturn(20L);
        when(resultSet.getString("sku_code")).thenReturn("P001-001");
        when(resultSet.getString("sku_name")).thenReturn("500ml");
        when(resultSet.getString("product_code")).thenReturn("P001");
        when(resultSet.getString("product_name")).thenReturn("Test product");
        when(resultSet.getLong("purchase_inbound_item_id")).thenReturn(7L);
        when(resultSet.getInt("initial_quantity")).thenReturn(48);
        when(resultSet.getInt("quantity")).thenReturn(5);
        when(resultSet.getString("status")).thenReturn("DAMAGED");
        when(resultSet.getBigDecimal("purchase_price")).thenReturn(new BigDecimal("48.00"));
        when(resultSet.getDate("production_date")).thenReturn(Date.valueOf(LocalDate.of(2026, 6, 1)));
        when(resultSet.getInt("shelf_life_days")).thenReturn(180);
        when(resultSet.getDate("expire_date")).thenReturn(Date.valueOf(LocalDate.of(2026, 11, 28)));
        when(resultSet.getTimestamp("create_time")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 6, 1, 10, 0)));
        when(resultSet.getTimestamp("update_time")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 6, 1, 10, 0)));

        com.supermarket.inventory.stockbatch.vo.StockBatchVO vo =
                (com.supermarket.inventory.stockbatch.vo.StockBatchVO) rowMapperCaptor.getValue().mapRow(resultSet, 0);

        assertThat(vo.getStatus()).isEqualTo("DAMAGED");
    }

    private StockBatch batch() {
        StockBatch batch = new StockBatch();
        batch.setBatchNo("SB20260601001");
        batch.setSkuId(20L);
        batch.setPurchaseInboundItemId(7L);
        batch.setInitialQuantity(48);
        batch.setQuantity(48);
        batch.setStatus("AVAILABLE");
        batch.setPurchasePrice(new BigDecimal("48.00"));
        batch.setProductionDate(LocalDate.of(2026, 6, 1));
        batch.setShelfLifeDays(180);
        batch.setExpireDate(LocalDate.of(2026, 11, 28));
        return batch;
    }
}
