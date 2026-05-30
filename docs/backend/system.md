# 系统信息模块开发记录与设计

## 1. 模块定位

系统信息模块用于向前端展示轻量化系统运行信息和项目说明入口。该模块不承担配置中心、审计中心、权限中心或运维监控平台职责。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Controller | `backend/src/main/java/com/supermarket/inventory/system/controller/SystemController.java` |
| Service | `backend/src/main/java/com/supermarket/inventory/system/service/SystemService.java` |

## 3. 数据表映射

当前系统信息模块不依赖业务数据表，返回内容由服务层组装。数据库中没有 `system_config`、`operation_log` 等表，因此第一版不设计系统配置修改或系统级审计功能。

## 4. 接口设计

| 方法 | 路径 | 权限 | 请求 | 返回 | 说明 |
|---|---|---|---|---|---|
| `GET` | `/api/system/info` | 已登录 | 无 | `Map<String,Object>` | 获取系统基础信息 |

## 5. 业务流程

```text
SystemController.info
-> SystemService.info
-> 组装系统信息
-> ApiResponse
```

## 6. 权限与安全

- `ADMIN` 和 `USER` 均可查看系统信息。
- 模块只读，不提供修改配置的接口。
- 返回内容不包含数据库密码、JWT 密钥、服务器敏感路径等信息。
- 配置变更应通过 Spring Profile 和环境变量完成，不通过页面直接修改。

## 7. 可维护性说明

- Controller 只负责接口入口。
- Service 统一维护系统信息字段。
- 不引入多余数据库表，保持与项目书范围一致。

## 8. 可扩展性说明

- 后续如需展示版本号、构建时间、运行环境，可扩展 `SystemService.info` 返回字段。
- 若未来需要配置中心，应新增独立模块和数据表，不混入当前轻量系统信息接口。
- 若未来需要审计中心，应新增操作日志表和审计服务，不复用系统信息模块。

## 9. 测试与验收

正常场景：

- 登录用户调用 `/api/system/info` 返回系统基础信息。
- 前端系统信息页可正常展示返回字段。

异常场景：

- 未登录访问返回 401。

只读验收：

- 调用接口不修改任何业务数据。
- 响应中不包含密钥、密码或数据库连接信息。
