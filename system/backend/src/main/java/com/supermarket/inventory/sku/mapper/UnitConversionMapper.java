package com.supermarket.inventory.sku.mapper;

import com.supermarket.inventory.sku.entity.SkuUnitConversion;
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
public class UnitConversionMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<SkuUnitConversion> conversionRowMapper = (rs, rowNum) -> {
        SkuUnitConversion conversion = new SkuUnitConversion();
        conversion.setId(rs.getLong("id"));
        conversion.setSkuId(rs.getLong("sku_id"));
        conversion.setUnitName(rs.getString("unit_name"));
        conversion.setConversionRate(rs.getInt("conversion_rate"));
        conversion.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return conversion;
    };

    public UnitConversionMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SkuUnitConversion> findBySkuId(Long skuId) {
        return jdbcTemplate.query(
                "select * from sku_unit_conversion where sku_id = ? order by id asc",
                conversionRowMapper,
                skuId
        );
    }

    public Optional<SkuUnitConversion> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from sku_unit_conversion where id = ?",
                    conversionRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsBySkuIdAndUnitName(Long skuId, String unitName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sku_unit_conversion where sku_id = ? and unit_name = ?",
                Integer.class,
                skuId,
                unitName
        );
        return count != null && count > 0;
    }

    public boolean existsOtherBySkuIdAndUnitName(Long skuId, Long id, String unitName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sku_unit_conversion where sku_id = ? and id <> ? and unit_name = ?",
                Integer.class,
                skuId,
                id,
                unitName
        );
        return count != null && count > 0;
    }

    public Long insert(SkuUnitConversion conversion) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into sku_unit_conversion(sku_id, unit_name, conversion_rate)
                    values (?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, conversion.getSkuId());
            ps.setString(2, conversion.getUnitName());
            ps.setInt(3, conversion.getConversionRate());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(SkuUnitConversion conversion) {
        jdbcTemplate.update(
                """
                update sku_unit_conversion
                set unit_name = ?, conversion_rate = ?
                where id = ?
                """,
                conversion.getUnitName(),
                conversion.getConversionRate(),
                conversion.getId()
        );
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from sku_unit_conversion where id = ?", id);
    }

    public void deleteBySkuId(Long skuId) {
        jdbcTemplate.update("delete from sku_unit_conversion where sku_id = ?", skuId);
    }

    public void deleteByProductId(Long productId) {
        jdbcTemplate.update(
                """
                delete c from sku_unit_conversion c
                inner join sku s on s.id = c.sku_id
                where s.product_id = ?
                """,
                productId
        );
    }
}
