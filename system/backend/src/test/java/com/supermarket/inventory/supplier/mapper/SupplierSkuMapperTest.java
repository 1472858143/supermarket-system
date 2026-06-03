package com.supermarket.inventory.supplier.mapper;

import com.supermarket.inventory.supplier.entity.SupplierSku;
import com.supermarket.inventory.supplier.vo.SupplierSkuVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.EmptyResultDataAccessException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
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
class SupplierSkuMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SupplierSkuMapper supplierSkuMapper;

    @BeforeEach
    void setUp() {
        supplierSkuMapper = new SupplierSkuMapper(jdbcTemplate);
    }

    @Test
    void findBySupplierId_joinsSkuAndProductAndSortsByNewest() {
        supplierSkuMapper.findBySupplierId(7L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(7L)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("select ss.*, k.product_id, k.sku_code, k.sku_name, k.spec, k.base_unit");
        assertThat(sql).contains("p.product_code, p.product_name");
        assertThat(sql).contains("from supplier_sku ss");
        assertThat(sql).contains("inner join sku k on k.id = ss.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("where ss.supplier_id = ?");
        assertThat(sql).contains("order by ss.id desc");
    }

    @Test
    void findEnabledBySupplierId_filtersEnabledBindings() {
        supplierSkuMapper.findEnabledBySupplierId(7L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(7L)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from supplier_sku ss");
        assertThat(sql).contains("inner join sku k on k.id = ss.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("where ss.supplier_id = ? and ss.status = 1");
        assertThat(sql).contains("order by ss.id desc");
    }

    @Test
    void findVOByIdAndSupplierId_joinsSkuAndProductAndFiltersTargetBinding() {
        Optional<SupplierSkuVO> binding = supplierSkuMapper.findVOByIdAndSupplierId(3L, 7L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(3L),
                eq(7L)
        );

        String sql = sqlCaptor.getValue();

        assertThat(binding).isEmpty();
        assertThat(sql).contains("select ss.*, k.product_id, k.sku_code, k.sku_name, k.spec, k.base_unit");
        assertThat(sql).contains("p.product_code, p.product_name");
        assertThat(sql).contains("from supplier_sku ss");
        assertThat(sql).contains("inner join sku k on k.id = ss.sku_id");
        assertThat(sql).contains("inner join product p on p.id = k.product_id");
        assertThat(sql).contains("where ss.id = ? and ss.supplier_id = ?");
    }

    @Test
    void findVOByIdAndSupplierId_returnsEmptyWhenMissing() {
        when(jdbcTemplate.queryForObject(any(String.class), any(RowMapper.class), eq(3L), eq(7L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<SupplierSkuVO> binding = supplierSkuMapper.findVOByIdAndSupplierId(3L, 7L);

        assertThat(binding).isEmpty();
    }

    @Test
    void findById_usesIdFilter() {
        Optional<SupplierSku> binding = supplierSkuMapper.findById(3L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(3L)
        );

        assertThat(binding).isEmpty();
        assertThat(sqlCaptor.getValue()).isEqualTo("select * from supplier_sku where id = ?");
    }

    @Test
    void findById_returnsEmptyWhenMissing() {
        when(jdbcTemplate.queryForObject(any(String.class), any(RowMapper.class), eq(3L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<SupplierSku> binding = supplierSkuMapper.findById(3L);

        assertThat(binding).isEmpty();
    }

    @Test
    void findBySupplierIdAndSkuId_usesSupplierAndSkuFilters() {
        Optional<SupplierSku> binding = supplierSkuMapper.findBySupplierIdAndSkuId(7L, 11L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(7L),
                eq(11L)
        );

        assertThat(binding).isEmpty();
        assertThat(sqlCaptor.getValue()).isEqualTo("select * from supplier_sku where supplier_id = ? and sku_id = ?");
    }

    @Test
    void findBySupplierIdAndSkuId_returnsEmptyWhenMissing() {
        when(jdbcTemplate.queryForObject(any(String.class), any(RowMapper.class), eq(7L), eq(11L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<SupplierSku> binding = supplierSkuMapper.findBySupplierIdAndSkuId(7L, 11L);

        assertThat(binding).isEmpty();
    }

    @Test
    void existsBySupplierIdAndSkuId_usesSupplierAndSkuFiltersAndReturnsTrueWhenPresent() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(7L), eq(11L))).thenReturn(1L);

        boolean exists = supplierSkuMapper.existsBySupplierIdAndSkuId(7L, 11L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                eq(Long.class),
                eq(7L),
                eq(11L)
        );

        assertThat(exists).isTrue();
        assertThat(sqlCaptor.getValue()).isEqualTo("select count(*) from supplier_sku where supplier_id = ? and sku_id = ?");
    }

    @Test
    void existsBySupplierIdAndSkuId_returnsFalseWhenMissing() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(7L), eq(11L))).thenReturn(0L);

        boolean exists = supplierSkuMapper.existsBySupplierIdAndSkuId(7L, 11L);

        assertThat(exists).isFalse();
    }

    @Test
    void update_doesNotModifySupplierIdOrSkuId() {
        SupplierSku binding = new SupplierSku();
        binding.setId(3L);
        binding.setSupplierId(7L);
        binding.setSkuId(11L);
        binding.setSupplierSkuCode("SUP-COLA");
        binding.setSupplierSkuName("Supplier Cola");
        binding.setSupplierSpec("24 bottles");
        binding.setDefaultPurchasePrice(new BigDecimal("12.50"));
        binding.setMinPurchaseQuantity(5);
        binding.setStatus(1);

        supplierSkuMapper.update(binding);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                sqlCaptor.capture(),
                eq("SUP-COLA"),
                eq("Supplier Cola"),
                eq("24 bottles"),
                eq(new BigDecimal("12.50")),
                eq(5),
                eq(1),
                eq(3L),
                eq(7L)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("update supplier_sku");
        assertThat(sql).contains("supplier_sku_code = ?");
        assertThat(sql).contains("supplier_sku_name = ?");
        assertThat(sql).contains("supplier_spec = ?");
        assertThat(sql).contains("default_purchase_price = ?");
        assertThat(sql).contains("min_purchase_quantity = ?");
        assertThat(sql).contains("status = ?");
        assertThat(sql).contains("where id = ? and supplier_id = ?");
        assertThat(sql).doesNotContain("supplier_id = ?,");
        assertThat(sql).doesNotContain("sku_id = ?");
    }

    @Test
    void delete_usesIdAndSupplierIdFilters() {
        supplierSkuMapper.delete(3L, 7L);

        verify(jdbcTemplate).update(
                "delete from supplier_sku where id = ? and supplier_id = ?",
                3L,
                7L
        );
    }

    @Test
    void countPurchaseInboundReferences_joinsInboundAndItemsAndFiltersSupplierSku() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(7L), eq(11L))).thenReturn(2L);

        long count = supplierSkuMapper.countPurchaseInboundReferences(7L, 11L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                eq(Long.class),
                eq(7L),
                eq(11L)
        );

        String sql = sqlCaptor.getValue();

        assertThat(count).isEqualTo(2L);
        assertThat(sql).contains("from purchase_inbound pi");
        assertThat(sql).contains("inner join purchase_inbound_item item on item.purchase_inbound_id = pi.id");
        assertThat(sql).contains("where pi.supplier_id = ? and item.sku_id = ?");
    }

    @Test
    void insert_usesGeneratedKeyHolderAndReturnsGeneratedId() throws Exception {
        SupplierSku binding = new SupplierSku();
        binding.setSupplierId(7L);
        binding.setSkuId(11L);
        binding.setSupplierSkuCode("SUP-COLA");
        binding.setSupplierSkuName("Supplier Cola");
        binding.setSupplierSpec("24 bottles");
        binding.setDefaultPurchasePrice(new BigDecimal("12.50"));
        binding.setMinPurchaseQuantity(5);
        binding.setStatus(1);

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
            keyHolder.getKeyList().add(Map.of("id", 99L));
            return 1;
        });

        Long id = supplierSkuMapper.insert(binding);

        verify(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        verify(connection).prepareStatement(any(String.class), eq(Statement.RETURN_GENERATED_KEYS));
        verify(preparedStatement).setLong(1, 7L);
        verify(preparedStatement).setLong(2, 11L);
        verify(preparedStatement).setString(3, "SUP-COLA");
        verify(preparedStatement).setString(4, "Supplier Cola");
        verify(preparedStatement).setString(5, "24 bottles");
        verify(preparedStatement).setBigDecimal(6, new BigDecimal("12.50"));
        verify(preparedStatement).setInt(7, 5);
        verify(preparedStatement).setInt(8, 1);

        assertThat(id).isEqualTo(99L);
        assertThat(insertSql.get()).contains("insert into supplier_sku(");
        assertThat(insertSql.get()).contains("supplier_id, sku_id, supplier_sku_code, supplier_sku_name");
        assertThat(insertSql.get()).contains("supplier_spec, default_purchase_price, min_purchase_quantity, status");
        assertThat(insertSql.get()).contains("values (?, ?, ?, ?, ?, ?, ?, ?)");
    }
}
