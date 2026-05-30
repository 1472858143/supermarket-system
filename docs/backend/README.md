# 后端开发记录总览

## 1. 开发定位

后端实现面向超市库存管理系统的业务服务层，采用 Spring Boot + MyBatis 的分层结构，服务前端后台管理页面。当前后端只覆盖项目书和需求基线确认的库存主线：认证、用户、商品、库存、入库、出库、盘点、报表、系统信息。

后端不扩展采购、供应商、多仓库、POS、财务对账、复杂审批流等项目书外功能，避免毕业设计范围膨胀。

## 2. 后端模块划分

| 模块 | 包路径 | 职责 | 文档 |
|---|---|---|---|
| 认证 | `auth` | 登录、JWT、当前用户、角色校验 | `auth.md` |
| 用户 | `user` | 用户维护、角色分配、密码哈希 | `user.md` |
| 商品 | `product` | 商品基础档案、上下架、初始化库存 | `product.md` |
| 库存 | `stock` | 库存查询、上下限、库存变更、库存日志 | `stock.md` |
| 入库 | `inbound` | 入库记录、增加库存 | `inbound.md` |
| 出库 | `outbound` | 出库记录、扣减库存、库存不足校验 | `outbound.md` |
| 盘点 | `stockcheck` | 盘点记录、差异计算、库存调整 | `stockcheck.md` |
| 报表 | `report` | 库存、入库、出库、预警统计 | `report.md` |
| 系统 | `system` | 轻量化系统信息 | `system.md` |
| 公共配置 | `common`、`config` | 统一响应、异常、分页、拦截器、多环境 | `common-config.md` |

## 3. 分层结构

后端遵循固定调用链：

```text
Controller -> Service -> Mapper -> Database
                 |
                 +-> DomainService
```

分层规则：

- `controller` 只负责接收请求、参数校验、返回统一响应。
- `service` 是业务入口，负责事务、业务编排、权限上下文使用。
- `mapper` 只负责数据库访问，不写业务规则。
- `domain` 放库存等领域规则，避免业务校验散落在多个服务。
- `dto` 接收请求参数，`vo` 返回前端展示数据，`entity` 映射数据库表。
- 入库、出库、盘点不得直接修改库存表，只能调用 `StockService`。

## 4. 认证与权限

当前后端采用 Token 认证与角色级权限控制：

- `POST /api/auth/login` 公开访问。
- 其他 `/api/**` 请求由 `AuthInterceptor` 拦截。
- 请求头使用 `Authorization: Bearer <token>`。
- `JwtTokenService` 解析 Token 后写入 `CurrentUserContext`。
- `@RequireRoles` 用于控制管理员接口。
- Token 用户信息包含用户 ID、用户名、角色编码。

角色边界：

| 角色 | 说明 |
|---|---|
| `ADMIN` | 管理员，可访问用户管理和全部业务操作 |
| `USER` | 普通用户，可访问业务查询和基础入库、出库操作，不能管理用户，不能维护商品、库存上下限和盘点写入 |

## 5. 安全设计

安全要求已落实到公共认证模块：

- 密码不明文保存，新增用户和重置密码统一调用 `PasswordService.encode`。
- 登录校验统一调用 `PasswordService.matches`。
- 当前实现使用 BCrypt。
- `UserVO`、`LoginVO` 等返回对象不包含 `password` 字段。
- 日志和接口响应不输出原始密码或密码哈希。
- 后端接口有基础权限校验，避免只依赖前端隐藏菜单。

## 6. 统一响应与异常

所有接口统一返回 `ApiResponse<T>`：

| 字段 | 说明 |
|---|---|
| `code` | 业务状态码，成功为 `0` |
| `message` | 提示信息 |
| `data` | 返回数据 |

分页接口统一返回 `PageResult<T>`：

| 字段 | 说明 |
|---|---|
| `items` | 当前页数据 |
| `total` | 总记录数 |
| `page` | 当前页码 |
| `pageSize` | 每页数量 |

业务异常统一使用 `BusinessException`，由 `GlobalExceptionHandler` 转换为统一响应。

## 7. 多环境配置

配置文件位于 `backend/src/main/resources`：

```text
application.yml
application-dev.yml
application-prod.yml
```

设计原则：

- `application.yml` 放公共配置。
- `application-dev.yml` 放开发环境配置。
- `application-prod.yml` 放生产环境配置。
- 数据库连接、JWT 密钥等敏感配置通过环境变量注入。
- 业务代码中不硬编码服务器 IP、数据库密码或 Token 密钥。

## 8. 数据一致性原则

库存一致性是后端核心约束：

- `StockService` 是唯一库存变更入口。
- `StockDomainService` 校验库存增加、扣减、盘点调整和上下限。
- 入库、出库、盘点服务通过事务调用库存服务。
- 库存变更写入 `stock_log`，用于追踪来源。
- 库存扣减不足时抛出业务异常，出库记录不会写入。

## 9. 验收重点

- 登录、Token 校验、过期或缺失 Token 返回 401。
- `USER` 访问管理员接口返回 403。
- 新增用户密码为 BCrypt 哈希，接口不返回密码。
- 新增商品后自动初始化库存。
- 入库增加库存并写库存日志。
- 出库库存不足时失败且不写出库记录。
- 盘点只允许 `ADMIN` 新增，并按实际库存调整。
- 报表和系统信息为只读接口。
- 所有分页接口返回统一结构。
