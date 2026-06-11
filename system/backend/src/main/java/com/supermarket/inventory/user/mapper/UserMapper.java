package com.supermarket.inventory.user.mapper;

import com.supermarket.inventory.user.entity.Role;
import com.supermarket.inventory.user.entity.User;
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
public class UserMapper {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmployeeNo(rs.getString("employee_no"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRealName(rs.getString("real_name"));
        user.setEmail(rs.getString("email"));
        user.setContactPhone(rs.getString("contact_phone"));
        user.setStatus(rs.getInt("status"));
        user.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        Timestamp lastLoginTime = rs.getTimestamp("last_login_time");
        user.setLastLoginTime(lastLoginTime == null ? null : lastLoginTime.toLocalDateTime());
        return user;
    };

    private final RowMapper<Role> roleRowMapper = (rs, rowNum) -> {
        Role role = new Role();
        role.setId(rs.getLong("id"));
        role.setRoleName(rs.getString("role_name"));
        role.setRoleCode(rs.getString("role_code"));
        role.setRemark(rs.getString("remark"));
        role.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return role;
    };

    public UserMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from user where username = ?",
                    userRowMapper,
                    username
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from user where id = ?",
                    userRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public long countUsers(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                from user
                where employee_no like ? or username like ? or real_name like ?
                    or email like ? or contact_phone like ?
                """,
                Long.class,
                like,
                like,
                like,
                like,
                like
        );
    }

    public List<User> findUsers(String keyword, int offset, int pageSize) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
                """
                select *
                from user
                where employee_no like ? or username like ? or real_name like ?
                    or email like ? or contact_phone like ?
                order by id desc
                limit ? offset ?
                """,
                userRowMapper,
                like,
                like,
                like,
                like,
                like,
                pageSize,
                offset
        );
    }

    public String findMaxEmployeeNo(String pattern) {
        return jdbcTemplate.queryForObject(
                "select max(employee_no) from user where employee_no like ?",
                String.class,
                pattern
        );
    }

    public Long insertUser(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    insert into user(
                        employee_no, username, password, real_name,
                        email, contact_phone, status
                    )
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, user.getEmployeeNo());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRealName());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getContactPhone());
            ps.setInt(7, user.getStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void updateUser(User user) {
        jdbcTemplate.update(
                """
                update user
                set real_name = ?, email = ?, contact_phone = ?, status = ?
                where id = ?
                """,
                user.getRealName(),
                user.getEmail(),
                user.getContactPhone(),
                user.getStatus(),
                user.getId()
        );
    }

    public void updatePassword(Long id, String passwordHash) {
        jdbcTemplate.update("update user set password = ? where id = ?", passwordHash, id);
    }

    public void updateLastLoginTime(Long id) {
        jdbcTemplate.update("update user set last_login_time = now() where id = ?", id);
    }

    public void deleteUser(Long id) {
        jdbcTemplate.update("delete from user where id = ?", id);
    }

    public void deleteUserRoles(Long userId) {
        jdbcTemplate.update("delete from user_role where user_id = ?", userId);
    }

    public void insertUserRole(Long userId, Long roleId) {
        jdbcTemplate.update("insert into user_role(user_id, role_id) values (?, ?)", userId, roleId);
    }

    public List<Role> findRolesByUserId(Long userId) {
        return jdbcTemplate.query(
                """
                select r.* from role r
                inner join user_role ur on ur.role_id = r.id
                where ur.user_id = ?
                order by r.id
                """,
                roleRowMapper,
                userId
        );
    }

    public List<Role> findAllRoles() {
        return jdbcTemplate.query("select * from role order by id", roleRowMapper);
    }

    public Optional<Role> findRoleByCode(String roleCode) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from role where role_code = ?",
                    roleRowMapper,
                    roleCode
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Role> findRoleById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from role where id = ?",
                    roleRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Role> findRoleByName(String roleName) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select * from role where role_name = ?",
                    roleRowMapper,
                    roleName
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public String findMaxRoleCode(String pattern) {
        return jdbcTemplate.queryForObject(
                "select max(role_code) from role where role_code like ?",
                String.class,
                pattern
        );
    }

    public Long insertRole(String roleName, String roleCode, String remark) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into role(role_name, role_code, remark) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, roleName);
            ps.setString(2, roleCode);
            ps.setString(3, remark);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public Long countUsersByRoleId(Long roleId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from user_role where role_id = ?",
                Long.class,
                roleId
        );
    }

    public Long countRolePermissions(Long roleId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from role_permission where role_id = ?",
                Long.class,
                roleId
        );
    }

    public List<String> findPermissionCodesByRoleId(Long roleId) {
        return jdbcTemplate.queryForList(
                "select permission_code from role_permission where role_id = ? order by permission_code",
                String.class,
                roleId
        );
    }

    public List<String> findPermissionCodesByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                """
                select distinct rp.permission_code
                from user_role ur
                inner join role_permission rp on rp.role_id = ur.role_id
                where ur.user_id = ?
                order by rp.permission_code
                """,
                String.class,
                userId
        );
    }

    public void deleteRolePermissions(Long roleId) {
        jdbcTemplate.update("delete from role_permission where role_id = ?", roleId);
    }

    public void insertRolePermission(Long roleId, String permissionCode) {
        jdbcTemplate.update(
                "insert into role_permission(role_id, permission_code) values (?, ?)",
                roleId,
                permissionCode
        );
    }
}
