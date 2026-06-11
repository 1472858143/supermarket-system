package com.supermarket.inventory.purchaseinbound.mapper;

import com.supermarket.inventory.purchaseinbound.entity.PurchaseInbound;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundApprovalLog;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundItem;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundItemVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    void insertInbound_writesPlanInboundAndCreatorColumns() throws Exception {
        PurchaseInbound inbound = new PurchaseInbound();
        inbound.setOrderNo("PI20260602001");
        inbound.setSupplierId(7L);
        inbound.setPlannedTotalQuantity(5);
        inbound.setPlannedTotalAmount(new BigDecimal("62.500000"));
        inbound.setInboundTotalQuantity(null);
        inbound.setInboundTotalAmount(null);
        inbound.setStatus("DRAFT");
        inbound.setCreatorUserId(1L);
        inbound.setCreatorUsername("admin");
        inbound.setOperator("admin");
        inbound.setRemark("arrived");

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
            keyHolder.getKeyList().add(Map.of("id", 100L));
            return 1;
        });

        Long id = purchaseInboundMapper.insertInbound(inbound);

        verify(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        verify(connection).prepareStatement(any(String.class), eq(Statement.RETURN_GENERATED_KEYS));
        verify(preparedStatement).setString(1, "PI20260602001");
        verify(preparedStatement).setLong(2, 7L);
        verify(preparedStatement).setInt(3, 5);
        verify(preparedStatement).setBigDecimal(4, new BigDecimal("62.500000"));
        verify(preparedStatement).setInt(5, 0);
        verify(preparedStatement).setBigDecimal(6, BigDecimal.ZERO);
        verify(preparedStatement).setString(7, "DRAFT");
        verify(preparedStatement).setObject(8, 1L);
        verify(preparedStatement).setString(9, "admin");
        verify(preparedStatement).setString(10, "admin");
        verify(preparedStatement).setString(11, "arrived");

        assertThat(id).isEqualTo(100L);
        assertThat(insertSql.get()).contains("planned_total_quantity");
        assertThat(insertSql.get()).contains("planned_total_amount");
        assertThat(insertSql.get()).contains("inbound_total_quantity");
        assertThat(insertSql.get()).contains("inbound_total_amount");
        assertThat(insertSql.get()).contains("creator_user_id");
        assertThat(insertSql.get()).contains("creator_username");
        assertThat(insertSql.get()).contains("values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    @Test
    void insertItem_writesSupplierSkuPlanAndInboundedColumns() throws Exception {
        PurchaseInboundItem item = new PurchaseInboundItem();
        item.setPurchaseInboundId(100L);
        item.setSkuId(20L);
        item.setSupplierSkuId(88L);
        item.setSupplierSkuCodeSnapshot("SUP-COLA");
        item.setSupplierSkuNameSnapshot("Supplier Cola");
        item.setSupplierSpecSnapshot("2箱");
        item.setPlannedQuantity(2);
        item.setUnit("箱");
        item.setConversionRate(24);
        item.setPlannedBaseQuantity(48);
        item.setPlannedAmount(new BigDecimal("96.000000"));
        item.setInboundedBaseQuantity(null);
        item.setInboundedAmount(null);
        item.setPurchasePrice(new BigDecimal("48.000000"));
        item.setCostPrice(new BigDecimal("2.00000000"));

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
            keyHolder.getKeyList().add(Map.of("id", 10L));
            return 1;
        });

        Long id = purchaseInboundMapper.insertItem(item);

        verify(preparedStatement).setLong(1, 100L);
        verify(preparedStatement).setLong(2, 20L);
        verify(preparedStatement).setObject(3, 88L);
        verify(preparedStatement).setString(4, "SUP-COLA");
        verify(preparedStatement).setString(5, "Supplier Cola");
        verify(preparedStatement).setString(6, "2箱");
        verify(preparedStatement).setInt(7, 2);
        verify(preparedStatement).setString(8, "箱");
        verify(preparedStatement).setInt(9, 24);
        verify(preparedStatement).setInt(10, 48);
        verify(preparedStatement).setBigDecimal(11, new BigDecimal("96.000000"));
        verify(preparedStatement).setInt(12, 0);
        verify(preparedStatement).setBigDecimal(13, BigDecimal.ZERO);
        verify(preparedStatement).setBigDecimal(14, new BigDecimal("48.000000"));
        verify(preparedStatement).setBigDecimal(15, new BigDecimal("2.00000000"));

        assertThat(id).isEqualTo(10L);
        assertThat(item.getId()).isEqualTo(10L);
        assertThat(insertSql.get()).contains("supplier_sku_id");
        assertThat(insertSql.get()).contains("supplier_sku_code_snapshot");
        assertThat(insertSql.get()).contains("supplier_sku_name_snapshot");
        assertThat(insertSql.get()).contains("supplier_spec_snapshot");
        assertThat(insertSql.get()).contains("planned_quantity");
        assertThat(insertSql.get()).contains("planned_base_quantity");
        assertThat(insertSql.get()).contains("planned_amount");
        assertThat(insertSql.get()).contains("inbounded_base_quantity");
        assertThat(insertSql.get()).contains("inbounded_amount");
    }

    @Test
    void insertApprovalLog_writesManualActionOnly() {
        PurchaseInboundApprovalLog log = new PurchaseInboundApprovalLog();
        log.setPurchaseInboundId(100L);
        log.setAction("SUBMIT");
        log.setFromStatus("DRAFT");
        log.setToStatus("SUBMITTED");
        log.setOperatorUserId(1L);
        log.setOperatorUsername("admin");
        log.setReason(null);
        log.setRemark("submit for approval");

        purchaseInboundMapper.insertApprovalLog(log);

        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("insert into purchase_inbound_approval_log"),
                eq(100L),
                eq("SUBMIT"),
                eq("DRAFT"),
                eq("SUBMITTED"),
                eq(1L),
                eq("admin"),
                eq(null),
                eq("submit for approval")
        );
    }

    @Test
    void updateInboundTotals_writesMainTableRollup() {
        purchaseInboundMapper.updateInboundTotals(100L, 48, new BigDecimal("96.000000"));

        verify(jdbcTemplate).update(
                "update purchase_inbound set inbound_total_quantity = ?, inbound_total_amount = ? where id = ?",
                48,
                new BigDecimal("96.000000"),
                100L
        );
    }

    @Test
    void findById_joinsSupplierAndMapsSupplierFields() throws Exception {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(100L))).thenAnswer(invocation -> {
            RowMapper<PurchaseInboundVO> rowMapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("id")).thenReturn(100L);
            when(rs.getString("order_no")).thenReturn("PI20260602001");
            when(rs.getLong("supplier_id")).thenReturn(7L);
            when(rs.getString("supplier_code")).thenReturn("SUP001");
            when(rs.getString("supplier_name")).thenReturn("Acme Foods");
            when(rs.getInt("planned_total_quantity")).thenReturn(5);
            when(rs.getInt("inbound_total_quantity")).thenReturn(3);
            when(rs.getBigDecimal("planned_total_amount")).thenReturn(new BigDecimal("62.50"));
            when(rs.getBigDecimal("inbound_total_amount")).thenReturn(new BigDecimal("37.50"));
            when(rs.getString("status")).thenReturn("COMPLETED");
            when(rs.getObject("creator_user_id", Long.class)).thenReturn(1L);
            when(rs.getString("creator_username")).thenReturn("admin");
            when(rs.getObject("submitter_user_id", Long.class)).thenReturn(null);
            when(rs.getString("submitter_username")).thenReturn(null);
            when(rs.getTimestamp("submit_time")).thenReturn(null);
            when(rs.getObject("approver_user_id", Long.class)).thenReturn(null);
            when(rs.getString("approver_username")).thenReturn(null);
            when(rs.getTimestamp("approve_time")).thenReturn(null);
            when(rs.getObject("cancel_user_id", Long.class)).thenReturn(null);
            when(rs.getString("cancel_username")).thenReturn(null);
            when(rs.getTimestamp("cancel_time")).thenReturn(null);
            when(rs.getString("cancel_reason")).thenReturn(null);
            when(rs.getObject("close_user_id", Long.class)).thenReturn(null);
            when(rs.getString("close_username")).thenReturn(null);
            when(rs.getTimestamp("close_time")).thenReturn(null);
            when(rs.getString("close_reason")).thenReturn(null);
            when(rs.getString("operator")).thenReturn("admin");
            when(rs.getString("remark")).thenReturn("arrived");
            when(rs.getTimestamp("create_time")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 6, 2, 9, 30)));
            return List.of(rowMapper.mapRow(rs, 0));
        });

        Optional<PurchaseInboundVO> result = purchaseInboundMapper.findById(100L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(100L)
        );

        assertThat(result).isPresent();
        PurchaseInboundVO vo = result.orElseThrow();
        assertThat(vo.getId()).isEqualTo(100L);
        assertThat(vo.getSupplierId()).isEqualTo(7L);
        assertThat(vo.getSupplierCode()).isEqualTo("SUP001");
        assertThat(vo.getSupplierName()).isEqualTo("Acme Foods");
        assertThat(vo.getPlannedTotalQuantity()).isEqualTo(5);
        assertThat(vo.getInboundTotalQuantity()).isEqualTo(3);
        assertThat(vo.getPlannedTotalAmount()).isEqualByComparingTo("62.50");
        assertThat(vo.getInboundTotalAmount()).isEqualByComparingTo("37.50");
        assertThat(vo.getCreatorUserId()).isEqualTo(1L);
        assertThat(vo.getCreatorUsername()).isEqualTo("admin");
        assertThat(sqlCaptor.getValue()).contains("select pi.*, s.supplier_code, s.supplier_name");
        assertThat(sqlCaptor.getValue()).contains("from purchase_inbound pi");
        assertThat(sqlCaptor.getValue()).contains("inner join supplier s on s.id = pi.supplier_id");
        assertThat(sqlCaptor.getValue()).contains("where pi.id = ?");
    }

    @Test
    void findPage_searchesOrderOperatorRemarkSupplierAndSkuProductThroughItems() {
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
                eq("%cola%"),
                eq("%cola%"),
                eq((String) null),
                eq((String) null),
                eq(10),
                eq(20)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("select pi.*, s.supplier_code, s.supplier_name");
        assertThat(sql).contains("from purchase_inbound pi");
        assertThat(sql).contains("inner join supplier s on s.id = pi.supplier_id");
        assertThat(sql).contains("exists (");
        assertThat(sql).contains("from purchase_inbound_item item");
        assertThat(sql).contains("inner join sku k on k.id = item.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("item.purchase_inbound_id = pi.id");
        assertThat(sql).contains("pi.order_no like ?");
        assertThat(sql).contains("pi.operator like ?");
        assertThat(sql).contains("pi.remark like ?");
        assertThat(sql).contains("s.supplier_code like ?");
        assertThat(sql).contains("s.supplier_name like ?");
        assertThat(sql).contains("k.sku_code like ?");
        assertThat(sql).contains("k.sku_name like ?");
        assertThat(sql).contains("p.product_code like ?");
        assertThat(sql).contains("p.product_name like ?");
        assertThat(sql).contains("order by pi.id desc");
        assertThat(sql).contains("limit ? offset ?");
    }

    @Test
    void findItemsByInboundId_joinsSkuAndProductWithoutLegacyBatchJoinAndMapsPlanFields() throws Exception {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(7L))).thenAnswer(invocation -> {
            RowMapper<PurchaseInboundItemVO> rowMapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("id")).thenReturn(10L);
            when(rs.getLong("sku_id")).thenReturn(20L);
            when(rs.getString("sku_code")).thenReturn("SKU-COLA");
            when(rs.getString("sku_name")).thenReturn("Cola");
            when(rs.getString("product_code")).thenReturn("P-COLA");
            when(rs.getString("product_name")).thenReturn("Cola Product");
            when(rs.getObject("supplier_sku_id", Long.class)).thenReturn(88L);
            when(rs.getString("supplier_sku_code_snapshot")).thenReturn("SUP-COLA");
            when(rs.getString("supplier_sku_name_snapshot")).thenReturn("Supplier Cola");
            when(rs.getString("supplier_spec_snapshot")).thenReturn("2箱");
            when(rs.getInt("planned_quantity")).thenReturn(2);
            when(rs.getString("unit")).thenReturn("箱");
            when(rs.getInt("conversion_rate")).thenReturn(24);
            when(rs.getInt("planned_base_quantity")).thenReturn(48);
            when(rs.getBigDecimal("planned_amount")).thenReturn(new BigDecimal("96.000000"));
            when(rs.getInt("inbounded_base_quantity")).thenReturn(24);
            when(rs.getBigDecimal("inbounded_amount")).thenReturn(new BigDecimal("48.000000"));
            when(rs.getBigDecimal("purchase_price")).thenReturn(new BigDecimal("48.000000"));
            when(rs.getBigDecimal("cost_price")).thenReturn(new BigDecimal("2.00000000"));
            when(rs.getTimestamp("create_time")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 6, 2, 10, 0)));
            return List.of(rowMapper.mapRow(rs, 0));
        });

        List<PurchaseInboundItemVO> items = purchaseInboundMapper.findItemsByInboundId(7L);

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
        assertThat(sql).doesNotContain("stock_batch");
        assertThat(sql).doesNotContain("purchase_inbound_item_id = item.id");
        assertThat(sql).contains("where item.purchase_inbound_id = ?");
        assertThat(sql).contains("order by item.id asc");

        PurchaseInboundItemVO item = items.get(0);
        assertThat(item.getSupplierSkuId()).isEqualTo(88L);
        assertThat(item.getSupplierSkuCodeSnapshot()).isEqualTo("SUP-COLA");
        assertThat(item.getSupplierSkuNameSnapshot()).isEqualTo("Supplier Cola");
        assertThat(item.getSupplierSpecSnapshot()).isEqualTo("2箱");
        assertThat(item.getPlannedQuantity()).isEqualTo(2);
        assertThat(item.getPlannedBaseQuantity()).isEqualTo(48);
        assertThat(item.getPlannedAmount()).isEqualByComparingTo("96.000000");
        assertThat(item.getInboundedBaseQuantity()).isEqualTo(24);
        assertThat(item.getInboundedAmount()).isEqualByComparingTo("48.000000");
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
                eq("%cola%"),
                eq("%cola%"),
                eq("%cola%"),
                eq((String) null),
                eq((String) null)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from purchase_inbound pi");
        assertThat(sql).contains("inner join supplier s on s.id = pi.supplier_id");
        assertThat(sql).contains("exists (");
        assertThat(sql).contains("from purchase_inbound_item item");
        assertThat(sql).contains("inner join sku k on k.id = item.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("item.purchase_inbound_id = pi.id");
        assertThat(sql).contains("pi.order_no like ?");
        assertThat(sql).contains("pi.operator like ?");
        assertThat(sql).contains("pi.remark like ?");
        assertThat(sql).contains("s.supplier_code like ?");
        assertThat(sql).contains("s.supplier_name like ?");
        assertThat(sql).contains("k.sku_code like ?");
        assertThat(sql).contains("k.sku_name like ?");
        assertThat(sql).contains("p.product_code like ?");
        assertThat(sql).contains("p.product_name like ?");
    }
}
