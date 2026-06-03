-- 05_inbound_outbound_stockcheck_by_sku.sql
-- 阶段 4：出库、盘点单据切换为直接关联 SKU。
-- 前置条件：已经执行 03_add_sku_tables.sql 和 04_stock_by_sku.sql，出库和盘点表的 sku_id 已回填。

USE market;

-- outbound_order：sku_id 约束强化，移除 product_id
-- ============================================================

ALTER TABLE outbound_order DROP FOREIGN KEY fk_outbound_sku;
ALTER TABLE outbound_order MODIFY sku_id BIGINT NOT NULL;
ALTER TABLE outbound_order
    ADD CONSTRAINT fk_outbound_sku FOREIGN KEY (sku_id) REFERENCES sku(id);
ALTER TABLE outbound_order DROP FOREIGN KEY fk_outbound_product;
ALTER TABLE outbound_order DROP COLUMN product_id;

-- ============================================================
-- stock_check：sku_id 约束强化，移除 product_id
-- ============================================================

ALTER TABLE stock_check DROP FOREIGN KEY fk_stock_check_sku;
ALTER TABLE stock_check MODIFY sku_id BIGINT NOT NULL;
ALTER TABLE stock_check
    ADD CONSTRAINT fk_stock_check_sku FOREIGN KEY (sku_id) REFERENCES sku(id);
ALTER TABLE stock_check DROP FOREIGN KEY fk_stock_check_product;
ALTER TABLE stock_check DROP COLUMN product_id;
