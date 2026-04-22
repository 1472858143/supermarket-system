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
        product.setCategory(rs.getString("category"));
        product.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        product.setSalePrice(rs.getBigDecimal("sale_price"));
        product.setStatus(rs.getInt("status"));
        product.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return product;
    };

    public ProductMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.queryForObject(
                "select count(*) from product where product_code like ? or product_name like ? or category like ?",
                Long.class,
                like,
                like,
                like
        );
    }

    public List<Product> findPage(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select * from product
                where product_code like ? or product_name like ? or category like ?
                order by id desc
                limit ? offset ?
                """,
                productRowMapper,
                like,
                like,
                like,
                pageSize,
                offset
        );
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
                    insert into product(product_code, product_name, category, purchase_price, sale_price, status)
                    values (?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, product.getProductCode());
            ps.setString(2, product.getProductName());
            ps.setString(3, product.getCategory());
            ps.setBigDecimal(4, product.getPurchasePrice());
            ps.setBigDecimal(5, product.getSalePrice());
            ps.setInt(6, product.getStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Product product) {
        jdbcTemplate.update(
                """
                update product
                set product_name = ?, category = ?, purchase_price = ?, sale_price = ?, status = ?
                where id = ?
                """,
                product.getProductName(),
                product.getCategory(),
                product.getPurchasePrice(),
                product.getSalePrice(),
                product.getStatus(),
                product.getId()
        );
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from product where id = ?", id);
    }
}
