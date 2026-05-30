# 前端公共组件开发记录

## 1. 模块定位

公共组件用于统一后台页面的表格、弹窗、工具栏、权限按钮和状态展示，降低页面重复代码，提高界面一致性和后续维护效率。

## 2. 涉及源码

| 组件 | 源码 | 职责 |
|---|---|---|
| `BaseTable` | `frontend/src/components/BaseTable.vue` | 表格、loading、empty、分页 |
| `BaseDialog` | `frontend/src/components/BaseDialog.vue` | 通用弹窗 |
| `PageToolbar` | `frontend/src/components/PageToolbar.vue` | 查询与操作区 |
| `PermissionButton` | `frontend/src/components/PermissionButton.vue` | 按角色显示按钮 |
| `StatusTag` | `frontend/src/components/StatusTag.vue` | 状态标签 |

## 3. 组件设计

### BaseTable

职责：

- 接收列表数据。
- 展示 loading。
- 展示 empty。
- 展示分页信息和分页切换。
- 通过插槽支持不同页面自定义列内容。

使用页面：

- 用户、商品、库存、入库、出库、盘点、报表预警等。

### BaseDialog

职责：

- 统一弹窗容器。
- 承载新增、编辑、确认类表单。
- 通过插槽放入具体表单内容。

使用页面：

- 用户新增编辑。
- 商品新增编辑。
- 库存上下限维护。
- 入库、出库、盘点新增。

### PageToolbar

职责：

- 统一页面查询区。
- 支持关键词输入、查询、重置和右侧操作按钮。

### PermissionButton

职责：

- 根据 `roles` 参数和当前 `authStore.roles` 判断是否渲染按钮。
- 用于按钮级权限控制。

### StatusTag

职责：

- 统一展示启用、禁用、上架、下架、库存预警等状态。
- 保证不同页面状态样式一致。

## 4. 权限控制

`PermissionButton` 是按钮权限入口：

```text
当前用户角色 -> hasAnyRole -> 是否渲染按钮
```

按钮级权限使用场景：

- 用户管理：新增、编辑、删除。
- 商品管理：新增、编辑、删除。
- 库存管理：维护上下限。
- 盘点管理：新增盘点。

## 5. UI/UX 设计

- 公共组件保证后台页面风格统一。
- 表格和弹窗复用减少交互差异。
- loading 和 empty 由基础组件统一展示。
- 状态标签避免页面重复写样式。

## 6. 异常处理

- 组件不直接调用后端，不处理业务异常。
- 页面负责捕获 API 异常并传入状态。
- `BaseTable` 在数据为空时保持页面结构稳定。

## 7. 可维护性说明

- 页面只组合组件，不重复实现表格分页和弹窗结构。
- 按钮权限逻辑集中在一个组件，后续调整权限判断方式影响范围小。
- 状态标签集中维护，后续新增状态类型可统一扩展。

## 8. 测试与验收

- 表格加载时显示 loading。
- 空列表显示 empty。
- 分页切换触发页面重新查询。
- 弹窗打开和关闭状态正常。
- `USER` 不渲染 `ADMIN` 专属按钮。
- 状态标签在不同页面展示一致。
