package com.supermarket.inventory.stock.mapper;

import com.supermarket.inventory.stock.vo.StockVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private StockMapper stockMapper;

    @BeforeEach
    void setUp() {
        stockMapper = new StockMapper(jdbcTemplate);
    }

    @Test
    void findPage_resolvesLowWarningFromAvailableQuantity() throws Exception {
        ArgumentCaptor<RowMapper<StockVO>> rowMapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), rowMapperCaptor.capture(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of());

        stockMapper.findPage("", 0, 10);

        StockVO vo = rowMapperCaptor.getValue().mapRow(resultSet(120, 4, 110, 6, 100), 0);

        assertThat(vo.getTotalQuantity()).isEqualTo(120);
        assertThat(vo.getAvailableQuantity()).isEqualTo(4);
        assertThat(vo.getLockedQuantity()).isEqualTo(110);
        assertThat(vo.getExpiredQuantity()).isEqualTo(6);
        assertThat(vo.getWarningStatus()).isEqualTo("LOW");
    }

    @Test
    void findPage_resolvesHighWarningFromTotalQuantity() throws Exception {
        ArgumentCaptor<RowMapper<StockVO>> rowMapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), rowMapperCaptor.capture(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of());

        stockMapper.findPage("", 0, 10);

        StockVO vo = rowMapperCaptor.getValue().mapRow(resultSet(101, 80, 1, 20, 10), 0);

        assertThat(vo.getWarningStatus()).isEqualTo("HIGH");
    }

    @Test
    void updateQuantities_writesAllAggregateBuckets() {
        stockMapper.updateQuantities(20L, 7, 5, 1, 1);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(7), eq(5), eq(1), eq(1), eq(20L));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("total_quantity = ?");
        assertThat(sql).contains("available_quantity = ?");
        assertThat(sql).contains("locked_quantity = ?");
        assertThat(sql).contains("expired_quantity = ?");
        assertThat(sql).contains("where sku_id = ?");
    }

    private ResultSet resultSet(int total, int available, int locked, int expired, int minStock) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getLong("sku_id")).thenReturn(20L);
        when(rs.getString("sku_code")).thenReturn("P001-001");
        when(rs.getString("sku_name")).thenReturn("500ml");
        when(rs.getString("spec")).thenReturn("500ml");
        when(rs.getString("base_unit")).thenReturn("bottle");
        when(rs.getString("product_code")).thenReturn("P001");
        when(rs.getString("product_name")).thenReturn("Test product");
        when(rs.getString("category")).thenReturn("Drink");
        when(rs.getInt("total_quantity")).thenReturn(total);
        when(rs.getInt("available_quantity")).thenReturn(available);
        when(rs.getInt("locked_quantity")).thenReturn(locked);
        when(rs.getInt("expired_quantity")).thenReturn(expired);
        when(rs.getInt("min_stock")).thenReturn(minStock);
        when(rs.getInt("max_stock")).thenReturn(100);
        when(rs.getTimestamp("update_time"))
                .thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 5, 31, 0, 0)));
        return rs;
    }
}
