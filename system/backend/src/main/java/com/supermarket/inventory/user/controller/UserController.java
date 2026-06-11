package com.supermarket.inventory.user.controller;

import com.supermarket.inventory.auth.security.RequireRoles;
import com.supermarket.inventory.common.response.ApiResponse;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.user.dto.RoleCreateRequest;
import com.supermarket.inventory.user.dto.RolePermissionUpdateRequest;
import com.supermarket.inventory.user.dto.UserCreateRequest;
import com.supermarket.inventory.user.dto.UserUpdateRequest;
import com.supermarket.inventory.user.service.UserService;
import com.supermarket.inventory.user.vo.PermissionModuleVO;
import com.supermarket.inventory.user.vo.RoleVO;
import com.supermarket.inventory.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequireRoles("ADMIN")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResult<UserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(userService.list(keyword, page, pageSize));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleVO>> roles() {
        return ApiResponse.success(userService.roles());
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionModuleVO>> permissions() {
        return ApiResponse.success(userService.permissionCatalog());
    }

    @PostMapping("/roles")
    public ApiResponse<RoleVO> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(userService.createRole(request));
    }

    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<RoleVO> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody RolePermissionUpdateRequest request
    ) {
        return ApiResponse.success(userService.updateRolePermissions(id, request));
    }

    @PostMapping
    public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }
}
