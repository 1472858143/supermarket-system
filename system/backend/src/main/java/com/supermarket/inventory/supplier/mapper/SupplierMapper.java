package com.supermarket.inventory.supplier.mapper;

import com.supermarket.inventory.supplier.entity.Supplier;
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
public class SupplierMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Supplier> supplierRowMapper = (rs, rowNum) -> {
        Supplier supplier = new Supplier();
        supplier.setId(rs.getLong("id"));
        supplier.setSupplierCode(rs.getString("supplier_code"));
        supplier.setSupplierName(rs.getString("supplier_name"));
        supplier.setContactPerson(rs.getString("contact_person"));
        supplier.setContactPhone(rs.getString("contact_phone"));
        supplier.setAddress(rs.getString("address"));
        supplier.setRemark(rs.getString("remark"));
        supplier.setStatus(rs.getInt("status"));
        supplier.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return supplier;
    };

    public SupplierMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from supplier
                where supplier_code like ? or supplier_name like ? or contact_person like ? or contact_phone like ?
                """,
                Long.class,
                like,
                like,
                like,
                like
        );
        return count == null ? 0L : count;
    }

    public List<Supplier> findPage(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select *
                from supplier
                where supplier_code like ? or supplier_name like ? or contact_person like ? or contact_phone like ?
                order by id desc
                limit ? offset ?
                """,
                supplierRowMapper,
                like,
                like,
                like,
                like,
                pageSize,
                offset
        );
    }

    public Optional<Supplier> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from supplier where id = ?",
                    supplierRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsByCode(String supplierCode) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from supplier where supplier_code = ?",
                Long.class,
                supplierCode
        );
        return count != null && count > 0;
    }

    public String findMaxCode(String pattern) {
        return jdbcTemplate.queryForObject(
                "select max(supplier_code) from supplier where supplier_code like ?",
                String.class,
                pattern
        );
    }

    public Long insert(Supplier supplier) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into supplier(
                        supplier_code, supplier_name, contact_person, contact_phone,
                        address, remark, status
                    )
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, supplier.getSupplierCode());
            ps.setString(2, supplier.getSupplierName());
            ps.setString(3, supplier.getContactPerson());
            ps.setString(4, supplier.getContactPhone());
            ps.setString(5, supplier.getAddress());
            ps.setString(6, supplier.getRemark());
            ps.setInt(7, supplier.getStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Supplier supplier) {
        jdbcTemplate.update(
                """
                update supplier
                set supplier_name = ?, contact_person = ?, contact_phone = ?,
                    address = ?, remark = ?, status = ?
                where id = ?
                """,
                supplier.getSupplierName(),
                supplier.getContactPerson(),
                supplier.getContactPhone(),
                supplier.getAddress(),
                supplier.getRemark(),
                supplier.getStatus(),
                supplier.getId()
        );
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from supplier where id = ?", id);
    }
}
