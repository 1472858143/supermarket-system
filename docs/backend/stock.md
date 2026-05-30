# 库存模块开发记录与设计

## 1. 模块定位

库存模块是系统的核心领域模块，负责库存查询、库存上下限维护、库存数量变更和库存日志记录。为保证库存一致性，只有 `StockService` 允许直接修改库存数量。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Controller | `backend/src/main/java/com/supermarket/inventory/stock/controller/StockController.java` |
| Service | `backend/src/main/java/com/supermarket/inventory/stock/service/StockService.java` |
| Domain | `backend/src/main/java/com/supermarket/inventory/stock/domain/StockDomainService.java` |
| Mapper | `backend/src/main/java/com/supermarket/inventory/stock/mapper/StockMapper.java` |
| Entity | `Stock.java` |
| DTO | `StockLimitUpdateRequest.java` |
| VO | `StockVO.java` |

## 3. 数据表映射

| 表 | 用途 |
|---|---|
| `stock` | 保存商品当前库存、库存上下限 |
| `stock_log` | 保存库存变更记录 |
| `product` | 库存查询时关联商品信息 |

关键字段：

- `stock.product_id`：商品 ID，唯一。
- `stock.quantity`：当前库存，不得小于 0。
- `stock.min_stock`：库存下限。
- `stock.max_stock`：库存上限，必须大于等于下限。
- `stock_log.change_type`：库存变更类型，当前为 `INBOUND`、`OUTBOUND`、`CHECK`。
- `stock_log.before_quantity`、`after_quantity`：变更前后库存。

## 4. 接口设计

| 方法 | 路径 | 权限 | 请求 | 返回 | 说明 |
|---|---|---|---|---|---|
| `GET` | `/api/stocks` | 已登录 | `keyword`、`page`、`pageSize` | `PageResult<StockVO>` | 库存分页查询 |
| `GET` | `/api/stocks/{productId}` | 已登录 | 路径参数 `productId` | `StockVO` | 查询单个商品库存 |
| `PUT` | `/api/stocks/{productId}/limit` | `ADMIN` | `StockLimitUpdateRequest` | `StockVO` | 维护库存上下限 |

说明：后端没有提供直接修改库存数量的公开接口，库存数量只能通过入库、出库、盘点流程变化。

## 5. 业务流程

初始化库存：

```text
ProductService.create
-> StockService.initializeStock
-> StockMapper.insertInitialStock
```

入库增加库存：

```text
InboundService.create
-> StockService.increase
-> lockStock
-> StockDomainService.increase
-> updateQuantity
-> insertLog(INBOUND)
```

出库扣减库存：

```text
OutboundService.create
-> StockService.decrease
-> lockStock
-> StockDomainService.decrease
-> updateQuantity
-> insertLog(OUTBOUND)
```

盘点调整库存：

```text
StockCheckService.create
-> StockService.adjustTo
-> lockStock
-> StockDomainService.adjustTo
-> updateQuantity
-> insertLog(CHECK)
```

## 6. 权限与安全

- 库存查询允许 `ADMIN` 和 `USER`。
- 维护库存上下限只允许 `ADMIN`。
- 库存数量没有直接编辑接口，防止绕过业务记录。
- 库存扣减不足时由 `StockDomainService` 抛出业务异常。
- 库存变更必须写入 `stock_log`。

## 7. 可维护性说明

- Controller 不包含库存计算逻辑。
- `StockService` 统一承接库存变更。
- `StockDomainService` 集中保存库存领域规则。
- Mapper 只负责查询、锁定、更新和写日志。
- `lockStock` 用于库存变更前锁定记录，降低并发更新风险。

## 8. 可扩展性说明

- 新增库存变化来源时，新增业务模块仍调用 `StockService`。
- 可扩展 `stock_log.change_type` 支持更多来源，例如退货、报损。
- 可新增库存预警策略，但不改变 `StockService` 的唯一变更入口。
- 若未来支持多仓库，可将 `stock` 扩展为商品加仓库维度，库存变更服务仍保留统一入口。

## 9. 测试与验收

正常场景：

- 登录用户可查询库存列表和单商品库存。
- `ADMIN` 修改库存上下限成功。
- 入库后库存增加并写入 `INBOUND` 日志。
- 出库后库存减少并写入 `OUTBOUND` 日志。
- 盘点后库存调整并写入 `CHECK` 日志。

异常场景：

- 库存不存在时返回 404 业务异常。
- 库存上限小于下限时维护失败。
- 出库数量大于当前库存时失败，库存不变化。

权限场景：

- `USER` 查询库存成功。
- `USER` 调用上下限维护接口返回 403。
