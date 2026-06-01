-- 13_add_brand.sql
-- 品牌管理与 SPU 绑定：brand 1:N product，SKU 不直接绑定品牌。

USE market;

CREATE TABLE IF NOT EXISTS brand (
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

INSERT INTO brand (brand_code, brand_name, status, remark)
SELECT 'BRD000001', '默认品牌', 1, '历史商品迁移默认品牌'
WHERE NOT EXISTS (
    SELECT 1 FROM brand WHERE brand_code = 'BRD000001' OR brand_name = '默认品牌'
);

ALTER TABLE product
    ADD COLUMN brand_id BIGINT NULL COMMENT '品牌ID，关联brand表' AFTER category_id;

UPDATE product
SET brand_id = (
    SELECT id FROM brand WHERE brand_code = 'BRD000001'
);

ALTER TABLE product
    MODIFY brand_id BIGINT NOT NULL COMMENT '品牌ID，关联brand表';

ALTER TABLE product
    ADD KEY idx_product_brand (brand_id),
    ADD CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES brand(id);
