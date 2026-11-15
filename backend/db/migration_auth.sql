-- =============================================================
-- 登录/管理员功能迁移脚本（仅对「已存在的旧库」执行一次）
-- 作用：给 user 表补充 登录用户名 / 密码 / 角色 三个字段
-- 注意：管理员账号由应用启动时自动创建（AdminInitializer），无需在此 INSERT
-- 执行方式：DataGrip 选中本文件执行，或命令行 source 本文件
-- =============================================================

USE shoping;

ALTER TABLE user
    ADD COLUMN username VARCHAR(64)  NULL COMMENT '登录用户名（管理员）' AFTER phone,
    ADD COLUMN password VARCHAR(255) NULL COMMENT '登录密码（BCrypt 加密）' AFTER username,
    ADD COLUMN role     TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0顾客 1管理员' AFTER password,
    ADD UNIQUE KEY uk_username (username);
