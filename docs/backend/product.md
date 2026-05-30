# 商品模块开发记录与设计

## 1. 模块定位

商品模块负责商品基础档案维护，包括商品编号、名称、分类、进价、售价和状态。商品模块不维护库存数量，新增商品后只负责调用库存模块初始化库存记录。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Controller | `backend/src/main/java/com/supermarket/inventory/product/controller/ProductController.java` |
| Service | `backend/src/main/java/com/supermarket/inventory/product/service/ProductService.java` |
| Mapper | `backend/src/main/java/com/supermarket/inventory/product/mapper/ProductMapper.java` |
| Entity | `Product.java` |
| DTO | `ProductRequest.java` |
| VO | `ProductVO.java` |
| 依赖服务 | `stock/service/StockService.java` |

## 3. 数据表映射

| 表 | 用途 |
|---|---|
| `product` | 保存商品基础信息 |
| `stock` | 新增商品后初始化一条库存记录 |

关键字段：

- `product.product_code`：商品编码，唯一。
- `product.purchase_price`：进价，不能小于 0。
- `product.sale_price`：售价，不能小于进价。
- `product.status`：上架或下架。

## 4. 接口设计

| 方法 | 路径 | 权限 | 请求 | 返回 | 说明 |
|---|---|---|---|---|---|
| `GET` | `/api/products` | 已登录 | `keyword`、`page`、`pageSize` | `PageResult<ProductVO>` | 商品分页查询 |
| `POST` | `/api/products` | `ADMIN` | `ProductRequest` | `ProductVO` | 新增商品 |
| `PUT` | `/api/products/{id}` | `ADMIN` | `ProductRequest` | `ProductVO` | 修改商品 |
| `DELETE` | `/api/products/{id}` | `ADMIN` | 路径参数 `id` | `Void` | 删除商品 |

`ProductRequest` 字段：

- `productCode`
- `productName`
- `category`
- `purchasePrice`
- `salePrice`
- `status`

## 5. 业务流程

新增商品：

```text
ProductController.create
-> ProductService.create
-> 校验售价不低于进价
-> 校验 productCode 唯一
-> ProductMapper.insert
-> StockService.initializeStock
-> 返回 ProductVO
```

修改商品：

```text
ProductController.update
-> ProductService.update
-> 查询商品是否存在
-> 校验售价不低于进价
-> 更新商品基础字段
-> 返回 ProductVO
```

删除商品：

```text
ProductController.delete
-> ProductService.delete
-> 查询商品是否存在
-> StockService.deleteStockByProductId
-> ProductMapper.delete
```

## 6. 权限与安全

- 商品查询允许 `ADMIN` 和 `USER`。
- 商品新增、修改、删除只允许 `ADMIN`。
- 商品模块不接收库存数量字段，避免绕过库存模块。
- 商品价格校验在 DTO 注解和 Service 层同时控制。

## 7. 可维护性说明

- 商品 Controller 只负责接口入口。
- 商品 Service 负责商品校验和调用库存初始化。
- 商品 Mapper 只访问 `product` 表。
- 库存初始化由 `StockService` 完成，避免商品模块直接操作库存表。

## 8. 可扩展性说明

- 后续新增商品条码、规格、单位等字段，可扩展 `ProductRequest` 和 `ProductVO`。
- 若新增商品分类字典，可增加分类模块，商品只保存分类引用。
- 若新增上下架审批，不影响库存模块的唯一库存变更入口。

## 9. 测试与验收

正常场景：

- 登录用户可分页查询商品。
- `ADMIN` 新增商品成功，并自动生成库存记录。
- `ADMIN` 修改商品名称、分类、价格、状态成功。
- `ADMIN` 删除未被业务记录引用的商品成功。

异常场景：

- 商品编码重复时新增失败。
- 售价低于进价时新增或修改失败。
- 删除已被入库、出库、盘点等业务记录引用的商品时，数据库外键会阻止破坏历史数据。

权限场景：

- `USER` 查询商品成功。
- `USER` 新增、修改、删除商品返回 403。
