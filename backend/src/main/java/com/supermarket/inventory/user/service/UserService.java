package com.supermarket.inventory.user.service;

import com.supermarket.inventory.auth.service.PasswordService;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.user.dto.UserCreateRequest;
import com.supermarket.inventory.user.dto.UserUpdateRequest;
import com.supermarket.inventory.user.entity.Role;
import com.supermarket.inventory.user.entity.User;
import com.supermarket.inventory.user.mapper.UserMapper;
import com.supermarket.inventory.user.vo.RoleVO;
import com.supermarket.inventory.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordService passwordService;

    public UserService(UserMapper userMapper, PasswordService passwordService) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
    }

    public PageResult<UserVO> list(String keyword, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        long total = userMapper.countUsers(keyword);
        List<UserVO> items = userMapper.findUsers(
                keyword,
                PageUtils.offset(normalizedPage, normalizedPageSize),
                normalizedPageSize
        ).stream().map(this::toVO).toList();
        return new PageResult<>(items, total, normalizedPage, normalizedPageSize);
    }

    public List<RoleVO> roles() {
        return userMapper.findAllRoles().stream().map(this::toRoleVO).toList();
    }

    @Transactional
    public UserVO create(UserCreateRequest request) {
        userMapper.findByUsername(request.getUsername()).ifPresent(user -> {
            throw new BusinessException("用户名已存在");
        });
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordService.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setStatus(normalizeStatus(request.getStatus()));
        Long userId = userMapper.insertUser(user);
        syncRoles(userId, request.getRoleIds());
        return toVO(userMapper.findById(userId).orElseThrow(() -> new BusinessException("用户创建失败")));
    }

    @Transactional
    public UserVO update(Long id, UserUpdateRequest request) {
        User user = userMapper.findById(id).orElseThrow(() -> new BusinessException(404, "用户不存在"));
        user.setRealName(request.getRealName());
        user.setStatus(normalizeStatus(request.getStatus()));
        userMapper.updateUser(user);
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            userMapper.updatePassword(id, passwordService.encode(request.getNewPassword()));
        }
        syncRoles(id, request.getRoleIds());
        return toVO(userMapper.findById(id).orElseThrow(() -> new BusinessException(404, "用户不存在")));
    }

    @Transactional
    public void delete(Long id) {
        userMapper.findById(id).orElseThrow(() -> new BusinessException(404, "用户不存在"));
        userMapper.deleteUserRoles(id);
        userMapper.deleteUser(id);
    }

    private void syncRoles(Long userId, List<Long> roleIds) {
        userMapper.deleteUserRoles(userId);
        for (Long roleId : roleIds) {
            userMapper.insertUserRole(userId, roleId);
        }
    }

    private Integer normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setRoles(userMapper.findRolesByUserId(user.getId()).stream().map(this::toRoleVO).toList());
        return vo;
    }

    private RoleVO toRoleVO(Role role) {
        return new RoleVO(role.getId(), role.getRoleName(), role.getRoleCode());
    }
}
