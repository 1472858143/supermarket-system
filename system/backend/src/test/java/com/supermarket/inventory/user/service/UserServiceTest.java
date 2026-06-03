package com.supermarket.inventory.user.service;

import com.supermarket.inventory.auth.service.PasswordService;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.user.dto.UserCreateRequest;
import com.supermarket.inventory.user.dto.UserUpdateRequest;
import com.supermarket.inventory.user.entity.Role;
import com.supermarket.inventory.user.entity.User;
import com.supermarket.inventory.user.mapper.UserMapper;
import com.supermarket.inventory.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordService passwordService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, passwordService);
    }

    @Test
    void create_generatesNextEmployeeNoAndSavesContactFields() {
        int year = Year.now().getValue();
        UserCreateRequest request = createRequest();
        String employeeNo = "EMP" + year + "0010";

        when(userMapper.findByUsername("alice")).thenReturn(Optional.empty());
        when(userMapper.findMaxEmployeeNo("EMP" + year + "%")).thenReturn("EMP" + year + "0009");
        when(passwordService.encode("secret")).thenReturn("hash");
        when(userMapper.insertUser(any(User.class))).thenReturn(7L);
        when(userMapper.findById(7L)).thenReturn(Optional.of(user(7L, employeeNo, "alice", "Alice")));
        when(userMapper.findRolesByUserId(7L)).thenReturn(List.of(role(1L, "ADMIN")));

        UserVO result = userService.create(request);

        assertThat(result.getEmployeeNo()).isEqualTo(employeeNo);
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getContactPhone()).isEqualTo("+86 13800000000");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insertUser(userCaptor.capture());
        User inserted = userCaptor.getValue();
        assertThat(inserted.getEmployeeNo()).isEqualTo(employeeNo);
        assertThat(inserted.getUsername()).isEqualTo("alice");
        assertThat(inserted.getPassword()).isEqualTo("hash");
        assertThat(inserted.getRealName()).isEqualTo("Alice");
        assertThat(inserted.getEmail()).isEqualTo("alice@example.com");
        assertThat(inserted.getContactPhone()).isEqualTo("+86 13800000000");
        assertThat(inserted.getStatus()).isEqualTo(1);
    }

    @Test
    void create_generatesFirstEmployeeNoWhenCurrentYearHasNoUsers() {
        int year = Year.now().getValue();
        UserCreateRequest request = createRequest();
        String employeeNo = "EMP" + year + "0001";

        when(userMapper.findByUsername("alice")).thenReturn(Optional.empty());
        when(userMapper.findMaxEmployeeNo("EMP" + year + "%")).thenReturn(null);
        when(passwordService.encode("secret")).thenReturn("hash");
        when(userMapper.insertUser(any(User.class))).thenReturn(7L);
        when(userMapper.findById(7L)).thenReturn(Optional.of(user(7L, employeeNo, "alice", "Alice")));
        when(userMapper.findRolesByUserId(7L)).thenReturn(List.of(role(1L, "ADMIN")));

        userService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insertUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmployeeNo()).isEqualTo(employeeNo);
    }

    @Test
    void create_rejectsBlankContactPhone() {
        UserCreateRequest request = createRequest();
        request.setContactPhone(" ");
        when(userMapper.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("联系方式不能为空");

        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    void create_rejectsInvalidContactPhone() {
        UserCreateRequest request = createRequest();
        request.setContactPhone("电话123");
        when(userMapper.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("联系方式只能包含数字、空格、+或-");

        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    void create_rejectsInvalidEmail() {
        UserCreateRequest request = createRequest();
        request.setEmail("bad-email");
        when(userMapper.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱格式不正确");

        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    void create_rejectsInvalidStatus() {
        UserCreateRequest request = createRequest();
        request.setStatus(2);
        when(userMapper.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户状态不正确");

        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    void update_doesNotChangeEmployeeNoOrUsername() {
        User existing = user(7L, "EMP20260001", "alice", "Alice");
        User updated = user(7L, "EMP20260001", "alice", "Alice New");
        UserUpdateRequest request = updateRequest();

        when(userMapper.findById(7L)).thenReturn(Optional.of(existing), Optional.of(updated));
        when(userMapper.findRolesByUserId(7L)).thenReturn(List.of(role(1L, "ADMIN")));

        UserVO result = userService.update(7L, request);

        assertThat(result.getEmployeeNo()).isEqualTo("EMP20260001");
        assertThat(result.getUsername()).isEqualTo("alice");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateUser(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmployeeNo()).isEqualTo("EMP20260001");
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getRealName()).isEqualTo("Alice New");
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getContactPhone()).isEqualTo("13800000000");
        assertThat(saved.getStatus()).isEqualTo(0);
    }

    private UserCreateRequest createRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("alice");
        request.setPassword("secret");
        request.setRealName(" Alice ");
        request.setEmail(" alice@example.com ");
        request.setContactPhone(" +86 13800000000 ");
        request.setStatus(1);
        request.setRoleIds(List.of(1L));
        return request;
    }

    private UserUpdateRequest updateRequest() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setRealName(" Alice New ");
        request.setEmail(" ");
        request.setContactPhone(" 13800000000 ");
        request.setStatus(0);
        request.setRoleIds(List.of(1L));
        return request;
    }

    private User user(Long id, String employeeNo, String username, String realName) {
        User user = new User();
        user.setId(id);
        user.setEmployeeNo(employeeNo);
        user.setUsername(username);
        user.setPassword("hash");
        user.setRealName(realName);
        user.setEmail("alice@example.com");
        user.setContactPhone("+86 13800000000");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        return user;
    }

    private Role role(Long id, String roleCode) {
        Role role = new Role();
        role.setId(id);
        role.setRoleName(roleCode);
        role.setRoleCode(roleCode);
        role.setCreateTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        return role;
    }
}
