# 库存管理前端开发记录

## 1. 页面定位

库存管理页面对应后端 `stock` 模块，负责库存查询、库存预警展示和库存上下限维护。页面不提供直接修改库存数量的能力。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| View | `frontend/src/views/stock/StocksView.vue` |
| API | `frontend/src/api/stock.js` |
| 组件 | `BaseTable.vue`、`BaseDialog.vue`、`PageToolbar.vue`、`PermissionButton.vue`、`StatusTag.vue` |
| 路由 | `frontend/src/router/index.js` |

## 3. 页面功能

- 库存分页查询。
- 按关键词搜索商品库存。
- 展示商品信息、当前库存、库存上下限。
- 展示库存预警状态。
- `ADMIN` 可维护库存上下限。

## 4. 权限控制

- 路由 `/stocks` 允许 `ADMIN` 和 `USER`。
- 维护上下限按钮只对 `ADMIN` 展示。
- `USER` 只读。
- 后端 `/api/stocks/{productId}/limit` 使用 `@RequireRoles("ADMIN")`。

## 5. 接口调用

| 前端方法 | 后端接口 | 用途 |
|---|---|---|
| `listStocks(params)` | `GET /api/stocks` | 库存分页 |
| `updateStockLimit(productId,data)` | `PUT /api/stocks/{productId}/limit` | 维护上下限 |

## 6. UI/UX 设计

- 表格展示库存核心字段。
- 使用状态标签区分正常、低库存、超上限等状态。
- 上下限维护使用弹窗表单。
- 查询中显示 loading。
- 无库存记录时显示 empty。
- 提交上下限时显示 loading 和结果反馈。

## 7. 异常处理

- 上限小于下限时展示后端错误。
- 库存不存在时展示业务错误。
- `USER` 越权调用上下限接口时后端返回 403。
- 401 由请求封装统一跳转登录。

## 8. 测试与验收

管理员场景：

- `ADMIN` 可查看库存列表。
- `ADMIN` 可维护库存上下限。
- 上下限保存后表格刷新。

普通用户场景：

- `USER` 可查看库存。
- `USER` 不显示上下限维护按钮。

边界场景：

- 页面没有直接修改当前库存数量的入口。
- 库存预警状态显示清晰。
