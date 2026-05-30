# 盘点模块开发记录与设计

## 1. 模块定位

盘点模块负责记录库存盘点结果，保存系统库存、实际库存和差异，并通过库存模块将当前库存调整为实际库存。盘点属于影响库存的高风险操作，当前只允许管理员新增。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Controller | `backend/src/main/java/com/supermarket/inventory/stockcheck/controller/StockCheckController.java` |
| Service | `backend/src/main/java/com/supermarket/inventory/stockcheck/service/StockCheckService.java` |
| Mapper | `backend/src/main/java/com/supermarket/inventory/stockcheck/mapper/StockCheckMapper.java` |
| DTO | `StockCheckRequest.java` |
| VO | `StockCheckVO.java` |
| 依赖服务 | `stock/service/StockService.java` |
| 依赖实体 | `stock/entity/Stock.java` |

## 3. 数据表映射

| 表 | 用途 |
|---|---|
| `stock_check` | 保存盘点记录 |
| `stock` | 锁定和调整当前库存 |
| `stock_log` | 记录盘点导致的库存变化 |
| `product` | 查询盘点列表时关联商品信息 |

关键字段：

- `stock_check.system_quantity`：盘点时系统库存。
- `stock_check.actual_quantity`：实际盘点库存，不得小于 0。
- `stock_check.difference`：实际库存减系统库存。
- `stock_log.change_type`：盘点调整记录为 `CHECK`。

## 4. 接口设计

| 方法 | 路径 | 权限 | 请求 | 返回 | 说明 |
|---|---|---|---|---|---|
| `GET` | `/api/stockchecks` | 已登录 | `keyword`、`page`、`pageSize` | `PageResult<StockCheckVO>` | 盘点记录分页查询 |
| `POST` | `/api/stockchecks` | `ADMIN` | `StockCheckRequest` | `Void` | 新增盘点并调整库存 |

`StockCheckRequest` 字段：

- `productId`：必填。
- `actualQuantity`：必填，最小为 0。

## 5. 业务流程

```text
StockCheckController.create
-> StockCheckService.create
-> StockService.lockStock
-> 读取 systemQuantity
-> 计算 difference = actualQuantity - systemQuantity
-> StockCheckMapper.insert
-> StockService.adjustTo
-> StockMapper.updateQuantity
-> StockMapper.insertLog(CHECK)
```

事务边界：

- `StockCheckService.create` 使用 `@Transactional`。
- 盘点记录写入与库存调整处于同一事务。
- 任一步失败时整体回滚。

## 6. 权限与安全

- 盘点记录查询允许 `ADMIN` 和 `USER`。
- 新增盘点只允许 `ADMIN`。
- 实际库存必须大于等于 0。
- 盘点模块不直接更新库存数量，必须调用 `StockService.adjustTo`。
- 库存变化通过 `stock_log` 记录为 `CHECK`。

## 7. 可维护性说明

- Controller 只暴露查询和新增接口。
- Service 负责锁定库存、计算差异和事务编排。
- Mapper 只负责盘点表查询和插入。
- 库存调整规则仍由库存模块负责。

## 8. 可扩展性说明

- 后续可新增盘点批次、盘点人、备注等字段。
- 若未来支持普通用户提交盘点草稿，可新增草稿接口，不直接调整库存。
- 若未来增加审批，审批通过后再调用 `StockService.adjustTo`。

## 9. 测试与验收

正常场景：

- 登录用户可查询盘点记录。
- `ADMIN` 新增盘点成功，库存调整为实际库存。
- 差异字段等于实际库存减系统库存。
- `stock_log` 增加 `CHECK` 日志。

异常场景：

- 实际库存为空或小于 0 时参数校验失败。
- 商品库存不存在时新增盘点失败并回滚。

权限场景：

- `USER` 查询盘点记录成功。
- `USER` 新增盘点返回 403。
