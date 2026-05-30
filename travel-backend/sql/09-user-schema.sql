USE `travel_user_db`;

-- ========================================
-- 表定义
-- ========================================

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  `username` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像地址',
  `email` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `balance` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色编号',
  `role_name` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键编号',
  `user_id` BIGINT NOT NULL COMMENT '用户编号',
  `role_id` BIGINT NOT NULL COMMENT '角色编号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 商家申请表
CREATE TABLE IF NOT EXISTS `merchant_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请编号',
  `user_id` BIGINT NOT NULL COMMENT '申请人用户编号',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝',
  `reason` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核原因',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人用户编号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='商家申请表';

-- Seata AT 模式回滚日志表（用户服务作为分布式事务参与者）
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

-- ========================================
-- 种子数据
-- ========================================

-- 初始化角色数据
INSERT INTO `sys_role` (`role_name`, `role_code`) VALUES
('管理员', 'ROLE_ADMIN'),
('普通用户', 'ROLE_USER'),
('商户', 'ROLE_MERCHANT');

-- 初始化管理员账号（密码: 123456）
INSERT INTO `sys_user` (`username`, `password`, `nickname`) VALUES
('admin', '$2a$10$EqKcp1WFKVQISheBxnFOheYMKMeFSmVPfaAJMRPJhVPPRHPVdOKGm', '管理员');

-- 初始化三位商家用户（密码: 123456）
INSERT INTO `sys_user` (`username`, `password`, `nickname`) VALUES
('zhang1', '$2a$10$EqKcp1WFKVQISheBxnFOheYMKMeFSmVPfaAJMRPJhVPPRHPVdOKGm', '北京商家'),
('zhang2', '$2a$10$EqKcp1WFKVQISheBxnFOheYMKMeFSmVPfaAJMRPJhVPPRHPVdOKGm', '深圳商家'),
('zhang3', '$2a$10$EqKcp1WFKVQISheBxnFOheYMKMeFSmVPfaAJMRPJhVPPRHPVdOKGm', '上海商家');

-- 关联管理员角色（admin -> ROLE_ADMIN）
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 关联商家用户角色（zhang1/zhang2/zhang3 -> ROLE_USER + ROLE_MERCHANT）
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(2, 2), (2, 3),
(3, 2), (3, 3),
(4, 2), (4, 3);
