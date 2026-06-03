USE market;

DROP PROCEDURE IF EXISTS migrate_stock_batch_lifecycle;

DELIMITER $$

CREATE PROCEDURE migrate_stock_batch_lifecycle()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE constraint_name VARCHAR(64);
    DECLARE status_column_count INT DEFAULT 0;
    DECLARE reason_column_count INT DEFAULT 0;
    DECLARE remark_column_count INT DEFAULT 0;
    DECLARE closed_quantity_constraint_count INT DEFAULT 0;

    DECLARE status_constraints CURSOR FOR
        SELECT tc.CONSTRAINT_NAME
        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
        JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
          ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
         AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
        WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
          AND tc.TABLE_NAME = 'stock_batch'
          AND tc.CONSTRAINT_TYPE = 'CHECK'
          AND LOWER(cc.CHECK_CLAUSE) LIKE '%status%'
          AND tc.CONSTRAINT_NAME <> 'ck_stock_batch_closed_quantity';

    DECLARE change_quantity_constraints CURSOR FOR
        SELECT tc.CONSTRAINT_NAME
        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
        JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
          ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
         AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
        WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
          AND tc.TABLE_NAME = 'stock_batch_log'
          AND tc.CONSTRAINT_TYPE = 'CHECK'
          AND LOWER(REPLACE(REPLACE(cc.CHECK_CLAUSE, ' ', ''), '`', '')) REGEXP 'change_quantity(<>|!=)0'
          AND LOWER(REPLACE(REPLACE(cc.CHECK_CLAUSE, ' ', ''), '`', '')) NOT LIKE '%before_quantity%'
          AND LOWER(REPLACE(REPLACE(cc.CHECK_CLAUSE, ' ', ''), '`', '')) NOT LIKE '%after_quantity%';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    SELECT COUNT(*)
      INTO status_column_count
      FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'stock_batch'
       AND COLUMN_NAME = 'status';

    IF status_column_count = 0 THEN
        SET @sql = 'ALTER TABLE stock_batch ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''AVAILABLE'' COMMENT ''AVAILABLE / DEPLETED / EXPIRED / LOCKED / DAMAGED / CLOSED'' AFTER quantity';
    ELSE
        SET @sql = 'ALTER TABLE stock_batch MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT ''AVAILABLE'' COMMENT ''AVAILABLE / DEPLETED / EXPIRED / LOCKED / DAMAGED / CLOSED''';
    END IF;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    SET done = 0;
    OPEN status_constraints;
    status_loop: LOOP
        FETCH status_constraints INTO constraint_name;
        IF done = 1 THEN
            LEAVE status_loop;
        END IF;
        SET @sql = CONCAT('ALTER TABLE stock_batch DROP CHECK `', REPLACE(constraint_name, '`', '``'), '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE status_constraints;

    ALTER TABLE stock_batch
        ADD CONSTRAINT ck_stock_batch_status CHECK (status IN ('AVAILABLE', 'DEPLETED', 'EXPIRED', 'LOCKED', 'DAMAGED', 'CLOSED'));

    SELECT COUNT(*)
      INTO closed_quantity_constraint_count
      FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND TABLE_NAME = 'stock_batch'
       AND CONSTRAINT_TYPE = 'CHECK'
       AND CONSTRAINT_NAME = 'ck_stock_batch_closed_quantity';

    IF closed_quantity_constraint_count = 0 THEN
        ALTER TABLE stock_batch
            ADD CONSTRAINT ck_stock_batch_closed_quantity CHECK (status <> 'CLOSED' OR quantity = 0);
    END IF;

    ALTER TABLE stock_batch_log
        MODIFY COLUMN change_type VARCHAR(20) NOT NULL COMMENT 'PURCHASE_INBOUND / OUTBOUND / CHECK / BATCH_STATUS / DAMAGE';

    SET done = 0;
    OPEN change_quantity_constraints;
    change_quantity_loop: LOOP
        FETCH change_quantity_constraints INTO constraint_name;
        IF done = 1 THEN
            LEAVE change_quantity_loop;
        END IF;
        SET @sql = CONCAT('ALTER TABLE stock_batch_log DROP CHECK `', REPLACE(constraint_name, '`', '``'), '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE change_quantity_constraints;

    SELECT COUNT(*)
      INTO reason_column_count
      FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'stock_batch_log'
       AND COLUMN_NAME = 'reason';

    IF reason_column_count = 0 THEN
        SET @sql = 'ALTER TABLE stock_batch_log ADD COLUMN reason VARCHAR(50) DEFAULT NULL COMMENT ''原因，主要用于报损'' AFTER source_id';
    ELSE
        SET @sql = 'ALTER TABLE stock_batch_log MODIFY COLUMN reason VARCHAR(50) DEFAULT NULL COMMENT ''原因，主要用于报损''';
    END IF;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    SELECT COUNT(*)
      INTO remark_column_count
      FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'stock_batch_log'
       AND COLUMN_NAME = 'remark';

    IF remark_column_count = 0 THEN
        SET @sql = 'ALTER TABLE stock_batch_log ADD COLUMN remark VARCHAR(255) DEFAULT NULL COMMENT ''备注'' AFTER reason';
    ELSE
        SET @sql = 'ALTER TABLE stock_batch_log MODIFY COLUMN remark VARCHAR(255) DEFAULT NULL COMMENT ''备注''';
    END IF;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    ALTER TABLE stock_log
        MODIFY COLUMN change_type VARCHAR(20) NOT NULL COMMENT 'PURCHASE_INBOUND / OUTBOUND / CHECK / DAMAGE';
END$$

DELIMITER ;

CALL migrate_stock_batch_lifecycle();

DROP PROCEDURE IF EXISTS migrate_stock_batch_lifecycle;
