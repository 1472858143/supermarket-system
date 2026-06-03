# 采购链路升级设计

日期：2026-06-02

## 背景

现有采购链路为：

```text
Supplier -> SupplierSku -> PurchaseInbound -> PurchaseInboundItem -> StockBatch -> Stock
```

现有实现中，采购入库创建时会立即完成单据、写入明细、增加库存并生成库存批次。该模型无法表达草稿、提交、审批、退回、部分到货、关闭剩余等业务过程，也无法清晰区分计划数量和实际入库数量。

本次升级目标是把采购计划和实际入库拆开，并补齐操作人快照、供应商侧商品快照、库存可用量模型、1:N 实际批次和成本价精度。

## 已确认业务口径

1. 审批通过不触发库存入库，库存只在“执行入库”操作时增加。
2. 允许部分入库。审批数量是计划数量，入库数量是实际到货数量。
3. 严格禁止超计划入库。
4. 执行入库只能登记已审批计划明细的实际到货结果。计划外 SKU、超计划数量、供应商变更不能在普通入库环节绕过审批。
5. 普通入库只登记数量和批次，价格跟随已审批计划。价格变化必须走变更审批，不在普通入库环节修改。
6. `RETURNED` 是独立的退回修改状态，不把单据状态改回 `DRAFT`。
7. `RETURNED` 状态下允许修改采购计划内容，不允许修改审批痕迹、单据身份、库存结果、入库结果。
8. `RETURNED` 状态下允许更换供应商，但必须按新供应商重新校验全部 SKU 绑定和采购规则。审批通过后不允许再更换供应商。
9. 没有入库结果的终止叫 `CANCELLED`。已经部分入库，只是不再收剩余数量的终止叫 `CLOSED`。
10. 总库存表示仓内仍存在的实物，包含可用、锁定、过期，不包含已出库、已报损、已关闭。
11. 缺货预警看可用库存，下限按 `available_quantity` 判断。超储预警看总库存，上限按 `total_quantity` 判断。
12. 过期扫描只是把可用库存转成过期库存，不改变总库存。真正扣总库存必须走报损、销毁或调整流程。
13. 锁定和解锁都是库存状态结构变化，不改变总库存。解锁时重新判断效期，未过期回 `AVAILABLE`，已过期进 `EXPIRED`。
14. 报损可以作用于可用、锁定、过期三类仍在仓内的实物库存。报损后要从对应状态量和总库存中扣减。
15. 部分报损只扣数量，批次状态保持。整批报损扣到 0，状态才变为 `DAMAGED`。
16. 盘点调整必须落到批次。批次是事实来源，库存汇总是结果，调整后要同步重算汇总。
17. 采用主表累计方案：`purchase_inbound` 主表落库累计实收数量和累计实收金额。
18. 执行入库事务中，更新计划明细累计实收后，必须同步滚动更新采购单主表累计实收字段。
19. `stock_batch` 的价格快照明确从 `purchase_inbound_receipt_batch` 拷贝。
20. 执行入库导致的 `PARTIALLY_INBOUNDED` 和 `INBOUNDED` 状态变化不写审批日志，由 receipt、库存日志和批次日志审计。
21. `SUBMIT / APPROVE / RETURN / CANCEL / CLOSE` 仍写入审批日志。

## 方案选择

采用“采购计划单 + 实际入库单分离”的方案。

升级后链路为：

```text
Supplier
  -> SupplierSku
  -> PurchaseInbound(采购计划和审批)
  -> PurchaseInboundItem(采购计划明细)
  -> PurchaseInboundReceipt(执行入库主记录)
  -> PurchaseInboundReceiptBatch(实际到货批次)
  -> StockBatch
  -> Stock
```

`purchase_inbound` 和 `purchase_inbound_item` 表示已审批采购计划。新增 `purchase_inbound_receipt` 和 `purchase_inbound_receipt_batch` 表示每次执行入库的实际到货。`stock_batch` 由实际入库批次生成，并关联到 `purchase_inbound_receipt_batch`。

该方案可以清晰表达计划和实际的差异，天然支持部分入库和多批次入库，并保留每次执行入库的操作人和时间。

## 状态机

采购单主状态：

