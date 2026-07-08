# Nursing Feedback Service — 开发计划书

> **版本**：v1.0
> **日期**：2026-07-08
> **作者**：Codex
> **状态**：待确认

---

## 一、概述

### 1.1 服务职责

`nursing-feedback-service` 为智慧护理平台提供**评价与投诉**领域的核心能力，包含：

- 评价提交与展示（按服务项目分页）
- 投诉提交、查询与处理跟踪
- 通过 Kafka 消费订单完成的 ORDER_PAID 事件，标记订单可评价
- 通过 Feign 同步调用 order-service 获取订单基本信息

### 1.2 技术栈对照

| 领域 | 技术选型 | 依据 |
|------|---------|------|
| 框架 | Spring Boot 3.5 + Spring Cloud Alibaba 2025 | 父 POM 已统一管理 |
| ORM | MyBatis-Plus 3.0.4 + MySQL 8.0 | 与 order-service 保持一致 |
| RPC | OpenFeign 4.3 | 跨服务调用 order-service |
| 消息 | Spring Kafka + Outbox 模式 | 基于架构文档设计要求 |
| ID 生成 | 雪花算法（worker-id=4, datacenter-id=1） | common 模块已提供 |
| 鉴权 | Gateway 统一 JWT 校验，服务端通过 Header 获取 userId | 架构规范 |
| 部署 | Docker Compose | 端口 8084，服务注册名 nursing-feedback-service |

### 1.3 模块文件树（目标结构）

```
nursing-feedback-service/
├── pom.xml
└── src/main/java/com/nursing/feedback/
    ├── FeedbackApplication.java              # 已有
    ├── controller/
    │   ├── ReviewController.java             # 评价 API
    │   └── ComplaintController.java          # 投诉 API
    ├── service/
    │   ├── ReviewService.java                # 评价业务逻辑
    │   ├── ComplaintService.java             # 投诉业务逻辑
    │   └── impl/
    │       ├── ReviewServiceImpl.java
    │       └── ComplaintServiceImpl.java
    ├── repository/
    │   ├── ReviewMapper.java
    │   ├── ReviewImageMapper.java
    │   ├── ComplaintMapper.java
    │   ├── ComplaintTrackMapper.java
    │   ├── IdempotentRecordMapper.java
    │   └── EventMessageMapper.java
    ├── entity/
    │   ├── Review.java
    │   ├── ReviewImage.java
    │   ├── Complaint.java
    │   ├── ComplaintTrack.java
    │   ├── IdempotentRecord.java
    │   └── EventMessage.java
    ├── dto/
    │   ├── request/
    │   │   ├── SubmitReviewRequest.java
    │   │   └── SubmitComplaintRequest.java
    │   └── response/
    │       ├── ReviewVO.java
    │       ├── ComplaintVO.java
    │       └── ComplaintTrackVO.java
    ├── event/
    │   ├── OrderEvent.java                   # 订单事件 DTO
    │   └── OrderEventType.java               # 事件类型枚举
    ├── consumer/
    │   └── OrderEventConsumer.java           # Kafka 消费者
    ├── feign/
    │   └── OrderFeignClient.java             # Feign 调用 order-service
    └── config/
        └── IdempotentConfig.java             # 幂等配置（雪花ID初始化等）
```

---

## 二、先行依赖分析

### 2.1 外部依赖排查

| 依赖项 | 当前状态 | 处理方式 |
|--------|---------|---------|
| `nursing-common` 基础类 | 已有：Result、PageResult、BusinessException、ApiCode、SnowflakeIdWorker、枚举类、GlobalExceptionHandler | 直接使用 |
| Feign 公共包 `com.nursing.common.feign` | **不存在** | 在 nursing-common 中新建 `feign` 包，放置 `OrderFeignClient` |
| order-service 上线进度 | 当前仅有 OrderApplication 骨架 | 先定义 Feign 接口，联调时补齐实现 |
| Kafka Topic `order_events` | 待 order-service 创建 | 先写好消费者代码，OrderEventConsumer 做好消费幂等 |
| MySQL feedback_db + 5 张表 | DDL 已就绪（04_feedback_schema.sql） | 启动前执行 DDL 即可 |

### 2.2 内部现有代码

| 文件 | 当前内容 | 是否需修改 |
|------|---------|----------|
| `FeedbackApplication.java` | 含 @EnableFeignClients 指向 `com.nursing.common.feign` | **不改** |
| `application.yml` | 端口 8084、数据库、Kafka、MyBatis 配置完整 | **不改** |
| `pom.xml` | 依赖涵盖 mybatis、mysql、common、web、nacos、feign、kafka | 检查是否需要补充 Lombok/validation |

