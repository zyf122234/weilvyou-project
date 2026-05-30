USE `seata`;

CREATE TABLE IF NOT EXISTS `global_table` (
  `xid` VARCHAR(128) NOT NULL,
  `transaction_id` BIGINT DEFAULT NULL,
  `status` TINYINT NOT NULL,
  `application_id` VARCHAR(32) DEFAULT NULL,
  `transaction_service_group` VARCHAR(32) DEFAULT NULL,
  `transaction_name` VARCHAR(128) DEFAULT NULL,
  `timeout` INT DEFAULT NULL,
  `begin_time` BIGINT DEFAULT NULL,
  `application_data` VARCHAR(2000) DEFAULT NULL,
  `gmt_create` DATETIME DEFAULT NULL,
  `gmt_modified` DATETIME DEFAULT NULL,
  PRIMARY KEY (`xid`),
  KEY `idx_status_gmt_modified` (`status`, `gmt_modified`),
  KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seata global transaction table';

CREATE TABLE IF NOT EXISTS `branch_table` (
  `branch_id` BIGINT NOT NULL,
  `xid` VARCHAR(128) NOT NULL,
  `transaction_id` BIGINT DEFAULT NULL,
  `resource_group_id` VARCHAR(32) DEFAULT NULL,
  `resource_id` VARCHAR(256) DEFAULT NULL,
  `branch_type` VARCHAR(8) DEFAULT NULL,
  `status` TINYINT DEFAULT NULL,
  `client_id` VARCHAR(64) DEFAULT NULL,
  `application_data` VARCHAR(2000) DEFAULT NULL,
  `gmt_create` DATETIME(6) DEFAULT NULL,
  `gmt_modified` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seata branch transaction table';

CREATE TABLE IF NOT EXISTS `lock_table` (
  `row_key` VARCHAR(128) NOT NULL,
  `xid` VARCHAR(128) DEFAULT NULL,
  `transaction_id` BIGINT DEFAULT NULL,
  `branch_id` BIGINT NOT NULL,
  `resource_id` VARCHAR(256) DEFAULT NULL,
  `table_name` VARCHAR(32) DEFAULT NULL,
  `pk` VARCHAR(36) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT '0' COMMENT '0:locked, 1:rollbacking',
  `gmt_create` DATETIME DEFAULT NULL,
  `gmt_modified` DATETIME DEFAULT NULL,
  PRIMARY KEY (`row_key`),
  KEY `idx_status` (`status`),
  KEY `idx_branch_id` (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seata lock table';

CREATE TABLE IF NOT EXISTS `distributed_lock` (
  `lock_key` CHAR(20) NOT NULL,
  `lock_value` VARCHAR(20) NOT NULL,
  `expire` BIGINT DEFAULT NULL,
  PRIMARY KEY (`lock_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seata distributed lock table';

INSERT INTO `distributed_lock` (`lock_key`, `lock_value`, `expire`)
VALUES ('AsyncCommitting', ' ', 0),
       ('RetryCommitting', ' ', 0),
       ('RetryRollbacking', ' ', 0),
       ('TxTimeoutCheck', ' ', 0)
ON DUPLICATE KEY UPDATE `lock_value` = VALUES(`lock_value`);
