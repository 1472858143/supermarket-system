package com.supermarket.inventory.config;

import com.supermarket.inventory.auth.service.PasswordService;
import com.supermarket.inventory.user.entity.User;
import com.supermarket.inventory.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Component
public class BootstrapDataInitializer implements CommandLineRunner {

    private static final String EMPLOYEE_NO_PREFIX = "EMP";
    private static final int EMPLOYEE_NO_SEQUENCE_LENGTH = 4;
    private static final String DEFAULT_CONTACT_PHONE = "00000000000";

    private final UserMapper userMapper;
    private final PasswordService passwordService;
    private final boolean enabled;
    private final String adminUsername;
    private final String adminPassword;
    private final String userUsername;
    private final String userPassword;

    public BootstrapDataInitializer(
            UserMapper userMapper,
            PasswordService passwordService,
            @Value("${app.bootstrap.enabled}") boolean enabled,
            @Value("${app.bootstrap.admin-username}") String adminUsername,
            @Value("${app.bootstrap.admin-password}") String adminPassword,
            @Value("${app.bootstrap.user-username}") String userUsername,
            @Value("${app.bootstrap.user-password}") String userPassword
    ) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.enabled = enabled;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.userUsername = userUsername;
        this.userPassword = userPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        // 初始化只补齐缺失的基础角色和演示账号，不覆盖用户已修改的数据。
        Long adminRoleId = userMapper.findRoleByCode("ADMIN")
                .map(role -> role.getId())
                .orElseGet(() -> userMapper.insertRole("管理员", "ADMIN", "系统管理员"));
        Long userRoleId = userMapper.findRoleByCode("USER")
                .map(role -> role.getId())
                .orElseGet(() -> userMapper.insertRole("普通用户", "USER", "普通业务用户"));

        ensureUser(adminUsername, adminPassword, "系统管理员", adminRoleId);
        ensureUser(userUsername, userPassword, "普通用户", userRoleId);
    }

    private void ensureUser(String username, String password, String realName, Long roleId) {
        if (userMapper.findByUsername(username).isPresent()) {
            return;
        }
        User user = new User();
        user.setEmployeeNo(nextEmployeeNo());
        user.setUsername(username);
        user.setPassword(passwordService.encode(password));
        user.setRealName(realName);
        user.setContactPhone(DEFAULT_CONTACT_PHONE);
        user.setStatus(1);
        Long userId = userMapper.insertUser(user);
        userMapper.insertUserRole(userId, roleId);
    }

    private String nextEmployeeNo() {
        int year = Year.now().getValue();
        String prefix = EMPLOYEE_NO_PREFIX + year;
        String maxEmployeeNo = userMapper.findMaxEmployeeNo(prefix + "%");
        int sequence = 1;
        if (maxEmployeeNo != null
                && maxEmployeeNo.startsWith(prefix)
                && maxEmployeeNo.length() == prefix.length() + EMPLOYEE_NO_SEQUENCE_LENGTH) {
            sequence = Integer.parseInt(maxEmployeeNo.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%0" + EMPLOYEE_NO_SEQUENCE_LENGTH + "d", sequence);
    }
}
