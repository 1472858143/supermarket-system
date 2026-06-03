package com.supermarket.inventory.stockbatch.mapper;

import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.entity.StockBatchLog;
import com.supermarket.inventory.stockbatch.vo.StockBatchVO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class StockBatchMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<StockBatch> stockBatchRowMapper = (rs, rowNum) -> {
        StockBatch batch = new StockBatch();
        batch.setId(rs.getLong("id"));
        batch.setBatchNo(rs.getString("batch_no"));
        batch.setSkuId(rs.getLong("sku_id"));
        batch.setPurchaseInboundReceiptBatchId(rs.getLong("purchase_inbound_receipt_batch_id"));
        batch.setInitialQuantity(rs.getInt("initial_quantity"));
        batch.setQuantity(rs.getInt("quantity"));
        batch.setStatus(rs.getString("status"));
        batch.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        batch.setCostPrice(rs.getBigDecimal("cost_price"));
        Date productionDate = rs.getDate("production_date");
        batch.setProductionDate(productionDate == null ? null : productionDate.toLocalDate());
        batch.setShelfLifeDays(rs.getInt("shelf_life_days"));
        Date expireDate = rs.getDate("expire_date");
        batch.setExpireDate(expireDate == null ? null : expireDate.toLocalDate());
        batch.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        batch.setUpdateTime(toLocalDateTime(rs.getTimestamp("update_time")));
        return batch;
    };

    private final RowMapper<StockBatchVO> rowMapper = (rs, rowNum) -> {
        StockBatchVO vo = new StockBatchVO();
        vo.setId(rs.getLong("id"));
        vo.setBatchNo(rs.getString("batch_no"));
        vo.setSkuId(rs.getLong("sku_id"));
        vo.setSkuCode(rs.getString("sku_code"));
        vo.setSkuName(rs.getString("sku_name"));
        vo.setProductCode(rs.getString("product_code"));
        vo.setProductName(rs.getString("product_name"));
        vo.setPurchaseInboundReceiptBatchId(rs.getLong("purchase_inbound_receipt_batch_id"));
        vo.setInitialQuantity(rs.getInt("initial_quantity"));
        vo.setQuantity(rs.getInt("quantity"));
        vo.setStatus(rs.getString("status"));
        vo.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        vo.setCostPrice(rs.getBigDecimal("cost_price"));
        Date productionDate = rs.getDate("production_date");
        vo.setProductionDate(productionDate == null ? null : productionDate.toLocalDate());
        vo.setShelfLifeDays(rs.getInt("shelf_life_days"));
        Date expireDate = rs.getDate("expire_date");
        vo.setExpireDate(expireDate == null ? null : expireDate.toLocalDate());
        vo.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        vo.setUpdateTime(toLocalDateTime(rs.getTimestamp("update_time")));
        return vo;
    };

    public StockBatchMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String findMaxBatchNo(String pattern) {
        return jdbcTemplate.queryForObject(
                "select max(batch_no) from stock_batch where batch_no like ?",
                String.class,
                pattern
        );
    }

    public Long insertBatch(StockBatch batch) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into stock_batch(
                        batch_no, sku_id, purchase_inbound_receipt_batch_id, initial_quantity, quantity,
                        status, purchase_price, cost_price, production_date, shelf_life_days, expire_date
                    )
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, batch.getBatchNo());
            ps.setLong(2, batch.getSkuId());
            ps.setLong(3, batch.getPurchaseInboundReceiptBatchId());
            ps.setInt(4, batch.getInitialQuantity());
            ps.setInt(5, batch.getQuantity());
            ps.setString(6, batch.getStatus());
            ps.setBigDecimal(7, batch.getPurchasePrice());
            ps.setBigDecimal(8, batch.getCostPrice());
            ps.setDate(9, Date.valueOf(batch.getProductionDate()));
            ps.setInt(10, batch.getShelfLifeDays());
            ps.setDate(11, Date.valueOf(batch.getExpireDate()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void insertLog(StockBatchLog log) {
        jdbcTemplate.update(
                """
                insert into stock_batch_log(
                    stock_batch_id, sku_id, change_type, change_quantity,
                    before_quantity, after_quantity, source_type, source_id, reason, remark
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                log.getStockBatchId(),
                log.getSkuId(),
                log.getChangeType(),
                log.getChangeQuantity(),
                log.getBeforeQuantity(),
                log.getAfterQuantity(),
                log.getSourceType(),
                log.getSourceId(),
                log.getReason(),
                log.getRemark()
        );
    }

    public Optional<StockBatch> findByIdAndSkuIdForUpdate(Long batchId, Long skuId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from stock_batch where id = ? and sku_id = ? for update",
                    stockBatchRowMapper,
                    batchId,
                    skuId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public int updateStatus(Long batchId, Long skuId, String status) {
        return jdbcTemplate.update(
                "update stock_batch set status = ? where id = ? and sku_id = ?",
                status,
                batchId,
                skuId
        );
    }

    public List<StockBatch> findExpiredAvailableBatchesForUpdate(LocalDate today) {
        return jdbcTemplate.query(
                """
                select * from stock_batch
                where status = 'AVAILABLE'
                  and quantity > 0
                  and expire_date < ?
                order by expire_date asc, id asc
                for update
                """,
                stockBatchRowMapper,
                Date.valueOf(today)
        );
    }

    public List<StockBatch> findAvailableBatchesForConsumption(Long skuId) {
        return jdbcTemplate.query(
                """
                select * from stock_batch
                where sku_id = ?
                  and status = 'AVAILABLE'
                  and quantity > 0
                order by expire_date asc, id asc
                for update
                """,
                stockBatchRowMapper,
                skuId
        );
    }

    public List<StockBatch> findConsumableBySkuIdForUpdate(Long skuId) {
        return findAvailableBatchesForConsumption(skuId);
    }

    public List<StockBatch> findCheckableBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(skuIds.size(), "?"));
        List<Object> params = new ArrayList<>(skuIds);
        return jdbcTemplate.query(
                """
                select * from stock_batch
                where status <> 'CLOSED'
                  and sku_id in (
                """ + placeholders + """
                  )
                order by sku_id asc, expire_date asc, id asc
                """,
                stockBatchRowMapper,
                params.toArray()
        );
    }

    public int updateRemainingQuantityAndStatus(Long batchId, Long skuId, int quantity, String status) {
        return jdbcTemplate.update(
                "update stock_batch set quantity = ?, status = ? where id = ? and sku_id = ?",
                quantity,
                status,
                batchId,
                skuId
        );
    }

    public int sumQuantityBySkuId(Long skuId) {
        Integer total = jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_batch where sku_id = ?",
                Integer.class,
                skuId
        );
        return total == null ? 0 : total;
    }

    public List<StockBatchVO> findBySkuId(Long skuId) {
        return jdbcTemplate.query(
                """
                select b.*, k.sku_code, k.sku_name, p.product_code, p.product_name
                from stock_batch b
                inner join sku k on k.id = b.sku_id
                inner join product p on p.id = k.product_id
                where b.sku_id = ?
                order by b.expire_date asc, b.id asc
                """,
                rowMapper,
                skuId
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
