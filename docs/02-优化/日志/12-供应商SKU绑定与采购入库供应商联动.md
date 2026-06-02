# 12 - 供应商SKU绑定与采购入库供应商联动

> 记录时间：2026-06-02
> 项目阶段：优化阶段 — 建立供应商与 SKU 的供货关系，并让采购入库强制按供应商选货

---

## 一、背景与目标

供应商已作为独立主数据落地（见 `05-供应商基础管理模块新增`），但与 SKU 之间还没有供货关系，采购入库也未关联供应商。本次优化解决：

- 一个供应商能供哪些 SKU
- 供应商侧的商品编码/名称/规格、默认采购价、最小采购量如何维护
- 采购入库时如何强制"先选供应商，再从该供应商启用绑定中选 SKU"

目标是建立 `供应商 ↔ SKU` 多对多供货关系，并把采购入库的 SKU 来源收敛到"当前供应商已启用绑定"这一范围内。

---

## 二、设计决策

| 决策点 | 方案 | 理由 |
|---|---|---|
| 供货关系建模 | 独立绑定表 `supplier_sku`（supplier_id + sku_id） | 多对多关系，承载供应商侧商品信息与采购参数 |
| 唯一约束 | `uk_supplier_sku (supplier_id, sku_id)` | 同一供应商对同一 SKU 只允许一条绑定 |
| 绑定不可改 SKU | 编辑时只改供应商侧字段与状态，不改 supplier/sku | 避免改变绑定身份导致数据语义混乱 |
| 采购入库关联供应商 | `purchase_inbound` 增加 `supplier_id NOT NULL` + 外键 | 采购单必须归属供应商，便于追溯与统计 |
| SKU 选择范围 | 仅该供应商 `status=1` 的启用绑定 | 防止采购到未授权供应的 SKU |
| 默认采购价 | 选中绑定后带出 `default_purchase_price` | 减少人工录入，价格可在明细上覆盖 |
| 最小采购量 | 绑定上设 `min_purchase_quantity`，前后端双重校验 | 满足供应商起订量约束 |
| 删除策略 | 已被采购入库引用的绑定禁止删除，提示改为禁用 | 保护历史单据引用完整性 |
| 采购单位 | 沿用 SKU 既有单位/换算规则（`UnitSelector`） | 不在绑定层重复定义单位体系 |

---

## 三、数据库变更

### 新增表：supplier_sku

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| supplier_id | BIGINT NOT NULL | 供应商ID（FK → supplier.id） |
| sku_id | BIGINT NOT NULL | SKU ID（FK → sku.id） |
| supplier_sku_code | VARCHAR(80) NOT NULL | 供应商侧商品编码 |
| supplier_sku_name | VARCHAR(120) NOT NULL | 供应商侧商品名称 |
| supplier_spec | VARCHAR(120) | 供应商侧规格 |
| default_purchase_price | DECIMAL(10,2) NOT NULL | 默认采购价（CHECK ≥ 0） |
| min_purchase_quantity | INT NOT NULL DEFAULT 1 | 最小采购量（CHECK > 0） |
| status | TINYINT NOT NULL DEFAULT 1 | 1 启用，0 禁用（CHECK IN (0,1)） |
| create_time / update_time | DATETIME | 创建/更新时间 |

约束与索引：`uk_supplier_sku(supplier_id, sku_id)`、`idx_supplier_sku_supplier_status(supplier_id, status)`、`idx_supplier_sku_sku(sku_id)`、外键 `fk_supplier_sku_supplier`、`fk_supplier_sku_sku`。

### 变更表：purchase_inbound

- 新增 `supplier_id BIGINT NOT NULL`（位于 `id` 之后）
- 新增索引 `idx_purchase_inbound_supplier(supplier_id, id)`
- 新增外键 `fk_purchase_inbound_supplier` → `supplier(id)`

### 迁移脚本

- 新增 `system/sql/14_add_supplier_sku_binding.sql`
- `system/sql/00_终版.sql` 已同步完整库结构

迁移执行详情见第六节。

---

## 四、后端变更

### 新增文件（supplier_sku 模块）

