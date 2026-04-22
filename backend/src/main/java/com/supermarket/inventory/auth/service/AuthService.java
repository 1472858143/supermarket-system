package com.supermarket.inventory.auth.service;

import com.supermarket.inventory.auth.dto.LoginRequest;
import com.supermarket.inventory.auth.security.CurrentUser;
import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.auth.vo.LoginVO;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.user.entity.Role;
import com.supermarket.inventory.user.entity.User;
import com.supermarket.inventory.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserMapper userMapper, PasswordService passwordService, JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginVO login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "用户已被禁用");
        }
        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        List<String> roles = userMapper.findRolesByUserId(user.getId())
                .stream()
                .map(Role::getRoleCode)
                .toList();
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), roles);
        String token = jwtTokenService.generateToken(currentUser);
        return new LoginVO(token, user.getId(), user.getUsername(), user.getRealName(), roles);
    }

    public LoginVO current() {
        CurrentUser currentUser = CurrentUserContext.get();
        User user = userMapper.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(401, "用户不存在或已失效"));
        return new LoginVO(null, user.getId(), user.getUsername(), user.getRealName(), currentUser.getRoles());
    }
}
