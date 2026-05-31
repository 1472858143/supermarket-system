package com.supermarket.inventory.inbound.mapper;

import com.supermarket.inventory.inbound.vo.InboundVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InboundMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<InboundVO> rowMapper = (rs, rowNum) -> {
        InboundVO vo = new InboundVO();
        vo.setId(rs.getLong("id"));
        vo.setSkuId(rs.getLong("sku_id"));
        vo.setSkuCode(rs.getString("sku_code"));
        vo.setSkuName(rs.getString("sku_name"));
        vo.setProductCode(rs.getString("product_code"));
        vo.setProductName(rs.getString("product_name"));
        vo.setQuantity(rs.getInt("quantity"));
        vo.setUnit(rs.getString("unit"));
        vo.setConversionRate(rs.getInt("conversion_rate"));
        vo.setBaseQuantity(rs.getInt("base_quantity"));
        vo.setOperator(rs.getString("operator"));
        vo.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return vo;
    };

    public InboundMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.queryForObject(
                """
                select count(*) from inbound_order o
                inner join sku k on k.id = o.sku_id
                inner join product p on p.id = k.product_id
                where p.product_code like ? or p.product_name like ?
                   or k.sku_code like ? or k.sku_name like ? or o.operator like ?
                """,
                Long.class,
                like,
                like,
                like,
                like,
                like
        );
    }

    public List<InboundVO> findPage(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select o.*, k.sku_code, k.sku_name, p.product_code, p.product_name
                from inbound_order o
                inner join sku k on k.id = o.sku_id
                inner join product p on p.id = k.product_id
                where p.product_code like ? or p.product_name like ?
                   or k.sku_code like ? or k.sku_name like ? or o.operator like ?
                order by o.id desc
                limit ? offset ?
                """,
                rowMapper,
                like,
                like,
                like,
                like,
                like,
                pageSize,
                offset
        );
    }

    public void insert(Long skuId, int quantity, String unit, int conversionRate, int baseQuantity, String operator) {
        jdbcTemplate.update(
                """
                insert into inbound_order(sku_id, quantity, unit, conversion_rate, base_quantity, operator)
                values (?, ?, ?, ?, ?, ?)
                """,
                skuId,
                quantity,
                unit,
                conversionRate,
                baseQuantity,
                operator
        );
    }
}
