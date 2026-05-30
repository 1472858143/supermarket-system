# 用户管理前端开发记录

## 1. 页面定位

用户管理页面对应后端 `user` 模块，负责管理员维护用户、状态和角色。该页面只对 `ADMIN` 可见，普通用户不可见也不可访问。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| View | `frontend/src/views/user/UsersView.vue` |
| API | `frontend/src/api/user.js` |
| 组件 | `BaseTable.vue`、`BaseDialog.vue`、`PageToolbar.vue`、`PermissionButton.vue`、`StatusTag.vue` |
| Store | `frontend/src/stores/auth.js` |
| 路由 | `frontend/src/router/index.js` |

## 3. 页面功能

- 用户分页查询。
- 关键词搜索。
- 新增用户。
- 编辑用户姓名、状态、角色。
- 可选重置密码。
- 删除用户。
- 查询角色列表并用于表单选择。

## 4. 权限控制

- 路由 `/users` 只允许 `ADMIN`。
- 菜单只对 `ADMIN` 展示。
- 新增、编辑、删除按钮使用 `PermissionButton` 限制 `ADMIN`。
- 后端 `/api/users/**` 类级标注 `@RequireRoles("ADMIN")`。

## 5. 接口调用

| 前端方法 | 后端接口 | 用途 |
|---|---|---|
| `listUsers(params)` | `GET /api/users` | 用户分页 |
| `listRoles()` | `GET /api/users/roles` | 角色选项 |
| `createUser(data)` | `POST /api/users` | 新增用户 |
| `updateUser(id,data)` | `PUT /api/users/{id}` | 修改用户 |
| `deleteUser(id)` | `DELETE /api/users/{id}` | 删除用户 |

## 6. UI/UX 设计

- 顶部工具栏提供搜索和新增入口。
- 表格展示用户名、姓名、状态、角色和操作。
- 状态使用 `StatusTag` 展示启用或禁用。
- 新增和编辑使用 `BaseDialog` 弹窗。
- 表单包含必填校验，新增时密码必填。
- 提交时显示 loading，避免重复提交。
- 删除前进行确认。

## 7. 异常处理

- 用户名重复、角色为空、用户不存在等错误展示后端 `message`。
- 接口 401 时跳转登录。
- 接口 403 时说明当前角色无权限。
- 空列表显示 empty。

## 8. 测试与验收

管理员场景：

- `ADMIN` 可看到用户管理菜单和页面。
- `ADMIN` 可新增、编辑、删除用户。
- 新增用户时角色必选。

普通用户场景：

- `USER` 看不到用户管理菜单。
- `USER` 手动访问 `/users` 会被路由守卫拦截。

安全场景：

- 页面不展示用户密码或密码哈希。
- 新密码只作为提交字段，不回显。
