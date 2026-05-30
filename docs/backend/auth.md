# 认证模块开发记录与设计

## 1. 模块定位

认证模块负责用户登录、Token 生成、当前登录用户查询、退出登录占位接口、请求身份解析和角色权限校验。该模块不处理库存、商品、入库、出库等业务数据，只提供安全认证基础能力。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Controller | `backend/src/main/java/com/supermarket/inventory/auth/controller/AuthController.java` |
| Service | `backend/src/main/java/com/supermarket/inventory/auth/service/AuthService.java` |
| 密码服务 | `PasswordService.java`、`BCryptPasswordService.java` |
| Token 服务 | `JwtTokenService.java` |
| 安全上下文 | `CurrentUser.java`、`CurrentUserContext.java` |
| 拦截器 | `AuthInterceptor.java` |
| 权限注解 | `RequireRoles.java` |
| DTO | `LoginRequest.java` |
| VO | `LoginVO.java` |
| 关联 Mapper | `user/mapper/UserMapper.java` |

## 3. 数据表映射

| 表 | 用途 |
|---|---|
| `user` | 根据 `username` 查询用户、读取 BCrypt 密码哈希、校验用户状态 |
| `role` | 查询角色名称和角色编码 |
| `user_role` | 查询用户拥有的角色 |

关键字段：

- `user.username`：登录账号，唯一。
- `user.password`：BCrypt 哈希，不保存明文密码。
- `user.status`：用户启用状态。
- `role.role_code`：权限判断使用的角色编码，例如 `ADMIN`、`USER`。

## 4. 接口设计

| 方法 | 路径 | 权限 | 请求 DTO | 返回 VO | 说明 |
|---|---|---|---|---|---|
| `POST` | `/api/auth/login` | 公开 | `LoginRequest` | `LoginVO` | 用户登录并签发 Token |
| `GET` | `/api/auth/me` | 已登录 | 无 | `LoginVO` | 获取当前登录用户信息 |
| `POST` | `/api/auth/logout` | 已登录 | 无 | `Void` | 前端清理登录态，后端返回成功 |

`LoginRequest` 字段：

- `username`：必填。
- `password`：必填，仅用于登录校验，不返回前端。

`LoginVO` 字段：

- `token`
- `userId`
- `username`
- `realName`
- `roles`

## 5. 业务流程

登录流程：

```text
LoginRequest
-> AuthController.login
-> AuthService.login
-> UserMapper.findByUsername
-> PasswordService.matches
-> UserMapper.findRolesByUserId
-> JwtTokenService.generateToken
-> LoginVO
```

请求认证流程：

```text
HTTP Authorization Header
-> AuthInterceptor
-> JwtTokenService.parseToken
-> CurrentUserContext.set
-> @RequireRoles 检查
-> Controller
-> CurrentUserContext.clear
```

## 6. 权限与安全

- `POST /api/auth/login` 被 `WebConfig` 排除出认证拦截。
- 其他 `/api/**` 请求必须携带 Bearer Token。
- 缺失或非法 Token 返回 401。
- 角色不足返回 403。
- Token 中携带用户 ID、用户名和角色编码，供后端接口做基础权限判断。
- 密码校验只通过 `PasswordService.matches` 完成，不在 Controller 中处理。
- 登录失败只返回统一错误信息，不返回密码相关细节。
- 接口响应不包含 `password`。

## 7. 可维护性说明

- 密码算法被封装在 `PasswordService`，业务服务不依赖 BCrypt 具体实现。
- Token 生成和解析集中在 `JwtTokenService`，便于后续调整过期策略和签名算法。
- 当前用户信息由 `CurrentUserContext` 管理，业务模块无需重复解析 Token。
- 权限规则使用 `@RequireRoles` 注解表达，Controller 代码保持清晰。

## 8. 可扩展性说明

- 若未来新增角色，只需在数据库增加 `role_code` 并在接口或前端配置中引用。
- 若未来升级密码算法，可替换 `PasswordService` 实现，上层调用不变。
- 若未来接入 Spring Security，可保留 Login DTO/VO 和角色模型，替换拦截器实现。
- 若未来需要刷新 Token，可在 `JwtTokenService` 增加刷新逻辑，不影响业务模块。

## 9. 测试与验收

正常场景：

- 使用正确账号密码登录成功，返回 Token 和角色。
- 携带 Token 调用 `/api/auth/me` 返回当前用户。
- 调用 `/api/auth/logout` 返回成功，前端清理本地状态。

异常场景：

- 用户名或密码为空，触发参数校验。
- 用户名不存在或密码错误，登录失败。
- Token 缺失、格式错误或过期，返回 401。

权限场景：

- `USER` 携带合法 Token 访问 `@RequireRoles("ADMIN")` 接口时返回 403。
- `ADMIN` 携带合法 Token 可访问管理员接口。

安全场景：

- 数据库 `user.password` 为 BCrypt 哈希。
- 登录响应、当前用户响应不包含 `password` 字段。
