-- 只在数据库不存在时创建；现有数据库不会因此被删除。
CREATE DATABASE IF NOT EXISTS citylink
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE citylink;

-- 用户名由唯一索引保证不能重复。
CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_user_username (username)
);