# 超市库存管理系统

本科毕业设计项目，采用前后端分离架构实现超市场景下的库存管理闭环。

## 技术栈

- 前端：Vue 3、Vite、Pinia、Vue Router、Axios
- 后端：Spring Boot、Spring JDBC、BCrypt、JWT
- 数据库：MySQL 8.0.44
- 部署：Nginx + Spring Boot Jar + MySQL，Redis 预留为可选扩展

## 项目结构

```text
E:\Supermarket
├─ backend/        Spring Boot 后端
├─ frontend/       Vue 3 前端
├─ docs/           项目文档
└─ sql/            数据库脚本
```

## 数据库初始化

先在 MySQL 中执行：

```sql
source E:/Supermarket/sql/market.sql;
```

后端启动时会在数据库为空时自动初始化两个角色和两个演示账号：

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `user` | `user123` | `USER` |

密码通过 `PasswordService` 使用 BCrypt 写入数据库，不保存明文。

## 后端运行

```powershell
cd E:\Supermarket\backend
mvn spring-boot:run
```

常用环境变量：

| 变量 | 说明 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` 或 `prod` |
| `DB_URL` | MySQL JDBC 地址 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥 |
| `BOOTSTRAP_ENABLED` | 是否初始化默认账号 |

## 前端运行

```powershell
cd E:\Supermarket\frontend
npm install
npm run dev
```

前端默认使用 `VITE_API_BASE_URL=/api`，开发环境通过 Vite proxy 转发到 `http://localhost:8080`。

## 权限说明

- `ADMIN`：可访问所有页面，可维护用户、商品、库存上下限，可执行入库、出库、盘点。
- `USER`：不可访问用户管理；可查看商品、库存、报表和系统信息；可执行入库、出库；不可维护商品、库存上下限和新增盘点。

前端实现路由权限、菜单权限、按钮权限；后端通过 `@RequireRoles` 和认证拦截器校验接口权限。

## 核心设计约束

- `stock` 是唯一允许直接修改库存表的模块。
- `inbound`、`outbound`、`stockcheck` 只能调用 `StockService` 变更库存。
- `report` 只读，不写业务数据。
- 前端不计算库存、不直接修改库存、不保存或展示密码。
- 所有部署地址和密钥通过配置或环境变量提供，代码中不硬编码服务器地址。