> ⚠️ **注意**：pom.xml 缺少 `spring-boot-starter-validation` 依赖，需补充用于 @Valid 参数校验。

---

## 三、分阶段实施计划

### 第一阶段：通用能力层

#### 3.1 新增 Feign 接口（nursing-common 模块）

在 `nursing-common` 中新建包 `com.nursing.common.feign`，创建：

**`OrderFeignClient.java`**
```java
@FeignClient(name = "nursing-order-service", path = "/api/v1/orders")
public interface OrderFeignClient {
    @GetMapping("/{id}")
    Result<OrderDTO> getOrder(@PathVariable("id") Long id);
}
```

同时定义 **`OrderDTO`** 供 Feign 响应反序列化，放在 `com.nursing.common.dto` 包中：

```java
public class OrderDTO implements Serializable {
    private Long orderId;
    private String orderNo;
    private Integer status;
    private Long serviceItemId;
    private String serviceItemName;
    private String specName;
    // ... 根据需要裁剪字段
}
```

#### 3.2 补充反馈服务专用错误码（ApiCode）

| code | message | 说明 |
|------|---------|------|
| 4001 | 评价内容不能为空 | — |
| 4002 | 订单状态不可评价 | 订单未完成 |
| 4003 | 该订单已评价 | 一单一评 |
| 4004 | 投诉内容不能为空 | — |
| 4005 | 投诉类型无效 | type 不在 1-4 范围 |
| 4006 | 投诉不存在 | — |
| 4007 | 无权操作该投诉 | 投诉不属于当前用户 |

---

### 第二阶段：数据持久层

#### 3.3 Entity 实体类（6 个）

| Entity | 对应表 | 关键字段 |
|--------|-------|---------|
| `Review` | review | id, orderId, userId, serviceItemId, rating, content, status, isDeleted, createTime, updateTime |
| `ReviewImage` | review_image | id, reviewId, imageUrl, sortOrder, isDeleted |
| `Complaint` | complaint | id, orderId, userId, type, content, images, status, idempotentKey, isDeleted, createTime, updateTime |
| `ComplaintTrack` | complaint_track | id, complaintId, operator, content, isDeleted, createTime, updateTime |
| `IdempotentRecord` | idempotent_record | id, idempotentKey, bizType, bizId, status, expireTime, createTime |
| `EventMessage` | event_message | id, topic, eventKey, payload, status, retryCount, createTime |

#### 3.4 Mapper 接口（6 个）+ MyBatis XML

每个 Entity 对应一个 `@Mapper` 接口和 `src/main/resources/mapper/*.xml` 文件。

| Mapper | 核心方法 |
|--------|---------|
| `ReviewMapper` | insert、selectById、selectPageByItemId、selectByOrderId |
| `ReviewImageMapper` | insertBatch、selectByReviewId |
| `ComplaintMapper` | insert、selectById、selectPageByUserId |
| `ComplaintTrackMapper` | selectByComplaintId、insert |
| `IdempotentRecordMapper` | insert、selectByKey、updateStatus |
| `EventMessageMapper` | insert、selectPendingEvents、updateStatus |

#### 3.5 数据库注意事项

- review 表的 `status` 默认值在 DDL 中为 `1`（待审核），代码中常量与枚举 `ReviewStatus.PENDING_REVIEW` 对应
- complaint 表 DDL 已有 `images` 字段（VARCHAR 1024，JSON 数组字符串），在 Entity 中用 `String` 存储
- 所有 ID 使用雪花算法生成，不依赖 MySQL 自增

---

### 第三阶段：业务服务层

#### 3.6 Service 接口与实现

**ReviewService：**
| 方法 | 说明 |
|------|------|
| `submitReview(SubmitReviewRequest req, Long userId)` | 提交评价，前置校验：订单归属 + 订单状态 = 已完成 + 未重复评价 |
| `pageReviews(Long itemId, int page, int size)` | 按服务项目分页，仅返回 status=APPROVED（已展示） |

**ComplaintService：**
| 方法 | 说明 |
|------|------|
| `submitComplaint(SubmitComplaintRequest req, Long userId)` | 提交投诉，前置校验：订单归属 + 幂等检查（uk_idempotent） |
| `pageComplaints(Long userId, int page, int size)` | 当前用户的投诉分页列表 |
| `getComplaintTracks(Long complaintId, Long userId)` | 查看投诉处理记录，校验投诉归属 |

#### 3.7 幂等处理策略

