# Travel 微服务旅游平台

一个基于 Spring Cloud Alibaba 和 Vue3 的微服务旅游平台项目，覆盖用户登录注册、商家入驻、商品管理、酒店搜索、订单支付、后台管理、智能客服、分布式事务、链路追踪、限流降级、任务调度和多级缓存等能力。

本项目定位为“具备生产化设计思路的微服务实战项目”，适合用于学习、简历展示和微服务组件集成实践。

## 项目亮点

- 微服务拆分：网关、用户、商品、订单、AI 客服等服务独立部署。
- 统一网关：基于 Spring Cloud Gateway 实现路由转发、JWT 鉴权、跨域处理。
- 权限控制：基于 Spring Security + JWT 实现登录认证和 RBAC 权限控制。
- 服务治理：接入 Nacos 注册发现，服务间通过 OpenFeign 调用。
- 限流降级：登录接口接入 Sentinel QPS 限流，Feign 接口配置 fallback。
- 分布式事务：订单支付使用 Seata AT 模式，保障订单状态和用户余额一致性。
- 搜索能力：商品搜索接入 Elasticsearch，支持关键词、城市、品牌、星级、价格区间筛选。
- 多级缓存：无筛选首页列表使用 Nginx proxy_cache + Redis + Caffeine 缓存。
- 缓存失效：商品新增、修改、上下架、删除后主动清理本地缓存、Redis 缓存和 Nginx 缓存。
- 异步处理：登录后通过 RabbitMQ 异步写入用户缓存。
- 分布式任务：使用 XXL-JOB 替代 Spring `@Scheduled`，定时清理超过 5 分钟的已取消订单。
- 链路追踪：接入 SkyWalking，支持查看网关、用户、商品、订单、AI 服务调用链路。
- 智能客服：基于 Spring AI + OpenAI 兼容客户端接入 DeepSeek，支持工具调用查询酒店、订单、余额等信息。
- 压测验证：提供 JMeter 脚本，用于首页商品列表接口压测。

## 技术栈

### 后端

| 分类 | 技术 |
| --- | --- |
| 基础框架 | Spring Boot 3.2.5、Spring Cloud 2023.0.1、Spring Cloud Alibaba 2023.0.1.0 |
| 服务注册与配置 | Nacos |
| 网关 | Spring Cloud Gateway |
| 安全认证 | Spring Security、JWT |
| 数据访问 | MyBatis-Plus、MySQL 8 |
| 缓存 | Redis、Caffeine、Nginx proxy_cache |
| 搜索 | Elasticsearch 7.12.1、Kibana |
| 消息队列 | RabbitMQ |
| 分布式事务 | Seata 2.0.0 |
| 限流降级 | Sentinel、OpenFeign fallback |
| 任务调度 | XXL-JOB 2.4.1 |
| 链路追踪 | SkyWalking 9.6 |
| AI 能力 | Spring AI、DeepSeek OpenAI 兼容接口 |
| 构建部署 | Maven、Docker Compose、Nginx |

### 前端

| 分类 | 技术 |
| --- | --- |
| 框架 | Vue 3 |
| 构建工具 | Vite |
| UI 组件 | Element Plus |
| 状态管理 | Pinia |
| 请求库 | Axios |
| 路由 | Vue Router |

## 项目结构

```text
.
├── travel-backend
│   ├── travel-gateway              # 网关服务
│   ├── travel-user                 # 用户服务
│   ├── travel-product              # 商品服务
│   ├── travel-order                # 订单服务
│   ├── travel-ai                   # 智能客服服务
│   ├── travel-api                  # Feign 接口与 DTO
│   ├── travel-common               # 公共模块
│   │   ├── travel-common-core
│   │   ├── travel-common-web
│   │   ├── travel-common-security
│   │   └── travel-common-data
│   ├── sql                         # 数据库初始化脚本
│   ├── nginx                       # Nginx 配置模板
│   ├── seata                       # Seata 配置
│   ├── Jmeter                      # 压测脚本
│   ├── docker-compose.yml
│   └── pom.xml
└── vue3-app                        # Vue3 前端项目
```

