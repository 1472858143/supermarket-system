# 报表模块开发记录与设计

## 1. 模块定位

报表模块负责库存、入库、出库和库存预警的统计展示。该模块是只读模块，不写入业务数据，不改变库存，不承担审计或配置中心职责。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| Controller | `backend/src/main/java/com/supermarket/inventory/report/controller/ReportController.java` |
| Service | `backend/src/main/java/com/supermarket/inventory/report/service/ReportService.java` |
| Mapper | `backend/src/main/java/com/supermarket/inventory/report/mapper/ReportMapper.java` |

## 3. 数据表映射

| 表 | 用途 |
|---|---|
| `product` | 统计商品数量、关联商品信息 |
| `stock` | 统计库存总量和库存预警 |
| `inbound_order` | 统计入库数量和趋势 |
| `outbound_order` | 统计出库数量和趋势 |
| `stock_log` | 可用于库存变化趋势扩展 |

## 4. 接口设计

| 方法 | 路径 | 权限 | 请求 | 返回 | 说明 |
|---|---|---|---|---|---|
| `GET` | `/api/reports/stock` | 已登录 | 无 | `Map<String,Object>` | 库存统计 |
| `GET` | `/api/reports/inbound` | 已登录 | 无 | `Map<String,Object>` | 入库统计 |
| `GET` | `/api/reports/outbound` | 已登录 | 无 | `Map<String,Object>` | 出库统计 |
| `GET` | `/api/reports/warning` | 已登录 | 无 | `List<Map<String,Object>>` | 库存预警列表 |

## 5. 业务流程

```text
ReportController
-> ReportService
-> ReportMapper
-> 只读 SQL 聚合查询
-> ApiResponse
```

报表模块不参与事务性库存变更，不调用 `StockService.increase`、`decrease` 或 `adjustTo`。

## 6. 权限与安全

- `ADMIN` 和 `USER` 均可查看报表。
- 报表接口只读，不暴露写接口。
- 报表返回聚合数据，不返回用户密码、Token、数据库连接等敏感信息。
- 报表模块不允许修改业务数据。

## 7. 可维护性说明

- Controller 按统计主题拆分接口。
- Service 作为报表聚合入口。
- Mapper 集中保存统计 SQL。
- 只读定位清晰，避免报表模块混入写业务逻辑。

## 8. 可扩展性说明

- 后续新增统计卡片时，可在 `ReportService` 增加只读聚合方法。
- 后续新增图表趋势，可扩展 Mapper 查询，不影响库存变更模块。
- 若未来需要复杂 BI，可单独新增分析模块，当前报表接口保持稳定。

## 9. 测试与验收

正常场景：

- 登录用户可获取库存、入库、出库统计。
- 登录用户可获取库存预警列表。
- 空数据时返回空列表或数值为 0 的统计结果。

异常场景：

- 未登录访问报表返回 401。

权限场景：

- `ADMIN` 和 `USER` 都可以访问报表。

只读验收：

- 调用报表接口不会新增、修改或删除任何业务表数据。
