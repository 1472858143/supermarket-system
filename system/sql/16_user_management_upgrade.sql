-- 16_user_management_upgrade.sql
-- 用户管理升级：工号、邮箱、联系方式、最近登录时间。

ALTER TABLE user
    ADD COLUMN employee_no VARCHAR(20) DEFAULT NULL COMMENT '工号，格式 EMPyyyyNNNN' AFTER id,
    ADD COLUMN email VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER real_name,
    ADD COLUMN contact_phone VARCHAR(30) DEFAULT NULL COMMENT '联系方式' AFTER email,
    ADD COLUMN last_login_time DATETIME DEFAULT NULL COMMENT '最近一次登录时间' AFTER create_time;

UPDATE user u
INNER JOIN (
    SELECT
        id,
        CONCAT(
            'EMP',
            DATE_FORMAT(create_time, '%Y'),
            LPAD(ROW_NUMBER() OVER (PARTITION BY YEAR(create_time) ORDER BY id), 4, '0')
        ) AS generated_employee_no
    FROM user
) seq ON seq.id = u.id
SET u.employee_no = seq.generated_employee_no
WHERE u.employee_no IS NULL OR TRIM(u.employee_no) = '';

UPDATE user
SET contact_phone = '00000000000'
WHERE contact_phone IS NULL OR TRIM(contact_phone) = '';

ALTER TABLE user
    MODIFY employee_no VARCHAR(20) NOT NULL COMMENT '工号，格式 EMPyyyyNNNN',
    MODIFY contact_phone VARCHAR(30) NOT NULL COMMENT '联系方式',
    ADD UNIQUE KEY uk_user_employee_no (employee_no);
