# API 请求封装开发记录

## 1. 模块定位

API 请求封装负责前端与后端 `/api` 接口通信，统一处理 baseURL、Token 注入、统一响应解析、401 登录失效和网络错误。页面不直接写后端 URL。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Axios 实例 | `frontend/src/api/request.js` |
| Token 工具 | `frontend/src/utils/token.js` |
| Auth Store | `frontend/src/stores/auth.js` |
| API 模块 | `frontend/src/api/*.js` |
| 环境配置 | `frontend/.env.development`、`frontend/.env.production`、`frontend/vite.config.js` |

## 3. 请求设计

Axios 实例配置：

- `baseURL = import.meta.env.VITE_API_BASE_URL || '/api'`
- `timeout = 12000`

请求拦截：

```text
读取 token
-> token 存在则写入 Authorization: Bearer <token>
-> 发起请求
```

响应拦截：

```text
读取 ApiResponse
-> code = 0 返回 data
-> code = 401 清理本地登录态并跳转 /login
-> 其他 code 抛出 Error(message)
```

## 4. API 模块划分

| 文件 | 对应后端模块 |
|---|---|
| `auth.js` | 认证 |
| `user.js` | 用户 |
| `product.js` | 商品 |
| `stock.js` | 库存 |
| `inbound.js` | 入库 |
| `outbound.js` | 出库 |
| `stockcheck.js` | 盘点 |
| `report.js` | 报表 |
| `system.js` | 系统信息 |

## 5. 环境变量设计

前端通过 Vite 环境变量切换后端地址：

```text
VITE_API_BASE_URL=/api
```

要求：

- 开发环境通过 Vite proxy 转发 `/api`。
- 生产环境通过 Nginx 反向代理 `/api`。
- API 文件中只写相对路径，例如 `/users`、`/stocks`。
- 不在页面中硬编码 IP、域名或端口。

## 6. 权限与安全

- Token 统一从 `utils/token.js` 读取。
- 请求头统一注入 Bearer Token。
- 401 时统一清除 Token 和用户信息。
- 登录接口由 `auth.js` 调用，不在页面中拼接 URL。
- 前端不保存密码，只保存 Token 和必要用户信息。

## 7. 异常处理

- 后端业务错误通过 `Error(message)` 抛给页面。
- 网络异常使用通用错误信息。
- 401 自动跳转登录。
- 页面在 `try/catch` 中显示错误反馈。

## 8. 可维护性说明

- 所有接口都从 `request.js` 发出，便于统一调整超时、Token、响应格式。
- API 按模块拆分，页面只导入所需方法。
- 环境切换不需要改页面和 API 代码。
- 后端统一响应结构变化时，只需调整 `request.js`。

## 9. 测试与验收

- 登录后请求头包含 Bearer Token。
- Token 失效时自动清理本地登录态并跳转登录页。
- 后端业务错误能在页面显示。
- 开发和生产环境均不需要改代码切换 API 地址。
- 搜索、分页、新增、编辑、删除等页面请求均走统一 Axios 实例。
