# 报表统计模块升级设计

## 目标

升级现有报表统计模块，在保留当前系统外壳和旧报表接口兼容性的前提下，实现三个报表页签：

- 数据看板：核心指标卡片、采购金额趋势图、出库数量趋势图、低库存预警、临期/过期预警。
- 库存分析：库存汇总、库存流水、批次效期。
- 采购分析：采购概况、供应商分析、采购明细。

## 已确认设计决策

1. 报表仍使用现有 `/reports` 单入口，不新增主菜单路由。
2. `/reports` 内部拆为 `数据看板 / 库存分析 / 采购分析` 三个 Tab。
3. 默认统计周期为近 30 天，页面顶部提供日期范围筛选。
4. 核心指标、采购金额趋势、出库数量趋势、采购明细跟随日期范围。
5. 低库存预警、临期/过期预警按实时库存和实时批次状态展示，不受日期范围限制。
6. 采购金额主口径为实际入库金额；计划采购金额仅作为辅助展示字段。
7. 临期默认阈值为 30 天，前端可切换 7 / 15 / 30 / 60 天并重新查询。
8. 图表第一版使用原生 SVG/CSS，不新增 ECharts 等图表依赖。
9. 库存流水主口径使用 `stock_log`，展示 SKU 总库存变化；批次追溯放在批次效期视图中，不混入库存流水主表。

## 视觉与样式边界

页面背景、侧边栏、顶部 header、面包屑、菜单高亮继续使用当前 Vue 系统的 `AdminLayout.vue` 和全局样式，不替换全局主题。

报表页内部参考 `system/pages` 中数据报表原型页的视觉语言：

- `sub-tabs`：用于报表一级 Tab 和库存/采购二级分段。
- `kpi-row`：用于核心指标卡片。
- `card` / `card-head` / `title-block`：用于图表、预警、表格区域。
- `date-range`：用于 7 天 / 30 天 / 季度 / 年等快速日期筛选。
- `pill`：用于低库存、临期、过期、状态标签。
- `table-host + t` 表格风格：用于库存汇总、库存流水、批次效期、供应商分析、采购明细。
- 原生 SVG 图表样式：折线、虚线辅助线、面积填充、坐标轴刻度。

样式实现采用局部作用域，不覆盖全局 `.card`、`.btn`、`.sub-tabs` 等类，避免影响库存中心、采购入库、商品管理等已有页面。推荐使用 `report-*` 前缀或 Vue scoped 样式承载迁移后的报表页内部样式。

字体策略：

- 中文标题与正文沿用当前系统字体栈。
- 指标数字、金额、数量、图表刻度参考原型页数字风格，优先使用 `"Orbitron", "DM Mono", Consolas, monospace` 这类 fallback 字体栈。
- 不依赖外网字体下载，避免构建或运行受网络影响。

## 后端接口设计

在现有 `com.supermarket.inventory.report` 包内升级。保留旧接口，保证首页概览继续可用：

- `GET /api/reports/stock`
- `GET /api/reports/inbound`
- `GET /api/reports/outbound`
- `GET /api/reports/warning`

新增只读报表接口：

### 数据看板

`GET /api/reports/dashboard?startDate&endDate&expiryWarningDays=30`

返回：

- 核心指标：实际采购金额、计划采购金额、出库总数量、库存 SKU 数、低库存数、临期批次数、过期批次数。
- 采购金额趋势：按日期聚合实际入库金额，辅助展示计划采购金额。
- 出库数量趋势：按日期聚合 `outbound_order.base_quantity`。
- 低库存预警：基于 `stock.total_quantity < stock.min_stock`。
- 临期/过期预警：基于 `stock_batch.expire_date` 和 `quantity > 0`。

### 库存分析

`GET /api/reports/inventory/summary?keyword&category&stockStatus&page&pageSize`

库存汇总，基于：

- `stock`
- `sku`
- `product`
- `category`

主要字段：

- 商品编号、商品名称
- SKU 编码、SKU 名称、规格、基础单位
- 当前库存、可用库存、锁定库存、过期库存
- 库存上下限
- 库存状态：正常、低库存、超上限、售罄
- 最近更新时间

`GET /api/reports/inventory/ledger?startDate&endDate&changeType&keyword&page&pageSize`

库存流水，基于 `stock_log`，关联 `sku`、`product`、`category` 补充展示字段。

主要字段：

