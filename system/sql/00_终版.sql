-- 00_终版.sql
-- 阶段 4 后的新库完整结构快照。
-- 用于全新初始化数据库；不要与 01/02/03/04/05 增量升级脚本串联执行。

CREATE DATABASE IF NOT EXISTS market
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE market;

CREATE TABLE user (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      username VARCHAR(50) NOT NULL,
                      password VARCHAR(255) NOT NULL COMMENT '密码哈希摘要，不存储明文密码',
                      real_name VARCHAR(50),
                      status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
                      create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      role_name VARCHAR(50) NOT NULL,
                      role_code VARCHAR(50) NOT NULL,
                      remark VARCHAR(100),
                      create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      UNIQUE KEY uk_role_name (role_name),
                      UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE category (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          name VARCHAR(50) NOT NULL COMMENT '分类名称',
                          parent_id BIGINT DEFAULT NULL COMMENT '父分类ID，NULL表示一级分类',
                          sort_order INT DEFAULT 0 COMMENT '排序序号，越小越靠前',
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (parent_id) REFERENCES category(id),
                          UNIQUE KEY uk_parent_name (parent_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE brand (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       brand_code VARCHAR(50) NOT NULL COMMENT '品牌编码',
                       brand_name VARCHAR(100) NOT NULL COMMENT '品牌名称',
                       status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
                       remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
                       create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       UNIQUE KEY uk_brand_code (brand_code),
                       UNIQUE KEY uk_brand_name (brand_name),
                       CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌表';

CREATE TABLE product (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         product_code VARCHAR(50) NOT NULL,
                         product_name VARCHAR(100) NOT NULL,
                         category_id BIGINT NOT NULL COMMENT '分类ID，关联category表',
                         brand_id BIGINT NOT NULL COMMENT '品牌ID，关联brand表',
                         status TINYINT NOT NULL DEFAULT 1 COMMENT '0-下架 1-上架',
                         create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE KEY uk_product_code (product_code),
                         KEY idx_product_brand (brand_id),
                         CONSTRAINT fk_product_category
                             FOREIGN KEY (category_id) REFERENCES category(id),
                         CONSTRAINT fk_product_brand
                             FOREIGN KEY (brand_id) REFERENCES brand(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_role (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           user_id BIGINT NOT NULL,
                           role_id BIGINT NOT NULL,
                           UNIQUE KEY uk_user_role (user_id, role_id),
                           CONSTRAINT fk_user_role_user
                               FOREIGN KEY (user_id) REFERENCES user(id),
                           CONSTRAINT fk_user_role_role
                               FOREIGN KEY (role_id) REFERENCES role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sku (
                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                     product_id BIGINT NOT NULL COMMENT '所属商品主体',
                     sku_code VARCHAR(60) NOT NULL COMMENT '自动生成，格式: {product_code}-{NNN}',
                     sku_name VARCHAR(100) NOT NULL COMMENT '默认继承商品名称',
                     spec VARCHAR(100) NOT NULL DEFAULT '默认规格' COMMENT '规格描述',
                     barcode VARCHAR(50) DEFAULT NULL COMMENT '条码，可选',
                     base_unit VARCHAR(20) NOT NULL DEFAULT '个' COMMENT '基础计量单位',
                     purchase_price DECIMAL(10,2) NOT NULL COMMENT '进价',
                     sale_price DECIMAL(10,2) NOT NULL COMMENT '售价',
                     status TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
                     is_default TINYINT NOT NULL DEFAULT 0 COMMENT '1=默认SKU',
                     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                     UNIQUE KEY uk_sku_code (sku_code),
                     UNIQUE KEY uk_sku_barcode (barcode),
                     CONSTRAINT fk_sku_product FOREIGN KEY (product_id) REFERENCES product(id),
                     CHECK (purchase_price >= 0),
                     CHECK (sale_price >= 0),
                     CHECK (sale_price >= purchase_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU 规格表';

CREATE TABLE sku_unit_conversion (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     sku_id BIGINT NOT NULL COMMENT '所属 SKU',
                                     unit_name VARCHAR(20) NOT NULL COMMENT '换算单位名称，如箱',
                                     conversion_rate INT NOT NULL COMMENT '换算率，如 24 表示 1箱=24个基础单位',
                                     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     UNIQUE KEY uk_sku_unit (sku_id, unit_name),
                                     CONSTRAINT fk_unit_sku FOREIGN KEY (sku_id) REFERENCES sku(id),
                                     CHECK (conversion_rate > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU 单位换算表';

CREATE TABLE supplier (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          supplier_code VARCHAR(50) NOT NULL COMMENT '供应商编码',
                          supplier_name VARCHAR(100) NOT NULL COMMENT '供应商名称',
                          contact_person VARCHAR(50) DEFAULT NULL COMMENT '联系人',
                          contact_phone VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
                          address VARCHAR(200) DEFAULT NULL COMMENT '地址',
                          remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
                          create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          UNIQUE KEY uk_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

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

CREATE TABLE stock (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       sku_id BIGINT NOT NULL,
                       total_quantity INT NOT NULL COMMENT '仓内实物总库存',
                       available_quantity INT NOT NULL DEFAULT 0 COMMENT '可用库存',
                       locked_quantity INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
                       expired_quantity INT NOT NULL DEFAULT 0 COMMENT '过期库存',
                       min_stock INT NOT NULL,
                       max_stock INT NOT NULL,
                       update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,
                       UNIQUE KEY uk_stock_sku (sku_id),
                       CONSTRAINT fk_stock_sku
                           FOREIGN KEY (sku_id) REFERENCES sku(id),
                       CHECK (total_quantity >= 0),
                       CHECK (available_quantity >= 0),
                       CHECK (locked_quantity >= 0),
                       CHECK (expired_quantity >= 0),
                       CHECK (min_stock >= 0),
                       CHECK (max_stock >= min_stock),
                       CONSTRAINT ck_stock_quantity_buckets CHECK (total_quantity = available_quantity + locked_quantity + expired_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbound_order (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                sku_id BIGINT NOT NULL,
                                quantity INT NOT NULL,
                                unit VARCHAR(20) NOT NULL DEFAULT '个' COMMENT '操作单位',
                                conversion_rate INT NOT NULL DEFAULT 1 COMMENT '换算率快照',
                                base_quantity INT NOT NULL DEFAULT 0 COMMENT '换算后基础单位数量',
                                operator VARCHAR(50) NOT NULL,
                                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_outbound_sku
                                    FOREIGN KEY (sku_id) REFERENCES sku(id),
                                CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE purchase_inbound (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    order_no VARCHAR(30) NOT NULL COMMENT '采购入库单号，格式 PI{YYYYMMDD}{NNN}',
    planned_total_quantity INT NOT NULL COMMENT '计划基础数量合计',
    inbound_total_quantity INT NOT NULL DEFAULT 0 COMMENT '累计实收基础数量合计',
    planned_total_amount DECIMAL(18,6) NOT NULL COMMENT '计划金额合计',
    inbound_total_amount DECIMAL(18,6) NOT NULL DEFAULT 0.000000 COMMENT '累计实收金额合计',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/RETURNED/APPROVED/PARTIALLY_INBOUNDED/INBOUNDED/CANCELLED/CLOSED',
    creator_user_id BIGINT NOT NULL COMMENT '创建人ID快照',
    creator_username VARCHAR(50) NOT NULL COMMENT '创建人用户名快照',
    submitter_user_id BIGINT DEFAULT NULL COMMENT '最近提交人ID快照',
    submitter_username VARCHAR(50) DEFAULT NULL COMMENT '最近提交人用户名快照',
    submit_time DATETIME DEFAULT NULL,
    approver_user_id BIGINT DEFAULT NULL COMMENT '最近审批人ID快照',
    approver_username VARCHAR(50) DEFAULT NULL COMMENT '最近审批人用户名快照',
    approve_time DATETIME DEFAULT NULL,
    cancel_user_id BIGINT DEFAULT NULL COMMENT '取消人ID快照',
    cancel_username VARCHAR(50) DEFAULT NULL COMMENT '取消人用户名快照',
    cancel_time DATETIME DEFAULT NULL,
    cancel_reason VARCHAR(200) DEFAULT NULL,
    close_user_id BIGINT DEFAULT NULL COMMENT '关闭人ID快照',
    close_username VARCHAR(50) DEFAULT NULL COMMENT '关闭人用户名快照',
    close_time DATETIME DEFAULT NULL,
    close_reason VARCHAR(200) DEFAULT NULL,
    operator VARCHAR(50) DEFAULT NULL COMMENT '旧操作人字段，保留迁移兼容',
    remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_purchase_inbound_supplier (supplier_id, id),
    CONSTRAINT fk_purchase_inbound_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库主表';

CREATE TABLE purchase_inbound_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_inbound_id BIGINT NOT NULL COMMENT '采购入库单ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    supplier_sku_id BIGINT NOT NULL COMMENT '供应商SKU绑定ID',
    supplier_sku_code_snapshot VARCHAR(80) NOT NULL COMMENT '供应商侧商品编码快照',
    supplier_sku_name_snapshot VARCHAR(120) NOT NULL COMMENT '供应商侧商品名称快照',
    supplier_spec_snapshot VARCHAR(120) DEFAULT NULL COMMENT '供应商侧规格快照',
    planned_quantity INT NOT NULL COMMENT '计划采购数量，操作单位',
    unit VARCHAR(20) NOT NULL COMMENT '操作单位快照',
    conversion_rate INT NOT NULL COMMENT '换算率快照',
    planned_base_quantity INT NOT NULL COMMENT '计划基础数量',
    inbounded_base_quantity INT NOT NULL DEFAULT 0 COMMENT '累计实收基础数量',
    inbounded_amount DECIMAL(18,6) NOT NULL DEFAULT 0.000000 COMMENT '累计实收金额',
    purchase_price DECIMAL(18,6) NOT NULL COMMENT '审批计划采购价快照',
    cost_price DECIMAL(18,8) NOT NULL COMMENT '基础单位成本价快照',
    planned_amount DECIMAL(18,6) NOT NULL COMMENT '计划金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_pi_item_id_sku (id, sku_id),
    KEY idx_pi_item_supplier_sku (supplier_sku_id),
    CONSTRAINT fk_pi_item_inbound FOREIGN KEY (purchase_inbound_id) REFERENCES purchase_inbound(id),
    CONSTRAINT fk_pi_item_sku FOREIGN KEY (sku_id) REFERENCES sku(id),
    CONSTRAINT fk_pi_item_supplier_sku FOREIGN KEY (supplier_sku_id) REFERENCES supplier_sku(id),
    CHECK (planned_quantity > 0),
    CHECK (purchase_price >= 0),
    CHECK (conversion_rate > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库明细表';

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

CREATE TABLE stock_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no VARCHAR(40) NOT NULL COMMENT '库存批次号，格式 SB{YYYYMMDD}{NNN}',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    purchase_inbound_receipt_batch_id BIGINT NOT NULL COMMENT '来源实际入库批次ID',
    initial_quantity INT NOT NULL COMMENT '批次原始入库数量，基础单位',
    quantity INT NOT NULL COMMENT '批次当前剩余数量，基础单位',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE / DEPLETED / EXPIRED / LOCKED / DAMAGED / CLOSED',
    purchase_price DECIMAL(18,6) NOT NULL COMMENT '采购价快照',
    cost_price DECIMAL(18,8) NOT NULL DEFAULT 0.00000000 COMMENT '基础单位成本价快照',
    production_date DATE NOT NULL COMMENT '生产日期',
    shelf_life_days INT NOT NULL COMMENT '保质期天数',
    expire_date DATE NOT NULL COMMENT '到期日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_batch_no (batch_no),
    UNIQUE KEY uk_stock_batch_receipt_batch (purchase_inbound_receipt_batch_id),
    KEY idx_stock_batch_sku_expire (sku_id, expire_date, id),
    KEY idx_stock_batch_id_sku (id, sku_id),
    CONSTRAINT fk_stock_batch_sku FOREIGN KEY (sku_id) REFERENCES sku(id),
    CONSTRAINT fk_stock_batch_receipt_batch FOREIGN KEY (purchase_inbound_receipt_batch_id) REFERENCES purchase_inbound_receipt_batch(id),
    CHECK (initial_quantity > 0),
    CHECK (quantity >= 0),
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

CREATE TABLE stock_check (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_no VARCHAR(32) NOT NULL COMMENT '盘点单号',
    name VARCHAR(100) NOT NULL COMMENT '盘点名称',
    scope_type VARCHAR(30) NOT NULL COMMENT 'ALL / CATEGORY_LEVEL1 / CATEGORY_LEVEL2 / SKU',
    category_id BIGINT DEFAULT NULL COMMENT '分类范围',
    sku_select_type VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT 'ALL / MULTI / SINGLE',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / COMPLETED / CANCELLED',
    total_sku_count INT NOT NULL DEFAULT 0,
    total_batch_count INT NOT NULL DEFAULT 0,
    total_difference INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complete_time DATETIME DEFAULT NULL,
    UNIQUE KEY uk_stock_check_no (check_no),
    KEY idx_stock_check_status_time (status, create_time),
    KEY idx_stock_check_category (category_id),
    CONSTRAINT fk_stock_check_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT ck_stock_check_scope CHECK (scope_type IN ('ALL', 'CATEGORY_LEVEL1', 'CATEGORY_LEVEL2', 'SKU')),
    CONSTRAINT ck_stock_check_sku_select CHECK (sku_select_type IN ('ALL', 'MULTI', 'SINGLE')),
    CONSTRAINT ck_stock_check_status CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED')),
    CHECK (total_sku_count >= 0),
    CHECK (total_batch_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单';

CREATE TABLE stock_check_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_check_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    stock_batch_id BIGINT NOT NULL,
    batch_no VARCHAR(40) NOT NULL,
    system_quantity INT NOT NULL COMMENT '创建盘点单时的批次账面数量',
    actual_quantity INT DEFAULT NULL COMMENT '实盘数量',
    difference INT DEFAULT NULL COMMENT '实盘数量 - 创建时账面数量',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / COUNTED',
    expire_date DATE DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_check_item_batch (stock_check_id, stock_batch_id),
    KEY idx_stock_check_item_check (stock_check_id, id),
    KEY idx_stock_check_item_sku (sku_id, id),
    KEY idx_stock_check_item_batch_id (stock_batch_id),
    CONSTRAINT fk_stock_check_item_check FOREIGN KEY (stock_check_id) REFERENCES stock_check(id),
    CONSTRAINT fk_stock_check_item_sku FOREIGN KEY (sku_id) REFERENCES sku(id),
    CONSTRAINT fk_stock_check_item_batch FOREIGN KEY (stock_batch_id) REFERENCES stock_batch(id),
    CONSTRAINT fk_stock_check_item_batch_sku FOREIGN KEY (stock_batch_id, sku_id) REFERENCES stock_batch(id, sku_id),
    CONSTRAINT ck_stock_check_item_status CHECK (status IN ('PENDING', 'COUNTED')),
    CHECK (system_quantity >= 0),
    CHECK (actual_quantity IS NULL OR actual_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点批次明细';

CREATE TABLE stock_log (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           sku_id BIGINT NOT NULL,
                           change_type VARCHAR(20) NOT NULL COMMENT 'PURCHASE_INBOUND / OUTBOUND / CHECK / DAMAGE',
                           change_quantity INT NOT NULL,
                           before_quantity INT NOT NULL,
                           after_quantity INT NOT NULL,
                           create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT fk_stock_log_sku
                               FOREIGN KEY (sku_id) REFERENCES sku(id),
                           CHECK (after_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
