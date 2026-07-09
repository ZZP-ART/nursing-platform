# nursing-feedback-service 开发计划书

> **版本**：v1.0
> **日期**：2026-07-08
> **负责人**：Codex
> **状态**：方案确认

---

## 一、概述

### 1.1 服务职责

`nursing-feedback-service` 为智慧护理平台提供**评价与投诉**两类核心能力，主要负责：

- 用户提交评价、分页查看评价列表
- 用户提交投诉、查询投诉列表与处理轨迹
- 消费订单支付完成后的 `ORDER_PAID` 事件，为“可评价”能力提供数据基础
- 通过内部接口查询 order-service，校验评价 / 投诉是否针对当前用户可操作的订单

### 1.2 技术栈与部署

| 领域 | 方案 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.5 + Spring Cloud Alibaba 2025 | 与父 POM 保持一致 |
| ORM | MyBatis + MySQL 8.0 | 与其他服务风格一致 |
| RPC | OpenFeign（经 common 模块封装） | 查询可信订单信息 |
| 消息 | Spring Kafka + Outbox / 幂等消费 | 对接订单支付事件 |
| ID 生成 | 雪花算法（worker-id=4, datacenter-id=1） | 使用 common 提供的 `SnowflakeIdWorker` |
| 鉴权 | Gateway 统一 JWT，服务内校验可信网关头 | 当前实现通过 `X-Gateway-Token` + `X-User-Id` |
| 部署 | Docker Compose | 端口 8084，注册名 `nursing-feedback-service` |

### 1.3 当前结构

```text
nursing-feedback-service/
+-- pom.xml
+-- src/main/java/com/nursing/feedback/
    +-- FeedbackApplication.java
    +-- controller/
    |   +-- ReviewController.java
    |   +-- ComplaintController.java
    +-- service/
    |   +-- ReviewService.java
    |   +-- ComplaintService.java
    |   +-- impl/
    |       +-- ReviewServiceImpl.java
    |       +-- ComplaintServiceImpl.java
    +-- repository/
    |   +-- ReviewMapper.java
    |   +-- ReviewImageMapper.java
    |   +-- ComplaintMapper.java
    |   +-- ComplaintTrackMapper.java
    |   +-- IdempotentRecordMapper.java
    |   +-- EventMessageMapper.java
    +-- entity/
    |   +-- Review.java
    |   +-- ReviewImage.java
    |   +-- Complaint.java
    |   +-- ComplaintTrack.java
    |   +-- IdempotentRecord.java
    |   +-- EventMessage.java
    +-- dto/
    |   +-- request/
    |   |   +-- SubmitReviewRequest.java
    |   |   +-- SubmitComplaintRequest.java
    |   +-- response/
    |       +-- ReviewSubmitResponse.java
    |       +-- ReviewVO.java
    |       +-- ComplaintSubmitResponse.java
    |       +-- ComplaintVO.java
    |       +-- ComplaintTrackVO.java
    |       +-- ComplaintTrackListVO.java
    +-- event/
    |   +-- OrderEvent.java
    |   +-- OrderEventType.java
    +-- consumer/
    |   +-- OrderEventConsumer.java
    +-- integration/
    |   +-- OrderQueryService.java
    |   +-- impl/
    |       +-- OrderQueryServiceImpl.java
    +-- config/
        +-- SnowflakeConfig.java
```

---

## 二、依赖与前置条件

### 2.1 外部依赖

| 依赖项 | 当前状态 | 接入方式 |
|--------|---------|---------|
| `nursing-common` | 已具备 Result、PageResult、BusinessException、SnowflakeIdWorker 等 | 直接复用 |
| order-service 内部查询接口 | 已存在内部控制器能力 | 通过 common 中的 Feign / DTO + integration 包封装 |
| Kafka `order_events` | 由 order-service 发布 | `OrderEventConsumer` 订阅消费 |
| MySQL `feedback_db` | DDL 已在 `04_feedback_schema.sql` | 启动前初始化 |
| Gateway 可信头 | 已定义 `X-Gateway-Token` / `X-User-Id` | 控制器内校验 |

### 2.2 POM 要求

除已有依赖外，需要确认启用：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## 三、数据层设计