| 状态 | 含义 | 是否终态 |
| --- | --- | --- |
| `DRAFT` | 草稿，可编辑计划内容 | 否 |
| `SUBMITTED` | 已提交，等待审批，不可编辑计划 | 否 |
| `RETURNED` | 退回修改，可编辑采购计划内容 | 否 |
| `APPROVED` | 已审批，计划冻结，可执行入库 | 否 |
| `PARTIALLY_INBOUNDED` | 已部分入库，可继续入库或关闭剩余 | 否 |
| `INBOUNDED` | 已全部入库 | 是 |
| `CANCELLED` | 未产生实际入库时终止 | 是 |
| `CLOSED` | 已部分入库后不再收剩余 | 是 |

允许流转：

```text
DRAFT -> SUBMITTED
SUBMITTED -> APPROVED
SUBMITTED -> RETURNED
RETURNED -> SUBMITTED
DRAFT / SUBMITTED / RETURNED / APPROVED -> CANCELLED, 前提是无实际入库
APPROVED -> PARTIALLY_INBOUNDED, 首次部分入库
APPROVED -> INBOUNDED, 首次即全部入库
PARTIALLY_INBOUNDED -> PARTIALLY_INBOUNDED, 继续部分入库
PARTIALLY_INBOUNDED -> INBOUNDED, 累计达到计划量
PARTIALLY_INBOUNDED -> CLOSED, 剩余不再收货
```

核心边界：

1. 采购计划只在 `DRAFT` 和 `RETURNED` 可修改。
2. 审批通过后，供应商、计划明细、计划数量、单位、价格和供应商侧商品快照全部冻结。
3. 审批通过本身不影响库存。
4. 执行入库只登记已审批计划明细的实际到货批次。
5. 执行入库不能新增计划外 SKU，不能超计划数量，不能更换供应商，不能修改价格。

## 数据模型

### purchase_inbound

采购计划主表。现有 `purchase_inbound` 升级为采购计划和审批主表。

新增或调整字段：

| 字段 | 含义 |
| --- | --- |
| `status` | 采购单状态机枚举 |
| `creator_user_id` | 创建人 ID 快照 |
| `creator_username` | 创建人用户名快照 |
| `submitter_user_id` | 最近一次提交人 ID 快照 |
| `submitter_username` | 最近一次提交人用户名快照 |
| `submit_time` | 最近一次提交时间 |
| `approver_user_id` | 最近一次审批通过人 ID 快照 |
| `approver_username` | 最近一次审批通过人用户名快照 |
| `approve_time` | 最近一次审批通过时间 |
| `cancel_user_id` | 取消人 ID 快照 |
| `cancel_username` | 取消人用户名快照 |
| `cancel_time` | 取消时间 |
| `cancel_reason` | 取消原因 |
| `close_user_id` | 关闭人 ID 快照 |
| `close_username` | 关闭人用户名快照 |
| `close_time` | 关闭时间 |
| `close_reason` | 关闭原因 |
| `planned_total_quantity` | 计划基础数量合计 |
| `inbound_total_quantity` | 累计实收基础数量合计，落库保存 |
| `planned_total_amount` | 计划金额合计 |
| `inbound_total_amount` | 累计实收金额合计，落库保存 |

确认 `purchase_inbound` 相关表为空，采用破坏式迁移：直接将 `total_quantity` 改名为 `planned_total_quantity`、`total_amount` 改名为 `planned_total_amount`，不保留旧字段，不做双写兼容。

累计实收采用主表落库方案。每次执行入库时，先更新对应计划明细的累计实收数量和金额，再在同一事务内滚动更新 `purchase_inbound.inbound_total_quantity` 和 `purchase_inbound.inbound_total_amount`，避免列表页和状态判断依赖运行时聚合。

### purchase_inbound_item

采购计划明细表。现有明细表从“实际入库明细”升级为“计划明细”。

新增或调整字段：

| 字段 | 含义 |
| --- | --- |
| `supplier_sku_id` | 审批时使用的供应商 SKU 绑定 |
| `supplier_sku_code_snapshot` | 供应商侧商品编码快照 |
| `supplier_sku_name_snapshot` | 供应商侧商品名称快照 |
| `supplier_spec_snapshot` | 供应商侧规格快照 |
| `planned_quantity` | 计划采购数量，操作单位 |
| `planned_base_quantity` | 计划基础数量 |
| `planned_amount` | 计划金额 |
| `inbounded_base_quantity` | 累计实收基础数量 |
| `inbounded_amount` | 累计实收金额 |
| `unit` | 操作单位快照 |
| `conversion_rate` | 单位换算率快照 |
| `purchase_price` | 审批计划采购价快照 |
| `cost_price` | 基础单位成本价快照 |