## 服务模块说明

| 服务 | 端口 | 说明 |
| --- | ---: | --- |
| travel-gateway | 8080 | 统一网关，负责路由、鉴权、跨域 |
| travel-user | 8081 | 用户注册登录、用户信息、商家申请、管理员审核、余额管理 |
| travel-product | 8082 | 商品管理、酒店搜索、ES 索引、首页缓存 |
| travel-order | 8084 | 下单、支付、取消订单、商家订单、管理员订单、XXL-JOB 执行器 |
| travel-ai | 8085 | 智能客服、会话存储、流式响应、工具调用 |
| travel-nginx | 80 | 前置反向代理和首页商品列表缓存 |
| xxl-job-admin | 9080 | XXL-JOB 调度中心 |
| skywalking-ui | 18080 | 链路追踪可视化页面 |
| nacos | 8848 | 注册中心和配置中心 |
| rabbitmq | 5672 / 15672 | 消息队列和管理台 |
| redis | 6379 | 缓存 |
| mysql | 3307 | 数据库 |
| elasticsearch | 9210 / 9300 | 搜索引擎 |
| kibana | 5601 | ES 可视化 |
| seata-server | 7091 / 8091 | Seata 控制台和事务协调器 |

## 核心业务流程

### 登录认证

```text
用户登录
  -> travel-gateway 转发
  -> travel-user 校验账号密码
  -> 生成 JWT
  -> RabbitMQ 异步通知写入 Redis 用户缓存
  -> 前端保存 token
  -> 后续请求通过 Authorization Header 访问
```

登录接口接入 Sentinel：

- 资源名：`userLogin`
- 默认 QPS：`10`
- 被限流时返回友好的业务提示

### 商品搜索与首页缓存

```text
前端 /lvyou 页面
  -> Nginx /api/product/hotel/search?current=1&all=true
  -> 命中 Nginx proxy_cache 则直接返回
  -> 未命中进入 Gateway
  -> travel-product 查询 Redis
  -> Redis 未命中查询 Caffeine
  -> 本地缓存未命中查询 Elasticsearch
  -> 写入 Caffeine + Redis
  -> 返回并写入 Nginx 缓存
```

当前只缓存无筛选首页列表请求：

```text
/api/product/hotel/search?current=1&all=true
```

带关键词、城市、品牌、星级或价格区间筛选时，不走首页缓存，直接查询 Elasticsearch。

### 商品变更缓存失效

商品新增、修改、上下架、删除后，会主动清理：

- Caffeine 本地缓存
- Redis 首页缓存
- Nginx proxy_cache 缓存文件

相关代码：

```text
travel-product/src/main/java/com/travel/product/service/HomeHotelCacheService.java
travel-product/src/main/java/com/travel/product/service/impl/ProductServiceImpl.java
```

### 订单支付与分布式事务

```text
用户支付订单
  -> travel-order 开启 Seata 全局事务
  -> 校验订单状态
  -> Feign 调用 travel-user 扣减余额
  -> 修改订单状态为已支付
  -> 任一环节失败，Seata 回滚订单和余额
```

核心注解：

```java
@GlobalTransactional(name = "order-pay", rollbackFor = Exception.class)
```

### XXL-JOB 订单清理任务

取消订单后，订单状态变为 `2`，并更新 `update_time`。

XXL-JOB 每分钟调度一次：

```text
任务名称：清理超过5分钟的已取消订单
JobHandler：deleteExpiredCanceledOrders
Cron：0 0/1 * * * ?
```

执行逻辑：

```sql
DELETE FROM travel_order
WHERE status = 2
  AND update_time <= 当前时间 - 5分钟
```

执行器：

```text
travel-order-executor
```

### 智能客服工具调用

AI 客服支持会话存储和流式响应。每个用户登录后进入智能客服页面，会生成当前登录状态下的会话，退出登录后当前会话失效。

