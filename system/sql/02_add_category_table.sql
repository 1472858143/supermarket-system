-- 02_add_category_table.sql
-- 商品分类独立化：新建 category 表，改造 product 表

-- 1. 创建分类表
CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID，NULL表示一级分类',
    sort_order INT DEFAULT 0 COMMENT '排序序号，越小越靠前',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES category(id),
    UNIQUE KEY uk_parent_name (parent_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 2. 改造 product 表：删除旧 category 列，添加 category_id 列
--    注意：开发阶段可以先清空 product 及其关联数据再执行
--    如需保留数据，需先插入分类、再更新 category_id、最后删列

-- 2a. 删除依赖 product 的外键表数据（开发阶段）
DELETE FROM stock_log;
DELETE FROM stock_check;
DELETE FROM inbound_order;
DELETE FROM outbound_order;
DELETE FROM stock;
DELETE FROM product;

-- 2b. 修改 product 表
ALTER TABLE product DROP COLUMN category;
ALTER TABLE product ADD COLUMN category_id BIGINT NOT NULL COMMENT '分类ID，关联category表' AFTER product_name;
ALTER TABLE product ADD CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id);
