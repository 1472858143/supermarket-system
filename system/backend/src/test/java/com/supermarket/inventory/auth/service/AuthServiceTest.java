package com.supermarket.inventory.auth.service;

import com.supermarket.inventory.auth.dto.LoginRequest;
import com.supermarket.inventory.auth.security.CurrentUser;
import com.supermarket.inventory.auth.vo.LoginVO;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.user.entity.Role;
import com.supermarket.inventory.user.entity.User;
import com.supermarket.inventory.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordService passwordService;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, passwordService, jwtTokenService);
    }

    @Test
    void login_updatesLastLoginTimeAfterSuccessfulPasswordVerification() {
        User user = user(7L, 1);
        LoginRequest request = request();

        when(userMapper.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordService.matches("secret", "hash")).thenReturn(true);
        when(userMapper.findRolesByUserId(7L)).thenReturn(List.of(role("ADMIN")));
        when(jwtTokenService.generateToken(any(CurrentUser.class))).thenReturn("token");

        LoginVO result = authService.login(request);

        assertThat(result.getToken()).isEqualTo("token");
        assertThat(result.getUserId()).isEqualTo(7L);
        verify(userMapper).updateLastLoginTime(7L);
    }

    @Test
    void login_doesNotUpdateLastLoginTimeWhenPasswordInvalid() {
        User user = user(7L, 1);
        LoginRequest request = request();

        when(userMapper.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordService.matches("secret", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");

        verify(userMapper, never()).updateLastLoginTime(7L);
    }

    @Test
    void login_doesNotUpdateLastLoginTimeWhenUserDisabled() {
        LoginRequest request = request();
        when(userMapper.findByUsername("alice")).thenReturn(Optional.of(user(7L, 0)));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户已被禁用");

        verify(userMapper, never()).updateLastLoginTime(7L);
    }

    private LoginRequest request() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");
        return request;
    }

    private User user(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setEmployeeNo("EMP20260001");
        user.setUsername("alice");
        user.setPassword("hash");
        user.setRealName("Alice");
        user.setContactPhone("13800000000");
        user.setStatus(status);
        user.setCreateTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        return user;
    }

    private Role role(String roleCode) {
        Role role = new Role();
        role.setId(1L);
        role.setRoleName(roleCode);
        role.setRoleCode(roleCode);
        role.setCreateTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        return role;
    }
}
