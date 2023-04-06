-- init exchange database

DROP DATABASE IF EXISTS stocktrade;

CREATE DATABASE stocktrade;

USE stocktrade;


CREATE TABLE match_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sequence_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    counter_order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    counter_user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    price DECIMAL(36,18) NOT NULL,
    quantity DECIMAL(36,18) NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT UNI_OID_COID UNIQUE (order_id, counter_order_id),
    INDEX IDX_OID_CT (order_id,created_at),
    PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE `order` (
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sequence_id BIGINT NOT NULL,
    direction VARCHAR(32) NOT NULL,
    price DECIMAL(36,18) NOT NULL,
    quantity DECIMAL(36,18) NOT NULL,
    unfilled_quantity DECIMAL(36,18) NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY(order_id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE event_unique (
    unique_id VARCHAR(50) NOT NULL,
    sequence_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY(unique_id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE event_detail (
    id BIGINT NOT NULL,
    sequence_id BIGINT NOT NULL,
    previous_id BIGINT NOT NULL,
    data VARCHAR(10000) NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE tick (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sequence_id BIGINT NOT NULL,
    taker_order_id BIGINT NOT NULL,
    maker_order_id BIGINT NOT NULL,
    taker_direction BIT NOT NULL,
    price DECIMAL(36,18) NOT NULL,
    quantity DECIMAL(36,18) NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT UNI_T_M UNIQUE (taker_order_id, maker_order_id),
    INDEX IDX_CAT (created_at),
    PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE user_profile (
    user_id BIGINT NOT NULL,
    email VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT UNI_EMAIL UNIQUE (email),
    PRIMARY KEY(user_id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE password_auth (
    user_id BIGINT NOT NULL,
    random VARCHAR(100) NOT NULL,
    passwd VARCHAR(100) NOT NULL,
    PRIMARY KEY(user_id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;
