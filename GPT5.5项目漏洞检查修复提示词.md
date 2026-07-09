# GPT5.5 项目漏洞检查与全量修复提示词

将下面整段提示词复制给 GPT5.5 / Codex 使用。

```text
你是 GPT5.5，当前任务是在本地项目中完成一次“漏洞检查、验收清单修复、自动化测试闭环”的端到端工程工作。不要只给建议；你需要直接修改代码、补齐测试、运行验证命令，并循环修复直到所有检查通过或遇到无法本地解决的外部阻塞。

工作目录：
C:\Users\zhuzh\Desktop\project

项目背景：
- 项目名称：互联网+智慧护理移动护理平台 App
- 后端路径：C:\Users\zhuzh\Desktop\project\nursing-platform
- 技术栈：Java 21、Spring Boot 3.5.0、Spring Cloud Alibaba、Spring Cloud Gateway、OpenFeign、MyBatis、MySQL 8、Redis、Kafka、Docker Compose
- 文档：项目说明.txt、系统架构设计.md、API接口文档.md、验收修复清单.md
- 当前核心目标：按验收修复清单完成漏洞与缺陷修复，使项目达到可验收状态。

最高优先级规则：
1. 先读取并遵守当前目录和上级目录中可能存在的 AGENTS.md / README / 项目说明；若没有文件，则遵守本提示词。
2. 先读取 `验收修复清单.md`、`API接口文档.md`、`系统架构设计.md`，再动手改代码。
3. 必须保护用户已有修改：不要执行 `git reset --hard`、`git checkout --`、大范围删除等破坏性操作。
4. 以 P0 -> P1 -> P2 -> P3 的顺序修复；P0/P1 未闭环前不要沉迷 P3 优化。
5. 对安全问题采用“默认不信任外部输入”的原则：鉴权、授权、签名、金额、敏感数据、配置、日志都必须真实校验。
6. 不要用伪实现绕过验收。mock 只能用于 dev/test profile，生产配置必须 fail-fast 或安全关闭。
7. 每完成一个缺陷，必须补充或更新自动化测试，并运行相关测试。
8. 不能只让 `mvn test` 形式通过；核心 happy path 和关键攻击/异常路径都要有测试覆盖。

必须修复的验收清单：

P0 阻塞项：
- P0-01 Gateway JWT 鉴权与可信用户头缺失
  - Gateway 实现 JWT Filter。
  - 白名单放行短信、注册、登录、重置密码、catalog 浏览、支付宝回调。
  - 校验 `Authorization: Bearer <token>`、过期、签名、Redis 黑名单。
  - 删除客户端伪造的 `X-User-Id`、`X-UserId`、`userId` 等请求头。
  - 由 Gateway 注入可信 `X-User-Id`。
  - order/feedback 只信任网关注入用户头或内部服务鉴权。
  - 测试未登录、伪造头、有效 JWT、登出黑名单、公开路径。

- P0-02 catalog 与 order-service 的 Feign DTO 字段不一致
  - 统一 catalog 详情响应契约为 `id`、`specs[].id`，或用 Jackson 注解兼容 `itemId/specId` 与 `id`。
  - 清理重复或冲突的 `CatalogServiceFeignClient` 设计。
  - 增加 catalog-order Feign 契约测试。
  - 增加下单集成测试：`serviceItemId=201`、`serviceSpecId=301` 能创建订单。

- P0-03 feedback 与 order-service 的 Feign 契约不闭环
  - 设计内部订单查询接口，例如 `GET /internal/v1/orders/{id}`。
  - 返回 `OrderDTO` 需要的 `orderId`、`orderNo`、`userId`、`status`、`serviceItemId`、`serviceItemName`、`specName`、`totalAmount`。
  - 内部接口必须有服务间鉴权，不能裸露给外部。
  - 修复评价/投诉前置校验。
  - 测试本人已完成订单可评价、非本人不可评价/投诉、非已完成不可评价、待服务或已完成可投诉。

- P0-04 支付回调未真实 RSA2 验签且未校验金额
  - 接入支付宝 SDK 或按支付宝规则实现 RSA2 验签。
  - 验签前排除 `sign`、`sign_type` 并按规则构造待签名字符串。
  - 校验 `app_id`、`seller_id` 或商户号。
  - 校验 `out_trade_no` 对应订单存在且状态为待支付。
  - 校验 `total_amount == order.total_amount`。
  - 只处理 `TRADE_SUCCESS` / `TRADE_FINISHED`。
  - mock 模式仅允许 dev/test profile，生产环境强制关闭。
  - 测试伪造签名、金额不一致、重复合法回调、非待支付订单回调。

P1 重要缺陷：
- P1-01 JWT secret 默认空导致登录/注册真实运行失败
  - 提供 `.env.example` 或 Nacos 配置样例。
  - dev profile 提供足够长度的开发 secret。
  - prod profile 禁止空 secret，启动 fail-fast。
  - Gateway 与 user-service 使用同一 secret 配置来源。
  - 测试注册/登录签发 Token，Gateway 可校验。

- P1-02 订单状态流转缺少完成、退款和超时取消
  - 增加服务完成接口或内部任务。
  - 增加支付超时自动取消，默认 30 分钟。
  - 增加待服务取消后的退款状态流转。
  - 更新 `payment_record` 退款状态和退款时间。
  - 所有状态变化写入 `order_operation_log`。
  - 明确订单完成后的 feedback 可评价触发机制。

- P1-03 Docker Compose 未编排业务服务与 Gateway
  - 为 gateway、user、catalog、order、feedback 增加 Dockerfile 或 Maven buildpacks/jib 配置。
  - Compose 编排 MySQL、Redis、Nacos、Kafka、Gateway、四个业务服务。
  - 服务间地址使用 Compose 网络服务名。
  - 通过环境变量注入 DB、Redis、Nacos、Kafka、JWT secret、支付配置。
  - 增加健康检查、依赖顺序和本地部署说明。

- P1-04 身份证号未加密存储，SQL 日志存在敏感信息泄露风险
  - 增加 AES-256 或等效加密组件。
  - 密钥来自环境变量或配置中心，禁止硬编码真实密钥。
  - 写库前加密，API 返回前脱敏。
  - 生产环境禁用 SQL stdout 参数日志。
  - 增加日志脱敏过滤器。
  - 测试数据库不存明文、API 脱敏、日志不泄露完整身份证号。

P2 一般缺陷：
- P2-01 统一错误 HTTP 状态与文档不一致
  - `BusinessException` 支持 HTTP status 或按错误码映射。
  - 对齐 user-service 与 common 异常模型。
  - 参数校验错误格式统一。
  - 测试 401、403、404、409、422、429 等状态。

- P2-02 测试覆盖不足
  - 增加 Gateway 鉴权、user 注册/登录/登出/黑名单、catalog-order 契约、order 幂等、支付回调、feedback-order 契约、Kafka Outbox、Docker/Testcontainers 初始化测试。

- P2-03 Outbox / Kafka 消息格式未验证
  - 统一消息值为 String JSON 或对象 JSON。
  - serializer/deserializer 必须匹配。
  - 测试 `ORDER_PAID` 可被 feedback 解析、重复事件幂等、投递失败重试并在 3 次后标记失败。

- P2-04 配置硬编码与环境隔离不足
  - 数据库、Redis、Nacos、Kafka 全部支持环境变量配置。
  - 提供 dev/prod 差异化配置。
  - prod 不含 root/root123、localhost 等生产不安全默认值。
  - 提供 `.env.example`。

P3 优化项：
- P3-01 修复 catalog 列表 N+1 查询，增加批量规格查询并测试 SQL 数量不随列表长度线性增长。
- P3-02 移除第三方 placeholder 图片依赖，改为项目默认资源或允许为空。
- P3-03 统一 API 文档、架构文档、计划书和 Controller/Gateway 路径，必要时生成 OpenAPI/Swagger 契约。

安全漏洞检查范围：
- 鉴权绕过、伪造请求头、越权访问、IDOR。
- JWT secret、Token 黑名单、会话过期、登出失效。
- SQL 注入、MyBatis 动态 SQL 安全性。
- XSS/输出编码风险，尤其是投诉、评价、昵称、文件名等用户输入。
- 支付回调签名、金额、商户号、订单状态、重复回调幂等。
- 敏感数据明文存储与日志泄露：身份证号、手机号、Token、密钥、支付参数。
- 配置泄露：硬编码密码、root 账号、localhost 生产配置、mock 支付。
- 文件上传：类型、大小、路径穿越、文件名净化、可执行文件风险。
- CORS、安全响应头、错误堆栈暴露。
- Kafka/Outbox 幂等、重试、消息格式不一致。
- 依赖漏洞与过期组件。至少检查 Maven dependency tree；如网络和环境允许，执行 OWASP Dependency Check 或等效扫描。

推荐执行流程：
1. 基线检查
   - 查看 `git status --short`，记录已有改动，不要覆盖用户改动。
   - 阅读文档和清单。
   - 阅读所有 `pom.xml`、`application*.yml`、Controller、Service、Feign、Mapper、Gateway、Docker Compose。
   - 运行一次基线命令，记录失败点：
     ```powershell
     cd C:\Users\zhuzh\Desktop\project\nursing-platform
     mvn clean test
     docker compose -f docker-compose\docker-compose.yml config
     ```

2. 逐项修复
   - 从 P0-01 开始，按优先级逐项处理。
   - 每项修复都必须包含代码、配置、测试、文档或清单状态更新。
   - 涉及安全配置时，优先使用环境变量和 profile 隔离。
   - 涉及服务间调用时，明确外部 API 与内部 API 边界。

3. 测试闭环
   - 每次修复后运行目标模块测试，例如：
     ```powershell
     mvn -pl nursing-gateway test
     mvn -pl nursing-user-service test
     mvn -pl nursing-order-service test
     mvn -pl nursing-feedback-service test
     mvn -pl nursing-catalog-service test
     ```
   - 最终必须运行：
     ```powershell
     cd C:\Users\zhuzh\Desktop\project\nursing-platform
     mvn clean test
     docker compose -f docker-compose\docker-compose.yml config
     docker compose -f docker-compose\docker-compose.yml up -d --build
     ```
   - 若启动完整 Compose 成本过高或本机缺少 Docker，必须说明外部阻塞，并至少保证 `docker compose config`、Maven 全量测试通过。
   - 若测试失败，读取失败日志，修复后重跑。重复此循环直到全绿。

4. 最小验收回归必须覆盖
   - 注册用户并返回 JWT。
   - 登录后 Gateway 注入可信 `X-User-Id`。
   - 未登录访问订单接口返回 401。
   - 伪造 `X-User-Id` 不带 JWT 被拒绝。
   - 浏览分类、服务列表、服务详情。
   - 新增地址并设置默认地址。
   - 获取 prepay-token。
   - 同一个 Idempotent-Key 重复下单返回同一订单。
   - 合法支付回调更新订单为待服务。
   - 非法签名或金额不一致回调被拒绝。
   - 订单完成后可评价。
   - 非本人订单不可评价/投诉。
   - 投诉提交后可查询投诉列表和处理记录。
   - `ORDER_PAID` Outbox 事件可投递并被 feedback 幂等消费。
   - Docker Compose 完整后端可启动并通过健康检查。

交付要求：
- 修改代码直到 P0/P1/P2/P3 全部闭环。
- 更新 `验收修复清单.md` 中对应状态、验证结果和必要说明。
- 新增或更新 `修复验证报告.md`，至少包含：
  - 修复摘要
  - 安全漏洞清单及处理结果
  - 关键代码变更
  - 新增测试清单
  - 执行过的命令和结果
  - 未能执行的命令及原因
  - 剩余风险，若没有则写“无已知剩余阻塞风险”
- 最终答复必须简洁列出：
  - 已修复内容
  - 测试命令与结果
  - Docker/集成验证结果
  - 是否还有外部阻塞

完成标准：
- `mvn clean test` 通过。
- `docker compose -f docker-compose\docker-compose.yml config` 通过。
- 若 Docker 可用，`docker compose -f docker-compose\docker-compose.yml up -d --build` 后核心服务健康检查通过。
- P0/P1/P2 验收项全部有自动化测试或明确验证证据。
- P3 优化项完成或有明确、合理、非阻塞说明。
- 不存在已知高危/关键安全漏洞。
```