支持的工具函数包括：

- `searchHotels`：查询酒店
- `getHotelDetail`：查询酒店详情
- `listMyOrders`：查询我的订单
- `getMyOrderDetail`：查询订单详情
- `getCurrentUserInfo`：查询用户信息和余额
- `getMerchantByKeyword`：查询商家信息

## 本地启动

### 环境要求

- JDK 17
- Maven 3.8+
- Node.js 18+
- Docker Desktop
- MySQL、Redis、RabbitMQ、Nacos 等通过 Docker Compose 启动

### 1. 配置环境变量

后端配置文件位于：

```text
travel-backend/.env
```

首次提交到 Git 仓库前，建议复制为：

```text
travel-backend/.env.example
```

并删除真实密钥，例如：

```text
DEEPSEEK_API_KEY=your_deepseek_api_key
MYSQL_ROOT_PASSWORD=your_mysql_password
TRAVEL_JWT_SECRET=your_jwt_secret
XXL_JOB_ACCESS_TOKEN=your_xxl_job_token
```

真实 `.env` 不建议提交到远程仓库。

### 2. 启动后端中间件和服务

进入后端目录：

```bash
cd travel-backend
```

编译后端：

```bash
mvn -DskipTests package
```

启动所有容器：

```bash
docker compose up -d
```

查看容器状态：

```bash
docker compose ps
```

如果只想重建某个服务，例如订单服务：

```bash
mvn -pl travel-order -am -DskipTests package
docker compose build travel-order
docker compose up -d travel-order
```

### 3. 启动前端

进入前端目录：

```bash
cd vue3-app
```

安装依赖：

```bash
npm install
```

启动开发服务：

```bash
npm run dev
```

生产构建：

```bash
npm run build
```

默认访问：

```text
http://localhost:5173
```

## 服务器部署与公网访问

项目在 Docker Compose 环境中通过 `travel-nginx` 作为统一入口。Nginx 负责提供前端静态页面，并将 `/api/` 请求反向代理到 `travel-gateway`。

生产或服务器环境推荐访问入口：

```text
http://服务器公网IP/
```

如果绑定了域名，也可以访问：

```text
http://你的域名/
```

### 部署流程

1. 构建前端静态资源

   ```bash
   cd vue3-app
   npm install
   npm run build
   ```

2. 将前端构建产物放到后端 Nginx 挂载目录

   `docker-compose.yml` 中 `travel-nginx` 挂载的是：

   ```text
   ./frontend/dist:/usr/share/nginx/html:ro
   ```

   因此服务器部署时需要保证后端目录下存在：

   ```text
   travel-backend/frontend/dist
   ```

   该目录内容应来自 `vue3-app/dist`。

3. 构建并启动后端服务

   ```bash
   cd travel-backend
   mvn -DskipTests package
   docker compose up -d
   ```

4. 检查容器健康状态

   ```bash
   docker compose ps
   ```

5. 访问公网入口

   ```text
   http://服务器公网IP/
   ```

### 公网端口说明

对外访问完整项目通常只需要开放：

| 端口 | 用途 |
| ---: | --- |
| 80 | Nginx 统一入口，提供前端页面并代理 `/api/` |
| 443 | HTTPS 入口，配置证书后使用 |

云服务器安全组、防火墙或宝塔面板中需要放行 `80` 端口。如果配置 HTTPS，则同时放行 `443` 端口。

不建议向公网开放 MySQL、Redis、RabbitMQ、Nacos、Elasticsearch、Seata、SkyWalking、XXL-JOB 等内部或管理端口。调试时如需访问这些控制台，建议仅允许自己的固定 IP 访问，或使用 SSH 隧道。

### 访问链路

```text
浏览器
  -> http://服务器公网IP/
  -> travel-nginx:80
  -> 前端静态页面
  -> /api/* 请求
  -> travel-gateway:8080
  -> travel-user / travel-product / travel-order / travel-ai
```

