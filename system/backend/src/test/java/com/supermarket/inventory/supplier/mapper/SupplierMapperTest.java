package com.supermarket.inventory.supplier.mapper;

import com.supermarket.inventory.supplier.entity.Supplier;
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
class SupplierMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SupplierMapper supplierMapper;

    @BeforeEach
    void setUp() {
        supplierMapper = new SupplierMapper(jdbcTemplate);
    }

    @Test
    void findPage_searchesSupplierFieldsAndSortsByNewest() {
        supplierMapper.findPage("fresh", 20, 10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq("%fresh%"),
                eq("%fresh%"),
                eq("%fresh%"),
                eq("%fresh%"),
                eq(10),
                eq(20)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from supplier");
        assertThat(sql).contains("supplier_code like ?");
        assertThat(sql).contains("supplier_name like ?");
        assertThat(sql).contains("contact_person like ?");
        assertThat(sql).contains("contact_phone like ?");
        assertThat(sql).contains("order by id desc");
        assertThat(sql).contains("limit ? offset ?");
    }

    @Test
    void count_usesSameSearchScopeAsFindPage() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object[].class))).thenReturn(0L);

        supplierMapper.count("fresh");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                eq(Long.class),
                eq("%fresh%"),
                eq("%fresh%"),
                eq("%fresh%"),
                eq("%fresh%")
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from supplier");
        assertThat(sql).contains("supplier_code like ?");
        assertThat(sql).contains("supplier_name like ?");
        assertThat(sql).contains("contact_person like ?");
        assertThat(sql).contains("contact_phone like ?");
    }

    @Test
    void update_doesNotModifySupplierCode() {
        Supplier supplier = new Supplier();
        supplier.setId(7L);
        supplier.setSupplierCode("S001");
        supplier.setSupplierName("Fresh Co");
        supplier.setStatus(1);

        supplierMapper.update(supplier);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                sqlCaptor.capture(),
                eq("Fresh Co"),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(1),
                eq(7L)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("update supplier");
        assertThat(sql).contains("supplier_name = ?");
        assertThat(sql).doesNotContain("supplier_code");
    }

    @Test
    void findMaxCode_usesPatternAndMaxSupplierCode() {
        supplierMapper.findMaxCode("SUP%");

        verify(jdbcTemplate).queryForObject(
                "select max(supplier_code) from supplier where supplier_code like ?",
                String.class,
                "SUP%"
        );
    }
}
