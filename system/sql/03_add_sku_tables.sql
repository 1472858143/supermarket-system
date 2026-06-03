-- 03_add_sku_tables.sql
-- 阶段 1：新建 SKU 相关表，现有表加列，迁移旧数据
-- 用于已执行 01_market.sql 和 02_add_category_table.sql 的旧库升级；
-- 全新初始化请使用 00_终版.sql，不要把 00 与本增量脚本串联执行。

USE market;

-- ============================================================
-- 第一部分：新建表
-- ============================================================

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

-- ============================================================
-- 第二部分：现有表加列
-- ============================================================

ALTER TABLE stock ADD COLUMN sku_id BIGINT NULL AFTER product_id;

ALTER TABLE stock_log ADD COLUMN sku_id BIGINT NULL AFTER product_id;

ALTER TABLE outbound_order ADD COLUMN sku_id BIGINT NULL AFTER product_id;
ALTER TABLE outbound_order ADD COLUMN unit VARCHAR(20) NOT NULL DEFAULT '个' COMMENT '操作单位';
ALTER TABLE outbound_order ADD COLUMN conversion_rate INT NOT NULL DEFAULT 1 COMMENT '换算率快照';
ALTER TABLE outbound_order ADD COLUMN base_quantity INT NOT NULL DEFAULT 0 COMMENT '换算后基础单位数量';

ALTER TABLE stock_check ADD COLUMN sku_id BIGINT NULL AFTER product_id;

-- ============================================================
-- 第三部分：旧数据迁移
-- ============================================================

INSERT INTO sku (product_id, sku_code, sku_name, spec, base_unit,
                 purchase_price, sale_price, status, is_default)
SELECT id,
       CONCAT(product_code, '-001'),
       product_name,
       '默认规格',
       '个',
       purchase_price,
       sale_price,
       status,
       1
FROM product;

UPDATE stock s
INNER JOIN sku k ON k.product_id = s.product_id AND k.is_default = 1
SET s.sku_id = k.id;

UPDATE stock_log sl
INNER JOIN sku k ON k.product_id = sl.product_id AND k.is_default = 1
SET sl.sku_id = k.id;

UPDATE outbound_order oo
INNER JOIN sku k ON k.product_id = oo.product_id AND k.is_default = 1
SET oo.sku_id = k.id,
    oo.unit = '个',
    oo.conversion_rate = 1,
    oo.base_quantity = oo.quantity;

UPDATE stock_check sc
INNER JOIN sku k ON k.product_id = sc.product_id AND k.is_default = 1
SET sc.sku_id = k.id;

ALTER TABLE stock
    ADD CONSTRAINT fk_stock_sku FOREIGN KEY (sku_id) REFERENCES sku(id);

ALTER TABLE stock_log
    ADD CONSTRAINT fk_stock_log_sku FOREIGN KEY (sku_id) REFERENCES sku(id);

ALTER TABLE outbound_order
    ADD CONSTRAINT fk_outbound_sku FOREIGN KEY (sku_id) REFERENCES sku(id);

ALTER TABLE stock_check
    ADD CONSTRAINT fk_stock_check_sku FOREIGN KEY (sku_id) REFERENCES sku(id);

-- ============================================================
-- 第四部分：阶段 2 移除 product 层价格
-- ============================================================
-- 旧价格已经迁移到默认 SKU。删除 product 价格列前，先动态删除
-- MySQL 自动命名的相关 CHECK 约束，避免硬编码约束名。

SET @product_price_checks := (
    SELECT GROUP_CONCAT(
               CONCAT('DROP CHECK `', REPLACE(tc.CONSTRAINT_NAME, '`', '``'), '`')
               SEPARATOR ', '
           )
    FROM information_schema.TABLE_CONSTRAINTS tc
    INNER JOIN information_schema.CHECK_CONSTRAINTS cc
        ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
       AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
    WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'product'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND (
          LOWER(cc.CHECK_CLAUSE) LIKE '%purchase_price%'
          OR LOWER(cc.CHECK_CLAUSE) LIKE '%sale_price%'
      )
);

SET @drop_product_price_checks_sql := IF(
    @product_price_checks IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE product ', @product_price_checks)
);

PREPARE drop_product_price_checks_stmt FROM @drop_product_price_checks_sql;
EXECUTE drop_product_price_checks_stmt;
DEALLOCATE PREPARE drop_product_price_checks_stmt;

ALTER TABLE product DROP COLUMN purchase_price;
ALTER TABLE product DROP COLUMN sale_price;
