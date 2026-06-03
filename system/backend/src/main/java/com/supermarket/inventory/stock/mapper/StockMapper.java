package com.supermarket.inventory.stock.mapper;

import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.vo.StockVO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class StockMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Stock> stockRowMapper = (rs, rowNum) -> {
        Stock stock = new Stock();
        stock.setId(rs.getLong("id"));
        stock.setSkuId(rs.getLong("sku_id"));
        stock.setTotalQuantity(rs.getInt("total_quantity"));
        stock.setAvailableQuantity(rs.getInt("available_quantity"));
        stock.setLockedQuantity(rs.getInt("locked_quantity"));
        stock.setExpiredQuantity(rs.getInt("expired_quantity"));
        stock.setMinStock(rs.getInt("min_stock"));
        stock.setMaxStock(rs.getInt("max_stock"));
        stock.setUpdateTime(toLocalDateTime(rs.getTimestamp("update_time")));
        return stock;
    };

    private final RowMapper<StockVO> stockVORowMapper = (rs, rowNum) -> {
        StockVO vo = new StockVO();
        vo.setId(rs.getLong("id"));
        vo.setSkuId(rs.getLong("sku_id"));
        vo.setSkuCode(rs.getString("sku_code"));
        vo.setSkuName(rs.getString("sku_name"));
        vo.setSpec(rs.getString("spec"));
        vo.setBaseUnit(rs.getString("base_unit"));
        vo.setProductCode(rs.getString("product_code"));
        vo.setProductName(rs.getString("product_name"));
        vo.setCategory(rs.getString("category"));
        vo.setTotalQuantity(rs.getInt("total_quantity"));
        vo.setAvailableQuantity(rs.getInt("available_quantity"));
        vo.setLockedQuantity(rs.getInt("locked_quantity"));
        vo.setExpiredQuantity(rs.getInt("expired_quantity"));
        vo.setMinStock(rs.getInt("min_stock"));
        vo.setMaxStock(rs.getInt("max_stock"));
        vo.setUpdateTime(toLocalDateTime(rs.getTimestamp("update_time")));
        vo.setWarningStatus(resolveWarningStatus(
                vo.getAvailableQuantity(),
                vo.getMinStock(),
                vo.getTotalQuantity(),
                vo.getMaxStock()
        ));
        return vo;
    };

    public StockMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                from stock s
                inner join sku k on k.id = s.sku_id
                inner join product p on p.id = k.product_id
                left join category c on c.id = p.category_id
                where p.product_code like ?
                   or p.product_name like ?
                   or k.sku_code like ?
                   or k.sku_name like ?
                   or c.name like ?
                """,
                Long.class,
                like,
                like,
                like,
                like,
                like
        );
    }

    public List<StockVO> findPage(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select s.*, k.sku_code, k.sku_name, k.spec, k.base_unit,
                       p.product_code, p.product_name, c.name as category
                from stock s
                inner join sku k on k.id = s.sku_id
                inner join product p on p.id = k.product_id
                left join category c on c.id = p.category_id
                where p.product_code like ?
                   or p.product_name like ?
                   or k.sku_code like ?
                   or k.sku_name like ?
                   or c.name like ?
                order by s.id desc
                limit ? offset ?
                """,
                stockVORowMapper,
                like,
                like,
                like,
                like,
                like,
                pageSize,
                offset
        );
    }

    public Optional<StockVO> findVOBySkuId(Long skuId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    select s.*, k.sku_code, k.sku_name, k.spec, k.base_unit,
                           p.product_code, p.product_name, c.name as category
                    from stock s
                    inner join sku k on k.id = s.sku_id
                    inner join product p on p.id = k.product_id
                    left join category c on c.id = p.category_id
                    where s.sku_id = ?
                    """,
                    stockVORowMapper,
                    skuId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Stock> findBySkuIdForUpdate(Long skuId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from stock where sku_id = ? for update",
                    stockRowMapper,
                    skuId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public void insertInitialStock(Long skuId) {
        jdbcTemplate.update(
                """
                insert into stock(
                    sku_id, total_quantity, available_quantity, locked_quantity, expired_quantity,
                    min_stock, max_stock
                ) values (?, 0, 0, 0, 0, 0, 100)
                """,
                skuId
        );
    }

    public void updateLimit(Long skuId, int minStock, int maxStock) {
        jdbcTemplate.update(
                "update stock set min_stock = ?, max_stock = ? where sku_id = ?",
                minStock,
                maxStock,
                skuId
        );
    }

    public void updateQuantity(Long skuId, int quantity) {
        jdbcTemplate.update(
                "update stock set total_quantity = ?, available_quantity = ? where sku_id = ?",
                quantity,
                quantity,
                skuId
        );
    }

    public void updateQuantities(Long skuId, int total, int available, int locked, int expired) {
        jdbcTemplate.update(
                """
                update stock
                set total_quantity = ?, available_quantity = ?, locked_quantity = ?, expired_quantity = ?
                where sku_id = ?
                """,
                total,
                available,
                locked,
                expired,
                skuId
        );
    }

    public void deleteBySkuId(Long skuId) {
        jdbcTemplate.update("delete from stock where sku_id = ?", skuId);
    }

    public void insertLog(Long skuId, String changeType, int changeQuantity, int beforeQuantity, int afterQuantity) {
        jdbcTemplate.update(
                """
                insert into stock_log(sku_id, change_type, change_quantity, before_quantity, after_quantity)
                values (?, ?, ?, ?, ?)
                """,
                skuId,
                changeType,
                changeQuantity,
                beforeQuantity,
                afterQuantity
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String resolveWarningStatus(int availableQuantity, int minStock, int totalQuantity, int maxStock) {
        if (availableQuantity < minStock) {
            return "LOW";
        }
        if (totalQuantity > maxStock) {
            return "HIGH";
        }
        return "NORMAL";
    }
}
