package com.supermarket.inventory.purchaseinbound.mapper;

import com.supermarket.inventory.purchaseinbound.entity.PurchaseInbound;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundApprovalLog;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundItem;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundApprovalLogVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundItemVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.List;

@Repository
public class PurchaseInboundMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PurchaseInboundVO> inboundRowMapper = (rs, rowNum) -> {
        PurchaseInboundVO vo = new PurchaseInboundVO();
        vo.setId(rs.getLong("id"));
        vo.setOrderNo(rs.getString("order_no"));
        vo.setSupplierId(rs.getLong("supplier_id"));
        vo.setSupplierCode(rs.getString("supplier_code"));
        vo.setSupplierName(rs.getString("supplier_name"));
        vo.setPlannedTotalQuantity(rs.getInt("planned_total_quantity"));
        vo.setInboundTotalQuantity(rs.getInt("inbound_total_quantity"));
        vo.setPlannedTotalAmount(rs.getBigDecimal("planned_total_amount"));
        vo.setInboundTotalAmount(rs.getBigDecimal("inbound_total_amount"));
        vo.setStatus(rs.getString("status"));
        vo.setCreatorUserId(rs.getObject("creator_user_id", Long.class));
        vo.setCreatorUsername(rs.getString("creator_username"));
        vo.setSubmitterUserId(rs.getObject("submitter_user_id", Long.class));
        vo.setSubmitterUsername(rs.getString("submitter_username"));
        vo.setSubmitTime(toLocalDateTime(rs.getTimestamp("submit_time")));
        vo.setApproverUserId(rs.getObject("approver_user_id", Long.class));
        vo.setApproverUsername(rs.getString("approver_username"));
        vo.setApproveTime(toLocalDateTime(rs.getTimestamp("approve_time")));
        vo.setCancelUserId(rs.getObject("cancel_user_id", Long.class));
        vo.setCancelUsername(rs.getString("cancel_username"));
        vo.setCancelTime(toLocalDateTime(rs.getTimestamp("cancel_time")));
        vo.setCancelReason(rs.getString("cancel_reason"));
        vo.setCloseUserId(rs.getObject("close_user_id", Long.class));
        vo.setCloseUsername(rs.getString("close_username"));
        vo.setCloseTime(toLocalDateTime(rs.getTimestamp("close_time")));
        vo.setCloseReason(rs.getString("close_reason"));
        vo.setOperator(rs.getString("operator"));
        vo.setRemark(rs.getString("remark"));
        vo.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
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
        vo.setSupplierSkuId(rs.getObject("supplier_sku_id", Long.class));
        vo.setSupplierSkuCodeSnapshot(rs.getString("supplier_sku_code_snapshot"));
        vo.setSupplierSkuNameSnapshot(rs.getString("supplier_sku_name_snapshot"));
        vo.setSupplierSpecSnapshot(rs.getString("supplier_spec_snapshot"));
        vo.setPlannedQuantity(rs.getInt("planned_quantity"));
        vo.setUnit(rs.getString("unit"));
        vo.setConversionRate(rs.getInt("conversion_rate"));
        vo.setPlannedBaseQuantity(rs.getInt("planned_base_quantity"));
        vo.setPlannedAmount(rs.getBigDecimal("planned_amount"));
        vo.setInboundedBaseQuantity(rs.getInt("inbounded_base_quantity"));
        vo.setInboundedAmount(rs.getBigDecimal("inbounded_amount"));
        vo.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        vo.setCostPrice(rs.getBigDecimal("cost_price"));
        vo.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        return vo;
    };

    private final RowMapper<PurchaseInboundApprovalLogVO> approvalLogRowMapper = (rs, rowNum) -> {
        PurchaseInboundApprovalLogVO vo = new PurchaseInboundApprovalLogVO();
        vo.setAction(rs.getString("action"));
        vo.setFromStatus(rs.getString("from_status"));
        vo.setToStatus(rs.getString("to_status"));
        vo.setOperatorUserId(rs.getObject("operator_user_id", Long.class));
        vo.setOperatorUsername(rs.getString("operator_username"));
        vo.setReason(rs.getString("reason"));
        vo.setRemark(rs.getString("remark"));
        vo.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
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
                    insert into purchase_inbound(
                        order_no, supplier_id, planned_total_quantity, planned_total_amount,
                        inbound_total_quantity, inbound_total_amount, status,
                        creator_user_id, creator_username, operator, remark
                    )
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, inbound.getOrderNo());
            ps.setLong(2, inbound.getSupplierId());
            ps.setInt(3, defaultInt(inbound.getPlannedTotalQuantity()));
            ps.setBigDecimal(4, inbound.getPlannedTotalAmount());
            ps.setInt(5, defaultInt(inbound.getInboundTotalQuantity()));
            ps.setBigDecimal(6, defaultAmount(inbound.getInboundTotalAmount()));
            ps.setString(7, inbound.getStatus());
            ps.setObject(8, inbound.getCreatorUserId());
            ps.setString(9, inbound.getCreatorUsername());
            ps.setString(10, inbound.getOperator());
            ps.setString(11, inbound.getRemark());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        Long id = key == null ? null : key.longValue();
        inbound.setId(id);
        return id;
    }

    public Long insertItem(PurchaseInboundItem item) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into purchase_inbound_item(
                        purchase_inbound_id, sku_id, supplier_sku_id,
                        supplier_sku_code_snapshot, supplier_sku_name_snapshot, supplier_spec_snapshot,
                        planned_quantity, unit, conversion_rate, planned_base_quantity, planned_amount,
                        inbounded_base_quantity, inbounded_amount, purchase_price, cost_price
                    )
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, item.getPurchaseInboundId());
            ps.setLong(2, item.getSkuId());
            ps.setObject(3, item.getSupplierSkuId());
            ps.setString(4, item.getSupplierSkuCodeSnapshot());
            ps.setString(5, item.getSupplierSkuNameSnapshot());
            ps.setString(6, item.getSupplierSpecSnapshot());
            ps.setInt(7, defaultInt(item.getPlannedQuantity()));
            ps.setString(8, item.getUnit());
            ps.setInt(9, defaultInt(item.getConversionRate()));
            ps.setInt(10, defaultInt(item.getPlannedBaseQuantity()));
            ps.setBigDecimal(11, item.getPlannedAmount());
            ps.setInt(12, defaultInt(item.getInboundedBaseQuantity()));
            ps.setBigDecimal(13, defaultAmount(item.getInboundedAmount()));
            ps.setBigDecimal(14, item.getPurchasePrice());
            ps.setBigDecimal(15, item.getCostPrice());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        Long id = key == null ? null : key.longValue();
        item.setId(id);
        return id;
    }

    public Optional<PurchaseInboundVO> findById(Long id) {
        List<PurchaseInboundVO> rows = jdbcTemplate.query(
                """
                select pi.*, s.supplier_code, s.supplier_name
                from purchase_inbound pi
                inner join supplier s on s.id = pi.supplier_id
                where pi.id = ?
                """,
                inboundRowMapper,
                id
        );
        return rows.stream().findFirst();
    }

    public Optional<PurchaseInboundVO> findByIdForUpdate(Long id) {
        List<PurchaseInboundVO> rows = jdbcTemplate.query(
                """
                select pi.*, s.supplier_code, s.supplier_name
                from purchase_inbound pi
                inner join supplier s on s.id = pi.supplier_id
                where pi.id = ?
                for update
                """,
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

    public List<PurchaseInboundItemVO> findItemsByInboundIdForUpdate(Long inboundId) {
        return jdbcTemplate.query(
                """
                select item.*, k.sku_code, k.sku_name, p.product_code, p.product_name
                from purchase_inbound_item item
                inner join sku k on k.id = item.sku_id
                inner join product p on p.id = k.product_id
                where item.purchase_inbound_id = ?
                order by item.id asc
                for update
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
                inner join supplier s on s.id = pi.supplier_id
                where pi.order_no like ? or pi.operator like ? or pi.remark like ?
                   or s.supplier_code like ? or s.supplier_name like ?
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
                select pi.*, s.supplier_code, s.supplier_name
                from purchase_inbound pi
                inner join supplier s on s.id = pi.supplier_id
                where pi.order_no like ? or pi.operator like ? or pi.remark like ?
                   or s.supplier_code like ? or s.supplier_name like ?
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
                like,
                like,
                pageSize,
                offset
        );
    }

    public void insertApprovalLog(PurchaseInboundApprovalLog log) {
        jdbcTemplate.update(
                """
                insert into purchase_inbound_approval_log(
                    purchase_inbound_id, action, from_status, to_status,
                    operator_user_id, operator_username, reason, remark
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                log.getPurchaseInboundId(),
                log.getAction(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getOperatorUserId(),
                log.getOperatorUsername(),
                log.getReason(),
                log.getRemark()
        );
    }

    public List<PurchaseInboundApprovalLogVO> findApprovalLogsByInboundId(Long inboundId) {
        return jdbcTemplate.query(
                """
                select action, from_status, to_status, operator_user_id, operator_username,
                       reason, remark, create_time
                from purchase_inbound_approval_log
                where purchase_inbound_id = ?
                order by id asc
                """,
                approvalLogRowMapper,
                inboundId
        );
    }

    public void updatePlan(PurchaseInbound inbound) {
        jdbcTemplate.update(
                """
                update purchase_inbound
                set supplier_id = ?, planned_total_quantity = ?, planned_total_amount = ?, remark = ?
                where id = ?
                """,
                inbound.getSupplierId(),
                defaultInt(inbound.getPlannedTotalQuantity()),
                inbound.getPlannedTotalAmount(),
                inbound.getRemark(),
                inbound.getId()
        );
    }

    public void deleteItemsByInboundId(Long inboundId) {
        jdbcTemplate.update("delete from purchase_inbound_item where purchase_inbound_id = ?", inboundId);
    }

    public void updateStatusForSubmit(Long id, String status, Long submitterUserId, String submitterUsername) {
        jdbcTemplate.update(
                """
                update purchase_inbound
                set status = ?, submitter_user_id = ?, submitter_username = ?,
                    submit_time = current_timestamp, operator = ?
                where id = ?
                """,
                status,
                submitterUserId,
                submitterUsername,
                submitterUsername,
                id
        );
    }

    public void updateStatusForApprove(Long id, String status, Long approverUserId, String approverUsername) {
        jdbcTemplate.update(
                """
                update purchase_inbound
                set status = ?, approver_user_id = ?, approver_username = ?,
                    approve_time = current_timestamp, operator = ?
                where id = ?
                """,
                status,
                approverUserId,
                approverUsername,
                approverUsername,
                id
        );
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("update purchase_inbound set status = ? where id = ?", status, id);
    }

    public void updateStatusForCancel(Long id, String status, Long cancelUserId, String cancelUsername, String cancelReason) {
        jdbcTemplate.update(
                """
                update purchase_inbound
                set status = ?, cancel_user_id = ?, cancel_username = ?,
                    cancel_time = current_timestamp, cancel_reason = ?, operator = ?
                where id = ?
                """,
                status,
                cancelUserId,
                cancelUsername,
                cancelReason,
                cancelUsername,
                id
        );
    }

    public void updateStatusForClose(Long id, String status, Long closeUserId, String closeUsername, String closeReason) {
        jdbcTemplate.update(
                """
                update purchase_inbound
                set status = ?, close_user_id = ?, close_username = ?,
                    close_time = current_timestamp, close_reason = ?, operator = ?
                where id = ?
                """,
                status,
                closeUserId,
                closeUsername,
                closeReason,
                closeUsername,
                id
        );
    }

    public void updateInboundTotals(Long id, int inboundTotalQuantity, BigDecimal inboundTotalAmount) {
        jdbcTemplate.update(
                "update purchase_inbound set inbound_total_quantity = ?, inbound_total_amount = ? where id = ?",
                inboundTotalQuantity,
                inboundTotalAmount,
                id
        );
    }

    public void updateItemInboundTotals(Long id, int inboundedBaseQuantity, BigDecimal inboundedAmount) {
        jdbcTemplate.update(
                "update purchase_inbound_item set inbounded_base_quantity = ?, inbounded_amount = ? where id = ?",
                inboundedBaseQuantity,
                inboundedAmount,
                id
        );
    }

    private static int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
