USE market;

ALTER TABLE purchase_inbound
    CHANGE COLUMN total_quantity planned_total_quantity INT NOT NULL COMMENT '计划基础数量合计',
    CHANGE COLUMN total_amount planned_total_amount DECIMAL(18,6) NOT NULL COMMENT '计划金额合计',
    ADD COLUMN inbound_total_quantity INT NOT NULL DEFAULT 0 COMMENT '累计实收基础数量合计' AFTER planned_total_quantity,
    ADD COLUMN inbound_total_amount DECIMAL(18,6) NOT NULL DEFAULT 0.000000 COMMENT '累计实收金额合计' AFTER planned_total_amount,
    ADD COLUMN creator_user_id BIGINT DEFAULT NULL COMMENT '创建人ID快照' AFTER status,
    ADD COLUMN creator_username VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名快照' AFTER creator_user_id,
    ADD COLUMN submitter_user_id BIGINT DEFAULT NULL COMMENT '最近提交人ID快照' AFTER creator_username,
    ADD COLUMN submitter_username VARCHAR(50) DEFAULT NULL COMMENT '最近提交人用户名快照' AFTER submitter_user_id,
    ADD COLUMN submit_time DATETIME DEFAULT NULL AFTER submitter_username,
    ADD COLUMN approver_user_id BIGINT DEFAULT NULL COMMENT '最近审批人ID快照' AFTER submit_time,
    ADD COLUMN approver_username VARCHAR(50) DEFAULT NULL COMMENT '最近审批人用户名快照' AFTER approver_user_id,
    ADD COLUMN approve_time DATETIME DEFAULT NULL AFTER approver_username,
    ADD COLUMN cancel_user_id BIGINT DEFAULT NULL COMMENT '取消人ID快照' AFTER approve_time,
    ADD COLUMN cancel_username VARCHAR(50) DEFAULT NULL COMMENT '取消人用户名快照' AFTER cancel_user_id,
    ADD COLUMN cancel_time DATETIME DEFAULT NULL AFTER cancel_username,
    ADD COLUMN cancel_reason VARCHAR(200) DEFAULT NULL AFTER cancel_time,
    ADD COLUMN close_user_id BIGINT DEFAULT NULL COMMENT '关闭人ID快照' AFTER cancel_reason,
    ADD COLUMN close_username VARCHAR(50) DEFAULT NULL COMMENT '关闭人用户名快照' AFTER close_user_id,
    ADD COLUMN close_time DATETIME DEFAULT NULL AFTER close_username,
    ADD COLUMN close_reason VARCHAR(200) DEFAULT NULL AFTER close_time,
    MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/RETURNED/APPROVED/PARTIALLY_INBOUNDED/INBOUNDED/CANCELLED/CLOSED',
    MODIFY COLUMN operator VARCHAR(50) DEFAULT NULL COMMENT '旧操作人字段，保留迁移兼容';

UPDATE purchase_inbound
SET creator_user_id = 0,
    creator_username = COALESCE(NULLIF(operator, ''), 'legacy'),
    status = CASE status WHEN 'COMPLETED' THEN 'INBOUNDED' ELSE status END;

ALTER TABLE purchase_inbound_item
    CHANGE COLUMN quantity planned_quantity INT NOT NULL COMMENT '计划采购数量，操作单位',
    CHANGE COLUMN base_quantity planned_base_quantity INT NOT NULL COMMENT '计划基础数量',
    CHANGE COLUMN amount planned_amount DECIMAL(18,6) NOT NULL COMMENT '计划金额',
    ADD COLUMN supplier_sku_id BIGINT DEFAULT NULL COMMENT '供应商SKU绑定ID' AFTER sku_id,
    ADD COLUMN supplier_sku_code_snapshot VARCHAR(80) DEFAULT NULL COMMENT '供应商侧商品编码快照' AFTER supplier_sku_id,
    ADD COLUMN supplier_sku_name_snapshot VARCHAR(120) DEFAULT NULL COMMENT '供应商侧商品名称快照' AFTER supplier_sku_code_snapshot,
    ADD COLUMN supplier_spec_snapshot VARCHAR(120) DEFAULT NULL COMMENT '供应商侧规格快照' AFTER supplier_sku_name_snapshot,
    ADD COLUMN inbounded_base_quantity INT NOT NULL DEFAULT 0 COMMENT '累计实收基础数量' AFTER planned_base_quantity,
    ADD COLUMN inbounded_amount DECIMAL(18,6) NOT NULL DEFAULT 0.000000 COMMENT '累计实收金额' AFTER inbounded_base_quantity,
    MODIFY COLUMN purchase_price DECIMAL(18,6) NOT NULL COMMENT '审批计划采购价快照',
    MODIFY COLUMN cost_price DECIMAL(18,8) NOT NULL COMMENT '基础单位成本价快照';

