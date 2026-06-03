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
