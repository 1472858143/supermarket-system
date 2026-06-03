-- 前置条件：
-- 执行本脚本前，purchase_inbound 必须为空。
-- 如果库中已有历史采购入库单，需要先制定 supplier_id 的供应商回填策略，
-- 不要直接执行本脚本，否则 ADD NOT NULL 字段和外键会带来迁移失败或数据归属错误风险。

DROP PROCEDURE IF EXISTS assert_purchase_inbound_empty_for_supplier_sku;

DELIMITER //
CREATE PROCEDURE assert_purchase_inbound_empty_for_supplier_sku()
BEGIN
    IF EXISTS (SELECT 1 FROM purchase_inbound LIMIT 1) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'purchase_inbound is not empty; backfill supplier_id before running 14_add_supplier_sku_binding.sql';
    END IF;
END//
DELIMITER ;

CALL assert_purchase_inbound_empty_for_supplier_sku();
DROP PROCEDURE assert_purchase_inbound_empty_for_supplier_sku;

CREATE TABLE supplier_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    supplier_sku_code VARCHAR(80) NOT NULL COMMENT '供应商侧商品编码',
    supplier_sku_name VARCHAR(120) NOT NULL COMMENT '供应商侧商品名称',
    supplier_spec VARCHAR(120) DEFAULT NULL COMMENT '供应商侧规格',
    default_purchase_price DECIMAL(10,2) NOT NULL COMMENT '默认采购价',
    min_purchase_quantity INT NOT NULL DEFAULT 1 COMMENT '最小采购量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_supplier_sku (supplier_id, sku_id),
    KEY idx_supplier_sku_supplier_status (supplier_id, status),
    KEY idx_supplier_sku_sku (sku_id),
    CONSTRAINT fk_supplier_sku_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    CONSTRAINT fk_supplier_sku_sku FOREIGN KEY (sku_id) REFERENCES sku(id),
    CHECK (default_purchase_price >= 0),
    CHECK (min_purchase_quantity > 0),
    CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商SKU供货关系表';

ALTER TABLE purchase_inbound
    ADD COLUMN supplier_id BIGINT NOT NULL COMMENT '供应商ID' AFTER id,
    ADD KEY idx_purchase_inbound_supplier (supplier_id, id),
    ADD CONSTRAINT fk_purchase_inbound_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id);
