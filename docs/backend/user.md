# 用户模块开发记录与设计

## 1. 模块定位

用户模块负责后台用户的分页查询、新增、修改、删除和角色分配。该模块属于系统管理能力，只允许管理员访问，不参与商品、库存、入库、出库、盘点等业务流程。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Controller | `backend/src/main/java/com/supermarket/inventory/user/controller/UserController.java` |
| Service | `backend/src/main/java/com/supermarket/inventory/user/service/UserService.java` |
| Mapper | `backend/src/main/java/com/supermarket/inventory/user/mapper/UserMapper.java` |
| Entity | `User.java`、`Role.java` |
| DTO | `UserCreateRequest.java`、`UserUpdateRequest.java` |
| VO | `UserVO.java`、`RoleVO.java` |
| 依赖服务 | `auth/service/PasswordService.java` |

## 3. 数据表映射

| 表 | 用途 |
|---|---|
| `user` | 保存用户账号、密码哈希、姓名、状态、创建时间 |
| `role` | 保存角色名称和角色编码 |
| `user_role` | 保存用户与角色的多对多关系 |

关键字段：

- `user.username`：唯一账号。
- `user.password`：BCrypt 哈希，禁止明文。
- `user.status`：启用或禁用。
- `role.role_code`：权限判断依据。

## 4. 接口设计

所有接口类级标注 `@RequireRoles("ADMIN")`。

| 方法 | 路径 | 权限 | 请求 | 返回 | 说明 |
|---|---|---|---|---|---|
| `GET` | `/api/users` | `ADMIN` | `keyword`、`page`、`pageSize` | `PageResult<UserVO>` | 用户分页查询 |
| `GET` | `/api/users/roles` | `ADMIN` | 无 | `List<RoleVO>` | 查询角色选项 |
| `POST` | `/api/users` | `ADMIN` | `UserCreateRequest` | `UserVO` | 新增用户 |
| `PUT` | `/api/users/{id}` | `ADMIN` | `UserUpdateRequest` | `UserVO` | 修改用户资料、状态、角色、可选重置密码 |
| `DELETE` | `/api/users/{id}` | `ADMIN` | 路径参数 `id` | `Void` | 删除用户 |

## 5. 业务流程

新增用户：

```text
UserController.create
-> UserService.create
-> 校验 username 唯一
-> PasswordService.encode
-> UserMapper.insertUser
-> UserMapper.insertUserRole
-> 返回 UserVO
```

修改用户：

```text
UserController.update
-> UserService.update
-> 查询用户是否存在
-> 更新姓名和状态
-> newPassword 非空时重新哈希
-> 删除旧角色关系
-> 写入新角色关系
-> 返回 UserVO
```

删除用户：

```text
UserController.delete
-> UserService.delete
-> 查询用户是否存在
-> 删除 user_role
-> 删除 user
```

## 6. 权限与安全

- 只有 `ADMIN` 可以访问用户模块。
- `USER` 角色无法访问 `/api/users/**`。
- 新增和重置密码统一调用 `PasswordService.encode`。
- `UserVO` 不包含 `password`。
- 查询列表不返回密码哈希。
- Controller 不打印请求体，避免密码泄露。

## 7. 可维护性说明

- Controller 不写密码处理、角色同步或分页逻辑。
- `UserService` 统一组织用户业务流程。
- `UserMapper` 只负责用户、角色和用户角色关联表访问。
- 用户入参 DTO 和出参 VO 分离，避免敏感字段泄露。

## 8. 可扩展性说明

- 新增角色只需要写入 `role` 和 `user_role`，不需要修改用户表结构。
- 若未来增加手机号、邮箱等字段，可扩展 DTO/VO 并保持旧字段兼容。
- 若未来增加更细粒度权限，可在角色基础上增加权限表，用户模块仍负责角色分配。

## 9. 测试与验收

正常场景：

- `ADMIN` 查询用户列表和角色列表成功。
- `ADMIN` 新增用户后数据库密码为 BCrypt 哈希。
- `ADMIN` 修改用户状态、角色和重置密码成功。
- `ADMIN` 删除用户后用户角色关系同步删除。

异常场景：

- 用户名重复时新增失败。
- 角色列表为空或未传角色时触发校验。
- 修改或删除不存在用户返回业务异常。

权限场景：

- `USER` 访问任意 `/api/users/**` 接口返回 403。

安全场景：

- 所有用户接口响应不包含 `password`。
