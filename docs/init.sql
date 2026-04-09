-- 创建数据库
CREATE DATABASE IF NOT EXISTS ds_0 DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ds_1 DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 在ds_0数据库中创建表
USE ds_0;
CREATE TABLE IF NOT EXISTS t_order_0 (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT,
    order_name VARCHAR(100),
    amount DOUBLE
);

CREATE TABLE IF NOT EXISTS t_order_1 (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT,
    order_name VARCHAR(100),
    amount DOUBLE
);

-- 在ds_1数据库中创建表
USE ds_1;
CREATE TABLE IF NOT EXISTS t_order_0 (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT,
    order_name VARCHAR(100),
    amount DOUBLE
);

CREATE TABLE IF NOT EXISTS t_order_1 (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT,
    order_name VARCHAR(100),
    amount DOUBLE
);