**评价提交（幂等流）：**
```
请求头 Idempotent-Key = UUID
  → IdempotentRecordMapper.selectByKey(key)
    → 存在且 status=1 → 直接返回已有 reviewId
    → 存在且 status=0 → 返回 409（处理中）
  → INSERT idempotent_record(status=0)
  → INSERT review + review_image
  → UPDATE idempotent_record SET status=1, biz_id=reviewId
  → 返回 reviewId
```

**投诉提交（幂等流）：**
```
投诉表自身有 uk_idempotent_key 唯一约束
  → 前端传 Idempotent-Key
  → INSERT complaint(idempotent_key=key)
  → 唯一约束冲突 → 查询已存在 complaintId → 返回
```

> 投诉的幂等通过表级唯一约束兜底，评价通过独立的 `idempotent_record` 表处理。

---

### 第四阶段：表现层（Controller）

#### 3.8 5 个 API 接口实现

##### POST /api/v1/reviews — 提交评价

- @PostMapping
- 请求头需要 `Authorization`（已有 Gateway 校验）、`Idempotent-Key`
- 请求体：`SubmitReviewRequest`（orderId, rating, content, images）
- 参数校验：@Valid rating ∈ [1,5], content ≤ 500, images ≤ 6
- 响应：`Result.success(new ReviewVO(reviewId))`
- 业务 code：4002（订单状态不可评）、4003（已评价）

##### GET /api/v1/reviews — 评价列表

- @GetMapping
- 请求参数：itemId (必填), page (默认1), size (默认20)
- 返回 `Result.success(PageResult<ReviewVO>)`
- 每个 ReviewVO 包含：reviewId, rating, content, images, userNickname, createTime
- 仅返回 status=APPROVED 的评价

##### POST /api/v1/complaints — 提交投诉

- @PostMapping
- 请求头需要 `Authorization`、`Idempotent-Key`
- 请求体：`SubmitComplaintRequest`（orderId, type, content, images）
- 参数校验：type ∈ [1,4], content ≤ 1000, images ≤ 6
- 响应：`Result.success(new ComplaintVO(complaintId))`
- 业务 code：4005（投诉类型无效）

##### GET /api/v1/complaints — 投诉列表

- @GetMapping
- 请求参数：page (默认1), size (默认20)
- 返回当前用户的投诉分页，按 create_time 倒序
- 每个 ComplaintVO 包含：complaintId, type, typeText, status, statusText, createTime

##### GET /api/v1/complaints/{id}/tracks — 投诉处理记录

- @GetMapping
- 路径参数：complaintId
- 返回投诉基础信息 + tracks 数组（按 create_time 升序）
- 校验当前用户是否为投诉发起人
- 业务 code：4006（投诉不存在）、4007（无权操作）

---

### 第五阶段：跨服务集成

#### 3.9 Kafka 消费者 — ORDER_PAID

**消费逻辑：**
```
@KafkaListener(topics = "order_events", groupId = "feedback-group")
  → 解析 OrderEvent JSON（包含 eventType, orderId, orderNo, userId, serviceItemId, timestamp）
  → eventType = "ORDER_PAID" 才处理
  → 查 IdempotentRecord（eventKey = "order_paid_" + orderId）
    → 有记录 → 跳过（消费幂等）
  → INSERT idempotent_record(eventKey, bizType="ORDER_PAID", status=1)
  → 记录到本地表（可选，预留标记可评价）
  → ACK
```

**备注**：当前阶段仅做幂等消费处理，标记已支付的订单为"可评价"状态，为前端展示"去评价"按钮提供数据基础。

#### 3.10 Feign 调用 — 订单信息展示

评价列表的 `ReviewVO` 需要展示订单相关的服务信息（如服务名称、规格名称），通过 Feign 调用 `order-service` 的 `GET /api/v1/orders/{id}` 获取。

**降级策略：**
- 设置 Feign 超时：connectTimeout=5000ms, readTimeout=5000ms
- 设置 Feign 熔断降级（fallback）：返回 null，VO 中 orderInfo 为空不展示
- 日志记录 WARN 级别

---

### 第四阶段补充：配置文件微调

#### 3.11 pom.xml 补充

在 feedback-service 的 pom.xml 中补充：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## 四、关键设计决策

