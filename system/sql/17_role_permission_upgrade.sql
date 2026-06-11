-- 17_role_permission_upgrade.sql
-- 用户管理引入角色权限配置：角色可手动创建，并保存细粒度权限码。

USE market;

CREATE TABLE IF NOT EXISTS role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_code VARCHAR(80) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_code),
    KEY idx_role_permission_code (permission_code),
    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限表';

INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.permission_code
FROM role r
JOIN (
    SELECT 'dashboard:view' AS permission_code
    UNION ALL SELECT 'product:view'
    UNION ALL SELECT 'product:create'
    UNION ALL SELECT 'product:update'
    UNION ALL SELECT 'product:delete'
    UNION ALL SELECT 'brand:view'
    UNION ALL SELECT 'brand:manage'
    UNION ALL SELECT 'category:view'
    UNION ALL SELECT 'category:manage'
    UNION ALL SELECT 'sku:view'
    UNION ALL SELECT 'sku:manage'
    UNION ALL SELECT 'supplier:view'
    UNION ALL SELECT 'supplier:create'
    UNION ALL SELECT 'supplier:update'
    UNION ALL SELECT 'supplier:delete'
    UNION ALL SELECT 'supplier-sku:manage'
    UNION ALL SELECT 'inventory:view'
    UNION ALL SELECT 'stock:view'
    UNION ALL SELECT 'stock:update'
    UNION ALL SELECT 'stockcheck:view'
    UNION ALL SELECT 'stockcheck:create'
    UNION ALL SELECT 'stockcheck:complete'
    UNION ALL SELECT 'purchase:view'
    UNION ALL SELECT 'purchase:create'
    UNION ALL SELECT 'purchase:approve'
    UNION ALL SELECT 'purchase:receive'
    UNION ALL SELECT 'outbound:view'
    UNION ALL SELECT 'outbound:create'
    UNION ALL SELECT 'report:view'
    UNION ALL SELECT 'system:view'
    UNION ALL SELECT 'user:view'
    UNION ALL SELECT 'user:create'
    UNION ALL SELECT 'user:update'
    UNION ALL SELECT 'user:delete'
    UNION ALL SELECT 'role:manage'
) p
WHERE r.role_code = 'ADMIN'
ON DUPLICATE KEY UPDATE permission_code = role_permission.permission_code;

INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.permission_code
FROM role r
JOIN (
    SELECT 'dashboard:view' AS permission_code
    UNION ALL SELECT 'product:view'
    UNION ALL SELECT 'brand:view'
    UNION ALL SELECT 'category:view'
    UNION ALL SELECT 'sku:view'
    UNION ALL SELECT 'supplier:view'
    UNION ALL SELECT 'inventory:view'
    UNION ALL SELECT 'stock:view'
    UNION ALL SELECT 'stockcheck:view'
    UNION ALL SELECT 'purchase:view'
    UNION ALL SELECT 'outbound:view'
    UNION ALL SELECT 'report:view'
    UNION ALL SELECT 'system:view'
) p
WHERE r.role_code = 'USER'
ON DUPLICATE KEY UPDATE permission_code = role_permission.permission_code;
