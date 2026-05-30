# 前端开发记录总览

## 1. 开发定位

前端实现超市库存管理系统的后台管理界面，采用 Vue 3 + Vite + Pinia + Vue Router + Axios。页面以项目书确认的 9 个业务模块为边界，提供登录、权限菜单、表格分页、表单校验、弹窗操作、loading、empty 和操作反馈。

前端只负责展示、输入和调用接口，不计算最终库存，不绕过后端权限，不硬编码后端 IP 或域名。

## 2. 目录结构

```text
frontend/src
├─ api/          # 按模块封装接口
├─ assets/       # 全局样式
├─ components/   # 公共组件
├─ layout/       # 后台布局
├─ router/       # 路由与页面权限
├─ stores/       # 登录态和用户状态
├─ utils/        # token、权限工具
└─ views/        # 页面模块
```

## 3. 页面模块

| 页面 | 路由 | 角色 | 文档 |
|---|---|---|---|
| 登录 | `/login` | 公开 | `auth-login.md` |
| 首页概览 | `/dashboard` | `ADMIN`、`USER` | `dashboard.md` |
| 用户管理 | `/users` | `ADMIN` | `user.md` |
| 商品管理 | `/products` | `ADMIN`、`USER` | `product.md` |
| 库存管理 | `/stocks` | `ADMIN`、`USER` | `stock.md` |
| 入库管理 | `/inbounds` | `ADMIN`、`USER` | `inbound.md` |
| 出库管理 | `/outbounds` | `ADMIN`、`USER` | `outbound.md` |
| 盘点管理 | `/stockchecks` | `ADMIN`、`USER` | `stockcheck.md` |
| 报表统计 | `/reports` | `ADMIN`、`USER` | `report.md` |
| 系统信息 | `/system` | `ADMIN`、`USER` | `system.md` |

## 4. 前端架构

前端采用后台管理布局：

```text
App
└─ AdminLayout
   ├─ Header
   │  ├─ 系统名称
   │  ├─ 当前用户
   │  └─ 退出登录
   ├─ Sidebar
   │  └─ 根据角色过滤 menuRoutes
   └─ Content
      └─ RouterView
```

页面调用链：

```text
View -> api/*.js -> request.js -> 后端 /api
View -> authStore -> token / user / roles
```

## 5. 权限控制

前端实现三层权限控制：

- 路由权限：`router/index.js` 的 `meta.roles`。
- 菜单权限：`AdminLayout.vue` 根据当前角色过滤 `menuRoutes`。
- 按钮权限：`PermissionButton.vue` 根据角色控制操作按钮显示。

注意：前端权限只负责用户体验和初步防护，最终权限仍以后端 `@RequireRoles` 和 Token 拦截器为准。

## 6. 请求与环境

统一请求封装位于 `src/api/request.js`：

- `baseURL` 使用 `import.meta.env.VITE_API_BASE_URL || '/api'`。
- 请求自动注入 `Authorization: Bearer <token>`。
- 响应统一解析 `ApiResponse`。
- `401` 自动清理本地登录态并跳转登录页。

环境变量：

- 开发和生产都通过 `VITE_API_BASE_URL` 配置 API 前缀。
- 页面和 API 模块不硬编码服务器 IP、域名或端口。

## 7. UI/UX 规范

当前页面统一采用：

- Header + Sidebar + Content 管理后台布局。
- 卡片区块承载查询区、表格区、表单弹窗。
- 表格分页。
- 弹窗新增和编辑。
- loading 状态。
- empty 状态。
- 成功和失败反馈。
- 表单必填校验和提交 loading。

## 8. 可维护性说明

- 页面只做展示、输入和调用 API。
- API 文件按业务模块拆分。
- 登录态和角色集中在 Pinia `authStore`。
- Token 存取集中在 `utils/token.js`。
- 权限判断集中在 `utils/permission.js` 和 `PermissionButton.vue`。
- 公共表格、弹窗、工具栏、状态标签复用组件，减少页面重复代码。

## 9. 验收重点

- 未登录访问业务页面跳转 `/login`。
- 登录后按角色显示菜单。
- `USER` 不显示用户管理菜单。
- `USER` 不显示商品新增、编辑、删除按钮。
- `USER` 不显示库存上下限维护按钮。
- `USER` 不显示盘点新增按钮。
- API 统一走 `/api` 前缀和 Axios 封装。
- 空列表、加载中、接口错误都有页面反馈。
