package com.supermarket.inventory.sku.mapper;

import com.supermarket.inventory.sku.entity.Sku;
import org.springframework.dao.EmptyResultDataAccessException;
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
public class SkuMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Sku> skuRowMapper = (rs, rowNum) -> {
        Sku sku = new Sku();
        sku.setId(rs.getLong("id"));
        sku.setProductId(rs.getLong("product_id"));
        sku.setSkuCode(rs.getString("sku_code"));
        sku.setSkuName(rs.getString("sku_name"));
        sku.setSpec(rs.getString("spec"));
        sku.setBarcode(rs.getString("barcode"));
        sku.setBaseUnit(rs.getString("base_unit"));
        sku.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        sku.setSalePrice(rs.getBigDecimal("sale_price"));
        sku.setStatus(rs.getInt("status"));
        sku.setIsDefault(rs.getInt("is_default"));
        sku.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return sku;
    };

    public SkuMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Sku> findByProductId(Long productId) {
        return jdbcTemplate.query(
                "select * from sku where product_id = ? order by is_default desc, id asc",
                skuRowMapper,
                productId
        );
    }

    public Optional<Sku> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("select * from sku where id = ?", skuRowMapper, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Sku> findByCode(String skuCode) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from sku where sku_code = ?",
                    skuRowMapper,
                    skuCode
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Sku> findDefaultByProductId(Long productId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from sku where product_id = ? and is_default = 1 limit 1",
                    skuRowMapper,
                    productId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public int countByProductId(Long productId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sku where product_id = ?",
                Integer.class,
                productId
        );
        return count == null ? 0 : count;
    }

    public Long insert(Sku sku) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into sku(product_id, sku_code, sku_name, spec, barcode, base_unit, purchase_price, sale_price, status, is_default)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, sku.getProductId());
            ps.setString(2, sku.getSkuCode());
            ps.setString(3, sku.getSkuName());
            ps.setString(4, sku.getSpec());
            ps.setString(5, sku.getBarcode());
            ps.setString(6, sku.getBaseUnit());
            ps.setBigDecimal(7, sku.getPurchasePrice());
            ps.setBigDecimal(8, sku.getSalePrice());
            ps.setInt(9, sku.getStatus());
            ps.setInt(10, sku.getIsDefault());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Sku sku) {
        jdbcTemplate.update(
                """
                update sku
                set sku_name = ?, spec = ?, barcode = ?, base_unit = ?, purchase_price = ?, sale_price = ?, status = ?
                where id = ?
                """,
                sku.getSkuName(),
                sku.getSpec(),
                sku.getBarcode(),
                sku.getBaseUnit(),
                sku.getPurchasePrice(),
                sku.getSalePrice(),
                sku.getStatus(),
                sku.getId()
        );
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from sku where id = ?", id);
    }

    public void deleteByProductId(Long productId) {
        jdbcTemplate.update("delete from sku where product_id = ?", productId);
    }
}
