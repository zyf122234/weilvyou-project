-- Seata AT 模式回滚日志表
-- 只需要在参与分布式事务的服务数据库中创建
-- 当前：travel-order（发起者）和 travel-user（参与者）需要此表
-- travel-product 不参与分布式事务，不需要此表

-- 用户服务数据库（参与者）
USE `travel_user_db`;

CREATE TABLE IF NOT EXISTS `undo_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键编号',
  `branch_id` BIGINT NOT NULL COMMENT '分支事务编号',
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事务编号',
  `context` VARCHAR(128) NOT NULL COMMENT '上下文',
  `rollback_info` LONGBLOB NOT NULL COMMENT '回滚信息',
  `log_status` INT NOT NULL COMMENT '0-正常 1-全局已完成',
  `log_created` DATETIME(6) NOT NULL COMMENT '创建时间',
  `log_modified` DATETIME(6) NOT NULL COMMENT '修改时间',
  `ext` VARCHAR(100) DEFAULT NULL COMMENT '扩展字段',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xid_branch_id` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seata AT 模式回滚日志表';

-- 订单服务数据库（发起者，已在 11-order-schema.sql 中创建）
