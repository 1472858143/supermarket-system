package com.supermarket.inventory.supplier.mapper;

import com.supermarket.inventory.supplier.entity.SupplierSku;
import com.supermarket.inventory.supplier.vo.SupplierSkuVO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class SupplierSkuMapper {

    private static final String BASE_VO_SELECT = """
            select ss.*, k.product_id, k.sku_code, k.sku_name, k.spec, k.base_unit,
                   p.product_code, p.product_name
            from supplier_sku ss
            inner join sku k on k.id = ss.sku_id
            inner join product p on p.id = k.product_id
            """;

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<SupplierSku> supplierSkuRowMapper = (rs, rowNum) -> {
        SupplierSku binding = new SupplierSku();
        binding.setId(rs.getLong("id"));
        binding.setSupplierId(rs.getLong("supplier_id"));
        binding.setSkuId(rs.getLong("sku_id"));
        binding.setSupplierSkuCode(rs.getString("supplier_sku_code"));
        binding.setSupplierSkuName(rs.getString("supplier_sku_name"));
        binding.setSupplierSpec(rs.getString("supplier_spec"));
        binding.setDefaultPurchasePrice(rs.getBigDecimal("default_purchase_price"));
        binding.setMinPurchaseQuantity(rs.getInt("min_purchase_quantity"));
        binding.setStatus(rs.getInt("status"));
        binding.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        Timestamp updateTime = rs.getTimestamp("update_time");
        binding.setUpdateTime(updateTime == null ? null : updateTime.toLocalDateTime());
        return binding;
    };

    private final RowMapper<SupplierSkuVO> supplierSkuVoRowMapper = (rs, rowNum) -> {
        SupplierSkuVO binding = new SupplierSkuVO();
        binding.setId(rs.getLong("id"));
        binding.setSupplierId(rs.getLong("supplier_id"));
        binding.setSkuId(rs.getLong("sku_id"));
        binding.setProductId(rs.getLong("product_id"));
        binding.setSkuCode(rs.getString("sku_code"));
        binding.setSkuName(rs.getString("sku_name"));
        binding.setProductCode(rs.getString("product_code"));
        binding.setProductName(rs.getString("product_name"));
        binding.setSpec(rs.getString("spec"));
        binding.setBaseUnit(rs.getString("base_unit"));
        binding.setSupplierSkuCode(rs.getString("supplier_sku_code"));
        binding.setSupplierSkuName(rs.getString("supplier_sku_name"));
        binding.setSupplierSpec(rs.getString("supplier_spec"));
        binding.setDefaultPurchasePrice(rs.getBigDecimal("default_purchase_price"));
        binding.setMinPurchaseQuantity(rs.getInt("min_purchase_quantity"));
        binding.setStatus(rs.getInt("status"));
        binding.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        Timestamp updateTime = rs.getTimestamp("update_time");
        binding.setUpdateTime(updateTime == null ? null : updateTime.toLocalDateTime());
        return binding;
    };

    public SupplierSkuMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SupplierSkuVO> findBySupplierId(Long supplierId) {
        return jdbcTemplate.query(
                BASE_VO_SELECT + """
                where ss.supplier_id = ?
                order by ss.id desc
                """,
                supplierSkuVoRowMapper,
                supplierId
        );
    }

    public List<SupplierSkuVO> findEnabledBySupplierId(Long supplierId) {
        return jdbcTemplate.query(
                BASE_VO_SELECT + """
                where ss.supplier_id = ? and ss.status = 1
                order by ss.id desc
                """,
                supplierSkuVoRowMapper,
                supplierId
        );
    }

    public Optional<SupplierSkuVO> findVOByIdAndSupplierId(Long id, Long supplierId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    BASE_VO_SELECT + "where ss.id = ? and ss.supplier_id = ?",
                    supplierSkuVoRowMapper,
                    id,
                    supplierId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<SupplierSku> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from supplier_sku where id = ?",
                    supplierSkuRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<SupplierSku> findBySupplierIdAndSkuId(Long supplierId, Long skuId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from supplier_sku where supplier_id = ? and sku_id = ?",
                    supplierSkuRowMapper,
                    supplierId,
                    skuId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsBySupplierIdAndSkuId(Long supplierId, Long skuId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from supplier_sku where supplier_id = ? and sku_id = ?",
                Long.class,
                supplierId,
                skuId
        );
        return count != null && count > 0;
    }

    public Long insert(SupplierSku binding) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into supplier_sku(
                        supplier_id, sku_id, supplier_sku_code, supplier_sku_name,
                        supplier_spec, default_purchase_price, min_purchase_quantity, status
                    )
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, binding.getSupplierId());
            ps.setLong(2, binding.getSkuId());
            ps.setString(3, binding.getSupplierSkuCode());
            ps.setString(4, binding.getSupplierSkuName());
            ps.setString(5, binding.getSupplierSpec());
            ps.setBigDecimal(6, binding.getDefaultPurchasePrice());
            ps.setInt(7, binding.getMinPurchaseQuantity());
            ps.setInt(8, binding.getStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(SupplierSku binding) {
        jdbcTemplate.update(
                """
                update supplier_sku
                set supplier_sku_code = ?, supplier_sku_name = ?, supplier_spec = ?,
                    default_purchase_price = ?, min_purchase_quantity = ?, status = ?
                where id = ? and supplier_id = ?
                """,
                binding.getSupplierSkuCode(),
                binding.getSupplierSkuName(),
                binding.getSupplierSpec(),
                binding.getDefaultPurchasePrice(),
                binding.getMinPurchaseQuantity(),
                binding.getStatus(),
                binding.getId(),
                binding.getSupplierId()
        );
    }

    public void delete(Long id, Long supplierId) {
        jdbcTemplate.update("delete from supplier_sku where id = ? and supplier_id = ?", id, supplierId);
    }

    public long countPurchaseInboundReferences(Long supplierId, Long skuId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from purchase_inbound pi
                inner join purchase_inbound_item item on item.purchase_inbound_id = pi.id
                where pi.supplier_id = ? and item.sku_id = ?
                """,
                Long.class,
                supplierId,
                skuId
        );
        return count == null ? 0L : count;
    }
}