INSERT INTO supplier_sku (
    supplier_id,
    sku_id,
    supplier_sku_code,
    supplier_sku_name,
    supplier_spec,
    default_purchase_price,
    min_purchase_quantity,
    status
)
SELECT
    pi.supplier_id,
    pii.sku_id,
    s.sku_code,
    s.sku_name,
    s.spec,
    ROUND(MAX(pii.purchase_price), 2),
    1,
    1
FROM purchase_inbound_item pii
JOIN purchase_inbound pi ON pi.id = pii.purchase_inbound_id
JOIN sku s ON s.id = pii.sku_id
LEFT JOIN supplier_sku ss ON ss.supplier_id = pi.supplier_id AND ss.sku_id = pii.sku_id
WHERE ss.id IS NULL
GROUP BY pi.supplier_id, pii.sku_id, s.sku_code, s.sku_name, s.spec;

UPDATE purchase_inbound_item pii
JOIN purchase_inbound pi ON pi.id = pii.purchase_inbound_id
JOIN supplier_sku ss ON ss.supplier_id = pi.supplier_id AND ss.sku_id = pii.sku_id
SET pii.supplier_sku_id = ss.id,
    pii.supplier_sku_code_snapshot = ss.supplier_sku_code,
    pii.supplier_sku_name_snapshot = ss.supplier_sku_name,
    pii.supplier_spec_snapshot = ss.supplier_spec,
    pii.inbounded_base_quantity = pii.planned_base_quantity,
    pii.inbounded_amount = pii.planned_amount;

ALTER TABLE purchase_inbound_item
    MODIFY COLUMN supplier_sku_id BIGINT NOT NULL COMMENT '供应商SKU绑定ID',
    MODIFY COLUMN supplier_sku_code_snapshot VARCHAR(80) NOT NULL COMMENT '供应商侧商品编码快照',
    MODIFY COLUMN supplier_sku_name_snapshot VARCHAR(120) NOT NULL COMMENT '供应商侧商品名称快照',
    ADD KEY idx_pi_item_supplier_sku (supplier_sku_id),
    ADD CONSTRAINT fk_pi_item_supplier_sku FOREIGN KEY (supplier_sku_id) REFERENCES supplier_sku(id);