- 时间
- 变动类型
- 变动数量
- 变动前库存
- 变动后库存
- SKU / 商品信息

`GET /api/reports/inventory/batches?expiryWarningDays&status&keyword&page&pageSize`

批次效期，基于：

- `stock_batch`
- `sku`
- `product`
- `purchase_inbound_receipt_batch`

主要字段：

- 批次号
- 商品 / SKU
- 剩余数量、原始数量
- 批次状态
- 生产日期、到期日期、剩余天数
- 效期状态：`NORMAL / NEAR_EXPIRY / EXPIRED`
- 采购价快照、成本价快照

### 采购分析

`GET /api/reports/purchase/overview?startDate&endDate`

采购概况，实际入库金额为主口径，计划采购金额辅助展示。

主要字段：

- 实际采购金额：基于 `purchase_inbound_receipt.total_amount`
- 计划采购金额：基于 `purchase_inbound.planned_total_amount`
- 实际入库数量：基于 `purchase_inbound_receipt.total_base_quantity`
- 采购单数、实际入库单数
- 待入库 / 部分入库 / 已入库数量
- 采购金额趋势

`GET /api/reports/purchase/suppliers?startDate&endDate&page&pageSize`

供应商分析，基于：

- `purchase_inbound_receipt`
- `purchase_inbound`
- `supplier`
- `purchase_inbound_receipt_batch`

主要字段：

- 供应商编码、供应商名称
- 实际采购金额
- 计划采购金额
- 实际入库单数
- 采购 SKU 数
- 金额占比
- 最近入库时间

`GET /api/reports/purchase/details?startDate&endDate&supplierId&keyword&page&pageSize`

采购明细，基于实际入库批次 `purchase_inbound_receipt_batch`，向上关联入库单、采购单、供应商、SKU。

主要字段：

- 实际入库单号
- 采购单号
- 供应商
- 商品 / SKU
- 实收数量、基础数量
- 实际入库金额
- 采购价快照、成本价快照
- 生产日期、到期日期
- 入库时间

## 数据口径

### 日期范围

服务层负责日期默认值。`startDate` / `endDate` 缺失时默认近 30 天。日期范围应用于趋势、采购概况、供应商分析、采购明细、库存流水。

### 采购金额

主口径为实际入库金额：

- 数据来源：`purchase_inbound_receipt.total_amount`
- 用途：核心指标、采购金额趋势、采购概况、供应商分析、采购明细

辅助口径为计划采购金额：

- 数据来源：`purchase_inbound.planned_total_amount`
- 用途：指标辅助字段、趋势辅助线、采购概况对比

取消、退回、未入库的采购计划不计入实际采购金额。

### 出库数量

出库数量趋势基于 `outbound_order.base_quantity`，按基础单位统计，按出库单 `create_time` 聚合。

### 库存预警

低库存预警基于实时库存：

- 低库存：`stock.total_quantity < stock.min_stock`
- 售罄：`stock.total_quantity = 0`
- 超上限：`stock.total_quantity > stock.max_stock`

低库存预警不受日期范围限制。

### 临期/过期预警

基于 `stock_batch` 中有剩余数量的批次：

- 过期：`expire_date < CURRENT_DATE`
- 临期：`CURRENT_DATE <= expire_date <= CURRENT_DATE + expiryWarningDays`
- 默认 `expiryWarningDays = 30`

批次状态需结合 `quantity > 0`，避免已耗尽批次产生无意义预警。

## 前端结构

### API 层

修改 `src/api/report.js`，新增接口函数：

- `getReportDashboard(params)`
- `getInventoryReportSummary(params)`
- `getInventoryReportLedger(params)`
- `getInventoryReportBatches(params)`
- `getPurchaseReportOverview(params)`
- `getPurchaseReportSuppliers(params)`
- `getPurchaseReportDetails(params)`

保留现有函数：

- `getStockReport()`
- `getInboundReport()`
- `getOutboundReport()`
- `getWarningReport()`

### 页面与组件

`src/views/report/ReportsView.vue`

负责：

- 页面标题与公共筛选
- 一级 Tab：数据看板、库存分析、采购分析
- 日期范围、临期阈值
- 当前 Tab 懒加载
- 全局加载与错误状态

`src/views/report/components/ReportKpiCard.vue`

负责指标卡展示，内部参考原型页 `kpi` 风格。

