package com.supermarket.inventory.purchaseinbound.mapper;

import com.supermarket.inventory.purchaseinbound.entity.PurchaseInbound;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundItem;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundItemVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class PurchaseInboundMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PurchaseInboundVO> inboundRowMapper = (rs, rowNum) -> {
        PurchaseInboundVO vo = new PurchaseInboundVO();
        vo.setId(rs.getLong("id"));
        vo.setOrderNo(rs.getString("order_no"));
        vo.setTotalQuantity(rs.getInt("total_quantity"));
        vo.setTotalAmount(rs.getBigDecimal("total_amount"));
        vo.setStatus(rs.getString("status"));
        vo.setOperator(rs.getString("operator"));
        vo.setRemark(rs.getString("remark"));
        vo.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return vo;
    };

    private final RowMapper<PurchaseInboundItemVO> itemRowMapper = (rs, rowNum) -> {
        PurchaseInboundItemVO vo = new PurchaseInboundItemVO();
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
        vo.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        vo.setCostPrice(rs.getBigDecimal("cost_price"));
        vo.setAmount(rs.getBigDecimal("amount"));
        return vo;
    };

    public PurchaseInboundMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String findMaxOrderNo(String pattern) {
        return jdbcTemplate.queryForObject(
                "select max(order_no) from purchase_inbound where order_no like ?",
                String.class,
                pattern
        );
    }

    public Long insertInbound(PurchaseInbound inbound) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into purchase_inbound(order_no, total_quantity, total_amount, status, operator, remark)
                    values (?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, inbound.getOrderNo());
            ps.setInt(2, inbound.getTotalQuantity());
            ps.setBigDecimal(3, inbound.getTotalAmount());
            ps.setString(4, inbound.getStatus());
            ps.setString(5, inbound.getOperator());
            ps.setString(6, inbound.getRemark());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void insertItems(List<PurchaseInboundItem> items) {
        jdbcTemplate.batchUpdate(
                """
                insert into purchase_inbound_item(
                    purchase_inbound_id, sku_id, quantity, unit, conversion_rate,
                    base_quantity, purchase_price, cost_price, amount
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                items,
                items.size(),
                (ps, item) -> {
                    ps.setLong(1, item.getPurchaseInboundId());
                    ps.setLong(2, item.getSkuId());
                    ps.setInt(3, item.getQuantity());
                    ps.setString(4, item.getUnit());
                    ps.setInt(5, item.getConversionRate());
                    ps.setInt(6, item.getBaseQuantity());
                    ps.setBigDecimal(7, item.getPurchasePrice());
                    ps.setBigDecimal(8, item.getCostPrice());
                    ps.setBigDecimal(9, item.getAmount());
                }
        );
    }

    public Optional<PurchaseInboundVO> findById(Long id) {
        List<PurchaseInboundVO> rows = jdbcTemplate.query(
                "select * from purchase_inbound where id = ?",
                inboundRowMapper,
                id
        );
        return rows.stream().findFirst();
    }

    public List<PurchaseInboundItemVO> findItemsByInboundId(Long inboundId) {
        return jdbcTemplate.query(
                """
                select item.*, k.sku_code, k.sku_name, p.product_code, p.product_name
                from purchase_inbound_item item
                inner join sku k on k.id = item.sku_id
                inner join product p on p.id = k.product_id
                where item.purchase_inbound_id = ?
                order by item.id asc
                """,
                itemRowMapper,
                inboundId
        );
    }

    public long count(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from purchase_inbound pi
                where pi.order_no like ? or pi.operator like ? or pi.remark like ?
                   or exists (
                       select 1
                       from purchase_inbound_item item
                       inner join sku k on k.id = item.sku_id
                       inner join product p on p.id = k.product_id
                       where item.purchase_inbound_id = pi.id
                         and (k.sku_code like ? or k.sku_name like ? or p.product_code like ? or p.product_name like ?)
                   )
                """,
                Long.class,
                like,
                like,
                like,
                like,
                like,
                like,
                like
        );
        return count == null ? 0L : count;
    }

    public List<PurchaseInboundVO> findPage(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select pi.*
                from purchase_inbound pi
                where pi.order_no like ? or pi.operator like ? or pi.remark like ?
                   or exists (
                       select 1
                       from purchase_inbound_item item
                       inner join sku k on k.id = item.sku_id
                       inner join product p on p.id = k.product_id
                       where item.purchase_inbound_id = pi.id
                         and (k.sku_code like ? or k.sku_name like ? or p.product_code like ? or p.product_name like ?)
                   )
                order by pi.id desc
                limit ? offset ?
                """,
                inboundRowMapper,
                like,
                like,
                like,
                like,
                like,
                like,
                like,
                pageSize,
                offset
        );
    }
}
