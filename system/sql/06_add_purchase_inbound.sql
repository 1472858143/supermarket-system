CREATE TABLE purchase_inbound (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(30) NOT NULL COMMENT '采购入库单号，格式 PI{YYYYMMDD}{NNN}',
    total_quantity INT NOT NULL COMMENT '总入库数量（基础单位合计）',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '总金额',
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' COMMENT '单据状态',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库主表';

CREATE TABLE purchase_inbound_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_inbound_id BIGINT NOT NULL COMMENT '采购入库单ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    quantity INT NOT NULL COMMENT '入库数量（操作单位）',
    unit VARCHAR(20) NOT NULL COMMENT '操作单位快照',
    conversion_rate INT NOT NULL COMMENT '换算率快照',
    base_quantity INT NOT NULL COMMENT '基础单位数量 = quantity * conversion_rate',
    purchase_price DECIMAL(10,2) NOT NULL COMMENT '采购单价（操作单位）',
    cost_price DECIMAL(10,4) NOT NULL COMMENT '成本单价（基础单位）= purchase_price / conversion_rate',
    amount DECIMAL(12,2) NOT NULL COMMENT '小计金额 = quantity * purchase_price',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pi_item_inbound FOREIGN KEY (purchase_inbound_id) REFERENCES purchase_inbound(id),
    CONSTRAINT fk_pi_item_sku FOREIGN KEY (sku_id) REFERENCES sku(id),
    CHECK (quantity > 0),
    CHECK (purchase_price >= 0),
    CHECK (conversion_rate > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库明细表';

ALTER TABLE stock_log MODIFY change_type VARCHAR(20) NOT NULL COMMENT 'PURCHASE_INBOUND / OUTBOUND / CHECK';
