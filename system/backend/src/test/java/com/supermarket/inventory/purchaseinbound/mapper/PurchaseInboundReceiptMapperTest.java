package com.supermarket.inventory.purchaseinbound.mapper;

import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundReceipt;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundReceiptBatch;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundReceiptBatchVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundReceiptVO;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseInboundReceiptMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PurchaseInboundReceiptMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PurchaseInboundReceiptMapper(jdbcTemplate);
    }

    @Test
    void insertReceipt_usesGeneratedKey() {
        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("id", 200L));
            return 1;
        });

        PurchaseInboundReceipt receipt = new PurchaseInboundReceipt();
        receipt.setReceiptNo("PIR202606030001");
        receipt.setPurchaseInboundId(100L);
        receipt.setOperatorUserId(1L);
        receipt.setOperatorUsername("admin");
        receipt.setTotalBaseQuantity(48);
        receipt.setTotalAmount(new BigDecimal("96.000000"));
        receipt.setRemark("first delivery");

        Long id = mapper.insertReceipt(receipt);

        assertThat(id).isEqualTo(200L);
        assertThat(receipt.getId()).isEqualTo(200L);
    }

    @Test
    void insertReceiptBatch_writesSnapshots() throws Exception {
        PurchaseInboundReceiptBatch batch = new PurchaseInboundReceiptBatch();
        batch.setReceiptId(200L);
        batch.setPurchaseInboundId(100L);
        batch.setPurchaseInboundItemId(10L);
        batch.setSkuId(20L);
        batch.setQuantity(2);
        batch.setBaseQuantity(48);
        batch.setProductionDate(LocalDate.of(2026, 6, 1));
        batch.setShelfLifeDays(180);
        batch.setExpireDate(LocalDate.of(2026, 11, 28));
        batch.setPurchasePriceSnapshot(new BigDecimal("48.000000"));
        batch.setCostPriceSnapshot(new BigDecimal("2.00000000"));
        batch.setAmount(new BigDecimal("96.000000"));
        batch.setSupplierSkuCodeSnapshot("SUP-COLA");
        batch.setSupplierSkuNameSnapshot("Supplier Cola");
        batch.setSupplierSpecSnapshot("2箱");

        AtomicReference<String> insertSql = new AtomicReference<>();
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(any(String.class), eq(Statement.RETURN_GENERATED_KEYS))).thenAnswer(invocation -> {
            insertSql.set(invocation.getArgument(0));
            return preparedStatement;
        });
        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            KeyHolder keyHolder = invocation.getArgument(1);
            creator.createPreparedStatement(connection);
            keyHolder.getKeyList().add(Map.of("id", 300L));
            return 1;
        });

        Long id = mapper.insertReceiptBatch(batch);

        verify(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        verify(preparedStatement).setLong(1, 200L);
        verify(preparedStatement).setLong(2, 100L);
        verify(preparedStatement).setLong(3, 10L);
        verify(preparedStatement).setLong(4, 20L);
        verify(preparedStatement).setInt(5, 2);
        verify(preparedStatement).setInt(6, 48);
        verify(preparedStatement).setDate(7, Date.valueOf(LocalDate.of(2026, 6, 1)));
        verify(preparedStatement).setObject(8, 180);
        verify(preparedStatement).setDate(9, Date.valueOf(LocalDate.of(2026, 11, 28)));
        verify(preparedStatement).setBigDecimal(10, new BigDecimal("48.000000"));
        verify(preparedStatement).setBigDecimal(11, new BigDecimal("2.00000000"));
        verify(preparedStatement).setBigDecimal(12, new BigDecimal("96.000000"));
        verify(preparedStatement).setString(13, "SUP-COLA");
        verify(preparedStatement).setString(14, "Supplier Cola");
        verify(preparedStatement).setString(15, "2箱");
        assertThat(id).isEqualTo(300L);
        assertThat(batch.getId()).isEqualTo(300L);
        assertThat(insertSql.get()).contains("purchase_price_snapshot");
        assertThat(insertSql.get()).contains("cost_price_snapshot");
        assertThat(insertSql.get()).contains("amount");
        assertThat(insertSql.get()).contains("supplier_sku_code_snapshot");
        assertThat(insertSql.get()).contains("supplier_sku_name_snapshot");
        assertThat(insertSql.get()).contains("supplier_spec_snapshot");
    }

    @Test
    void findReceiptsByInboundId_mapsReceiptRows() throws Exception {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(100L))).thenAnswer(invocation -> {
            RowMapper<PurchaseInboundReceiptVO> rowMapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("id")).thenReturn(200L);
            when(rs.getString("receipt_no")).thenReturn("PIR202606030001");
            when(rs.getLong("purchase_inbound_id")).thenReturn(100L);
            when(rs.getObject("operator_user_id", Long.class)).thenReturn(1L);
            when(rs.getString("operator_username")).thenReturn("admin");
            when(rs.getInt("total_base_quantity")).thenReturn(48);
            when(rs.getBigDecimal("total_amount")).thenReturn(new BigDecimal("96.000000"));
            when(rs.getString("remark")).thenReturn("first delivery");
            when(rs.getTimestamp("create_time")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 6, 3, 9, 0)));
            return List.of(rowMapper.mapRow(rs, 0));
        });

        List<PurchaseInboundReceiptVO> receipts = mapper.findReceiptsByInboundId(100L);

        assertThat(receipts).hasSize(1);
        PurchaseInboundReceiptVO receipt = receipts.get(0);
        assertThat(receipt.getId()).isEqualTo(200L);
        assertThat(receipt.getReceiptNo()).isEqualTo("PIR202606030001");
        assertThat(receipt.getOperatorUserId()).isEqualTo(1L);
        assertThat(receipt.getTotalBaseQuantity()).isEqualTo(48);
        assertThat(receipt.getTotalAmount()).isEqualByComparingTo("96.000000");
    }

    @Test
    void findReceiptBatchesByReceiptIds_mapsSnapshotPricesAndAmount() throws Exception {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(200L), eq(201L))).thenAnswer(invocation -> {
            RowMapper<PurchaseInboundReceiptBatchVO> rowMapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("id")).thenReturn(300L);
            when(rs.getLong("receipt_id")).thenReturn(200L);
            when(rs.getLong("purchase_inbound_id")).thenReturn(100L);
            when(rs.getLong("purchase_inbound_item_id")).thenReturn(10L);
            when(rs.getLong("sku_id")).thenReturn(20L);
            when(rs.getInt("quantity")).thenReturn(2);
            when(rs.getInt("base_quantity")).thenReturn(48);
            when(rs.getDate("production_date")).thenReturn(Date.valueOf(LocalDate.of(2026, 6, 1)));
            when(rs.getObject("shelf_life_days")).thenReturn(180);
            when(rs.getDate("expire_date")).thenReturn(Date.valueOf(LocalDate.of(2026, 11, 28)));
            when(rs.getBigDecimal("purchase_price_snapshot")).thenReturn(new BigDecimal("48.000000"));
            when(rs.getBigDecimal("cost_price_snapshot")).thenReturn(new BigDecimal("2.00000000"));
            when(rs.getBigDecimal("amount")).thenReturn(new BigDecimal("96.000000"));
            when(rs.getString("supplier_sku_code_snapshot")).thenReturn("SUP-COLA");
            when(rs.getString("supplier_sku_name_snapshot")).thenReturn("Supplier Cola");
            when(rs.getString("supplier_spec_snapshot")).thenReturn("2箱");
            when(rs.getTimestamp("create_time")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 6, 3, 9, 5)));
            return List.of(rowMapper.mapRow(rs, 0));
        });

        List<PurchaseInboundReceiptBatchVO> batches = mapper.findReceiptBatchesByReceiptIds(List.of(200L, 201L));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(200L), eq(201L));
        assertThat(sqlCaptor.getValue()).contains("where receipt_id in (?, ?)");
        assertThat(batches).hasSize(1);
        PurchaseInboundReceiptBatchVO batch = batches.get(0);
        assertThat(batch.getPurchasePriceSnapshot()).isEqualByComparingTo("48.000000");
        assertThat(batch.getCostPriceSnapshot()).isEqualByComparingTo("2.00000000");
        assertThat(batch.getAmount()).isEqualByComparingTo("96.000000");
        assertThat(batch.getSupplierSkuCodeSnapshot()).isEqualTo("SUP-COLA");
        assertThat(batch.getSupplierSkuNameSnapshot()).isEqualTo("Supplier Cola");
        assertThat(batch.getSupplierSpecSnapshot()).isEqualTo("2箱");
    }

    @Test
    void findMaxReceiptNo_queriesByPattern() {
        when(jdbcTemplate.queryForObject(
                "select max(receipt_no) from purchase_inbound_receipt where receipt_no like ?",
                String.class,
                "PIR20260603%"
        )).thenReturn("PIR202606030001");

        String maxReceiptNo = mapper.findMaxReceiptNo("PIR20260603%");

        assertThat(maxReceiptNo).isEqualTo("PIR202606030001");
    }
}
