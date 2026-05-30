# 出库管理前端开发记录

## 1. 页面定位

出库管理页面对应后端 `outbound` 模块，负责出库记录查询和新增出库。新增出库会由后端校验库存并扣减库存，库存不足时失败。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| View | `frontend/src/views/outbound/OutboundsView.vue` |
| API | `frontend/src/api/outbound.js`、`frontend/src/api/product.js` |
| 组件 | `BaseTable.vue`、`BaseDialog.vue`、`PageToolbar.vue` |
| 路由 | `frontend/src/router/index.js` |

## 3. 页面功能

- 出库记录分页查询。
- 关键词搜索。
- 新增出库。
- 商品选择。
- 输入出库数量。
- 可填写操作人，未填写时后端使用当前登录用户。

## 4. 权限控制

- 路由 `/outbounds` 允许 `ADMIN` 和 `USER`。
- 新增出库当前允许 `ADMIN` 和 `USER`。
- 后端接口要求登录 Token，但未限制到管理员。

## 5. 接口调用

| 前端方法 | 后端接口 | 用途 |
|---|---|---|
| `listOutbounds(params)` | `GET /api/outbounds` | 出库记录分页 |
| `createOutbound(data)` | `POST /api/outbounds` | 新增出库 |
| `listProducts(params)` | `GET /api/products` | 商品选择 |

## 6. UI/UX 设计

- 使用工具栏查询和新增。
- 使用表格展示出库记录。
- 新增出库使用弹窗。
- 数量必填且必须大于 0。
- 提交中显示 loading。
- 成功后刷新出库列表。

## 7. 异常处理

- 库存不足时展示后端返回的业务错误。
- 商品未选择或数量非法时阻止提交或展示错误。
- 网络错误展示通用错误。
- 401 自动跳转登录。

## 8. 测试与验收

管理员场景：

- `ADMIN` 可查询和新增出库。

普通用户场景：

- `USER` 可查询和新增出库。

业务场景：

- 库存充足时出库成功，库存减少。
- 库存不足时出库失败，页面展示错误，列表不新增错误记录。
- 空列表显示 empty。
