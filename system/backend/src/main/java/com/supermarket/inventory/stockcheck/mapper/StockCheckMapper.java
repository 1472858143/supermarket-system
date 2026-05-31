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
        vo.setSkuId(rs.getLong("sku_id"));
        vo.setSkuCode(rs.getString("sku_code"));
        vo.setSkuName(rs.getString("sku_name"));
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
                inner join sku k on k.id = c.sku_id
                inner join product p on p.id = k.product_id
                where p.product_code like ? or p.product_name like ?
                   or k.sku_code like ? or k.sku_name like ?
                """,
                Long.class,
                like,
                like,
                like,
                like
        );
    }

    public List<StockCheckVO> findPage(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select c.*, k.sku_code, k.sku_name, p.product_code, p.product_name
                from stock_check c
                inner join sku k on k.id = c.sku_id
                inner join product p on p.id = k.product_id
                where p.product_code like ? or p.product_name like ?
                   or k.sku_code like ? or k.sku_name like ?
                order by c.id desc
                limit ? offset ?
                """,
                rowMapper,
                like,
                like,
                like,
                like,
                pageSize,
                offset
        );
    }

    public void insert(Long skuId, int systemQuantity, int actualQuantity, int difference) {
        jdbcTemplate.update(
                """
                insert into stock_check(sku_id, system_quantity, actual_quantity, difference)
                values (?, ?, ?, ?)
                """,
                skuId,
                systemQuantity,
                actualQuantity,
                difference
        );
    }
}
