# 商品管理前端开发记录

## 1. 页面定位

商品管理页面对应后端 `product` 模块，负责商品基础档案查询和维护。页面不提供库存数量编辑入口，库存数量必须通过入库、出库、盘点流程变化。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| View | `frontend/src/views/product/ProductsView.vue` |
| API | `frontend/src/api/product.js` |
| 组件 | `BaseTable.vue`、`BaseDialog.vue`、`PageToolbar.vue`、`PermissionButton.vue`、`StatusTag.vue` |
| 路由 | `frontend/src/router/index.js` |

## 3. 页面功能

- 商品分页查询。
- 关键词搜索。
- 新增商品。
- 编辑商品名称、分类、价格、状态。
- 删除商品。
- 展示上架或下架状态。

## 4. 权限控制

- 路由 `/products` 允许 `ADMIN` 和 `USER`。
- `ADMIN` 可看到新增、编辑、删除按钮。
- `USER` 只能查看商品列表。
- 后端商品写操作使用 `@RequireRoles("ADMIN")`。

## 5. 接口调用

| 前端方法 | 后端接口 | 用途 |
|---|---|---|
| `listProducts(params)` | `GET /api/products` | 商品分页 |
| `createProduct(data)` | `POST /api/products` | 新增商品 |
| `updateProduct(id,data)` | `PUT /api/products/{id}` | 修改商品 |
| `deleteProduct(id)` | `DELETE /api/products/{id}` | 删除商品 |

## 6. UI/UX 设计

- 使用 `PageToolbar` 进行搜索和重置。
- 使用 `BaseTable` 展示分页、loading、empty。
- 新增和编辑使用 `BaseDialog`。
- 商品状态使用 `StatusTag`。
- 表单校验商品编码、名称、分类、进价和售价。
- 删除操作需要确认并展示结果反馈。

## 7. 异常处理

- 商品编码重复时展示后端错误。
- 售价低于进价时展示错误。
- 删除被业务记录引用的商品时展示失败提示。
- 网络异常和 401 由请求封装处理。

## 8. 测试与验收

管理员场景：

- `ADMIN` 可新增商品，新增后后端自动初始化库存。
- `ADMIN` 可编辑商品基础信息。
- `ADMIN` 可删除未被业务引用的商品。

普通用户场景：

- `USER` 可查看商品列表。
- `USER` 不显示新增、编辑、删除按钮。

边界场景：

- 页面没有库存数量输入框。
- 表格空数据时显示 empty。
