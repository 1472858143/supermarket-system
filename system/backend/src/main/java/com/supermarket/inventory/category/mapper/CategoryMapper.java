package com.supermarket.inventory.category.mapper;

import com.supermarket.inventory.category.entity.Category;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class CategoryMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Category> categoryRowMapper = (rs, rowNum) -> {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        long parentId = rs.getLong("parent_id");
        category.setParentId(rs.wasNull() ? null : parentId);
        category.setSortOrder(rs.getInt("sort_order"));
        category.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return category;
    };

    public CategoryMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Category> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM category ORDER BY sort_order ASC, id ASC",
                categoryRowMapper
        );
    }

    public Optional<Category> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM category WHERE id = ?",
                    categoryRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByParentIdAndName(Long parentId, String name) {
        String sql = parentId == null
                ? "SELECT COUNT(*) FROM category WHERE parent_id IS NULL AND name = ?"
                : "SELECT COUNT(*) FROM category WHERE parent_id = ? AND name = ?";
        Long count = parentId == null
                ? jdbcTemplate.queryForObject(sql, Long.class, name)
                : jdbcTemplate.queryForObject(sql, Long.class, parentId, name);
        return count != null && count > 0;
    }

    public boolean existsByParentIdAndNameExcluding(Long parentId, String name, Long excludeId) {
        String sql = parentId == null
                ? "SELECT COUNT(*) FROM category WHERE parent_id IS NULL AND name = ? AND id != ?"
                : "SELECT COUNT(*) FROM category WHERE parent_id = ? AND name = ? AND id != ?";
        Long count = parentId == null
                ? jdbcTemplate.queryForObject(sql, Long.class, name, excludeId)
                : jdbcTemplate.queryForObject(sql, Long.class, parentId, name, excludeId);
        return count != null && count > 0;
    }

    public int maxSortOrder(Long parentId) {
        String sql = parentId == null
                ? "SELECT COALESCE(MAX(sort_order), 0) FROM category WHERE parent_id IS NULL"
                : "SELECT COALESCE(MAX(sort_order), 0) FROM category WHERE parent_id = ?";
        Integer result = parentId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class)
                : jdbcTemplate.queryForObject(sql, Integer.class, parentId);
        return result != null ? result : 0;
    }

    public long countChildren(Long parentId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category WHERE parent_id = ?",
                Long.class,
                parentId
        );
        return count != null ? count : 0;
    }

    public long countProductsByCategory(Long categoryId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE category_id = ?",
                Long.class,
                categoryId
        );
        return count != null ? count : 0;
    }

    public Long insert(Category category) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO category(name, parent_id, sort_order) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, category.getName());
            if (category.getParentId() == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, category.getParentId());
            }
            ps.setInt(3, category.getSortOrder());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Category category) {
        jdbcTemplate.update(
                "UPDATE category SET name = ?, sort_order = ? WHERE id = ?",
                category.getName(),
                category.getSortOrder(),
                category.getId()
        );
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM category WHERE id = ?", id);
    }
}
