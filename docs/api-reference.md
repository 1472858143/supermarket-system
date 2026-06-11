# API 接口汇总文档

> 自动生成于 2026-06-05，基于后端 Controller 源码和前端 `src/api/` 目录汇总。

---

## 目录

- [1. 认证鉴权](#1-认证鉴权)
- [2. 用户管理](#2-用户管理)
- [3. 商品管理](#3-商品管理)
- [4. SKU 管理](#4-sku-管理)
- [5. 分类管理](#5-分类管理)
- [6. 品牌管理](#6-品牌管理)
- [7. 库存管理](#7-库存管理)
- [8. 库存批次](#8-库存批次)
- [9. 库存盘点](#9-库存盘点)
- [10. 供应商管理](#10-供应商管理)
- [11. 出库管理](#11-出库管理)
- [12. 采购入库](#12-采购入库)
- [13. 报表统计](#13-报表统计)
- [14. 系统信息](#14-系统信息)

---

## 通用说明

### 基础路径

- **后端基础路径：** `/api`（所有接口均以此开头）
- **前端 axios baseURL：** `import.meta.env.VITE_API_BASE_URL`，默认 `/api`

### 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

- `code === 0` 表示成功，其他值表示失败
- 前端 axios 拦截器自动解包 `response.data.data`

### 认证机制

- **方式：** JWT Bearer Token
- **Header：** `Authorization: Bearer <token>`
- **唯一无需认证的接口：** `POST /api/auth/login`
- **401 处理：** 前端自动清除 token 并跳转 `/login`

### 权限控制

- 使用自定义 `@RequireRoles` 注解
- 标注 `🔒 ADMIN` 的接口需要管理员角色
- 标注 `🔓 登录` 的接口只需有效 token（任意角色）
- 标注 `🌐 公开` 的接口无需认证

### 分页参数

通用分页查询参数（部分列表接口支持）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | Integer | 否 | 页码，默认 1 |
| `pageSize` | Integer | 否 | 每页条数，默认 20 |
| `keyword` | String | 否 | 搜索关键词 |

---

## 1. 认证鉴权

**后端文件：** `AuthController.java`
**前端文件：** `src/api/auth.js`
**基础路径：** `/api/auth`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `POST` | `/api/auth/login` | 用户登录 | 🌐 公开 | `login(data)` |
| `GET` | `/api/auth/me` | 获取当前用户信息 | 🔓 登录 | `currentUser()` |
| `POST` | `/api/auth/logout` | 退出登录 | 🔓 登录 | `logout()` |

### 登录请求体

```json
{
  "username": "admin",
  "password": "123456"
}
```

### 登录响应（LoginVO）

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "role": { "id": 1, "name": "ADMIN", "label": "管理员" }
  }
}
```

---

## 2. 用户管理

**后端文件：** `UserController.java`
**前端文件：** `src/api/user.js`
**基础路径：** `/api/users`
**权限：** 🔒 ADMIN（全部接口）

| 方法 | 路径 | 说明 | 前端函数 |
|------|------|------|----------|
| `GET` | `/api/users` | 用户列表（分页） | `listUsers(params)` |
| `GET` | `/api/users/roles` | 角色列表 | `listRoles()` |
| `GET` | `/api/users/permissions` | 权限模块列表 | `listPermissions()` |
| `POST` | `/api/users/roles` | 创建角色 | `createRole(data)` |
| `PUT` | `/api/users/roles/{id}/permissions` | 更新角色权限 | `updateRolePermissions(id, data)` |
| `POST` | `/api/users` | 创建用户 | `createUser(data)` |
| `PUT` | `/api/users/{id}` | 更新用户 | `updateUser(id, data)` |
| `DELETE` | `/api/users/{id}` | 删除用户 | `deleteUser(id)` |

### 用户查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 按用户名/真实姓名搜索 |
| `page` | Integer | 否 | 页码 |
| `pageSize` | Integer | 否 | 每页条数 |

### 创建角色请求体（RoleCreateRequest）

```json
{
  "name": "CASHIER",
  "label": "收银员",
  "permissionIds": [1, 2, 3]
}
```

### 创建用户请求体（UserCreateRequest）

```json
{
  "username": "cashier01",
  "password": "123456",
  "realName": "张三",
  "roleId": 2
}
```

---

## 3. 商品管理

**后端文件：** `ProductController.java`
**前端文件：** `src/api/product.js`
**基础路径：** `/api/products`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/products` | 商品列表（分页） | 🔓 登录 | `listProducts(params)` |
| `POST` | `/api/products` | 创建商品 | 🔒 ADMIN | `createProduct(data)` |
| `PUT` | `/api/products/{id}` | 更新商品 | 🔒 ADMIN | `updateProduct(id, data)` |
| `DELETE` | `/api/products/{id}` | 删除商品 | 🔒 ADMIN | `deleteProduct(id)` |

### 商品查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 按商品名称/编码搜索 |
| `brandId` | Long | 否 | 按品牌筛选 |
| `page` | Integer | 否 | 页码 |
| `pageSize` | Integer | 否 | 每页条数 |

### 商品请求体（ProductRequest）

```json
{
  "name": "农夫山泉矿泉水",
  "code": "NF001",
  "barcode": "6921168593057",
  "categoryId": 10,
  "brandId": 5,
  "status": "ACTIVE"
}
```

---

## 4. SKU 管理

**后端文件：** `SkuController.java`
**前端文件：** `src/api/sku.js`
**基础路径：** `/api/products/{productId}/skus`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/products/{productId}/skus` | SKU 列表 | 🔓 登录 | `listSkus(productId)` |
| `POST` | `/api/products/{productId}/skus` | 创建 SKU | 🔒 ADMIN | `createSku(productId, data)` |
| `PUT` | `/api/products/{productId}/skus/{skuId}` | 更新 SKU | 🔒 ADMIN | `updateSku(productId, skuId, data)` |
| `DELETE` | `/api/products/{productId}/skus/{skuId}` | 删除 SKU | 🔒 ADMIN | `deleteSku(productId, skuId)` |
| `GET` | `/api/products/{productId}/skus/{skuId}/units` | 单位换算列表 | 🔓 登录 | `listUnits(productId, skuId)` |
| `POST` | `/api/products/{productId}/skus/{skuId}/units` | 创建单位换算 | 🔒 ADMIN | `createUnit(productId, skuId, data)` |
| `PUT` | `/api/products/{productId}/skus/{skuId}/units/{unitId}` | 更新单位换算 | 🔒 ADMIN | `updateUnit(productId, skuId, unitId, data)` |
| `DELETE` | `/api/products/{productId}/skus/{skuId}/units/{unitId}` | 删除单位换算 | 🔒 ADMIN | `deleteUnit(productId, skuId, unitId)` |

### SKU 请求体（SkuRequest）

```json
{
  "spec": "550ml",
  "barcode": "6921168593058",
  "purchasePrice": 1.2,
  "salePrice": 2.0,
  "unit": "瓶",
  "status": "ACTIVE"
}
```

### 单位换算请求体（UnitConversionRequest）

```json
{
  "fromUnit": "箱",
  "toUnit": "瓶",
  "conversionRate": 24
}
```

---

## 5. 分类管理

**后端文件：** `CategoryController.java`
**前端文件：** `src/api/category.js`
**基础路径：** `/api/categories`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/categories` | 分类树 | 🔓 登录 | `getCategoryTree()` |
| `POST` | `/api/categories` | 创建分类 | 🔒 ADMIN | `createCategory(data)` |
| `PUT` | `/api/categories/{id}` | 更新分类 | 🔒 ADMIN | `updateCategory(id, data)` |
| `PUT` | `/api/categories/sort-order` | 批量更新排序 | 🔒 ADMIN | `updateCategorySortOrder(items)` |
| `DELETE` | `/api/categories/{id}` | 删除分类 | 🔒 ADMIN | `deleteCategory(id)` |

### 分类请求体（CategoryRequest）

```json
{
  "name": "饮料",
  "parentId": null,
  "sortOrder": 1
}
```

---

## 6. 品牌管理

**后端文件：** `BrandController.java`
**前端文件：** `src/api/brand.js`
**基础路径：** `/api/brands`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/brands` | 品牌列表（分页） | 🔓 登录 | `listBrands(params)` |
| `GET` | `/api/brands/options` | 品牌下拉选项 | 🔓 登录 | `listBrandOptions()` |
| `POST` | `/api/brands` | 创建品牌 | 🔒 ADMIN | `createBrand(data)` |
| `PUT` | `/api/brands/{id}` | 更新品牌 | 🔒 ADMIN | `updateBrand(id, data)` |
| `DELETE` | `/api/brands/{id}` | 删除品牌 | 🔒 ADMIN | `deleteBrand(id)` |

### 品牌查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 按品牌名称搜索 |
| `status` | String | 否 | 按状态筛选 |
| `page` | Integer | 否 | 页码 |
| `pageSize` | Integer | 否 | 每页条数 |

### 品牌请求体（BrandRequest）

```json
{
  "name": "农夫山泉",
  "logo": "https://example.com/logo.png",
  "status": "ACTIVE"
}
```

---

## 7. 库存管理

**后端文件：** `StockController.java`
**前端文件：** `src/api/stock.js`
**基础路径：** `/api/stocks`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/stocks` | 库存列表（分页） | 🔓 登录 | `listStocks(params)` |
| `GET` | `/api/stocks/{skuId}` | 按 SKU 查询库存 | 🔓 登录 | — |
| `PUT` | `/api/stocks/{skuId}/limit` | 更新库存上下限 | 🔒 ADMIN | `updateStockLimit(skuId, data)` |

### 库存查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 按商品名称/SKU 编码搜索 |
| `page` | Integer | 否 | 页码 |
| `pageSize` | Integer | 否 | 每页条数 |

### 库存上下限请求体（StockLimitUpdateRequest）

```json
{
  "minLimit": 10,
  "maxLimit": 1000
}
```

---

## 8. 库存批次

**后端文件：** `StockBatchController.java`
**前端文件：** `src/api/stockBatch.js`
**基础路径：** `/api/skus/{skuId}/stock-batches`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/skus/{skuId}/stock-batches` | 批次列表 | 🔓 登录 | `listStockBatches(skuId)` |
| `PUT` | `/api/skus/{skuId}/stock-batches/{batchId}/lock` | 锁定批次 | 🔓 登录 | `lockStockBatch(skuId, batchId)` |
| `PUT` | `/api/skus/{skuId}/stock-batches/{batchId}/unlock` | 解锁批次 | 🔓 登录 | `unlockStockBatch(skuId, batchId)` |
| `POST` | `/api/skus/{skuId}/stock-batches/{batchId}/damage` | 报损 | 🔓 登录 | `damageStockBatch(skuId, batchId, payload)` |
| `PUT` | `/api/skus/{skuId}/stock-batches/{batchId}/close` | 关闭批次 | 🔓 登录 | `closeStockBatch(skuId, batchId)` |

### 报损请求体（StockBatchDamageRequest）

```json
{
  "quantity": 5,
  "reason": "过期变质"
}
```

---

## 9. 库存盘点

**后端文件：** `StockCheckController.java`
**前端文件：** `src/api/stockcheck.js`
**基础路径：** `/api/stockchecks`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/stockchecks` | 盘点单列表（分页） | 🔓 登录 | `listStockchecks(params)` |
| `POST` | `/api/stockchecks` | 创建盘点单 | 🔒 ADMIN | `createStockcheck(data)` |
| `GET` | `/api/stockchecks/{id}` | 盘点单详情 | 🔓 登录 | `getStockcheck(id)` |
| `PUT` | `/api/stockchecks/{id}/items` | 更新盘点明细 | 🔒 ADMIN | `updateStockcheckItems(id, data)` |
| `POST` | `/api/stockchecks/{id}/complete` | 完成盘点 | 🔒 ADMIN | `completeStockcheck(id)` |

### 盘点查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 按盘点单号搜索 |
| `page` | Integer | 否 | 页码 |
| `pageSize` | Integer | 否 | 每页条数 |

### 创建盘点单请求体（StockCheckCreateRequest）

```json
{
  "type": "FULL",
  "note": "月度全面盘点"
}
```

---

## 10. 供应商管理

### 10.1 供应商基础

**后端文件：** `SupplierController.java`
**前端文件：** `src/api/supplier.js`
**基础路径：** `/api/suppliers`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/suppliers` | 供应商列表（分页） | 🔓 登录 | `listSuppliers(params)` |
| `POST` | `/api/suppliers` | 创建供应商 | 🔒 ADMIN | `createSupplier(data)` |
| `PUT` | `/api/suppliers/{id}` | 更新供应商 | 🔒 ADMIN | `updateSupplier(id, data)` |
| `DELETE` | `/api/suppliers/{id}` | 删除供应商 | 🔒 ADMIN | `deleteSupplier(id)` |

### 供应商查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 按供应商名称/编码搜索 |
| `page` | Integer | 否 | 页码 |
| `pageSize` | Integer | 否 | 每页条数 |

### 供应商请求体（SupplierRequest）

```json
{
  "name": "农夫山泉经销商",
  "code": "SUP001",
  "contact": "李四",
  "phone": "13800138000",
  "address": "浙江省杭州市"
}
```

### 10.2 供应商 SKU 绑定

**后端文件：** `SupplierSkuController.java`
**基础路径：** `/api/suppliers/{supplierId}/skus`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/suppliers/{supplierId}/skus` | 绑定列表 | 🔓 登录 | `listSupplierSkus(supplierId)` |
| `GET` | `/api/suppliers/{supplierId}/skus/enabled` | 启用的绑定列表 | 🔓 登录 | `listEnabledSupplierSkus(supplierId)` |
| `POST` | `/api/suppliers/{supplierId}/skus` | 新增绑定 | 🔒 ADMIN | `createSupplierSku(supplierId, data)` |
| `PUT` | `/api/suppliers/{supplierId}/skus/{bindingId}` | 更新绑定 | 🔒 ADMIN | `updateSupplierSku(supplierId, bindingId, data)` |
| `DELETE` | `/api/suppliers/{supplierId}/skus/{bindingId}` | 删除绑定 | 🔒 ADMIN | `deleteSupplierSku(supplierId, bindingId)` |

### 供应商 SKU 绑定请求体（SupplierSkuRequest）

```json
{
  "skuId": 1,
  "supplierPrice": 1.0,
  "supplierSkuCode": "NF-550ML",
  "status": "ENABLED"
}
```

---

## 11. 出库管理

**后端文件：** `OutboundController.java`
**前端文件：** `src/api/outbound.js`
**基础路径：** `/api/outbounds`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/outbounds` | 出库单列表（分页） | 🔓 登录 | `listOutbounds(params)` |
| `POST` | `/api/outbounds` | 创建出库单 | 🔓 登录 | `createOutbound(data)` |

### 出库查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 按出库单号搜索 |
| `page` | Integer | 否 | 页码 |
| `pageSize` | Integer | 否 | 每页条数 |

### 出库请求体（OutboundRequest）

```json
{
  "note": "门店补货",
  "items": [
    { "skuId": 1, "quantity": 100 },
    { "skuId": 2, "quantity": 50 }
  ]
}
```

---

## 12. 采购入库

**后端文件：** `PurchaseInboundController.java`
**前端文件：** `src/api/purchaseInbound.js`
**基础路径：** `/api/purchase-inbounds`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/purchase-inbounds` | 采购单列表（分页） | 🔓 登录 | `listPurchaseInbounds(params)` |
| `GET` | `/api/purchase-inbounds/{id}` | 采购单详情 | 🔓 登录 | `getPurchaseInbound(id)` |
| `POST` | `/api/purchase-inbounds/drafts` | 创建草稿 | 🔒 ADMIN | `createPurchaseInboundDraft(data)` |
| `PUT` | `/api/purchase-inbounds/{id}/plan` | 修改计划 | 🔒 ADMIN | `updatePurchaseInboundPlan(id, data)` |
| `POST` | `/api/purchase-inbounds/{id}/submit` | 提交审批 | 🔒 ADMIN | `submitPurchaseInbound(id)` |
| `POST` | `/api/purchase-inbounds/{id}/approve` | 审批通过 | 🔒 ADMIN | `approvePurchaseInbound(id)` |
| `POST` | `/api/purchase-inbounds/{id}/return` | 退回修改 | 🔒 ADMIN | `returnPurchaseInbound(id, data)` |
| `POST` | `/api/purchase-inbounds/{id}/cancel` | 取消 | 🔒 ADMIN | `cancelPurchaseInbound(id, data)` |
| `POST` | `/api/purchase-inbounds/{id}/close` | 关闭 | 🔒 ADMIN | `closePurchaseInbound(id, data)` |
| `POST` | `/api/purchase-inbounds/{id}/receipts` | 执行入库 | 🔒 ADMIN | `receivePurchaseInbound(id, data)` |

### 采购单查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 按单号搜索 |
| `status` | String | 否 | 按状态筛选（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED） |
| `page` | Integer | 否 | 页码 |
| `pageSize` | Integer | 否 | 每页条数 |

### 采购单请求体（PurchaseInboundRequest）

```json
{
  "supplierId": 1,
  "expectedDate": "2026-06-10",
  "note": "六月首批采购",
  "items": [
    { "skuId": 1, "quantity": 500, "purchasePrice": 1.0 },
    { "skuId": 2, "quantity": 200, "purchasePrice": 0.8 }
  ]
}
```

### 入库请求体（PurchaseInboundReceiptRequest）

```json
{
  "items": [
    { "skuId": 1, "receivedQuantity": 498, "batchNo": "B20260601" }
  ]
}
```

### 审批决定请求体（PurchaseInboundDecisionRequest）

```json
{
  "reason": "价格不合理，需要重新议价"
}
```

### 采购单状态流转

```
DRAFT → SUBMITTED → APPROVED → CLOSED
  ↓         ↓          ↑
  ↘       RETURN ←────┘
   ↘        ↓
    →→→ CANCELLED
```

---

## 13. 报表统计

**后端文件：** `ReportController.java`
**前端文件：** `src/api/report.js`
**基础路径：** `/api/reports`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/reports/stock` | 库存统计 | 🔓 登录 | `getStockReport()` |
| `GET` | `/api/reports/inbound` | 入库统计 | 🔓 登录 | `getInboundReport()` |
| `GET` | `/api/reports/outbound` | 出库统计 | 🔓 登录 | `getOutboundReport()` |
| `GET` | `/api/reports/warning` | 库存预警 | 🔓 登录 | `getWarningReport()` |

---

## 14. 系统信息

**后端文件：** `SystemController.java`
**前端文件：** `src/api/system.js`
**基础路径：** `/api/system`

| 方法 | 路径 | 说明 | 权限 | 前端函数 |
|------|------|------|------|----------|
| `GET` | `/api/system/info` | 系统信息 | 🔓 登录 | `getSystemInfo()` |

---

## 附录：统计汇总

| 指标 | 数量 |
|------|------|
| **后端 Controller** | 15 |
| **后端接口总数** | 72 |
| **前端 API 函数** | 71 |
| **前端 API 文件** | 14 |

### 按 HTTP 方法分布

| 方法 | 后端 | 前端 |
|------|------|------|
| GET | 31 | 34 |
| POST | 18 | 18 |
| PUT | 16 | 14 |
| DELETE | 7 | 5 |

### 按权限分布

| 权限 | 数量 |
|------|------|
| 🌐 公开（无需认证） | 1 |
| 🔓 登录（任意角色） | 35 |
| 🔒 ADMIN | 36 |

### 前端 API 文件清单

| 文件 | 函数数 | 对应模块 |
|------|--------|----------|
| `src/api/auth.js` | 3 | 认证鉴权 |
| `src/api/user.js` | 8 | 用户管理 |
| `src/api/product.js` | 4 | 商品管理 |
| `src/api/sku.js` | 8 | SKU 管理 |
| `src/api/category.js` | 5 | 分类管理 |
| `src/api/brand.js` | 5 | 品牌管理 |
| `src/api/stock.js` | 2 | 库存管理 |
| `src/api/stockBatch.js` | 5 | 库存批次 |
| `src/api/stockcheck.js` | 5 | 库存盘点 |
| `src/api/supplier.js` | 9 | 供应商管理 |
| `src/api/outbound.js` | 2 | 出库管理 |
| `src/api/purchaseInbound.js` | 10 | 采购入库 |
| `src/api/report.js` | 4 | 报表统计 |
| `src/api/system.js` | 1 | 系统信息 |
