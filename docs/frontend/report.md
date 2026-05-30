# 报表统计前端开发记录

## 1. 页面定位

报表统计页面对应后端 `report` 模块，用于展示库存、入库、出库和预警统计。页面只读，不提供任何业务写入入口。

## 2. 涉及源码

| 类型 | 源码 |
|---|---|
| View | `frontend/src/views/report/ReportsView.vue` |
| API | `frontend/src/api/report.js` |
| 组件 | `BaseTable.vue`、`StatusTag.vue` |
| 路由 | `frontend/src/router/index.js` |

## 3. 页面功能

- 展示库存统计。
- 展示入库统计。
- 展示出库统计。
- 展示库存预警数据。
- 支持加载和空状态。

## 4. 权限控制

- 路由 `/reports` 允许 `ADMIN` 和 `USER`。
- 页面不提供写操作按钮。
- 后端报表接口仅要求登录，不区分管理员和普通用户。

## 5. 接口调用

| 前端方法 | 后端接口 | 用途 |
|---|---|---|
| `getStockReport()` | `GET /api/reports/stock` | 库存统计 |
| `getInboundReport()` | `GET /api/reports/inbound` | 入库统计 |
| `getOutboundReport()` | `GET /api/reports/outbound` | 出库统计 |
| `getWarningReport()` | `GET /api/reports/warning` | 预警统计 |

## 6. UI/UX 设计

- 使用统计卡片展示关键指标。
- 使用表格展示预警列表。
- 状态使用标签增强识别。
- 查询中显示 loading。
- 无预警时显示 empty。
- 不出现新增、编辑、删除按钮。

## 7. 异常处理

- 报表接口失败时展示错误提示。
- 401 由请求封装处理。
- 空数据时展示 0 或空状态，避免页面空白。

## 8. 测试与验收

- `ADMIN` 可访问报表页。
- `USER` 可访问报表页。
- 页面无业务写入入口。
- 报表数据为空时仍能正常渲染。
- 接口失败时有明确反馈。
