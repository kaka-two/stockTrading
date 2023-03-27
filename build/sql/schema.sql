-- init exchange database

DROP DATABASE IF EXISTS stocktrade;

CREATE DATABASE stocktrade;

USE stocktrade;


CREATE TABLE match_details (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               sequenceId BIGINT NOT NULL,
                               orderId BIGINT NOT NULL,
                               counterOrderId BIGINT NOT NULL,
                               userId BIGINT NOT NULL,
                               counterUserId BIGINT NOT NULL,
                               type VARCHAR(32) NOT NULL,
                               direction VARCHAR(32) NOT NULL,
                               price DECIMAL(36,18) NOT NULL,
                               quantity DECIMAL(36,18) NOT NULL,
                               createdAt BIGINT NOT NULL,
                               CONSTRAINT UNI_OID_COID UNIQUE (orderId, counterOrderId),
                               INDEX IDX_OID_CT (orderId,createdAt),
                               PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE orders (
                        orderId BIGINT NOT NULL,
                        userId BIGINT NOT NULL,
                        sequenceId BIGINT NOT NULL,
                        direction VARCHAR(32) NOT NULL,
                        price DECIMAL(36,18) NOT NULL,
                        quantity DECIMAL(36,18) NOT NULL,
                        unfilledQuantity DECIMAL(36,18) NOT NULL,
                        orderStatus VARCHAR(32) NOT NULL,
                        createdAt BIGINT NOT NULL,
                        updatedAt BIGINT NOT NULL,
                        PRIMARY KEY(orderId)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE ticks (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       sequenceId BIGINT NOT NULL,
                       takerOrderId BIGINT NOT NULL,
                       makerOrderId BIGINT NOT NULL,
                       takerDirection BIT NOT NULL,
                       price DECIMAL(36,18) NOT NULL,
                       quantity DECIMAL(36,18) NOT NULL,
                       createdAt BIGINT NOT NULL,
                       CONSTRAINT UNI_T_M UNIQUE (takerOrderId, makerOrderId),
                       INDEX IDX_CAT (createdAt),
                       PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;
