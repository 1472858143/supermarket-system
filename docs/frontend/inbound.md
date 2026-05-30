# 入库管理前端开发记录

## 1. 页面定位

入库管理页面对应后端 `inbound` 模块，负责入库记录查询和新增入库。新增入库会由后端调用库存模块增加库存。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| View | `frontend/src/views/inbound/InboundsView.vue` |
| API | `frontend/src/api/inbound.js`、`frontend/src/api/product.js` |
| 组件 | `BaseTable.vue`、`BaseDialog.vue`、`PageToolbar.vue` |
| 路由 | `frontend/src/router/index.js` |

## 3. 页面功能

- 入库记录分页查询。
- 关键词搜索。
- 新增入库。
- 商品选择。
- 输入入库数量。
- 可填写操作人，未填写时后端使用当前登录用户。

## 4. 权限控制

- 路由 `/inbounds` 允许 `ADMIN` 和 `USER`。
- 新增入库当前允许 `ADMIN` 和 `USER`。
- 后端接口要求登录 Token，但未限制到管理员。

## 5. 接口调用

| 前端方法 | 后端接口 | 用途 |
|---|---|---|
| `listInbounds(params)` | `GET /api/inbounds` | 入库记录分页 |
| `createInbound(data)` | `POST /api/inbounds` | 新增入库 |
| `listProducts(params)` | `GET /api/products` | 商品选择 |

## 6. UI/UX 设计

- 顶部工具栏提供搜索和新增入口。
- 表格展示商品、数量、操作人、时间等信息。
- 新增入库使用弹窗表单。
- 商品选择使用后端商品列表数据。
- 数量必填且必须大于 0。
- 提交时显示 loading，成功后关闭弹窗并刷新列表。

## 7. 异常处理

- 商品未选择或数量非法时阻止提交或展示错误。
- 商品不存在、库存记录不存在等错误展示后端 `message`。
- 网络错误展示通用错误。
- 401 自动跳转登录。

## 8. 测试与验收

管理员场景：

- `ADMIN` 可查询入库记录并新增入库。

普通用户场景：

- `USER` 可查询入库记录并新增入库。

业务场景：

- 新增入库后库存列表中对应商品库存增加。
- 空列表显示 empty。
- 接口失败时弹窗不误关闭或有错误反馈。