前端请求使用相对路径 `/api`，因此部署到服务器后通常不需要把前端接口地址改成公网 IP；只要浏览器访问的是 Nginx 入口，接口请求就会自动走同一个域名或 IP。

## 常用访问地址

| 服务 | 地址 |
| --- | --- |
| 前端页面 | http://localhost:5173 |
| Nginx 入口 | http://localhost |
| Gateway | http://localhost:8080 |
| Nacos | http://localhost:8848/nacos |
| RabbitMQ 管理台 | http://localhost:15672 |
| Kibana | http://localhost:5601 |
| SkyWalking UI | http://localhost:18080 |
| Seata 控制台 | http://localhost:7091 |
| XXL-JOB Admin | http://localhost:9080/xxl-job-admin |

XXL-JOB 默认账号：

```text
admin / 123456
```

RabbitMQ 默认账号：

```text
guest / guest
```

## 主要接口

### 用户服务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/user/register` | 用户注册 |
| POST | `/api/user/login` | 用户登录 |
| POST | `/api/user/logout` | 退出登录 |
| GET | `/api/user/info` | 当前用户信息 |
| PUT | `/api/user/info` | 修改当前用户信息 |
| POST | `/api/user/recharge` | 余额充值 |
| POST | `/api/user/apply-merchant` | 申请成为商家 |
| GET | `/api/user/admin/users` | 管理员用户列表 |
| PUT | `/api/user/admin/users/{id}/status` | 管理员修改用户状态 |
| GET | `/api/user/admin/merchant-applications` | 商家申请列表 |
| PUT | `/api/user/admin/merchant-applications/{id}` | 审核商家申请 |

### 商品服务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/product/published` | 已发布商品列表 |
| GET | `/api/product/published/{id}` | 商品详情 |
| GET | `/api/product/hotel/search` | 酒店搜索 |
| GET | `/api/product/merchant` | 商家商品列表 |
| GET | `/api/product/admin` | 管理员商品列表 |
| POST | `/api/product` | 新增商品 |
| POST | `/api/product/cover` | 上传商品封面 |
| PUT | `/api/product/{id}` | 修改商品 |
| PATCH | `/api/product/{id}/status` | 修改商品状态 |
| DELETE | `/api/product/{id}` | 删除商品 |

### 订单服务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/order/orders` | 创建订单 |
| GET | `/api/order/orders/{id}` | 查询我的订单详情 |
| PATCH | `/api/order/orders/{id}/pay` | 支付订单 |
| PATCH | `/api/order/orders/{id}/cancel` | 取消订单 |
| GET | `/api/order/orders/mine` | 我的订单列表 |
| GET | `/api/order/orders/merchant` | 商家订单列表 |
| GET | `/api/order/orders/admin/all` | 管理员订单列表 |

### AI 客服服务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ai/customer-service/session/current` | 获取当前会话 |
| POST | `/api/ai/customer-service/chat` | 流式对话 |
| POST | `/api/ai/customer-service/session/invalidate-current` | 失效当前会话 |

## 压测说明

压测脚本目录：

```text
travel-backend/Jmeter
```

已包含：

- 首页酒店列表最大 QPS 脚本

推荐使用 JMeter 非 GUI 模式执行压测，例如：

```bash
jmeter -n -t travel-backend/Jmeter/home-hotel-search-all-max-qps-test.jmx -Jhost=localhost -Jport=80 -Jthreads=1000 -Jduration=60
```

压测时建议关注：

- 吞吐量
- 平均响应时间
- P95 / P99 响应时间
- 错误率
- 响应体大小
- Docker 容器 CPU 和内存
- MySQL / Redis / Elasticsearch 负载

## 缓存验证

首页无筛选商品列表走 Nginx 缓存。

请求：

```bash
curl -I "http://localhost/api/product/hotel/search?current=1&all=true&keyword=&city=&starName=&brand=&priceRange="
```

关注响应头：

```text
X-Cache-Status: HIT
X-Home-Cacheable: 1
```

常见状态：

