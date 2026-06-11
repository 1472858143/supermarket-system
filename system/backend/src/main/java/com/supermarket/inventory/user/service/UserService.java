package com.supermarket.inventory.user.service;

import com.supermarket.inventory.auth.service.PasswordService;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.user.dto.RoleCreateRequest;
import com.supermarket.inventory.user.dto.RolePermissionUpdateRequest;
import com.supermarket.inventory.user.dto.UserCreateRequest;
import com.supermarket.inventory.user.dto.UserUpdateRequest;
import com.supermarket.inventory.user.entity.Role;
import com.supermarket.inventory.user.entity.User;
import com.supermarket.inventory.user.mapper.UserMapper;
import com.supermarket.inventory.user.vo.PermissionModuleVO;
import com.supermarket.inventory.user.vo.PermissionVO;
import com.supermarket.inventory.user.vo.RoleVO;
import com.supermarket.inventory.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final String EMPLOYEE_NO_PREFIX = "EMP";
    private static final int EMPLOYEE_NO_SEQUENCE_LENGTH = 4;
    private static final String CUSTOM_ROLE_CODE_PREFIX = "CUSTOM_";
    private static final int CUSTOM_ROLE_CODE_SEQUENCE_LENGTH = 4;
    private static final String CONTACT_PHONE_PATTERN = "^[0-9+\\-\\s]+$";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final List<PermissionModuleVO> PERMISSION_CATALOG = List.of(
            new PermissionModuleVO("dashboard", "首页概览", List.of(
                    new PermissionVO("dashboard:view", "查看首页", "查看经营概览与待办摘要")
            )),
            new PermissionModuleVO("product", "商品资料", List.of(
                    new PermissionVO("product:view", "查看商品", "查看商品档案与 SKU 信息"),
                    new PermissionVO("product:create", "新增商品", "创建商品和 SKU"),
                    new PermissionVO("product:update", "编辑商品", "维护商品基础信息"),
                    new PermissionVO("product:delete", "删除商品", "删除商品档案"),
                    new PermissionVO("brand:view", "查看品牌", "查看品牌档案"),
                    new PermissionVO("brand:manage", "维护品牌", "新增、编辑和停用品牌"),
                    new PermissionVO("category:view", "查看分类", "查看商品分类"),
                    new PermissionVO("category:manage", "维护分类", "新增、编辑和排序分类"),
                    new PermissionVO("sku:view", "查看规格", "查看 SKU 与单位换算"),
                    new PermissionVO("sku:manage", "维护规格", "维护 SKU 与单位换算")
            )),
            new PermissionModuleVO("supplier", "供应商", List.of(
                    new PermissionVO("supplier:view", "查看供应商", "查看供应商档案"),
                    new PermissionVO("supplier:create", "新增供应商", "创建供应商档案"),
                    new PermissionVO("supplier:update", "编辑供应商", "维护供应商档案"),
                    new PermissionVO("supplier:delete", "删除供应商", "删除供应商档案"),
                    new PermissionVO("supplier-sku:manage", "供货绑定", "维护供应商 SKU 供货关系")
            )),
            new PermissionModuleVO("inventory", "库存中心", List.of(
                    new PermissionVO("inventory:view", "查看库存中心", "查看库存汇总与批次流水"),
                    new PermissionVO("stock:view", "查看库存", "查看库存台账"),
                    new PermissionVO("stock:update", "调整库存", "维护库存上下限与数量"),
                    new PermissionVO("stockcheck:view", "查看盘点", "查看盘点单"),
                    new PermissionVO("stockcheck:create", "新增盘点", "创建盘点单"),
                    new PermissionVO("stockcheck:complete", "完成盘点", "提交盘点结果")
            )),
            new PermissionModuleVO("purchase", "采购出入库", List.of(
                    new PermissionVO("purchase:view", "查看采购", "查看采购入库单"),
                    new PermissionVO("purchase:create", "新增采购", "创建采购入库单"),
                    new PermissionVO("purchase:approve", "审批采购", "审批采购入库单"),
                    new PermissionVO("purchase:receive", "办理入库", "登记实际入库批次"),
                    new PermissionVO("outbound:view", "查看出库", "查看出库记录"),
                    new PermissionVO("outbound:create", "新增出库", "创建出库单")
            )),
            new PermissionModuleVO("report", "报表与系统", List.of(
                    new PermissionVO("report:view", "查看报表", "查看库存、采购和出库报表"),
                    new PermissionVO("system:view", "查看系统信息", "查看系统运行信息")
            )),
            new PermissionModuleVO("user", "用户与权限", List.of(
                    new PermissionVO("user:view", "查看用户", "查看系统用户列表"),
                    new PermissionVO("user:create", "新增用户", "创建系统用户"),
                    new PermissionVO("user:update", "编辑用户", "维护用户状态和角色"),
                    new PermissionVO("user:delete", "删除用户", "删除系统用户"),
                    new PermissionVO("role:manage", "维护角色", "创建角色并配置权限")
            ))
    );
    private static final Set<String> ALL_PERMISSION_CODES = collectPermissionCodes(PERMISSION_CATALOG);
    private static final List<String> DEFAULT_USER_PERMISSION_CODES = List.of(
            "dashboard:view",
            "product:view",
            "brand:view",
            "category:view",
            "sku:view",
            "supplier:view",
            "inventory:view",
            "stock:view",
            "stockcheck:view",
            "purchase:view",
            "outbound:view",
            "report:view",
            "system:view"
    );

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

    public List<PermissionModuleVO> permissionCatalog() {
        return PERMISSION_CATALOG;
    }

    @Transactional
    public RoleVO createRole(RoleCreateRequest request) {
        String roleName = trimToNull(request.getRoleName());
        if (roleName == null) {
            throw new BusinessException("角色名称不能为空");
        }
        userMapper.findRoleByName(roleName).ifPresent(role -> {
            throw new BusinessException("角色名称已存在");
        });
        List<String> permissionCodes = normalizePermissionCodes(request.getPermissionCodes());
        if (permissionCodes.isEmpty()) {
            throw new BusinessException("角色权限不能为空");
        }
        Long roleId = userMapper.insertRole(roleName, nextCustomRoleCode(), trimToNull(request.getRemark()));
        syncRolePermissions(roleId, permissionCodes);
        return toRoleVO(userMapper.findRoleById(roleId).orElseThrow(() -> new BusinessException("角色创建失败")));
    }

    @Transactional
    public RoleVO updateRolePermissions(Long id, RolePermissionUpdateRequest request) {
        Role role = userMapper.findRoleById(id).orElseThrow(() -> new BusinessException(404, "角色不存在"));
        if ("ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException("系统管理员角色权限不可修改");
        }
        List<String> permissionCodes = normalizePermissionCodes(request.getPermissionCodes());
        if (permissionCodes.isEmpty()) {
            throw new BusinessException("角色权限不能为空");
        }
        syncRolePermissions(id, permissionCodes);
        return toRoleVO(role);
    }

    @Transactional
    public void ensureDefaultRolePermissions(Long adminRoleId, Long userRoleId) {
        if (adminRoleId != null && userMapper.countRolePermissions(adminRoleId) == 0) {
            syncRolePermissions(adminRoleId, List.copyOf(ALL_PERMISSION_CODES));
        }
        if (userRoleId != null && userMapper.countRolePermissions(userRoleId) == 0) {
            syncRolePermissions(userRoleId, DEFAULT_USER_PERMISSION_CODES);
        }
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

    private void syncRolePermissions(Long roleId, List<String> permissionCodes) {
        userMapper.deleteRolePermissions(roleId);
        for (String permissionCode : permissionCodes) {
            userMapper.insertRolePermission(roleId, permissionCode);
        }
    }

    private List<String> normalizePermissionCodes(List<String> values) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String code = trimToNull(value);
            if (code == null) {
                continue;
            }
            if (!ALL_PERMISSION_CODES.contains(code)) {
                throw new BusinessException("权限码不存在：" + code);
            }
            normalized.add(code);
        }
        return List.copyOf(normalized);
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

    private String nextCustomRoleCode() {
        String maxRoleCode = userMapper.findMaxRoleCode(CUSTOM_ROLE_CODE_PREFIX + "%");
        int sequence = 1;
        if (maxRoleCode != null) {
            if (!maxRoleCode.startsWith(CUSTOM_ROLE_CODE_PREFIX)
                    || maxRoleCode.length() != CUSTOM_ROLE_CODE_PREFIX.length() + CUSTOM_ROLE_CODE_SEQUENCE_LENGTH) {
                throw new BusinessException("角色编码序号异常");
            }
            try {
                sequence = Integer.parseInt(maxRoleCode.substring(CUSTOM_ROLE_CODE_PREFIX.length())) + 1;
            } catch (NumberFormatException exception) {
                throw new BusinessException("角色编码序号异常");
            }
        }
        return CUSTOM_ROLE_CODE_PREFIX + String.format("%0" + CUSTOM_ROLE_CODE_SEQUENCE_LENGTH + "d", sequence);
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
        RoleVO vo = new RoleVO(role.getId(), role.getRoleName(), role.getRoleCode());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        vo.setUserCount(userMapper.countUsersByRoleId(role.getId()));
        List<String> permissionCodes = userMapper.findPermissionCodesByRoleId(role.getId());
        vo.setPermissionCodes(permissionCodes == null ? List.of() : permissionCodes);
        return vo;
    }

    private static Set<String> collectPermissionCodes(List<PermissionModuleVO> modules) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (PermissionModuleVO module : modules) {
            for (PermissionVO permission : module.getPermissions()) {
                codes.add(permission.getCode());
            }
        }
        return Set.copyOf(codes);
    }
}
