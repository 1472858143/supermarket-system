# 03 - 商品 SKU 规格与单位换算升级

> 记录时间：2026-05-31
> 项目阶段：优化阶段 — 将简单商品模型升级为 SPU + SKU + 单位换算 + 按 SKU 库存管理

---

## 一、背景与动机

原系统中商品模型为单层结构（product 表直接包含编号、名称、分类、进价、售价、状态），存在以下局限：

- 同一商品不同规格（如 500ml / 1L）无法独立管理
- 无 SKU 概念，无法支持条码扫描、多规格定价
- 无单位换算能力，无法支持"按箱采购、按瓶销售"的业务场景
- 库存按商品主体管理，无法按规格区分库存
- 入库/出库单据无单位信息，无法追溯实际操作单位

---

## 二、设计决策

| 决策点 | 方案 | 理由 |
|---|---|---|
| SKU 编码 | 系统自动生成，格式 `{product_code}-{NNN}` | 减少人工输入错误，编码与商品编号关联便于识别 |
| 单位换算层级 | 绑定到 SKU | 同一商品不同规格可能有不同装箱量 |
| 历史单据处理 | 迁移脚本关联到默认 SKU | 旧数据不丢失，系统完整切换 |
| 默认 SKU | 每个旧商品自动生成一个 | 兼容旧数据，平滑过渡 |
| 升级策略 | 4 阶段渐进式 | 每阶段可独立验证和回滚 |
| 创建商品与 SKU 边界 | 商品创建不录入价格，SKU 中管理 | 职责清晰，商品是主体资料，SKU 是规格与价格载体 |
| 库存数量类型 | 保持 INT，按基础单位存储 | 满足超市场景，无需小数精度 |
| 单据快照 | 保存 unit + conversion_rate | 历史单据不受后续单位配置变更影响 |

---

## 三、升级阶段总览

| 阶段 | 内容 | 核心变更 |
|---|---|---|
| 1 | 数据层准备 | 新建 sku / sku_unit_conversion 表；现有表加 sku_id 列；旧数据迁移 |
| 2 | 商品模块升级 | SKU CRUD + 单位换算管理；product 移除价格字段 |
| 3 | 库存模块升级 | stock/stock_log 从 product_id 切换到 sku_id；入库/出库/盘点加过渡代码 |
| 4 | 入库/出库/盘点升级 | 单据关联 SKU + 单位选择 + 换算率快照；移除过渡代码 |

---

## 四、数据库变更

### 新增表

| 表名 | 说明 |
|---|---|
| `sku` | SKU 规格表（product_id, sku_code, sku_name, spec, barcode, base_unit, purchase_price, sale_price, status, is_default） |
| `sku_unit_conversion` | 单位换算表（sku_id, unit_name, conversion_rate） |

### 修改表

| 表名 | 变更内容 |
|---|---|
| `product` | 移除 purchase_price、sale_price 列 |
| `stock` | product_id → sku_id（新增列、迁移数据、删旧列） |
| `stock_log` | product_id → sku_id |
| `inbound_order` | product_id → sku_id；新增 unit、conversion_rate、base_quantity |
| `outbound_order` | 同 inbound_order |
| `stock_check` | product_id → sku_id |

### 迁移脚本

- `system/sql/03_add_sku_tables.sql` — 阶段 1 全部 DDL + DML

---

## 五、后端变更

### 新增文件

```
sku/
├── controller/SkuController.java       SKU + 单位换算 REST 端点
├── dto/SkuRequest.java                 SKU 创建/编辑请求
├── dto/UnitConversionRequest.java      单位换算请求
├── entity/Sku.java                     SKU 实体
├── entity/SkuUnitConversion.java       单位换算实体
├── mapper/SkuMapper.java               SKU 数据访问
├── mapper/UnitConversionMapper.java    单位换算数据访问
├── service/SkuService.java             SKU 业务逻辑
├── vo/SkuVO.java                       SKU 视图对象
└── vo/UnitConversionVO.java            单位换算视图对象
```

### 新增 API 端点

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| GET | /api/products/{productId}/skus | 登录后 | 列出商品下所有 SKU |
| POST | /api/products/{productId}/skus | ADMIN | 新增 SKU |
| PUT | /api/products/{productId}/skus/{skuId} | ADMIN | 编辑 SKU |
| DELETE | /api/products/{productId}/skus/{skuId} | ADMIN | 删除 SKU（禁删默认） |
| GET | /api/products/{productId}/skus/{skuId}/units | 登录后 | 列出单位换算 |
| POST | /api/products/{productId}/skus/{skuId}/units | ADMIN | 新增单位换算 |
| PUT | /api/products/{productId}/skus/{skuId}/units/{unitId} | ADMIN | 编辑单位换算 |
| DELETE | /api/products/{productId}/skus/{skuId}/units/{unitId} | ADMIN | 删除单位换算 |