CREATE TABLE purchase_inbound_approval_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_inbound_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    from_status VARCHAR(30) NOT NULL,
    to_status VARCHAR(30) NOT NULL,
    operator_user_id BIGINT NOT NULL,
    operator_username VARCHAR(50) NOT NULL,
    reason VARCHAR(200) DEFAULT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_pi_approval_log_inbound (purchase_inbound_id, id),
    CONSTRAINT fk_pi_approval_log_inbound FOREIGN KEY (purchase_inbound_id) REFERENCES purchase_inbound(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库审批与人工状态动作日志';

CREATE TABLE purchase_inbound_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receipt_no VARCHAR(40) NOT NULL COMMENT '实际入库单号，格式PIR{YYYYMMDD}{NNNN}',
    purchase_inbound_id BIGINT NOT NULL,
    operator_user_id BIGINT NOT NULL,
    operator_username VARCHAR(50) NOT NULL,
    total_base_quantity INT NOT NULL,
    total_amount DECIMAL(18,6) NOT NULL,
    remark VARCHAR(200) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pi_receipt_no (receipt_no),
    KEY idx_pi_receipt_inbound (purchase_inbound_id, id),
    CONSTRAINT fk_pi_receipt_inbound FOREIGN KEY (purchase_inbound_id) REFERENCES purchase_inbound(id),
    CHECK (total_base_quantity > 0),
    CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购实际入库主表';

CREATE TABLE purchase_inbound_receipt_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    purchase_inbound_id BIGINT NOT NULL,
    purchase_inbound_item_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL COMMENT '实收数量，操作单位',
    base_quantity INT NOT NULL COMMENT '实收基础数量',
    production_date DATE NOT NULL,
    shelf_life_days INT NOT NULL,
    expire_date DATE NOT NULL,
    purchase_price_snapshot DECIMAL(18,6) NOT NULL,
    cost_price_snapshot DECIMAL(18,8) NOT NULL,
    amount DECIMAL(18,6) NOT NULL,
    supplier_sku_code_snapshot VARCHAR(80) NOT NULL,
    supplier_sku_name_snapshot VARCHAR(120) NOT NULL,
    supplier_spec_snapshot VARCHAR(120) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_pi_receipt_batch_receipt (receipt_id, id),
    KEY idx_pi_receipt_batch_item (purchase_inbound_item_id, id),
    KEY idx_pi_receipt_batch_inbound (purchase_inbound_id, id),
    CONSTRAINT fk_pi_receipt_batch_receipt FOREIGN KEY (receipt_id) REFERENCES purchase_inbound_receipt(id),
    CONSTRAINT fk_pi_receipt_batch_inbound FOREIGN KEY (purchase_inbound_id) REFERENCES purchase_inbound(id),
    CONSTRAINT fk_pi_receipt_batch_item FOREIGN KEY (purchase_inbound_item_id) REFERENCES purchase_inbound_item(id),
    CONSTRAINT fk_pi_receipt_batch_sku FOREIGN KEY (sku_id) REFERENCES sku(id),
    CHECK (quantity > 0),
    CHECK (base_quantity > 0),
    CHECK (shelf_life_days > 0),
    CHECK (expire_date >= production_date),
    CHECK (purchase_price_snapshot >= 0),
    CHECK (cost_price_snapshot >= 0),
    CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购实际入库批次表';

INSERT INTO purchase_inbound_receipt (
    receipt_no,
    purchase_inbound_id,
    operator_user_id,
    operator_username,
    total_base_quantity,
    total_amount,
    remark
)
SELECT
    CONCAT('PIRLEGACY', LPAD(pi.id, 20, '0')),
    pi.id,
    0,
    COALESCE(pi.creator_username, NULLIF(pi.operator, ''), 'legacy'),
    SUM(sb.initial_quantity),
    SUM(ROUND(sb.initial_quantity * pii.cost_price, 6)),
    'legacy stock_batch migration'
FROM stock_batch sb
JOIN purchase_inbound_item pii ON pii.id = sb.purchase_inbound_item_id
JOIN purchase_inbound pi ON pi.id = pii.purchase_inbound_id
GROUP BY pi.id, pi.creator_username, pi.operator;

INSERT INTO purchase_inbound_receipt_batch (
    id,
    receipt_id,
    purchase_inbound_id,
    purchase_inbound_item_id,
    sku_id,
    quantity,
    base_quantity,
    production_date,
    shelf_life_days,
    expire_date,
    purchase_price_snapshot,
    cost_price_snapshot,
    amount,
    supplier_sku_code_snapshot,
    supplier_sku_name_snapshot,
    supplier_spec_snapshot,
    create_time
)
SELECT
    sb.id,
    pir.id,
    pi.id,
    pii.id,
    sb.sku_id,
    pii.planned_quantity,
    sb.initial_quantity,
    sb.production_date,
    sb.shelf_life_days,
    sb.expire_date,
    pii.purchase_price,
    pii.cost_price,
    ROUND(sb.initial_quantity * pii.cost_price, 6),
    pii.supplier_sku_code_snapshot,
    pii.supplier_sku_name_snapshot,
    pii.supplier_spec_snapshot,
    sb.create_time
FROM stock_batch sb
JOIN purchase_inbound_item pii ON pii.id = sb.purchase_inbound_item_id
JOIN purchase_inbound pi ON pi.id = pii.purchase_inbound_id
JOIN purchase_inbound_receipt pir ON pir.purchase_inbound_id = pi.id;

UPDATE purchase_inbound_receipt pir
JOIN (
    SELECT receipt_id, SUM(base_quantity) AS total_base_quantity, SUM(amount) AS total_amount
    FROM purchase_inbound_receipt_batch
    GROUP BY receipt_id
) rb ON rb.receipt_id = pir.id
SET pir.total_base_quantity = rb.total_base_quantity,
    pir.total_amount = rb.total_amount;

UPDATE purchase_inbound pi
LEFT JOIN (
    SELECT purchase_inbound_id, SUM(base_quantity) AS inbound_total_quantity, SUM(amount) AS inbound_total_amount
    FROM purchase_inbound_receipt_batch
    GROUP BY purchase_inbound_id
) rb ON rb.purchase_inbound_id = pi.id
SET pi.inbound_total_quantity = COALESCE(rb.inbound_total_quantity, pi.planned_total_quantity),
    pi.inbound_total_amount = COALESCE(rb.inbound_total_amount, pi.planned_total_amount);

ALTER TABLE stock_batch
    DROP FOREIGN KEY fk_stock_batch_pi_item_sku,
    DROP FOREIGN KEY fk_stock_batch_pi_item,
    DROP INDEX uk_stock_batch_pi_item,
    ADD COLUMN purchase_inbound_receipt_batch_id BIGINT DEFAULT NULL COMMENT '来源实际入库批次ID' AFTER sku_id,
    ADD COLUMN cost_price DECIMAL(18,8) DEFAULT NULL COMMENT '基础单位成本价快照' AFTER purchase_price,
    MODIFY COLUMN purchase_price DECIMAL(18,6) NOT NULL COMMENT '采购价快照';

UPDATE stock_batch sb
JOIN purchase_inbound_receipt_batch rb ON rb.id = sb.id
SET sb.purchase_inbound_receipt_batch_id = rb.id,
    sb.cost_price = rb.cost_price_snapshot;

ALTER TABLE stock_batch
    MODIFY COLUMN purchase_inbound_receipt_batch_id BIGINT NOT NULL COMMENT '来源实际入库批次ID',
    MODIFY COLUMN cost_price DECIMAL(18,8) NOT NULL DEFAULT 0.00000000 COMMENT '基础单位成本价快照',
    DROP COLUMN purchase_inbound_item_id,
    ADD UNIQUE KEY uk_stock_batch_receipt_batch (purchase_inbound_receipt_batch_id),
    ADD CONSTRAINT fk_stock_batch_receipt_batch FOREIGN KEY (purchase_inbound_receipt_batch_id) REFERENCES purchase_inbound_receipt_batch(id);

ALTER TABLE stock
    CHANGE COLUMN quantity total_quantity INT NOT NULL COMMENT '仓内实物总库存',
    ADD COLUMN available_quantity INT NOT NULL DEFAULT 0 COMMENT '可用库存' AFTER total_quantity,
    ADD COLUMN locked_quantity INT NOT NULL DEFAULT 0 COMMENT '锁定库存' AFTER available_quantity,
    ADD COLUMN expired_quantity INT NOT NULL DEFAULT 0 COMMENT '过期库存' AFTER locked_quantity;

UPDATE stock
SET available_quantity = total_quantity,
    locked_quantity = 0,
    expired_quantity = 0;

ALTER TABLE stock
    ADD CONSTRAINT ck_stock_quantity_buckets CHECK (total_quantity = available_quantity + locked_quantity + expired_quantity);

ALTER TABLE purchase_inbound
    MODIFY COLUMN creator_user_id BIGINT NOT NULL COMMENT '创建人ID快照',
    MODIFY COLUMN creator_username VARCHAR(50) NOT NULL COMMENT '创建人用户名快照';
