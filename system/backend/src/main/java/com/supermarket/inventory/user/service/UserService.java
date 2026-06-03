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

import java.time.Year;
import java.util.List;

@Service
public class UserService {

    private static final String EMPLOYEE_NO_PREFIX = "EMP";
    private static final int EMPLOYEE_NO_SEQUENCE_LENGTH = 4;
    private static final String CONTACT_PHONE_PATTERN = "^[0-9+\\-\\s]+$";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

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
        String realName = trimToNull(request.getRealName());
        String email = normalizeEmail(request.getEmail());
        String contactPhone = normalizeContactPhone(request.getContactPhone());
        Integer status = normalizeStatus(request.getStatus());
        User user = new User();
        user.setEmployeeNo(nextEmployeeNo());
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordService.encode(request.getPassword()));
        user.setRealName(realName);
        user.setEmail(email);
        user.setContactPhone(contactPhone);
        user.setStatus(status);
        Long userId = userMapper.insertUser(user);
        syncRoles(userId, request.getRoleIds());
        return toVO(userMapper.findById(userId).orElseThrow(() -> new BusinessException("用户创建失败")));
    }

    @Transactional
    public UserVO update(Long id, UserUpdateRequest request) {
        User user = userMapper.findById(id).orElseThrow(() -> new BusinessException(404, "用户不存在"));
        String realName = trimToNull(request.getRealName());
        String email = normalizeEmail(request.getEmail());
        String contactPhone = normalizeContactPhone(request.getContactPhone());
        Integer status = normalizeStatus(request.getStatus());
        user.setRealName(realName);
        user.setEmail(email);
        user.setContactPhone(contactPhone);
        user.setStatus(status);
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
        if (status == null) {
            return 1;
        }
        if (status == 0 || status == 1) {
            return status;
        }
        throw new BusinessException("用户状态不正确");
    }

    private String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (!trimmed.matches(EMAIL_PATTERN)) {
            throw new BusinessException("邮箱格式不正确");
        }
        return trimmed;
    }

    private String normalizeContactPhone(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException("联系方式不能为空");
        }
        if (!trimmed.matches(CONTACT_PHONE_PATTERN)) {
            throw new BusinessException("联系方式只能包含数字、空格、+或-");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nextEmployeeNo() {
        int year = Year.now().getValue();
        String prefix = EMPLOYEE_NO_PREFIX + year;
        String maxEmployeeNo = userMapper.findMaxEmployeeNo(prefix + "%");
        int sequence = 1;
        if (maxEmployeeNo != null) {
            if (!maxEmployeeNo.startsWith(prefix)
                    || maxEmployeeNo.length() != prefix.length() + EMPLOYEE_NO_SEQUENCE_LENGTH) {
                throw new BusinessException("用户工号序号异常");
            }
            try {
                sequence = Integer.parseInt(maxEmployeeNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException exception) {
                throw new BusinessException("用户工号序号异常");
            }
        }
        return prefix + String.format("%0" + EMPLOYEE_NO_SEQUENCE_LENGTH + "d", sequence);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmployeeNo(user.getEmployeeNo());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setContactPhone(user.getContactPhone());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setRoles(userMapper.findRolesByUserId(user.getId()).stream().map(this::toRoleVO).toList());
        return vo;
    }

    private RoleVO toRoleVO(Role role) {
        return new RoleVO(role.getId(), role.getRoleName(), role.getRoleCode());
    }
}
