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
        vo.setProductId(rs.getLong("product_id"));
        vo.setProductCode(rs.getString("product_code"));
        vo.setProductName(rs.getString("product_name"));
        vo.setQuantity(rs.getInt("quantity"));
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
                inner join product p on p.id = o.product_id
                where p.product_code like ? or p.product_name like ? or o.operator like ?
                """,
                Long.class,
                like,
                like,
                like
        );
    }

    public List<InboundVO> findPage(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select o.*, p.product_code, p.product_name
                from inbound_order o
                inner join product p on p.id = o.product_id
                where p.product_code like ? or p.product_name like ? or o.operator like ?
                order by o.id desc
                limit ? offset ?
                """,
                rowMapper,
                like,
                like,
                like,
                pageSize,
                offset
        );
    }

    public void insert(Long productId, int quantity, String operator) {
        jdbcTemplate.update(
                "insert into inbound_order(product_id, quantity, operator) values (?, ?, ?)",
                productId,
                quantity,
                operator
        );
    }
}
