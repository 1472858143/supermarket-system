# 盘点管理前端开发记录

## 1. 页面定位

盘点管理页面对应后端 `stockcheck` 模块，负责盘点记录查询和管理员新增盘点。新增盘点会调整库存，属于高风险写操作。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| View | `frontend/src/views/stockcheck/StockchecksView.vue` |
| API | `frontend/src/api/stockcheck.js`、`frontend/src/api/product.js` |
| 组件 | `BaseTable.vue`、`BaseDialog.vue`、`PageToolbar.vue`、`PermissionButton.vue` |
| 路由 | `frontend/src/router/index.js` |

## 3. 页面功能

- 盘点记录分页查询。
- 关键词搜索。
- `ADMIN` 新增盘点。
- 商品选择。
- 输入实际库存。
- 展示系统库存、实际库存和差异。

## 4. 权限控制

- 路由 `/stockchecks` 允许 `ADMIN` 和 `USER`。
- 新增盘点按钮只对 `ADMIN` 展示。
- `USER` 只读。
- 后端 `POST /api/stockchecks` 使用 `@RequireRoles("ADMIN")`。

## 5. 接口调用

| 前端方法 | 后端接口 | 用途 |
|---|---|---|
| `listStockchecks(params)` | `GET /api/stockchecks` | 盘点记录分页 |
| `createStockcheck(data)` | `POST /api/stockchecks` | 新增盘点 |
| `listProducts(params)` | `GET /api/products` | 商品选择 |

## 6. UI/UX 设计

- 表格展示盘点历史。
- 差异字段用于识别盈亏。
- 新增盘点使用弹窗。
- 实际库存必填且不能小于 0。
- 提交时显示 loading，成功后刷新列表。
- `USER` 无写操作入口，页面呈只读状态。

## 7. 异常处理

- 实际库存非法时阻止提交或展示错误。
- 商品库存不存在时展示后端错误。
- `USER` 越权调用新增接口时后端返回 403。
- 网络异常显示通用错误。

## 8. 测试与验收

管理员场景：

- `ADMIN` 可查询盘点记录。
- `ADMIN` 可新增盘点，库存调整为实际库存。

普通用户场景：

- `USER` 可查询盘点记录。
- `USER` 不显示新增盘点按钮。

业务场景：

- 盘点成功后库存页显示调整后的库存。
- 差异值与系统库存、实际库存一致。