| # | 决策 | 方案 | 理由 |
|---|------|------|------|
| 1 | 评价状态初始值 | PENDING_REVIEW（待审核） | 架构文档要求，后续客服审核后改为 APPROVED |
| 2 | 投诉状态初始值 | PENDING（待处理） | 架构文档要求 |
| 3 | 评价列表中 userNickname | 从 review 表本身是否能获取？ | 当前 review 表无 nickname 字段，预留字段或由前端从 user 信息获取（登录时已缓存） |
| 4 | serviceItemName 在评价列表展示 | Feign 调用 order-service 获取快照字段 | review 表本身没冗余 nickname/serviceName，通过 Feign 获取订单快照 |
| 5 | 投诉 images 存储格式 | JSON 数组字符串（VARCHAR 1024） | DDL 已定，保持兼容 |
| 6 | 是否使用 MyBatis-Plus | **否**，使用原生 MyBatis + XML | 父 POM 没有引入 MP，order-service 也没用，保持一致 |
| 7 | 雪花 ID 如何注入 | 通过 @Bean 注入 SnowflakeIdWorker 实例 | common 模块已提供工具类，每个服务自行注册 Bean |

---

## 五、工作量估算

| 阶段 | 模块 | 预估文件数 | 复杂度 |
|------|------|-----------|--------|
| 通用层 | Feign 接口 + DTO（nursing-common 模块） | 2 个 | ⭐⭐ |
| 数据层 | Entity 6 个 + Mapper 6 个 + XML 6 个 | 18 个 | ⭐⭐ |
| 业务层 | Service 接口 2 个 + 实现 2 个 | 4 个 | ⭐⭐⭐ |
| 表现层 | Controller 2 个 + DTO（request+response）5 个 | 7 个 | ⭐⭐ |
| 集成 | Kafka Consumer 1 个 + Event DTO 2 个 | 3 个 | ⭐⭐ |
| 配置 | pom.xml 补充 + 幂等配置 | 2 个 | ⭐ |
| **合计** | | **约 36 个** | **整体 ⭐⭐⭐** |

> 估算基于项目骨架已就绪、DDL 已完成、公共组件已可用的前提。

---

## 六、依赖风险与注意事项

### 6.1 外部依赖风险

| 风险项 | 影响 | 缓解措施 |
|--------|------|---------|
| order-service 尚未实现订单 API | Feign 调用在联调前不可用 | 先定义接口，Mock 测试，联调时补 URL |
| Kafka Topic 未创建 | 消费者启动后无消息消费 | 消费者做好空轮询处理 |
| 其他服务的 Entity/枚举尚未对齐 | 跨服务 DTO 字段不一致 | 尽早对齐接口契约（接口文档已定义） |

### 6.2 实现注意事项

- **消费幂等**：Kafka 消费者必须使用 `idempotent_record` 表做消费记录，避免重复处理
- **事务一致性**：评价提交涉及 review + reviewImage + idempotent_record 三表操作，需 @Transactional
- **投诉归属校验**：所有投诉查询接口必须校验 `userId` 与当前登录用户一致
- **MyBatis XML 路径**：`application.yml` 中已配置 `mapper-locations: classpath:mapper/*.xml`
- **日期格式**：API 文档要求 ISO 8601 格式，Spring Boot 默认 Jackson 序列化已满足

---

## 七、验收标准

### 7.1 功能验收

| # | 验收项 | 通过条件 |
|---|--------|---------|
| 1 | 评价提交 | 幂等校验通过 → 返回 reviewId；重复提交 → 返回已有 reviewId |
| 2 | 评价列表 | 按 itemId 分页，仅返回 APPROVED 状态 |
| 3 | 投诉提交 | 幂等（uk_idempotent）→ 返回 complaintId |
| 4 | 投诉列表 | 分页、按时间倒序、仅当前用户数据 |
| 5 | 投诉处理记录 | 返回 tracks 时间线 |
| 6 | Kafka 消费 | ORDER_PAID 消息被正确幂等消费 |
| 7 | Feign 调用 | 调用 order-service 返回订单信息或降级处理 |

### 7.2 非功能验收

| # | 验收项 | 通过条件 |
|---|--------|---------|
| 1 | 编译通过 | `mvn clean compile -pl nursing-feedback-service -am` 成功 |
| 2 | 启动正常 | 服务在 8084 端口正常注册到 Nacos |
| 3 | 代码风格 | 符合阿里 Java 开发手册规范 |

---

## 八、后续计划

实现阶段的详细任务清单将在本计划书确认后展开，按以下顺序推进：

1. **Step 1**：补充 nursing-common 中 Feign 接口 + DTO
2. **Step 2**：创建 feedback-service 的 Entity + Mapper + XML
3. **Step 3**：实现 Service 层业务逻辑
4. **Step 4**：实现 Controller 层 API
5. **Step 5**：实现 Kafka 消费者
6. **Step 6**：单元测试 + 本地启动验证

---

> **请确认本计划书**。如需调整范围、设计决策或优先级，请告知，我再修改。