价格精度建议：

| 字段 | 精度 |
| --- | --- |
| `purchase_price` | `DECIMAL(18,6)` |
| `cost_price` | `DECIMAL(18,8)` |
| `planned_amount` | `DECIMAL(18,6)` |
| `inbounded_amount` | `DECIMAL(18,6)` |

展示层再按业务场景格式化为 2 位或 4 位，避免成本价换算时发生累计误差。

### purchase_inbound_approval_log

新增审批和状态动作日志表。

核心字段：

| 字段 | 含义 |
| --- | --- |
| `purchase_inbound_id` | 采购单 ID |
| `action` | `SUBMIT / APPROVE / RETURN / CANCEL / CLOSE` |
| `from_status` | 操作前状态 |
| `to_status` | 操作后状态 |
| `operator_user_id` | 操作人 ID 快照 |
| `operator_username` | 操作人用户名快照 |
| `reason` | 原因 |
| `remark` | 备注 |
| `create_time` | 操作时间 |

采购单主表只保留当前状态和最近动作快照，审批和人工状态动作历史通过该日志表追溯。

该表只记录 `SUBMIT / APPROVE / RETURN / CANCEL / CLOSE`。执行入库导致的 `APPROVED -> PARTIALLY_INBOUNDED`、`APPROVED -> INBOUNDED`、`PARTIALLY_INBOUNDED -> INBOUNDED` 状态变化不写入审批日志，由 `purchase_inbound_receipt`、库存日志和批次日志审计。

### purchase_inbound_receipt

新增实际入库主表，表示一次执行入库动作。

核心字段：

| 字段 | 含义 |
| --- | --- |
| `receipt_no` | 实际入库单号，格式 `PIR{YYYYMMDD}{NNNN}` |
| `purchase_inbound_id` | 采购计划单 ID |
| `operator_user_id` | 执行入库人 ID 快照 |
| `operator_username` | 执行入库人用户名快照 |
| `total_base_quantity` | 本次实收基础数量 |
| `total_amount` | 本次实收金额 |
| `remark` | 备注 |
| `create_time` | 执行入库时间 |

### purchase_inbound_receipt_batch

新增实际入库批次表，表示每次执行入库中的实际到货批次。

核心字段：

| 字段 | 含义 |
| --- | --- |
| `receipt_id` | 实际入库主表 ID |
| `purchase_inbound_id` | 采购计划单 ID |
| `purchase_inbound_item_id` | 采购计划明细 ID |
| `sku_id` | SKU ID，来自计划明细 |
| `quantity` | 实收数量，操作单位 |
| `base_quantity` | 实收基础数量 |
| `production_date` | 生产日期 |
| `shelf_life_days` | 保质期天数 |
| `expire_date` | 到期日期 |
| `purchase_price_snapshot` | 审批计划采购价快照 |
| `cost_price_snapshot` | 审批计划成本价快照 |
| `amount` | 本批次实收金额 |
| `supplier_sku_code_snapshot` | 供应商侧商品编码快照 |
| `supplier_sku_name_snapshot` | 供应商侧商品名称快照 |
| `supplier_spec_snapshot` | 供应商侧规格快照 |

价格快照精度与计划明细一致：`purchase_price_snapshot` 用 `DECIMAL(18,6)`，`cost_price_snapshot` 用 `DECIMAL(18,8)`，`amount` 用 `DECIMAL(18,6)`。

### stock_batch

库存批次由实际入库批次生成。

调整规则：

