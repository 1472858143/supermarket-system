package com.supermarket.inventory.stock.mapper;

import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.vo.StockVO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StockMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Stock> stockRowMapper = (rs, rowNum) -> {
        Stock stock = new Stock();
        stock.setId(rs.getLong("id"));
        stock.setProductId(rs.getLong("product_id"));
        stock.setQuantity(rs.getInt("quantity"));
        stock.setMinStock(rs.getInt("min_stock"));
        stock.setMaxStock(rs.getInt("max_stock"));
        stock.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        return stock;
    };

    private final RowMapper<StockVO> stockVORowMapper = (rs, rowNum) -> {
        StockVO vo = new StockVO();
        vo.setId(rs.getLong("id"));
        vo.setProductId(rs.getLong("product_id"));
        vo.setProductCode(rs.getString("product_code"));
        vo.setProductName(rs.getString("product_name"));
        vo.setCategory(rs.getString("category"));
        vo.setQuantity(rs.getInt("quantity"));
        vo.setMinStock(rs.getInt("min_stock"));
        vo.setMaxStock(rs.getInt("max_stock"));
        vo.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        vo.setWarningStatus(resolveWarningStatus(vo.getQuantity(), vo.getMinStock(), vo.getMaxStock()));
        return vo;
    };

    public StockMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.queryForObject(
                """
                select count(*) from stock s
                inner join product p on p.id = s.product_id
                left join category c on c.id = p.category_id
                where p.product_code like ? or p.product_name like ? or c.name like ?
                """,
                Long.class,
                like,
                like,
                like
        );
    }

    public List<StockVO> findPage(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select s.*, p.product_code, p.product_name, c.name as category
                from stock s
                inner join product p on p.id = s.product_id
                left join category c on c.id = p.category_id
                where p.product_code like ? or p.product_name like ? or c.name like ?
                order by s.id desc
                limit ? offset ?
                """,
                stockVORowMapper,
                like,
                like,
                like,
                pageSize,
                offset
        );
    }

    public Optional<StockVO> findVOByProductId(Long productId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    select s.*, p.product_code, p.product_name, c.name as category
                    from stock s
                    inner join product p on p.id = s.product_id
                    left join category c on c.id = p.category_id
                    where s.product_id = ?
                    """,
                    stockVORowMapper,
                    productId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Stock> findByProductIdForUpdate(Long productId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from stock where product_id = ? for update",
                    stockRowMapper,
                    productId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public void insertInitialStock(Long productId) {
        jdbcTemplate.update(
                "insert into stock(product_id, quantity, min_stock, max_stock) values (?, 0, 0, 100)",
                productId
        );
    }

    public void updateLimit(Long productId, int minStock, int maxStock) {
        jdbcTemplate.update(
                "update stock set min_stock = ?, max_stock = ? where product_id = ?",
                minStock,
                maxStock,
                productId
        );
    }

    public void updateQuantity(Long productId, int quantity) {
        jdbcTemplate.update("update stock set quantity = ? where product_id = ?", quantity, productId);
    }

    public void deleteByProductId(Long productId) {
        jdbcTemplate.update("delete from stock where product_id = ?", productId);
    }

    public void insertLog(Long productId, String changeType, int changeQuantity, int beforeQuantity, int afterQuantity) {
        jdbcTemplate.update(
                """
                insert into stock_log(product_id, change_type, change_quantity, before_quantity, after_quantity)
                values (?, ?, ?, ?, ?)
                """,
                productId,
                changeType,
                changeQuantity,
                beforeQuantity,
                afterQuantity
        );
    }

    private String resolveWarningStatus(int quantity, int minStock, int maxStock) {
        if (quantity < minStock) {
            return "LOW";
        }
        if (quantity > maxStock) {
            return "HIGH";
        }
        return "NORMAL";
    }
}
