package com.supermarket.inventory.stockcheck.mapper;

import com.supermarket.inventory.stockcheck.vo.StockCheckVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StockCheckMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<StockCheckVO> rowMapper = (rs, rowNum) -> {
        StockCheckVO vo = new StockCheckVO();
        vo.setId(rs.getLong("id"));
        vo.setProductId(rs.getLong("product_id"));
        vo.setProductCode(rs.getString("product_code"));
        vo.setProductName(rs.getString("product_name"));
        vo.setSystemQuantity(rs.getInt("system_quantity"));
        vo.setActualQuantity(rs.getInt("actual_quantity"));
        vo.setDifference(rs.getInt("difference"));
        vo.setCheckTime(rs.getTimestamp("check_time").toLocalDateTime());
        return vo;
    };

    public StockCheckMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.queryForObject(
                """
                select count(*) from stock_check c
                inner join product p on p.id = c.product_id
                where p.product_code like ? or p.product_name like ?
                """,
                Long.class,
                like,
                like
        );
    }

    public List<StockCheckVO> findPage(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select c.*, p.product_code, p.product_name
                from stock_check c
                inner join product p on p.id = c.product_id
                where p.product_code like ? or p.product_name like ?
                order by c.id desc
                limit ? offset ?
                """,
                rowMapper,
                like,
                like,
                pageSize,
                offset
        );
    }

    public void insert(Long productId, int systemQuantity, int actualQuantity, int difference) {
        jdbcTemplate.update(
                """
                insert into stock_check(product_id, system_quantity, actual_quantity, difference)
                values (?, ?, ?, ?)
                """,
                productId,
                systemQuantity,
                actualQuantity,
                difference
        );
    }
}