1. 移除 `purchase_inbound_item_id` 列及其唯一约束。库存批次以 `purchase_inbound_receipt_batch_id` 作为唯一采购入库来源关联，计划明细通过 receipt batch 反查，默认不在 stock_batch 冗余保留。
2. 新增 `purchase_inbound_receipt_batch_id`，并建唯一约束，保证一个实际入库批次只生成一个库存批次。
3. `purchase_price` 升级为 `DECIMAL(18,6)`，新增 `cost_price DECIMAL(18,8)`，与计划明细和 receipt batch 的价格精度保持一致，避免出库成本换算时累计误差。
4. `purchase_price` 和 `cost_price` 快照必须从 `purchase_inbound_receipt_batch.purchase_price_snapshot` 与 `purchase_inbound_receipt_batch.cost_price_snapshot` 拷贝，不从执行入库请求或 SKU 当前价格重新取值。

### stock

库存汇总表从单一数量升级为结构化库存数量。

字段口径：

| 字段 | 含义 |
| --- | --- |
| `total_quantity` | 仓内仍存在的实物数量 |
| `available_quantity` | 可出库数量 |
| `locked_quantity` | 锁定数量 |
| `expired_quantity` | 过期隔离数量 |
| `min_stock` | 库存下限，和 `available_quantity` 比较 |
| `max_stock` | 库存上限，和 `total_quantity` 比较 |

旧 `stock.quantity` 语义为总库存（含可用、锁定、过期），迁移时只能作为 `total_quantity` 的来源。`available_quantity / locked_quantity / expired_quantity` 必须从 `stock_batch` 按批次状态聚合重算，不能直接复用旧 `quantity`，否则会把锁定和过期量误计入可用。

## API 设计

### 采购计划 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/purchase-inbounds/drafts` | 创建草稿，只保存计划，不入库 |
| `PUT` | `/api/purchase-inbounds/{id}/plan` | 修改计划，仅 `DRAFT / RETURNED` 允许 |
| `POST` | `/api/purchase-inbounds/{id}/submit` | 提交审批 |
| `POST` | `/api/purchase-inbounds/{id}/approve` | 审批通过 |
| `POST` | `/api/purchase-inbounds/{id}/return` | 退回修改 |
| `POST` | `/api/purchase-inbounds/{id}/cancel` | 取消，要求无实际入库 |
| `POST` | `/api/purchase-inbounds/{id}/close` | 关闭剩余，要求已部分入库 |
| `GET` | `/api/purchase-inbounds/{id}` | 查看详情、计划明细、审批日志、入库记录、累计进度 |
| `GET` | `/api/purchase-inbounds` | 列表，支持状态、供应商、关键字筛选 |

### 执行入库 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/purchase-inbounds/{id}/receipts` | 执行一次实际入库 |

请求只传计划明细 ID 和实际批次：

```json
{
  "items": [
    {
      "purchaseInboundItemId": 10,
      "batches": [
        {
          "quantity": 5,
          "productionDate": "2026-06-01",
          "shelfLifeDays": 180
        }
      ]
    }
  ],
  "remark": "第一批到货"
}
```

后端按 `purchaseInboundItemId` 反查已审批计划明细，计算 `baseQuantity = quantity * conversionRate`，并严格校验累计实收不能超过计划基础数量。价格、SKU、供应商、供应商侧商品快照都来自计划明细，不接受请求覆盖。

## 服务边界

| 服务 | 职责 |
| --- | --- |
| `PurchaseInboundService` | 采购计划、状态流、审批动作、计划校验 |
| `PurchaseInboundReceiptService` | 执行入库、写 receipt、写 receipt batch、推进累计入库进度 |
| `StockService` | 按实际入库批次增加 `available_quantity` 和 `total_quantity`；出库经 `decrease` 同步减少二者；维护 SKU 汇总库存 |
| `StockBatchService` | 库存批次创建、FEFO 消费、过期、锁定、解锁、报损、关闭，并在同一事务内同步 stock 结构量 |
| `StockCheckService` | 盘点落到批次，完成后按 stock_batch 状态重算 SKU 汇总 |
| `OutboundService` | 触发出库，经 `StockService.decrease` 同步扣减 `available_quantity` 和 `total_quantity` |

库存汇总维护边界（结构化数量的关键约束）：

1. `StockBatchService` 执行 `lock / unlock / markExpiredBatches / damage` 时不得只更新 `stock_batch`，必须在同一事务内锁定对应 `stock` 行，并同步维护 `total_quantity / available_quantity / locked_quantity / expired_quantity`。
2. 锁定、解锁、过期扫描不改变 `total_quantity`，只在 `available / locked / expired` 之间转移；报损同时扣减对应状态量和 `total_quantity`。
3. `StockService.decrease`（出库）同步减少 `available_quantity` 和 `total_quantity`。
4. `StockCheckService` 盘点完成后，按 `stock_batch` 各状态分别聚合，重算 `total / available / locked / expired`，不直接改单列。