| 状态 | 说明 |
| --- | --- |
| MISS | 首次请求，缓存未命中 |
| HIT | 缓存命中 |
| STALE | 使用过期缓存兜底 |
| UPDATING | 后台正在刷新缓存 |
| BYPASS | 不符合缓存条件，直接转发后端 |

## XXL-JOB 验证

进入管理台：

```text
http://localhost:9080/xxl-job-admin
```

检查：

1. 执行器管理中存在 `travel-order-executor`
2. 任务管理中存在 `清理超过5分钟的已取消订单`
3. 调度日志中 `trigger_code=200` 且 `handle_code=200`

也可以直接查询数据库：

```sql
SELECT id, job_id, executor_handler, trigger_time, trigger_code, handle_time, handle_code
FROM xxl_job.xxl_job_log
ORDER BY id DESC
LIMIT 5;
```

## SkyWalking 验证

访问：

```text
http://localhost:18080
```

触发登录、商品搜索、订单支付等接口后，可以看到：

- travel-gateway
- travel-user
- travel-product
- travel-order
- travel-ai
- MySQL
- Redis
- Elasticsearch

如果看不到某个服务，先确认该服务容器已启动，并且 Dockerfile 中已加入 SkyWalking Java Agent。

## Seata 验证

支付订单会触发 Seata 全局事务。

核心场景：

```text
travel-order 支付订单
  -> Feign 调用 travel-user 扣减余额
  -> 更新订单状态
```

可以观察：

- Seata 控制台：http://localhost:7091
- `seata.global_table`
- `seata.branch_table`
- 各业务库 `undo_log`

## 开发注意事项

1. 不要提交真实 `.env`

   `.env` 中包含 API Key、数据库密码、JWT 密钥等敏感信息。提交代码前应改为 `.env.example`。

2. 开发环境的 `restart` 策略为 `"no"`

   当前 `docker-compose.yml` 中容器重启策略是：

   ```yaml
   restart: "no"
   ```

   适合开发调试。生产环境建议改为：

   ```yaml
   restart: unless-stopped
   ```

3. 部分模块开启 SQL stdout 日志

   `travel-product` 和 `travel-order` 当前存在 MyBatis SQL 输出配置，方便调试，但生产环境建议关闭或按 profile 区分。

4. 公网部署时只开放必要入口

   Docker Compose 中多个基础组件和管理控制台都配置了宿主机端口映射。服务器部署时建议安全组只开放 `80/443`，其他端口仅允许内网或固定管理 IP 访问。

5. 前端构建存在大 chunk 警告

   当前前端可正常构建，但部分打包产物较大。后续可以通过路由懒加载和 `manualChunks` 优化。

6. 建议补充测试

   当前项目更偏功能集成实战，单元测试和集成测试还不完整。建议优先补：

   - 用户登录测试
   - 订单支付事务测试
   - 商品搜索测试
   - XXL-JOB 任务测试
   - AI 工具调用测试

## 描述示例

```text
微服务旅游平台：基于 Spring Cloud Alibaba + Vue3 实现用户、商品、订单、商家后台和智能客服等功能。
系统拆分为 Gateway、User、Product、Order、AI 等服务，接入 Nacos、Redis、RabbitMQ、Elasticsearch、Seata、Sentinel、SkyWalking、XXL-JOB 等组件。
负责订单支付链路的 Seata 分布式事务、首页商品列表的 Nginx + Redis + Caffeine 多级缓存、登录接口 Sentinel 限流、Feign 降级、XXL-JOB 订单清理任务和 Spring AI 工具调用客服能力。
使用 JMeter 对首页商品列表接口进行压测，并根据响应体大小、缓存命中率和链路追踪结果进行性能分析。
```

## 当前项目状态

- 后端全量 Maven 构建通过。
- 前端 `npm run build` 构建通过。
- Docker Compose 中核心服务可启动并保持健康。
- XXL-JOB 任务已成功调度。
- 首页缓存可通过 `X-Cache-Status` 响应头验证。
