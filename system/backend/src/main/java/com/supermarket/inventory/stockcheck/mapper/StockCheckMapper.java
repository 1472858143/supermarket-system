package com.supermarket.inventory.stockcheck.mapper;

import com.supermarket.inventory.stockcheck.entity.StockCheck;
import com.supermarket.inventory.stockcheck.entity.StockCheckItem;
import com.supermarket.inventory.stockcheck.vo.StockCheckItemVO;
import com.supermarket.inventory.stockcheck.vo.StockCheckVO;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class StockCheckMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<StockCheck> stockCheckRowMapper = (rs, rowNum) -> {
        StockCheck stockCheck = new StockCheck();
        stockCheck.setId(rs.getLong("id"));
        stockCheck.setCheckNo(rs.getString("check_no"));
        stockCheck.setName(rs.getString("name"));
        stockCheck.setScopeType(rs.getString("scope_type"));
        long categoryId = rs.getLong("category_id");
        stockCheck.setCategoryId(rs.wasNull() ? null : categoryId);
        stockCheck.setSkuSelectType(rs.getString("sku_select_type"));
        stockCheck.setStatus(rs.getString("status"));
        stockCheck.setTotalSkuCount(rs.getInt("total_sku_count"));
        stockCheck.setTotalBatchCount(rs.getInt("total_batch_count"));
        stockCheck.setTotalDifference(rs.getInt("total_difference"));
        stockCheck.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        stockCheck.setCompleteTime(toLocalDateTime(rs.getTimestamp("complete_time")));
        return stockCheck;
    };

    private final RowMapper<StockCheckVO> stockCheckVORowMapper = (rs, rowNum) -> {
        StockCheckVO vo = new StockCheckVO();
        vo.setId(rs.getLong("id"));
        vo.setCheckNo(rs.getString("check_no"));
        vo.setName(rs.getString("name"));
        vo.setScopeType(rs.getString("scope_type"));
        long categoryId = rs.getLong("category_id");
        vo.setCategoryId(rs.wasNull() ? null : categoryId);
        vo.setCategoryName(rs.getString("category_name"));
        vo.setSkuSelectType(rs.getString("sku_select_type"));
        vo.setStatus(rs.getString("status"));
        vo.setTotalSkuCount(rs.getInt("total_sku_count"));
        vo.setTotalBatchCount(rs.getInt("total_batch_count"));
        vo.setTotalDifference(rs.getInt("total_difference"));
        vo.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        vo.setCompleteTime(toLocalDateTime(rs.getTimestamp("complete_time")));
        return vo;
    };

    private final RowMapper<StockCheckItem> stockCheckItemRowMapper = (rs, rowNum) -> {
        StockCheckItem item = new StockCheckItem();
        item.setId(rs.getLong("id"));
        item.setStockCheckId(rs.getLong("stock_check_id"));
        item.setSkuId(rs.getLong("sku_id"));
        item.setStockBatchId(rs.getLong("stock_batch_id"));
        item.setBatchNo(rs.getString("batch_no"));
        item.setSystemQuantity(rs.getInt("system_quantity"));
        int actualQuantity = rs.getInt("actual_quantity");
        item.setActualQuantity(rs.wasNull() ? null : actualQuantity);
        int difference = rs.getInt("difference");
        item.setDifference(rs.wasNull() ? null : difference);
        item.setStatus(rs.getString("status"));
        Date expireDate = rs.getDate("expire_date");
        item.setExpireDate(expireDate == null ? null : expireDate.toLocalDate());
        item.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        item.setUpdateTime(toLocalDateTime(rs.getTimestamp("update_time")));
        return item;
    };

    private final RowMapper<StockCheckItemVO> stockCheckItemVORowMapper = (rs, rowNum) -> {
        StockCheckItemVO vo = new StockCheckItemVO();
        vo.setId(rs.getLong("id"));
        vo.setStockCheckId(rs.getLong("stock_check_id"));
        vo.setSkuId(rs.getLong("sku_id"));
        vo.setSkuCode(rs.getString("sku_code"));
        vo.setSkuName(rs.getString("sku_name"));
        vo.setProductCode(rs.getString("product_code"));
        vo.setProductName(rs.getString("product_name"));
        vo.setStockBatchId(rs.getLong("stock_batch_id"));
        vo.setBatchNo(rs.getString("batch_no"));
        vo.setBatchStatus(rs.getString("batch_status"));
        Date expireDate = rs.getDate("expire_date");
        vo.setExpireDate(expireDate == null ? null : expireDate.toLocalDate());
        vo.setSystemQuantity(rs.getInt("system_quantity"));
        int actualQuantity = rs.getInt("actual_quantity");
        vo.setActualQuantity(rs.wasNull() ? null : actualQuantity);
        int difference = rs.getInt("difference");
        vo.setDifference(rs.wasNull() ? null : difference);
        vo.setStatus(rs.getString("status"));
        vo.setUpdateTime(toLocalDateTime(rs.getTimestamp("update_time")));
        return vo;
    };

    public StockCheckMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String findMaxCheckNo(String pattern) {
        return jdbcTemplate.queryForObject(
                "select max(check_no) from stock_check where check_no like ?",
                String.class,
                pattern
        );
    }

    public Long insert(StockCheck stockCheck) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into stock_check(
                        check_no, name, scope_type, category_id, sku_select_type, status,
                        total_sku_count, total_batch_count, total_difference
                    )
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, stockCheck.getCheckNo());
            ps.setString(2, stockCheck.getName());
            ps.setString(3, stockCheck.getScopeType());
            if (stockCheck.getCategoryId() == null) {
                ps.setObject(4, null);
            } else {
                ps.setLong(4, stockCheck.getCategoryId());
            }
            ps.setString(5, stockCheck.getSkuSelectType());
            ps.setString(6, stockCheck.getStatus());
            ps.setInt(7, stockCheck.getTotalSkuCount());
            ps.setInt(8, stockCheck.getTotalBatchCount());
            ps.setInt(9, stockCheck.getTotalDifference());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void insertItem(StockCheckItem item) {
        jdbcTemplate.update(
                """
                insert into stock_check_item(
                    stock_check_id, sku_id, stock_batch_id, batch_no, system_quantity,
                    actual_quantity, difference, status, expire_date
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                item.getStockCheckId(),
                item.getSkuId(),
                item.getStockBatchId(),
                item.getBatchNo(),
                item.getSystemQuantity(),
                item.getActualQuantity(),
                item.getDifference(),
                item.getStatus(),
                item.getExpireDate() == null ? null : Date.valueOf(item.getExpireDate())
        );
    }

    public long count(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                from stock_check c
                left join category cat on cat.id = c.category_id
                where c.check_no like ?
                   or c.name like ?
                   or c.status like ?
                   or c.scope_type like ?
                   or cat.name like ?
                """,
                Long.class,
                like,
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
                select c.*, cat.name as category_name
                from stock_check c
                left join category cat on cat.id = c.category_id
                where c.check_no like ?
                   or c.name like ?
                   or c.status like ?
                   or c.scope_type like ?
                   or cat.name like ?
                order by c.id desc
                limit ? offset ?
                """,
                stockCheckVORowMapper,
                like,
                like,
                like,
                like,
                like,
                pageSize,
                offset
        );
    }

    public Optional<StockCheck> findByIdForUpdate(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from stock_check where id = ? for update",
                    stockCheckRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<StockCheckVO> findVOById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    select c.*, cat.name as category_name
                    from stock_check c
                    left join category cat on cat.id = c.category_id
                    where c.id = ?
                    """,
                    stockCheckVORowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<StockCheckItem> findItemByIdForUpdate(Long stockCheckId, Long itemId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from stock_check_item where stock_check_id = ? and id = ? for update",
                    stockCheckItemRowMapper,
                    stockCheckId,
                    itemId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public List<StockCheckItem> findItemsByCheckIdForUpdate(Long stockCheckId) {
        return jdbcTemplate.query(
                """
                select *
                from stock_check_item
                where stock_check_id = ?
                order by sku_id asc, expire_date asc, id asc
                for update
                """,
                stockCheckItemRowMapper,
                stockCheckId
        );
    }

    public List<StockCheckItemVO> findItemVOsByCheckId(Long stockCheckId) {
        return jdbcTemplate.query(
                """
                select i.*, k.sku_code, k.sku_name, p.product_code, p.product_name,
                       b.status as batch_status
                from stock_check_item i
                inner join sku k on k.id = i.sku_id
                inner join product p on p.id = k.product_id
                inner join stock_batch b on b.id = i.stock_batch_id
                where i.stock_check_id = ?
                order by k.sku_code asc, i.expire_date asc, i.id asc
                """,
                stockCheckItemVORowMapper,
                stockCheckId
        );
    }

    public void updateItemActualQuantity(Long stockCheckId, Long itemId, int actualQuantity, int difference) {
        jdbcTemplate.update(
                """
                update stock_check_item
                set actual_quantity = ?, difference = ?, status = 'COUNTED'
                where stock_check_id = ? and id = ?
                """,
                actualQuantity,
                difference,
                stockCheckId,
                itemId
        );
    }

    public void complete(Long stockCheckId, int totalDifference) {
        jdbcTemplate.update(
                """
                update stock_check
                set status = 'COMPLETED', total_difference = ?, complete_time = current_timestamp
                where id = ?
                """,
                totalDifference,
                stockCheckId
        );
    }

    public List<Long> findSkuIdsForAll() {
        return jdbcTemplate.queryForList(
                """
                select k.id
                from sku k
                inner join product p on p.id = k.product_id
                where k.status = 1 and p.status = 1
                order by k.id asc
                """,
                Long.class
        );
    }

    public List<Long> findSkuIdsByLevel1Category(Long categoryId) {
        return jdbcTemplate.queryForList(
                """
                select k.id
                from sku k
                inner join product p on p.id = k.product_id
                inner join category c on c.id = p.category_id
                where k.status = 1
                  and p.status = 1
                  and (c.id = ? or c.parent_id = ?)
                order by k.id asc
                """,
                Long.class,
                categoryId,
                categoryId
        );
    }

    public List<Long> findSkuIdsByLevel2Category(Long categoryId) {
        return jdbcTemplate.queryForList(
                """
                select k.id
                from sku k
                inner join product p on p.id = k.product_id
                where k.status = 1
                  and p.status = 1
                  and p.category_id = ?
                order by k.id asc
                """,
                Long.class,
                categoryId
        );
    }

    public List<Long> filterActiveSkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(skuIds.size(), "?"));
        List<Object> params = new ArrayList<>(skuIds);
        return jdbcTemplate.queryForList(
                """
                select k.id
                from sku k
                inner join product p on p.id = k.product_id
                where k.status = 1
                  and p.status = 1
                  and k.id in (
                """ + placeholders + """
                  )
                order by k.id asc
                """,
                Long.class,
                params.toArray()
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