执行入库事务顺序：

1. 锁定采购单和计划明细。
2. 校验状态为 `APPROVED` 或 `PARTIALLY_INBOUNDED`。
3. 校验每个实收批次不超剩余计划量。
4. 写 `purchase_inbound_receipt`。
5. 逐条写 `purchase_inbound_receipt_batch`。
6. 逐条生成 `stock_batch`，状态为 `AVAILABLE`，价格快照从对应 `purchase_inbound_receipt_batch` 拷贝。
7. 同步 `stock.available_quantity += baseQuantity`。
8. 同步 `stock.total_quantity += baseQuantity`。
9. 写库存日志和批次日志。
10. 更新计划明细累计实收。
11. 同步滚动更新 `purchase_inbound.inbound_total_quantity` 和 `purchase_inbound.inbound_total_amount`。
12. 根据累计进度更新采购单状态，按计划明细逐条判断：当且仅当所有 `purchase_inbound_item` 的 `inbounded_base_quantity` 都达到各自的 `planned_base_quantity` 时，整单进入 `INBOUNDED`；否则进入或保持 `PARTIALLY_INBOUNDED`。该入库进度状态变化不写 `purchase_inbound_approval_log`。

## 库存同步规则

库存字段口径：

```text
total_quantity = available_quantity + locked_quantity + expired_quantity
```

`total_quantity` 不包含已出库、已报损、已关闭。

动作同步：

| 动作 | 批次变化 | 汇总变化 |
| --- | --- | --- |
| 执行入库 | 新建 `AVAILABLE` 批次 | `available += qty`, `total += qty` |
| 出库 FEFO | 只消费 `AVAILABLE` 批次 | `available -= qty`, `total -= qty` |
| 过期扫描 | `AVAILABLE -> EXPIRED` | `available -= qty`, `expired += qty`, `total` 不变 |
| 锁定 | `AVAILABLE -> LOCKED` | `available -= qty`, `locked += qty`, `total` 不变 |
| 解锁未过期 | `LOCKED -> AVAILABLE` | `locked -= qty`, `available += qty`, `total` 不变 |
| 解锁已过期 | `LOCKED -> EXPIRED` | `locked -= qty`, `expired += qty`, `total` 不变 |
| 部分报损 | 原状态保持，仅扣批次数量 | 扣对应状态量和 `total` |
| 整批报损 | 扣到 0 后 `DAMAGED` | 扣对应状态量和 `total` |
| 盘点调整 | 调整具体批次数量或状态 | 按批次事实重算 SKU 汇总 |

库存预警：

```text
LOW: available_quantity < min_stock
HIGH: total_quantity > max_stock
NORMAL: 其他情况
```

若同时满足低库存和高库存，建议返回复合状态或优先返回 `LOW` 并在 VO 中增加布尔字段。首期可保持单字段，优先级设为 `LOW` 高于 `HIGH`，因为缺货更影响履约。

## 核心校验

1. 只有 `DRAFT` 和 `RETURNED` 能修改采购计划。
2. 只有 `APPROVED` 和 `PARTIALLY_INBOUNDED` 能执行入库。
3. 累计实收基础数量严格不能超过计划基础数量。
4. 普通入库不能新增计划外 SKU、不能改供应商、不能改价格。
5. `CANCELLED` 要求没有任何入库记录。
6. `CLOSED` 要求已有部分入库且尚未全部入库。
7. 供应商更换只允许 `DRAFT` 和 `RETURNED`，保存或重新提交时必须重校验全部 SKU 绑定和采购规则。
8. 供应商侧商品快照在计划明细保存，执行入库和库存批次沿用该快照。
9. 计划金额和实收金额都使用高精度金额字段保存，展示层负责格式化。
10. 盘点调整必须落到批次，不能直接改库存汇总跳过批次事实。

## 前端交互

采购入库页面从“新增即完成”升级为状态驱动工作台：

