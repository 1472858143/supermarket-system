# 09 - 库存批次生命周期 SQL 迁移执行

> 记录时间：2026-06-02
> 项目阶段：优化阶段 - 执行库存批次生命周期数据库迁移，修复因表结构未升级导致的系统内部异常

---

## 一、背景

库存批次生命周期功能已经扩展为：

- `AVAILABLE`
- `DEPLETED`
- `EXPIRED`
- `LOCKED`
- `DAMAGED`
- `CLOSED`

如果本地数据库仍停留在旧表结构，后端在执行批次状态流转、报损、关闭或日志写入时，可能因为字段、约束或日志列缺失而返回“系统内部异常”。

---

## 二、执行内容

本次按要求只执行 SQL 迁移，不运行后端测试、不运行前端构建。

执行的迁移等价于：

```text
system/sql/10_extend_stock_batch_lifecycle.sql
```

数据库连接使用 dev 配置：

```text
jdbc:mysql://127.0.0.1:3306/market
username=root
```

由于当前环境未找到 `mysql` 命令行客户端，本次通过 Java `jshell` 加载 MySQL JDBC 驱动直接连接数据库执行迁移 SQL。

---

## 三、迁移效果

本次迁移覆盖以下结构变更：

| 表名 | 变更内容 |
|---|---|
| `stock_batch` | 扩展 `status` 为六状态生命周期 |
| `stock_batch` | 增加或更新 `ck_stock_batch_status` 检查约束 |
| `stock_batch` | 增加 `ck_stock_batch_closed_quantity`，保证 `CLOSED` 批次数量必须为 0 |
| `stock_batch_log` | 扩展 `change_type` 注释，支持 `BATCH_STATUS`、`DAMAGE` |
| `stock_batch_log` | 移除 `change_quantity <> 0` 类限制，允许状态流水写入 0 |
| `stock_batch_log` | 增加或更新 `reason` 字段 |
| `stock_batch_log` | 增加或更新 `remark` 字段 |
| `stock_log` | 扩展 `change_type` 注释，支持 `DAMAGE` |

---

## 四、执行结果

执行命令返回成功，输出：

```text
SQL_EXECUTED: 10_extend_stock_batch_lifecycle.sql
```

本次未执行自动化测试，未启动服务，未修改业务数据。

---

## 五、后续注意

如果仍出现“系统内部异常”，优先检查：

- 当前运行服务是否连接同一个 `market` 数据库。
- 是否还有其它环境数据库未执行 `10_extend_stock_batch_lifecycle.sql`。
- 应用是否需要重启以重新加载连接或最新代码。

