# 首页概览前端开发记录

## 1. 页面定位

首页概览用于进入系统后快速查看关键库存指标、入库出库摘要和库存预警。页面主要消费后端 `report` 模块接口，属于只读展示页面。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| View | `frontend/src/views/dashboard/DashboardView.vue` |
| API | `frontend/src/api/report.js` |
| 组件 | `BaseTable.vue`、`StatusTag.vue` |
| 路由 | `frontend/src/router/index.js` |

## 3. 页面功能

- 展示商品数量、库存总量等库存摘要。
- 展示入库总量摘要。
- 展示出库总量摘要。
- 展示库存预警列表。
- 支持 loading 和 empty 状态。

## 4. 权限控制

- 路由 `/dashboard` 允许 `ADMIN` 和 `USER`。
- 页面不提供写操作按钮。
- 后端报表接口也只允许已登录用户访问。

## 5. 接口调用

| 前端方法 | 后端接口 | 用途 |
|---|---|---|
| `getStockReport()` | `GET /api/reports/stock` | 库存摘要 |
| `getInboundReport()` | `GET /api/reports/inbound` | 入库摘要 |
| `getOutboundReport()` | `GET /api/reports/outbound` | 出库摘要 |
| `getWarningReport()` | `GET /api/reports/warning` | 库存预警 |

## 6. UI/UX 设计

- 卡片展示核心指标，便于快速浏览。
- 预警数据使用表格展示。
- 库存状态通过 `StatusTag` 强化识别。
- 数据加载时展示 loading。
- 无预警数据时展示 empty 状态。

## 7. 异常处理

- 报表接口失败时展示错误提示。
- 单个接口异常不应造成登录态误判，除非后端返回 401。
- 401 由请求封装跳转登录。

## 8. 测试与验收

- `ADMIN` 登录后进入首页能看到统计卡片。
- `USER` 登录后进入首页能看到统计卡片。
- 没有预警商品时显示空状态。
- 报表接口失败时页面有错误反馈。
- 首页不提供任何修改库存或业务数据的入口。
