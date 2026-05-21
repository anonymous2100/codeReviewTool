# Code Review Bot

> 面向 Java 项目的 AI 自动化代码审查工具 —— 基于 LangChain4j 框架集成大模型（DeepSeek / 智谱 / Kimi / Doubao 等），对 Git 提交变更进行智能评审，结果回写 GitLab合并请求/Commit 评论区。

---

## 实现原理

```
Developer push/MR ──► GitLab Webhook ──► Code Review Bot ──► AI API (LangChain4j)
                                                  │
                                           ┌──────┴──────┐
                                           ▼              ▼
                                     审查报告生成    合并请求 评论回写
```

1. **事件感知**：GitLab 在 Push / Merge Request 事件发生时，通过 Webhook 将事件 Payload POST 到 Bot 服务
2. **Diff 获取**：Bot 调用 GitLab API 拉取合并请求/Commit 的完整 Diff（统一 diff 格式），按文件拆分
3. **AI 分析**：逐文件构建审查 Prompt，通过 LangChain4j 并发调用 AI 模型进行三维度分析（安全/性能/规范）
4. **结果聚合**：解析 AI 返回的结构化输出，聚合为 Markdown 格式审查报告
5. **评论回写**：通过 GitLab合并请求 Note API 或 Commit Comment API 将报告以评论形式写入

---

## 架构设计

```
┌──────────────────────────────────────────────────────────────┐
│                   GitLab / Git Client                         │
│     Push Event  ──►  Webhook ──► 合并请求 Event                   │
└──────────────────────┬───────────────────────────────────────┘
                        │ HTTP POST
                        ▼
┌──────────────────────────────────────────────────────────────┐
│            Code Review Bot (Spring Boot 3 + Java 17)          │
│                                                               │
│  Webhook/GitHookController ─► SignatureFilter ─► RateLimit   │
│                                           │                   │
│                                           ▼                   │
│                                    EventDispatcher           │
│                                     │        │               │
│                          ┌──────────┼────────┼──────┐        │
│                          ▼          ▼        ▼      ▼        │
│                    DedupService  DiffFetcher  BranchCheck     │
│                    (Caffeine+DB)    │                         │
│                                     ▼                         │
│                              GitLabClient ───────────────► GitLab API
│                                     │                        │
│                          ┌──────────┘                        │
│                          ▼                                   │
│                    FileFilter ─► AiReviewService ─────────► AI API
│                    (并发 Semaphore)                           │
│                          │                                   │
│                          ▼                                   │
│                    ReportBuilder                              │
│                          │                                   │
│                          ▼                                   │
│                   GitLabCommentService ──────────────────► GitLab API
│                          │                                   │
│                          ▼                                   │
│                   ReviewRecordService ──────────────────► MySQL
└──────────────────────────────────────────────────────────────┘
                        │
                        ▼
           ┌────────────────────────┐
           │  Merge Request /       │
           │  Commit Comment        │
           │  (结构化审查报告)       │
           └────────────────────────┘
```

### 管道式处理链路

```
Webhook → HMAC 验签 → IP 限流 → 去重(内存+DB) →
拉取Diff → 文件过滤 → 分支匹配 → 并发AI分析(信号量控制) →
报告聚合 → 回写评论 → 持久化 → Prometheus 指标
```

---

## 技术栈

### 后端

| 技术 | 用途 |
|------|------|
| Java 17 | 运行环境 |
| Spring Boot 3.2 | 应用框架 |
| MyBatis-Plus | ORM 框架 |
| LangChain4j (OpenAI 兼容) | AI 大模型框架（多厂商抽象） |
| Spring Retry | 注解驱动重试机制 |
| Spring AOP | 面向切面编程 |
| Spring Boot Actuator | 健康检查 / 监控端点 |
| Maven | 构建工具 |
| MySQL | 数据库 |
| Caffeine Cache | 本地缓存（幂等去重 + IP 限流） |
| Google Guava | 工具库（ThreadFactoryBuilder 等） |
| Jackson | JSON 序列化 |
| Micrometer + Prometheus | 指标暴露与采集 |
| Testcontainers | 集成测试容器 |
| OkHttp MockWebServer | HTTP 模拟测试 |
| Spotless (Eclipse Formatter) | 代码格式检查（Allman 风格） |
| OpenRewrite | 自动代码修复（补全大括号等） |

### DevOps

| 技术 | 用途 |
|------|------|
| Prometheus | 监控指标采集 |



---

## 模块功能说明

### 后端模块