### 3.1 Entity 清单

| Entity | 对应表 | 关键字段 |
|--------|-------|---------|
| `Review` | `review` | id, orderId, userId, serviceItemId, rating, content, status, isDeleted, createTime, updateTime |
| `ReviewImage` | `review_image` | id, reviewId, imageUrl, sortOrder, isDeleted |
| `Complaint` | `complaint` | id, orderId, userId, type, content, images, status, idempotentKey, isDeleted, createTime, updateTime |
| `ComplaintTrack` | `complaint_track` | id, complaintId, operator, content, isDeleted, createTime, updateTime |
| `IdempotentRecord` | `idempotent_record` | id, idempotentKey, bizType, bizId, status, expireTime, createTime |
| `EventMessage` | `event_message` | id, topic, eventKey, payload, status, retryCount, createTime |

### 3.2 Mapper 设计

| Mapper | 核心方法 |
|--------|---------|
| `ReviewMapper` | insert / selectById / selectPageByItemId / selectByOrderId |
| `ReviewImageMapper` | insertBatch / selectByReviewId |
| `ComplaintMapper` | insert / selectById / selectPageByUserId |
| `ComplaintTrackMapper` | selectByComplaintId / insert |
| `IdempotentRecordMapper` | insert / selectByKey / updateStatus |
| `EventMessageMapper` | insert / selectPendingEvents / updateStatus |

### 3.3 数据注意事项

- `review.status` 默认建议与“待审核 / 已通过 / 已驳回”枚举对齐
- `complaint.images` 建议以 JSON 字符串存储
- 主键统一使用雪花 ID，避免数据库自增耦合
- Mapper XML 统一放在 `resources/mapper/*.xml`

---

## 四、业务层设计

### 4.1 ReviewService

| 方法 | 说明 |
|------|------|
| `submitReview(request, userId, idempotentKey)` | 提交评价；校验订单归属、订单状态、是否重复评价 |
| `pageReviews(itemId, page, size)` | 分页查询评价列表，仅返回可展示状态 |

### 4.2 ComplaintService

| 方法 | 说明 |
|------|------|
| `submitComplaint(request, userId, idempotentKey)` | 提交投诉；校验订单归属并做幂等控制 |
| `pageComplaints(userId, page, size)` | 当前用户投诉分页列表 |
| `getComplaintTracks(complaintId, userId)` | 查询投诉处理轨迹，并校验当前用户权限 |

### 4.3 订单查询集成

通过 `OrderQueryService` 封装对 order-service 的可信内部查询，避免反馈服务直接信任外部可伪造参数。

核心目标：

- 校验订单是否属于当前用户
- 校验订单状态是否允许评价 / 投诉
- 获取服务项名称、规格信息等展示字段

---

## 五、幂等设计

### 5.1 评价提交

```text
请求头携带 Idempotent-Key
-> 查询 idempotent_record
-> 未处理则插入 status=0
-> 写 review + review_image
-> 更新 idempotent_record.status=1, bizId=reviewId
-> 返回 reviewId
```

若发现同一订单已评价，则直接返回业务错误，防止“一单多评”。

### 5.2 投诉提交

```text
请求头携带 Idempotent-Key
-> complaint 表保存 idempotentKey
-> 配合唯一约束防止重复提交
-> 冲突时查询既有 complaintId 并返回
```

### 5.3 Kafka 消费幂等

```text
消费 ORDER_PAID
-> eventKey = order_paid_{orderId}
-> 查询 idempotent_record
-> 已存在则忽略
-> 否则写消费记录并继续处理
```

---

## 六、Controller 设计

### 6.1 ReviewController

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/reviews` | 提交评价，需要 `Idempotent-Key` |
| GET | `/api/v1/reviews` | 分页查询评价列表，参数 `itemId/page/size` |

请求规则：

- 通过可信网关头提取 `userId`
- 校验 `X-Gateway-Token`
- 评价请求体字段包括 `orderId`、`rating`、`content`、`images`

### 6.2 ComplaintController

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/complaints` | 提交投诉，需要 `Idempotent-Key` |
| GET | `/api/v1/complaints` | 当前用户投诉列表 |
| GET | `/api/v1/complaints/{id}/tracks` | 查询投诉轨迹 |

