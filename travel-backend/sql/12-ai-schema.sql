CREATE DATABASE IF NOT EXISTS `travel_ai_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `travel_ai_db`;

CREATE TABLE IF NOT EXISTS `ai_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `conversation_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AI conversation id',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `login_session_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Current login session id',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-invalid 1-active',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `invalid_time` DATETIME DEFAULT NULL COMMENT 'Invalid time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_id` (`conversation_id`),
  KEY `idx_user_login_session` (`user_id`, `login_session_id`),
  KEY `idx_user_status_updated` (`user_id`, `status`, `updated_time`),
  KEY `idx_login_session_status` (`login_session_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI customer service conversation';

CREATE TABLE IF NOT EXISTS `ai_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `conversation_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AI conversation id',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `role` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'USER or ASSISTANT',
  `content` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Message content',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_time` (`conversation_id`, `created_time`, `id`),
  KEY `idx_user_time` (`user_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI customer service message';
