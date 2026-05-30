# 出库模块开发记录与设计

## 1. 模块定位

出库模块负责记录商品出库业务，并通过库存模块扣减库存。出库模块必须防止库存扣减后为负数，库存不足时整个出库流程失败。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Controller | `backend/src/main/java/com/supermarket/inventory/outbound/controller/OutboundController.java` |
| Service | `backend/src/main/java/com/supermarket/inventory/outbound/service/OutboundService.java` |
| Mapper | `backend/src/main/java/com/supermarket/inventory/outbound/mapper/OutboundMapper.java` |
| DTO | `OutboundRequest.java` |
| VO | `OutboundVO.java` |
| 依赖服务 | `stock/service/StockService.java` |
| 安全上下文 | `auth/security/CurrentUserContext.java` |

## 3. 数据表映射

| 表 | 用途 |
|---|---|
| `outbound_order` | 保存出库记录 |
| `stock` | 通过 `StockService` 扣减库存 |
| `stock_log` | 记录库存变更日志 |
| `product` | 查询出库列表时关联商品信息 |

关键字段：

- `outbound_order.product_id`：出库商品。
- `outbound_order.quantity`：出库数量，必须大于 0。
- `outbound_order.operator`：操作人，前端未传时使用当前登录用户名。
- `stock.quantity`：扣减后不得小于 0。

## 4. 接口设计

| 方法 | 路径 | 权限 | 请求 | 返回 | 说明 |
|---|---|---|---|---|---|
| `GET` | `/api/outbounds` | 已登录 | `keyword`、`page`、`pageSize` | `PageResult<OutboundVO>` | 出库记录分页查询 |
| `POST` | `/api/outbounds` | 已登录 | `OutboundRequest` | `Void` | 新增出库记录并扣减库存 |

`OutboundRequest` 字段：

- `productId`：必填。
- `quantity`：必填，最小为 1。
- `operator`：可选。

## 5. 业务流程

```text
OutboundController.create
-> OutboundService.create
-> resolveOperator
-> StockService.decrease
-> StockDomainService.decrease
-> StockMapper.updateQuantity
-> StockMapper.insertLog(OUTBOUND)
-> OutboundMapper.insert
```

事务边界：

- `OutboundService.create` 使用 `@Transactional`。
- 库存扣减和出库记录写入在同一事务内。
- 库存不足时 `StockService.decrease` 抛出业务异常，出库记录不会写入。

## 6. 权限与安全

- 当前出库查询和新增允许所有已登录用户。
- 出库数量由 DTO 校验为大于 0。
- 库存不足由 `StockDomainService` 校验，不能依赖前端判断。
- 出库模块不直接修改库存表。

## 7. 可维护性说明

- Controller 保持薄层，只做接口入口。
- Service 组织事务和库存扣减调用。
- Mapper 只做出库表访问。
- 库存不足和日志写入由库存模块统一完成。

## 8. 可扩展性说明

- 后续可新增出库原因、领用部门、备注等字段。
- 若未来增加审批，可在审批通过后调用 `StockService.decrease`。
- 若未来按角色限制出库，可通过 `@RequireRoles` 或扩展权限配置实现。

## 9. 测试与验收

正常场景：

- 登录用户可查询出库记录。
- 库存充足时新增出库成功，库存减少。
- `stock_log` 增加一条 `OUTBOUND` 日志。
- 操作人为空时自动使用当前登录用户名。

异常场景：

- 数量为空或小于 1 时参数校验失败。
- 库存不足时返回业务错误，库存和出库记录均不变化。
- 商品或库存不存在时业务失败。

权限场景：

- 未登录访问返回 401。
- `ADMIN` 和 `USER` 登录后均可新增出库。