### 修改文件

| 文件 | 变更内容 |
|---|---|
| Product.java | 移除 purchasePrice / salePrice |
| ProductRequest.java | 移除 purchasePrice / salePrice |
| ProductVO.java | 移除价格，新增 List\<SkuVO\> skus |
| ProductMapper.java | SQL 移除 price 列 |
| ProductService.java | create 不再处理价格和库存初始化；delete 检查 SKU 引用 |
| Stock.java | productId → skuId |
| StockMapper.java | 所有 SQL 从 product_id 切换为 sku_id，JOIN sku → product |
| StockVO.java | 新增 skuId/skuCode/skuName/spec/baseUnit |
| StockService.java | 所有方法签名从 productId 改为 skuId |
| StockController.java | 路径参数改为 skuId |
| InboundRequest.java | productId → skuId，新增 unit/conversionRate |
| InboundService.java | 单位换算计算 + stockService.increase(skuId, baseQuantity) |
| InboundMapper.java | SQL 改为 JOIN sku → product，INSERT 新增字段 |
| InboundVO.java | 新增 skuId/skuCode/skuName/unit/conversionRate/baseQuantity |
| OutboundRequest.java | 同 Inbound |
| OutboundService.java | 同 Inbound |
| OutboundMapper.java | 同 Inbound |
| OutboundVO.java | 同 Inbound |
| StockCheckRequest.java | productId → skuId |
| StockCheckService.java | 按 skuId 操作 |
| StockCheckMapper.java | SQL 改为 JOIN sku → product |
| StockCheckVO.java | 新增 skuId/skuCode/skuName |
| ReportMapper.java | warningStocks JOIN sku；入库/出库统计改用 base_quantity |

---

## 六、前端变更

### 新增文件

| 文件 | 说明 |
|---|---|
| api/sku.js | SKU CRUD + 单位换算 CRUD 接口 |
| SKU 管理面板/组件 | 商品详情中管理 SKU 列表 + 单位换算 |
| SkuSelector 组件 | 两级联动：选商品 → 选 SKU |
| 单位选择器 | 基础单位 + 换算单位下拉 |

### 修改文件

| 文件 | 变更内容 |
|---|---|
| ProductsView.vue | 移除进价/售价列；新增 SKU 管理入口 |
| StocksView.vue | 按 SKU 维度展示；新增 SKU 编码/规格/基础单位列 |
| InboundsView.vue | 两级联动选 SKU + 单位选择 + 换算显示 |
| OutboundsView.vue | 同 Inbound |
| StockchecksView.vue | 两级联动选 SKU |
| ReportsView.vue | 预警列表增加 SKU 信息 |
| api/stock.js | 参数从 productId 改为 skuId |

---

## 七、业务规则汇总

| 规则 | 说明 |
|---|---|
| 默认 SKU 不可删除 | is_default = 1 时拒绝删除操作 |
| SKU 编码自动生成 | 格式 `{product_code}-{NNN}`，N 递增 |
| 售价 >= 进价 | 在 SKU 层校验 |
| 库存按基础单位存储 | 所有换算在写入前完成 |
| 单据快照不可变 | unit + conversion_rate 写入后不随配置变更 |
| 入库/出库换算 | baseQuantity = quantity × conversionRate |
| 商品首个 SKU 自动标记默认 | 首次创建的 SKU 自动设 is_default = 1 |
| 删除商品前校验 | 所有 SKU 无单据引用才允许删除 |
| 同一 SKU 下单位名称唯一 | UNIQUE(sku_id, unit_name) |

---

## 八、文件清单汇总

| 类型 | 数量 | 说明 |
|---|---|---|
| 新增（后端） | ~10 | SKU 模块全套（entity/dto/vo/mapper/service/controller） |
| 新增（前端） | ~4 | api/sku.js + SKU 管理组件 + SKU 选择器 + 单位选择器 |
| 新增（SQL） | 1 | 03_add_sku_tables.sql |
| 修改（后端） | ~23 | product/stock/inbound/outbound/stockcheck/report 模块 |
| 修改（前端） | ~7 | 商品/库存/入库/出库/盘点/报表页面 + stock API |
| 总影响 | ~45 | — |

---

## 九、升级后系统数据模型

```
product (SPU)  ──1:N──  sku  ──1:N──  sku_unit_conversion
                          │
                          └──1:1──  stock
                          │
                          └──1:N──  inbound_order (含 unit/conversion_rate/base_quantity 快照)
                          └──1:N──  outbound_order (同上)
                          └──1:N──  stock_check
                          └──1:N──  stock_log
```

数据库表从 10 张增至 12 张（新增 sku、sku_unit_conversion）。

---

*本次升级完成后，系统具备完整的 SPU-SKU 模型、多规格管理、单位换算、按 SKU 粒度的库存管理能力。*