```
supplier/
├── entity/SupplierSku.java              绑定实体
├── vo/SupplierSkuVO.java                绑定响应（含 SKU/商品/供应商侧/单位换算信息）
├── dto/SupplierSkuRequest.java          绑定新增/编辑请求
├── mapper/SupplierSkuMapper.java        JdbcTemplate 数据访问
├── service/SupplierSkuService.java      校验与 VO 组装
└── controller/SupplierSkuController.java 供应商视角绑定接口
```

### 修改文件（采购入库强制关联供应商）

```
purchaseinbound/
├── dto/PurchaseInboundRequest.java      新增 supplierId（@NotNull）
├── entity/PurchaseInbound.java          新增 supplierId
├── vo/PurchaseInboundVO.java            新增 supplierId/supplierCode/supplierName
├── mapper/PurchaseInboundMapper.java    插入与查询关联供应商、列表搜索按供应商
└── service/PurchaseInboundService.java  创建时校验供应商、绑定状态、最小采购量
```

### 新增 API 端点

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| GET | /api/suppliers/{supplierId}/skus | 登录后 | 供应商全部供货 SKU |
| GET | /api/suppliers/{supplierId}/skus/enabled | 登录后 | 该供应商启用绑定（采购入库选货用） |
| POST | /api/suppliers/{supplierId}/skus | ADMIN | 新增绑定 |
| PUT | /api/suppliers/{supplierId}/skus/{bindingId} | ADMIN | 编辑绑定 |
| DELETE | /api/suppliers/{supplierId}/skus/{bindingId} | ADMIN | 删除绑定 |

### 后端业务规则

| 规则 | 说明 |
|---|---|
| 重复绑定拒绝 | 同一供应商已绑定该 SKU 时拒绝新增 |
| 编辑不改身份 | 更新只改供应商侧字段与状态，不改 supplier/sku |
| 删除引用保护 | 该绑定已有采购入库记录时禁止删除，提示改为禁用 |
| 启用绑定列表 | `listEnabled` 在供应商被禁用时拒绝返回 |
| 采购单供应商必填 | 创建采购入库时 supplierId 为空直接拒绝（写单前） |
| 供应商启用校验 | 供应商禁用时拒绝创建采购入库 |
| 绑定启用校验 | SKU 未绑定当前供应商或绑定禁用时拒绝 |
| 最小采购量校验 | 明细数量低于绑定 `min_purchase_quantity` 时拒绝（写单前） |

---

## 五、前端变更

### 修改文件

| 文件 | 变更内容 |
|---|---|
| `system/frontend/src/api/supplier.js` | 新增 5 个绑定接口：列表 / 启用列表 / 新增 / 编辑 / 删除 |
| `system/frontend/src/views/supplier/SuppliersView.vue` | 供应商行新增"供货SKU"入口，弹出绑定清单与新增/编辑/删除表单 |
| `system/frontend/src/views/purchaseinbound/PurchaseInboundsView.vue` | 采购入库先选供应商，SKU 只能从启用绑定中选，自动带出默认采购价，列表/详情展示供应商 |

### 页面能力

**供应商页（Task 5）**

- 供应商行"供货SKU"入口，打开该供应商的绑定清单
- 绑定清单展示 SKU 编码/名称、商品名称、供应商侧编码/名称/规格、默认采购价、最小采购量、状态
- 新增绑定：通过商品 → SKU 级联选择器选 SKU，填写供应商侧信息与采购参数
- 编辑绑定：隐藏 SKU 选择器（不可改 SKU），只改供应商侧字段与状态
- 启用/禁用通过状态字段维护；重复绑定、删除失败等由后端文案透出到消息条

**采购入库页（Task 6）**

- 新增弹窗先选供应商；切换供应商时重置明细
- SKU 下拉只来自该供应商 `/enabled` 启用绑定列表
- 选中 SKU 自动带出默认采购价，单位/换算沿用 `UnitSelector`
- 前端校验"请选择供应商"与"数量不能低于最小采购量"
- 提交 payload 增加 `supplierId`；列表新增"供应商"列，详情展示供应商编码/名称
- 因 SKU 选择改为按供应商绑定，移除了页面内不再使用的 `SkuSelector`/`listProducts` 引用

