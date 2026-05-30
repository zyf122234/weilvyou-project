USE `travel_order_db`;

-- ========================================
-- 表定义
-- ========================================

CREATE TABLE IF NOT EXISTS `travel_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单编号',
  `order_no` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '购买用户编号',
  `product_id` BIGINT NOT NULL COMMENT '商品编号',
  `product_name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称快照',
  `product_cover_url` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商品封面快照',
  `merchant_id` BIGINT DEFAULT NULL COMMENT '商家用户编号',
  `merchant_name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商家名称快照',
  `date_start` DATE DEFAULT NULL COMMENT '开始日期',
  `date_end` DATE DEFAULT NULL COMMENT '结束日期',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `unit_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品单价快照',
  `total_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总价',
  `contact_name` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系人姓名',
  `phone` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='旅游订单表';

-- Seata AT 模式回滚日志表（订单服务作为分布式事务发起者）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='Seata AT 模式回滚日志表';