请求规则：

- 通过可信网关头提取 `userId`
- 只有投诉人本人可以查看投诉轨迹

### 6.3 返回模型

| DTO | 说明 |
|-----|------|
| `ReviewSubmitResponse` | 返回 `reviewId` 等提交结果 |
| `ReviewVO` | 评价列表项 |
| `ComplaintSubmitResponse` | 返回 `complaintId` |
| `ComplaintVO` | 投诉列表项 |
| `ComplaintTrackVO` | 单条投诉轨迹 |
| `ComplaintTrackListVO` | 投诉详情 + 轨迹集合 |

---

## 七、消息与集成

### 7.1 Kafka 消费 `ORDER_PAID`

`OrderEventConsumer` 订阅 `order_events`：

1. 反序列化 `OrderEvent`
2. 过滤非 `ORDER_PAID` 事件
3. 做事件幂等校验
4. 记录“订单已支付，可评价”的业务基础数据
5. ACK 成功

### 7.2 与 order-service 的协作

| 场景 | 调用内容 |
|------|---------|
| 提交评价 | 校验订单归属、状态、服务项 |
| 提交投诉 | 校验订单归属、状态 |
| 列表展示 | 补充服务名称、规格信息等辅助字段 |

失败策略：

- Feign / 内部查询失败时记录 WARN
- 查询能力不可用时，提交型接口失败优先，展示型字段可降级为空

---

## 八、错误码建议（feedback 业务域 4001-4099）

| code | message | 说明 |
|------|---------|------|
| 4001 | 请求参数不能为空 | 通用参数错误 |
| 4002 | 当前订单状态不允许评价 | 订单未完成或状态不符 |
| 4003 | 该订单已评价 | 一单多评拦截 |
| 4004 | 投诉内容不能为空 | 参数错误 |
| 4005 | 投诉类型无效 | `type` 不在约定范围 |
| 4006 | 投诉不存在 | 查询不到对应投诉 |
| 4007 | 无权查看该投诉 | 非投诉人访问 |

---

## 九、实施计划

### Phase 1：通用能力
- 完成 common 模块中的订单查询契约与 DTO
- 补齐 validation 依赖

### Phase 2：数据层
- Entity 6 个
- Mapper 6 个
- XML 6 份

### Phase 3：业务层
- `ReviewService` / `ComplaintService`
- 订单归属与状态校验
- 幂等控制

### Phase 4：接口层
- `ReviewController`
- `ComplaintController`
- DTO 返回模型

### Phase 5：消息与联调
- `OrderEventConsumer`
- `OrderQueryService`
- 本地联调与回归验证

---

## 十、测试要点

| 类型 | 覆盖内容 |
|------|---------|
| 评价提交 | 幂等校验、订单状态校验、重复评价拦截 |
| 评价列表 | itemId 分页查询，仅返回可展示状态 |
| 投诉提交 | 幂等校验、订单归属校验 |
| 投诉列表 | 当前用户分页查询 |
| 投诉轨迹 | 权限校验、轨迹排序 |
| Kafka 消费 | `ORDER_PAID` 事件幂等消费 |
| 集成查询 | order-service 不可用时的失败 / 降级行为 |

---

## 十一、已确认的关键决策

| # | 决策 | 说明 |
|---|------|------|
| 1 | 反馈服务通过可信内部查询获取订单信息 | 避免使用可伪造外部参数 |
| 2 | 评价与投诉都要求幂等键 | 减少客户端重复提交风险 |
| 3 | Kafka 消费必须做事件幂等 | 防止重复消费导致重复状态推进 |
| 4 | 控制器校验可信网关头 | 当前服务内兜底鉴权策略 |
| 5 | 数据层保持 MyBatis 风格统一 | 与项目其他服务一致，便于维护 |

---

## 十二、验收标准

1. `mvn clean compile -pl nursing-feedback-service -am` 通过。
2. 评价、投诉、轨迹查询接口可正常返回。
3. Kafka 消费逻辑具备幂等保护。
4. 订单归属和状态校验链路完整。
5. 服务可在本地 / Docker Compose 环境正常启动并注册。