1. 列表展示状态、计划数量、已入库数量、计划金额、实收金额、创建人、审批人、最近动作时间。
2. `DRAFT` 支持编辑、提交、取消。
3. `SUBMITTED` 支持审批通过、退回、取消。
4. `RETURNED` 支持编辑计划、重新提交、取消。
5. `APPROVED` 支持执行入库、取消。
6. `PARTIALLY_INBOUNDED` 支持继续入库、关闭剩余。
7. `INBOUNDED / CANCELLED / CLOSED` 只读。
8. 详情页展示计划明细、累计实收进度、审批日志、每次实际入库记录和实际批次。
9. 执行入库弹窗只允许选择已审批计划明细并录入实际批次数量、生产日期、保质期。
10. 执行入库弹窗显示剩余可入库数量，不显示可编辑价格。

库存页面升级：

1. 库存列表展示总库存、可用库存、锁定库存、过期库存。
2. 预警下限按可用库存判断，上限按总库存判断。
3. 批次列表展示批次来源 receipt batch，方便从库存追溯到采购计划和执行入库。

## 测试策略

服务单元测试：

1. 状态流：草稿提交、审批通过、退回再提交、取消、关闭、非法状态流转。
2. 编辑边界：`RETURNED` 可改采购计划，不能改审批痕迹、单据身份、入库结果和库存结果。
3. 供应商变更：`RETURNED` 可换供应商，并重校验全部 SKU 绑定和采购规则。
4. 审批冻结：`APPROVED` 后禁止修改供应商、明细、数量和价格。
5. 部分入库：多次 receipt 累计推进 `PARTIALLY_INBOUNDED` 到 `INBOUNDED`。
6. 严格禁止超计划入库。
7. 执行入库不能传入计划外 SKU，不能改价格。

Mapper 测试：

1. 新增字段写入和读取。
2. 审批日志查询。
3. receipt 和 receipt batch 插入、查询、详情聚合。
4. 库存汇总字段映射和预警状态计算。

库存服务测试：

1. 执行入库增加 `available_quantity` 和 `total_quantity`。
2. 出库 FEFO 只消费 `AVAILABLE`，同步扣 `available_quantity` 和 `total_quantity`。
3. 过期扫描 `available -> expired`，`total_quantity` 不变。
4. 锁定和解锁同步库存结构量，`total_quantity` 不变。
5. 报损按原批次状态扣对应状态量和 `total_quantity`。
6. 部分报损保持状态，整批报损到 0 后变为 `DAMAGED`。
7. 盘点按批次调整后重算 SKU 汇总。

前端交互测试：

1. 状态按钮显示和禁用逻辑。
2. 草稿编辑、退回修改、重新提交。
3. 审批通过、退回、取消、关闭。
4. 多批次执行入库。
5. 详情页进度、审批日志、入库记录展示。

SQL 迁移验收：

1. 旧 `purchase_inbound.total_quantity` 正确迁移为 `planned_total_quantity`。
2. 旧 `purchase_inbound.total_amount` 正确迁移为 `planned_total_amount`。
3. 旧 `stock.quantity` 仅迁移为 `total_quantity`；`available / locked / expired` 由 `stock_batch` 按状态聚合得到，三者之和等于 `total_quantity`。
4. `stock_batch.purchase_inbound_item_id` 列被移除，来源关联改为 `purchase_inbound_receipt_batch_id`。
5. `stock_batch.purchase_inbound_receipt_batch_id` 唯一约束生效。
6. 新增外键和索引满足计划、实际入库、库存批次之间的追溯。

## 开发顺序建议

1. SQL 迁移和实体/VO/DTO 字段升级。
2. 采购状态机和审批日志。
3. 采购计划保存、修改、提交、审批、退回、取消、关闭。
4. 实际入库 receipt 和 receipt batch。
5. 库存汇总字段升级和实际入库生成库存批次。
6. 出库、过期、锁定、解锁、报损、盘点同步新库存口径。
7. 前端采购入库工作台升级。
8. 前端库存列表和批次追溯升级。

## 非目标

1. 本次不实现普通入库环节改价。价格变化走独立变更审批。
2. 本次不允许超收容差。
3. 本次不允许普通入库新增计划外 SKU。
4. 本次不把 `RETURNED` 退回修改伪装为新的 `DRAFT`。
