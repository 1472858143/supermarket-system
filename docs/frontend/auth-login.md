# 登录与认证前端开发记录

## 1. 页面定位

登录页负责用户输入账号密码、调用后端登录接口、保存 Token 和用户信息，并在登录后进入后台管理布局。登录页对应后端 `auth` 模块。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| View | `frontend/src/views/login/LoginView.vue` |
| API | `frontend/src/api/auth.js` |
| Store | `frontend/src/stores/auth.js` |
| Token 工具 | `frontend/src/utils/token.js` |
| 请求封装 | `frontend/src/api/request.js` |
| 路由守卫 | `frontend/src/router/index.js` |

## 3. 页面功能

- 输入用户名和密码。
- 调用登录接口。
- 登录成功后保存 Token 和用户信息。
- 根据 `redirect` 参数跳转回原访问页面，否则进入 `/dashboard`。
- 登录失败展示错误信息。
- 支持退出后重新登录。

## 4. 权限控制

- `/login` 设置 `meta.public = true`，不要求 Token。
- 已登录状态由 `authStore.isLoggedIn` 判断。
- 业务页面未登录时路由守卫跳转 `/login`。

## 5. 接口调用

| 前端方法 | 后端接口 | 说明 |
|---|---|---|
| `login(data)` | `POST /api/auth/login` | 登录并获取 Token |
| `currentUser()` | `GET /api/auth/me` | 获取当前登录用户 |
| `logout()` | `POST /api/auth/logout` | 退出登录 |

## 6. UI/UX 设计

- 登录表单突出账号和密码输入。
- 提交按钮在请求中进入 loading 状态。
- 登录失败显示后端返回的 `message`。
- 表单基础校验避免空账号或空密码提交。
- 页面不展示密码明文以外的敏感信息。

## 7. 异常处理

- 登录接口返回业务错误时展示错误提示。
- 网络错误时展示通用错误信息。
- 登录态失效时由 `request.js` 清理 Token 并跳转 `/login`。
- 后端返回 401 时，前端不继续保留过期用户信息。

## 8. 测试与验收

管理员场景：

- 使用管理员账号登录成功，进入首页并显示管理员菜单。

普通用户场景：

- 使用普通用户账号登录成功，用户管理菜单不可见。

异常场景：

- 空用户名或空密码不提交或提示错误。
- 密码错误时不保存 Token。
- Token 失效后访问业务页面跳转登录页。
