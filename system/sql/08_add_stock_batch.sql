USE market;

ALTER TABLE purchase_inbound_item
    ADD KEY idx_pi_item_id_sku (id, sku_id);

CREATE TABLE stock_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no VARCHAR(40) NOT NULL COMMENT '库存批次号，格式 SB{YYYYMMDD}{NNN}',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    purchase_inbound_item_id BIGINT NOT NULL COMMENT '来源采购入库明细 ID',
    initial_quantity INT NOT NULL COMMENT '批次原始入库数量，基础单位',
    quantity INT NOT NULL COMMENT '批次当前剩余数量，基础单位',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE / DEPLETED / EXPIRED / LOCKED / DAMAGED / CLOSED',
    purchase_price DECIMAL(10,2) NOT NULL COMMENT '采购操作单位进价快照',
    production_date DATE NOT NULL COMMENT '生产日期',
    shelf_life_days INT NOT NULL COMMENT '保质期天数',
    expire_date DATE NOT NULL COMMENT '到期日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_batch_no (batch_no),
    UNIQUE KEY uk_stock_batch_pi_item (purchase_inbound_item_id),
    KEY idx_stock_batch_sku_expire (sku_id, expire_date, id),
    KEY idx_stock_batch_id_sku (id, sku_id),
    CONSTRAINT fk_stock_batch_sku FOREIGN KEY (sku_id) REFERENCES sku(id),
    CONSTRAINT fk_stock_batch_pi_item FOREIGN KEY (purchase_inbound_item_id) REFERENCES purchase_inbound_item(id),
    CONSTRAINT fk_stock_batch_pi_item_sku FOREIGN KEY (purchase_inbound_item_id, sku_id) REFERENCES purchase_inbound_item(id, sku_id),
    CHECK (initial_quantity > 0),
    CHECK (quantity >= 0),
    CHECK (quantity <= initial_quantity),
    CONSTRAINT ck_stock_batch_status CHECK (status IN ('AVAILABLE', 'DEPLETED', 'EXPIRED', 'LOCKED', 'DAMAGED', 'CLOSED')),
    CONSTRAINT ck_stock_batch_closed_quantity CHECK (status <> 'CLOSED' OR quantity = 0),
    CHECK (purchase_price >= 0),
    CHECK (shelf_life_days > 0),
    CHECK (expire_date >= production_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存批次表';

CREATE TABLE stock_batch_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_batch_id BIGINT NOT NULL COMMENT '库存批次 ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    change_type VARCHAR(20) NOT NULL COMMENT 'PURCHASE_INBOUND / OUTBOUND / CHECK / BATCH_STATUS / DAMAGE',
    change_quantity INT NOT NULL COMMENT '本次变化数量',
    before_quantity INT NOT NULL COMMENT '变化前批次数量',
    after_quantity INT NOT NULL COMMENT '变化后批次数量',
    source_type VARCHAR(30) NOT NULL COMMENT '来源类型',
    source_id BIGINT NOT NULL COMMENT '来源 ID',
    reason VARCHAR(50) DEFAULT NULL COMMENT '原因，主要用于报损',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_stock_batch_log_batch (stock_batch_id, id),
    KEY idx_stock_batch_log_sku (sku_id, id),
    CONSTRAINT fk_stock_batch_log_batch FOREIGN KEY (stock_batch_id) REFERENCES stock_batch(id),
    CONSTRAINT fk_stock_batch_log_sku FOREIGN KEY (sku_id) REFERENCES sku(id),
    CONSTRAINT fk_stock_batch_log_batch_sku FOREIGN KEY (stock_batch_id, sku_id) REFERENCES stock_batch(id, sku_id),
    CHECK (after_quantity >= 0),
    CHECK (before_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存批次流水表';
