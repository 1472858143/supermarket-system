package com.supermarket.inventory.product.mapper;

import com.supermarket.inventory.product.entity.Product;
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
public class ProductMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Product> productRowMapper = (rs, rowNum) -> {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setProductCode(rs.getString("product_code"));
        product.setProductName(rs.getString("product_name"));
        product.setCategoryId(rs.getLong("category_id"));
        product.setBrandId(rs.getLong("brand_id"));
        product.setStatus(rs.getInt("status"));
        product.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return product;
    };

    public ProductMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(String keyword, Long brandId) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String brandFilter = brandId == null ? "" : " and p.brand_id = ?";
        String sql = """
                select count(*) from product p
                left join category c on c.id = p.category_id
                left join brand b on b.id = p.brand_id
                where (p.product_code like ? or p.product_name like ? or c.name like ? or b.brand_code like ? or b.brand_name like ?)
                """ + brandFilter;
        Long count = brandId == null
                ? jdbcTemplate.queryForObject(sql, Long.class, like, like, like, like, like)
                : jdbcTemplate.queryForObject(sql, Long.class, like, like, like, like, like, brandId);
        return count == null ? 0L : count;
    }

    public List<Product> findPage(String keyword, Long brandId, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String brandFilter = brandId == null ? "" : " and p.brand_id = ?";
        String sql = """
                select p.* from product p
                left join category c on c.id = p.category_id
                left join brand b on b.id = p.brand_id
                where (p.product_code like ? or p.product_name like ? or c.name like ? or b.brand_code like ? or b.brand_name like ?)
                """ + brandFilter + """
                order by p.id desc
                limit ? offset ?
                """;
        if (brandId == null) {
            return jdbcTemplate.query(sql, productRowMapper, like, like, like, like, like, pageSize, offset);
        }
        return jdbcTemplate.query(sql, productRowMapper, like, like, like, like, like, brandId, pageSize, offset);
    }

    public Optional<Product> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("select * from product where id = ?", productRowMapper, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Product> findByCode(String productCode) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from product where product_code = ?",
                    productRowMapper,
                    productCode
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Long insert(Product product) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into product(product_code, product_name, category_id, brand_id, status)
                    values (?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, product.getProductCode());
            ps.setString(2, product.getProductName());
            ps.setLong(3, product.getCategoryId());
            ps.setLong(4, product.getBrandId());
            ps.setInt(5, product.getStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Product product) {
        jdbcTemplate.update(
                """
                update product
                set product_name = ?, category_id = ?, brand_id = ?, status = ?
                where id = ?
                """,
                product.getProductName(),
                product.getCategoryId(),
                product.getBrandId(),
                product.getStatus(),
                product.getId()
        );
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from product where id = ?", id);
    }
}
