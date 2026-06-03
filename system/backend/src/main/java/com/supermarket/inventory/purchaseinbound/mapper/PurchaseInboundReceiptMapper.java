package com.supermarket.inventory.purchaseinbound.mapper;

import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundReceipt;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundReceiptBatch;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundReceiptBatchVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundReceiptVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PurchaseInboundReceiptMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PurchaseInboundReceiptVO> receiptRowMapper = (rs, rowNum) -> {
        PurchaseInboundReceiptVO vo = new PurchaseInboundReceiptVO();
        vo.setId(rs.getLong("id"));
        vo.setReceiptNo(rs.getString("receipt_no"));
        vo.setPurchaseInboundId(rs.getLong("purchase_inbound_id"));
        vo.setOperatorUserId(rs.getObject("operator_user_id", Long.class));
        vo.setOperatorUsername(rs.getString("operator_username"));
        vo.setTotalBaseQuantity(rs.getInt("total_base_quantity"));
        vo.setTotalAmount(rs.getBigDecimal("total_amount"));
        vo.setRemark(rs.getString("remark"));
        vo.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        return vo;
    };

    private final RowMapper<PurchaseInboundReceiptBatchVO> batchRowMapper = (rs, rowNum) -> {
        PurchaseInboundReceiptBatchVO vo = new PurchaseInboundReceiptBatchVO();
        vo.setId(rs.getLong("id"));
        vo.setReceiptId(rs.getLong("receipt_id"));
        vo.setPurchaseInboundId(rs.getLong("purchase_inbound_id"));
        vo.setPurchaseInboundItemId(rs.getLong("purchase_inbound_item_id"));
        vo.setSkuId(rs.getLong("sku_id"));
        vo.setQuantity(rs.getInt("quantity"));
        vo.setBaseQuantity(rs.getInt("base_quantity"));
        Date productionDate = rs.getDate("production_date");
        vo.setProductionDate(productionDate == null ? null : productionDate.toLocalDate());
        vo.setShelfLifeDays((Integer) rs.getObject("shelf_life_days"));
        Date expireDate = rs.getDate("expire_date");
        vo.setExpireDate(expireDate == null ? null : expireDate.toLocalDate());
        vo.setPurchasePriceSnapshot(rs.getBigDecimal("purchase_price_snapshot"));
        vo.setCostPriceSnapshot(rs.getBigDecimal("cost_price_snapshot"));
        vo.setAmount(rs.getBigDecimal("amount"));
        vo.setSupplierSkuCodeSnapshot(rs.getString("supplier_sku_code_snapshot"));
        vo.setSupplierSkuNameSnapshot(rs.getString("supplier_sku_name_snapshot"));
        vo.setSupplierSpecSnapshot(rs.getString("supplier_spec_snapshot"));
        vo.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        return vo;
    };

    public PurchaseInboundReceiptMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insertReceipt(PurchaseInboundReceipt receipt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into purchase_inbound_receipt(
                        receipt_no, purchase_inbound_id, operator_user_id, operator_username,
                        total_base_quantity, total_amount, remark
                    )
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, receipt.getReceiptNo());
            ps.setLong(2, receipt.getPurchaseInboundId());
            ps.setObject(3, receipt.getOperatorUserId());
            ps.setString(4, receipt.getOperatorUsername());
            ps.setInt(5, receipt.getTotalBaseQuantity());
            ps.setBigDecimal(6, receipt.getTotalAmount());
            ps.setString(7, receipt.getRemark());
            return ps;
        }, keyHolder);
        Long id = toLongKey(keyHolder);
        receipt.setId(id);
        return id;
    }

    public Long insertReceiptBatch(PurchaseInboundReceiptBatch batch) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into purchase_inbound_receipt_batch(
                        receipt_id, purchase_inbound_id, purchase_inbound_item_id, sku_id,
                        quantity, base_quantity, production_date, shelf_life_days, expire_date,
                        purchase_price_snapshot, cost_price_snapshot, amount,
                        supplier_sku_code_snapshot, supplier_sku_name_snapshot, supplier_spec_snapshot
                    )
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, batch.getReceiptId());
            ps.setLong(2, batch.getPurchaseInboundId());
            ps.setLong(3, batch.getPurchaseInboundItemId());
            ps.setLong(4, batch.getSkuId());
            ps.setInt(5, batch.getQuantity());
            ps.setInt(6, batch.getBaseQuantity());
            ps.setDate(7, batch.getProductionDate() == null ? null : Date.valueOf(batch.getProductionDate()));
            ps.setObject(8, batch.getShelfLifeDays());
            ps.setDate(9, batch.getExpireDate() == null ? null : Date.valueOf(batch.getExpireDate()));
            ps.setBigDecimal(10, batch.getPurchasePriceSnapshot());
            ps.setBigDecimal(11, batch.getCostPriceSnapshot());
            ps.setBigDecimal(12, batch.getAmount());
            ps.setString(13, batch.getSupplierSkuCodeSnapshot());
            ps.setString(14, batch.getSupplierSkuNameSnapshot());
            ps.setString(15, batch.getSupplierSpecSnapshot());
            return ps;
        }, keyHolder);
        Long id = toLongKey(keyHolder);
        batch.setId(id);
        return id;
    }

    public List<PurchaseInboundReceiptVO> findReceiptsByInboundId(Long purchaseInboundId) {
        return jdbcTemplate.query(
                """
                select *
                from purchase_inbound_receipt
                where purchase_inbound_id = ?
                order by id asc
                """,
                receiptRowMapper,
                purchaseInboundId
        );
    }

    public List<PurchaseInboundReceiptBatchVO> findReceiptBatchesByReceiptIds(List<Long> receiptIds) {
        if (receiptIds == null || receiptIds.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = receiptIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));
        String sql = """
                select *
                from purchase_inbound_receipt_batch
                where receipt_id in (%s)
                order by receipt_id asc, id asc
                """.formatted(placeholders);
        return jdbcTemplate.query(sql, batchRowMapper, receiptIds.toArray());
    }

    public String findMaxReceiptNo(String pattern) {
        return jdbcTemplate.queryForObject(
                "select max(receipt_no) from purchase_inbound_receipt where receipt_no like ?",
                String.class,
                pattern
        );
    }

    private static Long toLongKey(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