`src/views/report/components/ReportTrendChart.vue`

负责原生 SVG 趋势图，支持：

- 单条折线
- 主折线 + 辅助虚线
- 面积填充
- 空态
- 坐标轴标签

`src/views/report/components/DashboardPanel.vue`

负责数据看板：

- 核心指标卡片
- 采购金额趋势
- 出库数量趋势
- 低库存预警
- 临期/过期预警

`src/views/report/components/InventoryAnalysisPanel.vue`

负责库存分析：

- 二级分段：库存汇总、库存流水、批次效期
- 各分段筛选与分页
- 表格空态和加载态

`src/views/report/components/PurchaseAnalysisPanel.vue`

负责采购分析：

- 二级分段：采购概况、供应商分析、采购明细
- 采购概况指标与趋势
- 供应商排行/表格
- 采购明细分页表格

`src/views/report/reportFormat.js`

负责格式化与归一化：

- 金额
- 数量
- 日期
- 预警状态
- 趋势点数据
- 空值兜底

## 交互设计

1. 首次进入 `/reports` 时默认打开 `数据看板`，自动请求 `/api/reports/dashboard`。
2. 切换一级 Tab 时懒加载当前 Tab 数据，避免一次拉取所有分页报表。
3. 日期范围变化后刷新数据看板、采购分析、库存流水。
4. 临期阈值变化后刷新数据看板中的效期预警和库存分析中的批次效期。
5. 库存汇总、库存流水、批次效期、供应商分析、采购明细均采用服务端分页。
6. 每个表格保留加载态、空态、错误态，不用弹窗承载普通报表错误。

## 错误处理

后端所有新增接口沿用现有 `ApiResponse`。

服务层规则：

- 日期缺失时默认近 30 天。
- `expiryWarningDays` 缺失时默认 30。
- `expiryWarningDays` 非法时归一为 30；合法范围为 1 到 365。
- 聚合结果为空时返回 0 和空数组。
- 分页接口统一返回现有 `PageResult`。
- 分页参数缺失时默认 `page = 1`、`pageSize = 20`。
- `pageSize` 最大值为 100，超过时按 100 处理。

前端规则：

- 接口错误显示页面内错误提示。
- 图表无数据时显示空态，不渲染空坐标轴。
- 指标字段缺失时显示 0 或 `--`。
- 表格字段缺失时显示 `-`。

## 测试策略

### 后端测试

优先补充 `ReportMapperTest`，覆盖 SQL 关键口径：

- 实际采购金额必须来自 `purchase_inbound_receipt.total_amount`。
- 计划采购金额必须来自 `purchase_inbound.planned_total_amount`。
- 供应商分析必须 join `purchase_inbound + supplier`。
- 库存流水必须来自 `stock_log`。
- 批次效期必须使用 `stock_batch.expire_date`。
- 出库趋势必须使用 `outbound_order.base_quantity`。

补充 `ReportServiceTest`：

- 默认日期范围为近 30 天。
- 默认临期阈值为 30 天。
- 空聚合结果返回稳定结构。
- 分页参数归一化。

### 前端验证

当前项目未配置前端测试框架，第一版采用构建和 smoke 验证：

- `npm run build` 验证构建。
- 新增或更新脚本级检查，验证 `/reports` 能渲染三 Tab、指标卡、SVG 图表和表格空态。
- 后续如果引入组件测试框架，再补充报表组件级测试。

## 实施约束

- 不新增业务写入表。
- 不新增图表依赖。
- 不破坏现有首页概览使用的旧报表接口。
- 不全局替换当前 Vue 系统主题。
- 报表页内部样式必须局部化，避免影响其他模块。
- SQL 查询以现有 MySQL 表结构为准，避免引用历史字段名。

## 建议类名

后端建议新增以下 DTO / VO 类，实施计划可按此命名展开：

- `ReportDateRangeRequest`
- `ReportDashboardVO`
- `ReportMetricVO`
- `ReportTrendPointVO`
- `ReportWarningVO`
- `InventoryReportSummaryVO`
- `InventoryReportLedgerVO`
- `InventoryReportBatchVO`
- `PurchaseReportOverviewVO`
- `PurchaseSupplierReportVO`
- `PurchaseReportDetailVO`

前端 smoke 验证复用现有 Vite 服务脚本模式，新增 `system/frontend/scripts/verify-report-statistics.mjs`。