| 模块 | 核心功能 |
|------|----------|
| **Webhook Controller** | 接收 GitLab Push /合并请求 Webhook 事件；使用 `X-Gitlab-Event` 头路由分发；Push→单Commit审查，MR→全量Diff审查 |
| **Git Hook Controller** | 接收 Git 原生 post-receive Hook（适用于自建仓库），解析 `oldrev` / `newrev` / `refname` |
| **Project Controller** | 查询当前 Token 有权限的 GitLab 项目列表，用于前端项目选择器 |
| **Webhook Signature Filter** | `OncePerRequestFilter` + `ContentCachingRequestWrapper` 缓存请求体；HMAC-SHA256 签名验签防止重放与伪造；支持 `X-Gitlab-Token` 和 `X-Hook-Secret` 两种头 |
| **Rate Limit Interceptor** | 基于 IP 的 Caffeine 本地缓存限流（60次/分钟），返回 HTTP 429 |
| **Event Dispatcher** | 事件路由与全流程编排；`@Async` 异步执行避免 GitLab Webhook 超时；MR 事件先校验 `shouldTriggerReview`（仅 opened + 目标分支匹配）；异常时自动写兜底评论 |
| **Dedup Service** | 基于 Caffeine 本地缓存 (24h TTL, 10K 上限) + 数据库 `existsByCommitSha` 双重去重 |
| **Diff Fetcher** | 调用 GitLab API 获取合并请求 Diff 或 Commit Diff，支持自动分页（`X-Next-Page`） |
| **File Filter** | 跳过：非 Java 文件、仅删除文件、超行数 Diff、Glob 排除模式（`**/*Test.java` / `**/generated/**` / `**/target/**`）、`@Generated` 注解代码 |
| **AI Review Engine** | 基于 LangChain4j `ChatLanguageModel`（OpenAI 兼容），逐文件构建 Prompt；`Semaphore(maxConcurrency)` + 专用线程池并发调用 AI API；正则解析结构化输出（section + issue pattern）；切换厂商只需改配置 |
| **Retry Handler** | Spring Retry 注解驱动，指数退避重试（1s→2s→4s→10s, 最多4次），仅对 `RateLimitException` / `TimeoutException` / `SocketTimeoutException` 重试；超出后抛 `ReviewFailedException` |
| **Report Builder** | 聚合各文件分析结果，生成包含严重度标记、行号定位、修复建议、风险等级的 Markdown 报告 |
| **GitLab Comment** | 将报告写入合并请求 Notes 或 Commit Comments；AI 不可用时写入兜底评论提示人工 Review |
| **Review Record** | MyBatis-Plus 持久化审查记录和文件级详情；支持按项目+时间范围查询统计和趋势 |
| **Review Metrics** | 暴露 Prometheus 指标：`reviewbot.reviews.total`, `reviewbot.api.call.duration`, `reviewbot.api.call.failures`, `reviewbot.files.skipped`, `reviewbot.reviews.in_progress` |
| **Global Exception Handler** | 统一错误码映射：1001(参数错误), 1002(Token校验), 2001(GitLab API错误), 2002(AI不可用/Diff拉取失败), 2003(超时), 3001(内部错误), 3002(请求体错误) |

### AI 审查三维度

| 维度 | 检查项 |
|------|--------|
|  **安全漏洞** | SQL/命令/表达式注入, 敏感信息泄露, 权限绕过, 不安全加密/随机数 |
|  **性能隐患** | N+1查询, 大事务/长事务, 全表查询, 大对象创建/内存泄漏, 线程安全 |
|  **编码规范** | 异常处理不当, 空指针风险, 资源未释放, 日志级别不当 |

---

## 使用说明

### 1. 环境准备

- **JDK 17+** (编译运行)
- **Maven 3.9+** (后端构建)
- **GitLab 项目** (需要有 API 访问权限)
- **AI API Key**（DeepSeek / 智谱 / Kimi / Doubao 任选一种）

### 2. GitLab 配置

只有将 Bot 部署到公网服务器时才需要配置 Webhook，用于 GitLab 自动推送事件到 Bot。

1. 进入 GitLab 项目 → **Settings** → **Webhooks**
2. **URL**: 填写 Bot 的公网可访问地址，例如 `https://your-bot-domain.com/api/webhook/gitlab`
3. **Secret Token**: 设置一个随机字符串（与 `WEBHOOK_SECRET` 环境变量一致）。GitLab 会以该密钥对请求体进行 HMAC-SHA256 签名并放入 `X-Gitlab-Token` 头
4. 勾选事件: **Push events** + **Merge request events**
5. 进入 **Settings** → **Access Tokens**, 创建一个 Project Access Token:
   - 权限: `read_api`
   - 记录 Token 值（作为 `GITLAB_TOKEN` 环境变量）

### 2.1 使用 GitLab SaaS 测试（推荐）

