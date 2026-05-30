# 布局与权限前端开发记录

## 1. 页面定位

布局与权限模块负责后台管理框架、侧边栏菜单、顶部用户信息、退出登录、路由权限、菜单权限和按钮权限。该模块是前端权限体验的核心。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Layout | `frontend/src/layout/AdminLayout.vue` |
| Router | `frontend/src/router/index.js` |
| Store | `frontend/src/stores/auth.js` |
| 权限工具 | `frontend/src/utils/permission.js` |
| 按钮组件 | `frontend/src/components/PermissionButton.vue` |
| Token 工具 | `frontend/src/utils/token.js` |

## 3. 布局结构

```text
AdminLayout
├─ Header
│  ├─ 系统名称
│  ├─ 当前用户名
│  └─ 退出登录
├─ Sidebar
│  └─ 角色过滤后的菜单
└─ Content
   └─ RouterView
```

## 4. 路由权限

`router/index.js` 中的业务路由通过 `meta.roles` 声明允许访问的角色：

| 路由 | 角色 |
|---|---|
| `/dashboard` | `ADMIN`、`USER` |
| `/users` | `ADMIN` |
| `/products` | `ADMIN`、`USER` |
| `/stocks` | `ADMIN`、`USER` |
| `/inbounds` | `ADMIN`、`USER` |
| `/outbounds` | `ADMIN`、`USER` |
| `/stockchecks` | `ADMIN`、`USER` |
| `/reports` | `ADMIN`、`USER` |
| `/system` | `ADMIN`、`USER` |

路由守卫逻辑：

```text
public 页面 -> 放行
未登录 -> 跳转 /login
角色不匹配 -> 跳转 /dashboard
角色匹配 -> 放行
```

## 5. 菜单权限

`AdminLayout.vue` 从 `menuRoutes` 获取菜单，使用 `hasAnyRole(authStore.roles, item.meta.roles)` 过滤。用户只看到自己有权限访问的菜单。

菜单权限示例：

- `ADMIN` 可看到用户管理。
- `USER` 看不到用户管理。
- `ADMIN` 和 `USER` 都可看到库存、入库、出库、报表、系统信息。

## 6. 按钮权限

`PermissionButton.vue` 接收 `roles` 参数，根据当前登录用户角色决定是否渲染按钮。

当前使用场景：

- 用户管理新增、编辑、删除：`ADMIN`。
- 商品管理新增、编辑、删除：`ADMIN`。
- 库存上下限维护：`ADMIN`。
- 盘点新增：`ADMIN`。

## 7. 异常处理

- 用户未登录访问页面，统一跳转登录。
- 用户角色不足访问页面，跳转首页。
- 后端返回 403 时，页面应展示请求错误提示。
- 401 由请求封装统一处理，清理登录态。

## 8. 可维护性说明

- 路由权限集中在 `router/index.js`。
- 菜单权限复用路由配置，不单独维护重复菜单。
- 角色判断集中在 `utils/permission.js`。
- 按钮权限通过组件封装，页面只声明允许角色。

## 9. 测试与验收

- 未登录访问 `/stocks` 跳转 `/login`。
- `USER` 登录后侧边栏不显示用户管理。
- `USER` 手动访问 `/users` 被重定向到 `/dashboard`。
- `USER` 不显示商品维护、库存上下限、盘点新增按钮。
- `ADMIN` 显示全部菜单和管理员操作按钮。
