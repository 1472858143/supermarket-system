-- 阶段 12：盘点升级为盘点单 + 批次明细。
-- 旧 stock_check 为按 SKU 的即时盘点记录，升级前需要不存在 stock_check_legacy。

RENAME TABLE stock_check TO stock_check_legacy;

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

DROP PROCEDURE IF EXISTS drop_stock_batch_initial_quantity_limit;

DELIMITER $$

CREATE PROCEDURE drop_stock_batch_initial_quantity_limit()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE constraint_name VARCHAR(128);
    DECLARE constraint_cursor CURSOR FOR
        SELECT tc.CONSTRAINT_NAME
        FROM information_schema.TABLE_CONSTRAINTS tc
        INNER JOIN information_schema.CHECK_CONSTRAINTS cc
            ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
           AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
        WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
          AND tc.TABLE_NAME = 'stock_batch'
          AND tc.CONSTRAINT_TYPE = 'CHECK'
          AND LOWER(REPLACE(REPLACE(cc.CHECK_CLAUSE, ' ', ''), '`', '')) LIKE '%quantity<=initial_quantity%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN constraint_cursor;
    constraint_loop: LOOP
        FETCH constraint_cursor INTO constraint_name;
        IF done THEN
            LEAVE constraint_loop;
        END IF;
        SET @sql = CONCAT('ALTER TABLE stock_batch DROP CHECK `', REPLACE(constraint_name, '`', '``'), '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE constraint_cursor;
END$$

DELIMITER ;

CALL drop_stock_batch_initial_quantity_limit();

DROP PROCEDURE IF EXISTS drop_stock_batch_initial_quantity_limit;