---

## 六、数据库迁移执行

迁移在 dev 配置库执行：

```text
jdbc:mysql://127.0.0.1:3306/market
username=root
```

### 与脚本预期的偏差与处理

`14_add_supplier_sku_binding.sql` 的前置条件是 `purchase_inbound` 为空（脚本内用存储过程断言，非空即中止）。本次执行时开发库已存在 **6 条历史采购入库单**（含 7 条明细），并已级联出 5 条 `stock_batch`、17 条 `stock_batch_log`、4 条 `stock_check_item`。直接清空会波及整个库存事务链路。

因此本次采用**回填方案**（零数据丢失），执行等价于：

1. 创建 `supplier_sku` 表
2. `purchase_inbound` 增加**可空** `supplier_id`
3. `UPDATE purchase_inbound SET supplier_id = 1`（历史单统一归到「测试供应商」，id=1）
4. 将 `supplier_id` 改为 `NOT NULL`，并补充索引与外键

> 该回填为本开发库的一次性数据处理，**未修改**已提交的 `14_add_supplier_sku_binding.sql`——脚本对空库/全新环境仍然正确。

### 执行结果验证

| 验证项 | 结果 |
|---|---|
| `supplier_sku` 表 | 已创建（唯一键、索引、外键、CHECK 约束齐全） |
| `purchase_inbound.supplier_id` | `bigint NOT NULL`，位于 `id` 之后 |
| 历史单回填 | 6 条全部 `supplier_id = 1` |
| 外键 `fk_purchase_inbound_supplier` | 已建，指向 `supplier(id)` |
| 索引 `idx_purchase_inbound_supplier` | 已建 |
| 库存批次/流水/盘点数据 | 全部保留，未删 |

---

## 七、测试与验证

| 验证项 | 结果 |
|---|---|
| `mvn test -Dtest=SupplierSkuServiceTest,SupplierSkuMapperTest,PurchaseInboundServiceTest,PurchaseInboundMapperTest` | 通过，52 个测试，BUILD SUCCESS |
| `npm run build` | 通过（141 模块） |
| 数据库迁移 | 执行成功并验证（见第六节） |

> 本会话聚焦 Task 5/6 前端与迁移执行，未重跑全量 `mvn test`；手动浏览器冒烟测试（登录 → 维护绑定 → 采购入库按供应商选货）尚未执行。

相关后端测试覆盖：

| 测试文件 | 覆盖内容 |
|---|---|
| `SupplierSkuMapperTest` | JOIN SKU/商品、启用过滤、插入/更新字段、采购引用计数 SQL |
| `SupplierSkuServiceTest` | 重复绑定拒绝、字段修整与默认值、跨供应商越权、引用保护、采购价校验 |
| `PurchaseInboundServiceTest` | 供应商必填/禁用/最小采购量校验、写单前拦截 |
| `PurchaseInboundMapperTest` | 供应商 JOIN、列表搜索参数、插入 supplier_id |

---

## 八、文件清单汇总

| 类型 | 数量 | 说明 |
|---|---|---|
| 新增（SQL） | 1 | `14_add_supplier_sku_binding.sql` |
| 修改（SQL） | 1 | `00_终版.sql` |
| 新增（后端） | 6 | supplier_sku 模块全套 |
| 新增（后端测试） | 2 | SupplierSku service + mapper 测试 |
| 修改（后端） | 5 | purchaseinbound 强制关联供应商 |
| 修改（后端测试） | 2 | purchaseinbound service + mapper 测试 |
| 修改（前端） | 3 | supplier api + 供应商页 + 采购入库页 |
| 总影响 | 22 | — |

---

## 九、后续可扩展方向

- 历史采购单供应商回填的精确化（按真实供应来源逐单修正，而非统一归 id=1）
- 供应商侧采购价历史与价格变更审计
- 绑定批量导入/导出
- 采购入库按供应商的采购金额、到货及时率等统计分析

---

*本次升级完成后，系统建立了供应商与 SKU 的供货绑定关系，采购入库强制按供应商选择启用绑定 SKU 并自动带出默认采购价，采购单据可追溯到供应商。*
