USE market;

-- stock_batch.status is created by 08_add_stock_batch.sql with the final
-- AVAILABLE / DEPLETED / EXPIRED / LOCKED / DAMAGED / CLOSED lifecycle states.
-- ck_stock_batch_closed_quantity is created by 08_add_stock_batch.sql, and
-- 10_extend_stock_batch_lifecycle.sql adds it for existing databases if needed.
-- This migration is intentionally a no-op for the current 08 -> 09 execution path
-- to avoid duplicate column or duplicate constraint failures.