无需本地搭建 GitLab 服务器，直接使用 [gitlab.com](https://gitlab.com) 即可完成全流程测试。

**① 创建测试项目**

1. 登录 [gitlab.com](https://gitlab.com) → **New project** → **Create blank project**
2. 项目名称随意（如 `review-bot-test`）
3. 创建后记下 **Project ID**（项目首页右上角，如 `12345678`）

**② 生成 Personal Access Token**

1. 点右上角头像 → **Preferences** → **Access Tokens** → **Add new token**
2. 填写：
   - **Token name**: `review-bot`
   - **Expiration date**: 选一个较长的有效期
   - **Scopes**: 勾选 `read_api`
3. 点 **Create project access token**，**复制 Token 值**

**③ 创建测试合并请求**

1. 进入项目 → **Code** → **Branches** → **New branch**，建一个 `feature/test-branch`
2. 新建一个 `TestService.java`，写入一段包含空指针或 SQL 拼接的 Java 代码
3. 提交后 **Create merge request**，目标分支选 `main`
4. 记下合并请求 的 IID（列表页的数字编号，如 `1`）和最新 Commit SHA

**④ 启动服务并手动触发**

```bash
# 配置环境变量
set AI_API_KEY=sk-your-key
set AI_BASE_URL=https://api.deepseek.com/v1
set AI_MODEL=deepseek-chat
set AI_PROVIDER=deepseek
set GITLAB_TOKEN=glpat-你生成的token
set GITLAB_BASE_URL=https://gitlab.com/api/v4
set WEBHOOK_SECRET=test123
set DB_PASSWORD=your-mysql-password

# 启动后端
mvn spring-boot:run
```

另开终端，用真实 Project ID 模拟 Webhook 触发审查：

```powershell
$secret = "test123"
$body = '{"object_kind":"merge_request","project":{"id":你的ProjectID},"object_attributes":{"iid":MR的IID,"source_branch":"feature/test-branch","target_branch":"main","last_commit":{"id":"最新Commit SHA"},"state":"opened"}}'
$hmac = [System.Convert]::ToBase64String((New-Object System.Security.Cryptography.HMACSHA256(([text.encoding]::UTF8.GetBytes($secret)))).ComputeHash([text.encoding]::UTF8.GetBytes($body)))
curl -X POST http://localhost:8099/api/webhook/gitlab -H "Content-Type: application/json" -H "X-Gitlab-Event: Merge Request Hook" -H "X-Gitlab-Token: $hmac" -d $body
```

**⑤ 验证结果**

```bash
# 查看日志
tail -f logs/review-bot.log

# 健康检查
curl http://localhost:8099/actuator/health

# 去 GitLab合并请求 页面查看 Bot 写的评论
```

### 3. 环境变量配置

```bash
cp .env.example .env
```

编辑 `.env` 文件:

```env
# AI Provider Configuration (supports DeepSeek, Zhipu, Kimi, Doubao via OpenAI-compatible API)
AI_PROVIDER=deepseek
AI_API_KEY=sk-your-api-key-here
AI_BASE_URL=https://api.deepseek.com/v1
AI_MODEL=deepseek-chat

# GitLab Configuration
GITLAB_TOKEN=glpat-your-gitlab-token-here
GITLAB_BASE_URL=https://gitlab.com/api/v4

# Webhook Security
WEBHOOK_SECRET=your-webhook-secret-here

# Application
APP_PORT=8099
SPRING_PROFILES_ACTIVE=prod

# Database
DB_HOST=localhost
DB_PORT=3306
DB_USER=reviewbot
DB_PASSWORD=your-db-password-here
DB_ROOT_PASSWORD=your-root-password-here

# Docker Image
IMAGE_TAG=latest
```

### 4. 后端启动

```bash
# 需先启动 MySQL，然后直接运行
mvn spring-boot:run

# 验证
curl http://localhost:8099/actuator/health
```

### 5. API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/webhook/gitlab` | GitLab Webhook 事件接收 |
| POST | `/api/hook/post-receive` | Git post-receive Hook 接收 |
| GET | `/api/projects` | 获取有权限的 GitLab 项目列表 |
| GET | `/api/reviews?projectId=&page=&size=` | 审查记录分页查询 |
| GET | `/api/reviews/{id}` | 审查记录详情（含文件级问题） |
| GET | `/api/reviews/stats?projectId=&from=&to=` | 审查统计 |
| GET | `/api/reviews/trend?projectId=&days=30` | 质量趋势 |
| GET | `/actuator/health` | 健康检查 |
| GET | `/actuator/prometheus` | Prometheus 指标 |

### 6. 配置说明

关键配置项 (详见 `application.yml`):

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | 8099 | 服务端口 |
| `review.max-concurrency` | 5 | 同时调用 AI API 的最大并发数 |
| `review.max-diff-chars` | 50000 | 单文件 Diff 截断阈值 (字符) |
| `review.max-diff-lines-per-file` | 500 | 单文件最大 diff 行数 |
| `review.max-files-per-mr` | 50 | 单次合并请求 最多审查文件数 |
| `review.review-timeout` | 120s | 单次审查总超时 |
| `review.skip-patterns` | `**/*Test.java, **/generated/**, **/target/**` | 跳过审查的文件 Glob 模式 |
| `review.target-branches` | `main,master,develop,dev,release/*` | 触发审查的目标分支 |
| `review.retry.max-attempts` | 4 | AI API 最大重试次数 |
| `review.retry.initial-delay` | 1000ms | 重试初始延迟 |
| `review.retry.multiplier` | 2.0 | 退避倍数 |
| `review.retry.max-delay` | 10s | 重试最大延迟 |
| `ai.temperature` | 0.15 | AI 采样温度（越低越确定） |
| `ai.max-tokens` | 2048 | 最大生成 token 数 |
| `executor.review.core-pool-size` | 2 | 审查线程池核心线程数 |

---

## 开发调试

### 后端调试

```bash
# 运行所有单元测试
mvn test

# 运行单个测试类
mvn test -Dtest=DiffParserTest

# 跳过测试构建
mvn package -DskipTests

# 代码格式自动格式化
mvn spotless:apply

# 代码自动修复（补全大括号等）
mvn rewrite:run

# 本地测试 Webhook（需用脚本计算 HMAC-SHA256）
$secret = "your-webhook-secret"
$body = '{"object_kind":"merge_request","project":{"id":123},"object_attributes":{"iid":1,"state":"opened","source_branch":"feature/test","target_branch":"main","last_commit":{"id":"abc123def456"}}}'
$hmac = [System.Convert]::ToBase64String((New-Object System.Security.Cryptography.HMACSHA256(([text.encoding]::UTF8.GetBytes($secret)))).ComputeHash([text.encoding]::UTF8.GetBytes($body)))
curl -X POST http://localhost:8099/api/webhook/gitlab -H "Content-Type: application/json" -H "X-Gitlab-Event: Merge Request Hook" -H "X-Gitlab-Token: $hmac" -d $body
```

### 查看日志

```bash
# 文件日志（滚动策略：50MB / 保留30天）
tail -f logs/review-bot.log
```

---

## 常见问题

| 现象 | 可能原因 | 排查步骤 |
|------|----------|----------|
| Webhook 返回 403 | HMAC 签名不匹配 | 对比 GitLab Webhook 配置与 `WEBHOOK_SECRET`，确认 Secret Token 一致 |
|合并请求 未出现审查评论 | 目标分支不在 target-branches 中 | 检查 `review.target-branches` 配置（包含 `dev`/`develop`/`release/*`） |
| 评论显示"审查失败" | API Key 无效或额度不足 | `curl` 直接测试 AI API（确认 `ai.base-url` / `ai.api-key` / `ai.model` 是否正确） |
| 审查耗时过长 | 变更文件过多或 API 限流 | 调整 `review.max-concurrency` 或 `review.review-timeout` |
| 同一合并请求 出现重复评论 | 去重缓存因重启丢失 | 数据库级去重会自动兜底（commit_sha 唯一性） |
| 服务返回 429 | 单 IP 请求超过 60次/分钟 | 等待 1 分钟后重试，或调整 `RateLimitInterceptor.MAX_REQUESTS_PER_MINUTE` |

---

## 效果展示

### Webhook 事件测试

![gitlab-webhook事件测试](pics/gitlab-webhook%E4%BA%8B%E4%BB%B6%E6%B5%8B%E8%AF%95.png)

### 内网穿透工具运行画面

![内网穿透工具运行画面](pics/%E5%86%85%E7%BD%91%E7%A9%BF%E9%80%8F%E5%B7%A5%E5%85%B7%E8%BF%90%E8%A1%8C%E7%94%BB%E9%9D%A2.png)

### Code Review Bot 审查报告

![codeReviewBot审查报告1](pics/codeReviewBot%E5%AE%A1%E6%9F%A5%E6%8A%A5%E5%91%8A1.png)

![codeReviewBot审查报告2](pics/codeReviewBot%E5%AE%A1%E6%9F%A5%E6%8A%A5%E5%91%8A2.png)


## 附录：内网穿透
### 工具说明
本人使用的内网穿透工具是NATAPP，官网地址为：https://natapp.cn/ ，使用此工具需要注册账号，并购买一个内网穿透隧道，免费版的完全可以用，但是每次重启域名都会变，需要重新修改gitlab webhook接口地址；

收费版本的隧道域名是固定的，重启也不会变；工具使用还是很简单的，详情看官网说明。

其他工具如ChmlFrp（https://www.chmlfrp.net/）不建议使用， 因为在开发过程中一直遇到域名被阿里云拦截的情况。

### 为什么要使用内网穿透
原因很简单：在开发过程中尽量模拟生产实际情况，提前发现程序中存在的一些问题。


