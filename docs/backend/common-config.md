# 公共配置与基础设施开发记录

## 1. 模块定位

公共配置模块为后端业务模块提供统一响应、分页、异常处理、认证拦截、跨域配置、初始化数据和多环境配置。该模块不承载具体库存业务。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| 启动类 | `backend/src/main/java/com/supermarket/inventory/InventoryApplication.java` |
| 统一响应 | `common/response/ApiResponse.java` |
| 分页响应 | `common/response/PageResult.java` |
| 异常 | `common/exception/BusinessException.java`、`GlobalExceptionHandler.java` |
| 分页工具 | `common/util/PageUtils.java` |
| Web 配置 | `config/WebConfig.java` |
| 初始化 | `config/BootstrapDataInitializer.java` |
| 配置文件 | `backend/src/main/resources/application*.yml` |

## 3. 统一响应设计

接口统一返回：

```text
ApiResponse<T>
├─ code
├─ message
└─ data
```

成功响应：

- `code = 0`
- `message` 为成功提示
- `data` 为业务数据

失败响应：

- 业务异常由 `BusinessException` 携带状态码或默认业务错误码。
- 未登录和无权限由 `AuthInterceptor` 直接返回 401 或 403。
- 参数校验和未知异常由 `GlobalExceptionHandler` 统一处理。

## 4. 分页设计

分页接口统一返回 `PageResult<T>`：

```text
PageResult<T>
├─ items
├─ total
├─ page
└─ pageSize
```

`PageUtils` 负责规范化页码、每页数量和 SQL 偏移量，避免每个模块重复写分页计算。

## 5. 认证拦截与跨域配置

`WebConfig` 注册 `AuthInterceptor`：

- 拦截路径：`/api/**`
- 排除路径：`/api/auth/login`
- 允许方法：`GET`、`POST`、`PUT`、`DELETE`、`OPTIONS`

`AuthInterceptor` 职责：

- 放行预检请求。
- 校验 Bearer Token。
- 解析当前用户并写入 `CurrentUserContext`。
- 根据 `@RequireRoles` 判断角色权限。
- 请求完成后清理当前用户上下文。

## 6. 初始化数据

`BootstrapDataInitializer` 用于初始化毕业设计演示所需基础数据：

- 角色：`ADMIN`、`USER`。
- 默认管理员账号。
- 默认普通用户账号。

初始化密码通过 `PasswordService` 编码后入库，仍满足禁止明文密码要求。

## 7. 多环境配置

配置文件：

```text
application.yml
application-dev.yml
application-prod.yml
```

设计要求：

- 公共配置放在 `application.yml`。
- 开发和生产差异分别放到 `dev`、`prod` Profile。
- 数据库连接、JWT 密钥等敏感配置通过环境变量注入。
- 代码中不写死服务器地址、数据库密码或部署域名。

## 8. 可维护性说明

- 统一响应避免各模块返回结构不一致。
- 统一异常避免 Controller 重复 try-catch。
- 分页工具避免分页参数处理散落。
- 拦截器统一认证权限逻辑，业务 Controller 只声明角色要求。
- 初始化数据与业务服务隔离，便于演示和部署。

## 9. 可扩展性说明

- 后续新增模块只需复用 `ApiResponse`、`PageResult`、`BusinessException`。
- 后续新增角色仍使用 `@RequireRoles` 或扩展权限注解。
- 后续接入 Redis、审计日志、接口限流时，可在公共配置层扩展，不改业务接口契约。
- 后续新增环境可增加新的 Profile 文件，例如 `application-test.yml`。

## 10. 测试与验收

结构验收：

- 所有业务接口响应结构一致。
- 所有分页接口返回 `items`、`total`、`page`、`pageSize`。

安全验收：

- 未登录访问 `/api/**` 非登录接口返回 401。
- 角色不足访问管理员接口返回 403。
- 默认账号密码以 BCrypt 哈希保存。

配置验收：

- 开发环境可通过 dev Profile 启动。
- 生产环境可通过 prod Profile 和环境变量切换。
