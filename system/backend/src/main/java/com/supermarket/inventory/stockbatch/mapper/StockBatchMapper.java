package com.supermarket.inventory.stockbatch.mapper;

import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.entity.StockBatchLog;
import com.supermarket.inventory.stockbatch.vo.StockBatchVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class StockBatchMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<StockBatchVO> rowMapper = (rs, rowNum) -> {
        StockBatchVO vo = new StockBatchVO();
        vo.setId(rs.getLong("id"));
        vo.setBatchNo(rs.getString("batch_no"));
        vo.setSkuId(rs.getLong("sku_id"));
        vo.setSkuCode(rs.getString("sku_code"));
        vo.setSkuName(rs.getString("sku_name"));
        vo.setProductCode(rs.getString("product_code"));
        vo.setProductName(rs.getString("product_name"));
        vo.setPurchaseInboundItemId(rs.getLong("purchase_inbound_item_id"));
        vo.setInitialQuantity(rs.getInt("initial_quantity"));
        vo.setQuantity(rs.getInt("quantity"));
        vo.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        Date productionDate = rs.getDate("production_date");
        vo.setProductionDate(productionDate == null ? null : productionDate.toLocalDate());
        vo.setShelfLifeDays(rs.getInt("shelf_life_days"));
        Date expireDate = rs.getDate("expire_date");
        vo.setExpireDate(expireDate == null ? null : expireDate.toLocalDate());
        vo.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        vo.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
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
                        batch_no, sku_id, purchase_inbound_item_id, initial_quantity, quantity,
                        purchase_price, production_date, shelf_life_days, expire_date
                    )
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, batch.getBatchNo());
            ps.setLong(2, batch.getSkuId());
            ps.setLong(3, batch.getPurchaseInboundItemId());
            ps.setInt(4, batch.getInitialQuantity());
            ps.setInt(5, batch.getQuantity());
            ps.setBigDecimal(6, batch.getPurchasePrice());
            ps.setDate(7, Date.valueOf(batch.getProductionDate()));
            ps.setInt(8, batch.getShelfLifeDays());
            ps.setDate(9, Date.valueOf(batch.getExpireDate()));
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
                    before_quantity, after_quantity, source_type, source_id
                )
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                log.getStockBatchId(),
                log.getSkuId(),
                log.getChangeType(),
                log.getChangeQuantity(),
                log.getBeforeQuantity(),
                log.getAfterQuantity(),
                log.getSourceType(),
                log.getSourceId()
        );
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
}
