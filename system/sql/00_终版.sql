-- 00_终版.sql
-- 阶段 2 后的新库完整结构快照。
-- 用于全新初始化数据库；不要与 01/02/03 增量升级脚本串联执行。

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

CREATE TABLE product (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         product_code VARCHAR(50) NOT NULL,
                         product_name VARCHAR(100) NOT NULL,
                         category_id BIGINT NOT NULL COMMENT '分类ID，关联category表',
                         status TINYINT NOT NULL DEFAULT 1 COMMENT '0-下架 1-上架',
                         create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE KEY uk_product_code (product_code),
                         CONSTRAINT fk_product_category
                             FOREIGN KEY (category_id) REFERENCES category(id)
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

CREATE TABLE stock (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       sku_id BIGINT NOT NULL,
                       quantity INT NOT NULL,
                       min_stock INT NOT NULL,
                       max_stock INT NOT NULL,
                       update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,
                       UNIQUE KEY uk_stock_sku (sku_id),
                       CONSTRAINT fk_stock_sku
                           FOREIGN KEY (sku_id) REFERENCES sku(id),
                       CHECK (quantity >= 0),
                       CHECK (min_stock >= 0),
                       CHECK (max_stock >= min_stock)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inbound_order (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               product_id BIGINT NOT NULL,
                               sku_id BIGINT NULL,
                               quantity INT NOT NULL,
                               unit VARCHAR(20) NOT NULL DEFAULT '个' COMMENT '操作单位',
                               conversion_rate INT NOT NULL DEFAULT 1 COMMENT '换算率快照',
                               base_quantity INT NOT NULL DEFAULT 0 COMMENT '换算后基础单位数量',
                               operator VARCHAR(50) NOT NULL,
                               create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_inbound_product
                                   FOREIGN KEY (product_id) REFERENCES product(id),
                               CONSTRAINT fk_inbound_sku
                                   FOREIGN KEY (sku_id) REFERENCES sku(id),
                               CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbound_order (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                product_id BIGINT NOT NULL,
                                sku_id BIGINT NULL,
                                quantity INT NOT NULL,
                                unit VARCHAR(20) NOT NULL DEFAULT '个' COMMENT '操作单位',
                                conversion_rate INT NOT NULL DEFAULT 1 COMMENT '换算率快照',
                                base_quantity INT NOT NULL DEFAULT 0 COMMENT '换算后基础单位数量',
                                operator VARCHAR(50) NOT NULL,
                                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_outbound_product
                                    FOREIGN KEY (product_id) REFERENCES product(id),
                                CONSTRAINT fk_outbound_sku
                                    FOREIGN KEY (sku_id) REFERENCES sku(id),
                                CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_check (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             product_id BIGINT NOT NULL,
                             sku_id BIGINT NULL,
                             system_quantity INT NOT NULL,
                             actual_quantity INT NOT NULL,
                             difference INT NOT NULL,
                             check_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_stock_check_product
                                 FOREIGN KEY (product_id) REFERENCES product(id),
                             CONSTRAINT fk_stock_check_sku
                                 FOREIGN KEY (sku_id) REFERENCES sku(id),
                             CHECK (system_quantity >= 0),
                             CHECK (actual_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_log (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           sku_id BIGINT NOT NULL,
                           change_type VARCHAR(20) NOT NULL COMMENT 'INBOUND / OUTBOUND / CHECK',
                           change_quantity INT NOT NULL,
                           before_quantity INT NOT NULL,
                           after_quantity INT NOT NULL,
                           create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT fk_stock_log_sku
                               FOREIGN KEY (sku_id) REFERENCES sku(id),
                           CHECK (after_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
