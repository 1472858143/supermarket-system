-- 04_stock_by_sku.sql
-- 阶段 3：库存和库存日志切换为按 SKU 管理。
-- 前置条件：已经执行 03_add_sku_tables.sql，stock.sku_id 和 stock_log.sku_id 已回填。

USE market;

-- ============================================================
-- stock 表：sku_id 约束强化，移除 product_id
-- ============================================================

ALTER TABLE stock MODIFY sku_id BIGINT NOT NULL;
ALTER TABLE stock ADD UNIQUE KEY uk_stock_sku (sku_id);

ALTER TABLE stock DROP FOREIGN KEY fk_stock_product;
ALTER TABLE stock DROP INDEX uk_stock_product;
ALTER TABLE stock DROP COLUMN product_id;

-- ============================================================
-- stock_log 表：sku_id 约束强化，移除 product_id
-- ============================================================

ALTER TABLE stock_log MODIFY sku_id BIGINT NOT NULL;

ALTER TABLE stock_log DROP FOREIGN KEY fk_stock_log_product;
ALTER TABLE stock_log DROP COLUMN product_id;
