-- ========================================
-- 创建数据库（仅微服务架构）
-- ========================================

-- Nacos 服务注册与配置中心
CREATE DATABASE IF NOT EXISTS `nacos` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Seata Server DB 模式存储库
CREATE DATABASE IF NOT EXISTS `seata` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 用户服务
CREATE DATABASE IF NOT EXISTS `travel_user_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 商品服务
CREATE DATABASE IF NOT EXISTS `travel_product_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 订单服务
CREATE DATABASE IF NOT EXISTS `travel_order_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AI service
CREATE DATABASE IF NOT EXISTS `travel_ai_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- XXL-JOB 调度中心
CREATE DATABASE IF NOT EXISTS `xxl_job` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
