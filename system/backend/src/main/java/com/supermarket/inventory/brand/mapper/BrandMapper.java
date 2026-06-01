package com.supermarket.inventory.brand.mapper;

import com.supermarket.inventory.brand.entity.Brand;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class BrandMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Brand> brandRowMapper = (rs, rowNum) -> {
        Brand brand = new Brand();
        brand.setId(rs.getLong("id"));
        brand.setBrandCode(rs.getString("brand_code"));
        brand.setBrandName(rs.getString("brand_name"));
        brand.setStatus(rs.getInt("status"));
        brand.setRemark(rs.getString("remark"));
        Timestamp createTime = rs.getTimestamp("create_time");
        brand.setCreateTime(createTime == null ? null : createTime.toLocalDateTime());
        return brand;
    };

    public BrandMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(String keyword, Integer status) {
        QueryParts query = buildSearchQuery(
                """
                select count(*)
                from brand
                """,
                keyword,
                status
        );
        Long count = jdbcTemplate.queryForObject(query.sql(), Long.class, query.args().toArray());
        return count == null ? 0L : count;
    }

    public List<Brand> findPage(String keyword, Integer status, int offset, int pageSize) {
        QueryParts query = buildSearchQuery(
                """
                select *
                from brand
                """,
                keyword,
                status
        );
        String sql = query.sql() + "\norder by id desc\nlimit ? offset ?";
        List<Object> args = new ArrayList<>(query.args());
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql, brandRowMapper, args.toArray());
    }

    public List<Brand> findEnabledOptions() {
        return jdbcTemplate.query(
                """
                select *
                from brand
                where status = 1
                order by brand_name asc, id asc
                """,
                brandRowMapper
        );
    }

    public Optional<Brand> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from brand where id = ?",
                    brandRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsByName(String brandName) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from brand where brand_name = ?",
                Long.class,
                brandName
        );
        return count != null && count > 0;
    }

    public boolean existsByNameExcluding(String brandName, Long excludeId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from brand where brand_name = ? and id <> ?",
                Long.class,
                brandName,
                excludeId
        );
        return count != null && count > 0;
    }

    public String findMaxCode(String pattern) {
        return jdbcTemplate.queryForObject(
                "select max(brand_code) from brand where brand_code like ?",
                String.class,
                pattern
        );
    }

    public long countProductsByBrandId(Long brandId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from product where brand_id = ?",
                Long.class,
                brandId
        );
        return count == null ? 0L : count;
    }

    public Long insert(Brand brand) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into brand(brand_code, brand_name, status, remark)
                    values (?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, brand.getBrandCode());
            ps.setString(2, brand.getBrandName());
            ps.setInt(3, brand.getStatus());
            ps.setString(4, brand.getRemark());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Brand brand) {
        jdbcTemplate.update(
                """
                update brand
                set brand_name = ?, status = ?, remark = ?
                where id = ?
                """,
                brand.getBrandName(),
                brand.getStatus(),
                brand.getRemark(),
                brand.getId()
        );
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from brand where id = ?", id);
    }

    private QueryParts buildSearchQuery(String selectSql, String keyword, Integer status) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        StringBuilder sql = new StringBuilder(selectSql)
                .append("where (brand_code like ? or brand_name like ?)");
        List<Object> args = new ArrayList<>();
        args.add(like);
        args.add(like);
        if (status != null) {
            sql.append(" and status = ?");
            args.add(status);
        }
        return new QueryParts(sql.toString(), args);
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
