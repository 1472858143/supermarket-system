# 入库模块开发记录与设计

## 1. 模块定位

入库模块负责记录商品入库业务，并通过库存模块增加库存。入库模块不直接更新 `stock` 表，库存变化必须委托 `StockService.increase` 完成。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Controller | `backend/src/main/java/com/supermarket/inventory/inbound/controller/InboundController.java` |
| Service | `backend/src/main/java/com/supermarket/inventory/inbound/service/InboundService.java` |
| Mapper | `backend/src/main/java/com/supermarket/inventory/inbound/mapper/InboundMapper.java` |
| DTO | `InboundRequest.java` |
| VO | `InboundVO.java` |
| 依赖服务 | `stock/service/StockService.java` |
| 安全上下文 | `auth/security/CurrentUserContext.java` |

## 3. 数据表映射

| 表 | 用途 |
|---|---|
| `inbound_order` | 保存入库记录 |
| `stock` | 通过 `StockService` 增加库存 |
| `stock_log` | 记录库存变更日志 |
| `product` | 查询入库列表时关联商品信息 |

关键字段：

- `inbound_order.product_id`：入库商品。
- `inbound_order.quantity`：入库数量，必须大于 0。
- `inbound_order.operator`：操作人，前端未传时使用当前登录用户名。

## 4. 接口设计

| 方法 | 路径 | 权限 | 请求 | 返回 | 说明 |
|---|---|---|---|---|---|
| `GET` | `/api/inbounds` | 已登录 | `keyword`、`page`、`pageSize` | `PageResult<InboundVO>` | 入库记录分页查询 |
| `POST` | `/api/inbounds` | 已登录 | `InboundRequest` | `Void` | 新增入库记录并增加库存 |

`InboundRequest` 字段：

- `productId`：必填。
- `quantity`：必填，最小为 1。
- `operator`：可选。

## 5. 业务流程

```text
InboundController.create
-> InboundService.create
-> resolveOperator
-> InboundMapper.insert
-> StockService.increase
-> StockMapper.updateQuantity
-> StockMapper.insertLog(INBOUND)
```

事务边界：

- `InboundService.create` 使用 `@Transactional`。
- 入库记录写入和库存增加在同一事务内完成。
- 任一步失败，事务回滚，避免出现有入库记录但库存未增加的情况。

## 6. 权限与安全

- 当前入库查询和新增允许所有已登录用户。
- 操作人为空时使用 `CurrentUserContext` 中的用户名。
- 入库数量由 DTO 校验为大于 0。
- 入库模块不直接修改库存表，防止绕过库存日志。

## 7. 可维护性说明

- Controller 只暴露列表和新增接口。
- Service 负责事务编排和操作人解析。
- Mapper 只负责入库记录查询和写入。
- 库存增加逻辑位于 `StockService`，入库模块只表达业务来源。

## 8. 可扩展性说明

- 后续可新增入库单号、备注、供应商等字段，扩展 `InboundRequest` 和 `InboundVO`。
- 若未来增加入库审核，可在审核通过后调用 `StockService.increase`。
- 若未来限制普通用户入库，可在 Controller 方法上增加 `@RequireRoles("ADMIN")` 或新角色。

## 9. 测试与验收

正常场景：

- 登录用户可查询入库记录分页列表。
- 新增入库记录成功后库存增加。
- 操作人为空时自动使用当前登录用户名。
- `stock_log` 增加一条 `INBOUND` 日志。

异常场景：

- 商品 ID 为空或数量小于 1 时参数校验失败。
- 商品不存在或库存记录不存在时业务失败并回滚。

权限场景：

- 未登录访问返回 401。
- `ADMIN` 和 `USER` 登录后均可新增入库。
