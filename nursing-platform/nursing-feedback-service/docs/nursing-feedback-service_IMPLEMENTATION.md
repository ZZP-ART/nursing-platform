# feedback-service 接口实现逻辑

> 本文描述当前评价与投诉模块的请求处理、订单依赖、事件消费、幂等和后台维护逻辑。

## 1. 架构与通用规则

```text
Gateway -> ReviewController / ComplaintController
  -> ReviewServiceImpl / ComplaintServiceImpl
  -> MyBatis Mapper -> feedback_db
  -> OrderQueryService -> order-service internal API

Kafka order_events -> OrderEventConsumer -> review_eligibility
```

数据表包括 `review`、`review_image`、`review_eligibility`、`complaint`、`complaint_track`、`idempotent_record` 和 `event_message`。Controller 统一要求可信网关令牌和用户 ID；共享 `GlobalExceptionHandler` 负责业务异常、参数校验异常与未处理异常的响应封装。

## 2. 评价提交

```text
POST /reviews
  -> 校验 Idempotent-Key、内容、请求指纹
  -> 查询/创建 REVIEW 幂等记录
  -> 通过内部订单接口校验订单
  -> 检查订单未重复评价与 review_eligibility
  -> 写 review、review_image
  -> 标记幂等记录完成并返回 reviewId
```

1. 幂等范围是 `(biz_type=REVIEW, subject_id=userId, idempotent_key)`。记录保留一天；同键同请求回放 `reviewId`，不同请求或仍在处理中返回冲突。
2. 订单查询通过 `OrderQueryService` 调用订单服务内部接口。订单必须属于当前用户、状态为 `COMPLETED`，并有同订单、同用户、同项目的 `review_eligibility` 记录。
3. `review.order_id` 具有唯一约束，所以每个订单只能有一条评价；并发重复插入由数据库约束兜底。
4. 评价创建时写入服务项目名称与规格名称快照，图片逐项去空白后批量写入 `review_image`。
5. 当前提交记录的 `status` 为 `1`；评价列表 Mapper 同样按 `status=1` 查询。该状态在源码中常量名为 `REVIEW_STATUS_PENDING`，接口不另行翻译其业务语义。

## 3. 评价列表

`ReviewServiceImpl.pageReviews` 先校验项目 ID，再规整分页值，执行评价分页和总数两次查询。存在结果时再通过一次批量图片查询按 `review_id` 分组回填，避免 N+1 查询。用户显示名不访问 user-service，而是用用户 ID 后四位组成 `user{last4}`。

## 4. 投诉提交与查询

### 提交

投诉幂等不使用通用幂等表，而使用 `complaint` 中 `(user_id, idempotent_key)` 唯一约束和 `request_hash`：

1. 校验幂等键、非空内容、图片 JSON 化和请求指纹。
2. 先查同用户同键记录；指纹一致则回放，指纹不一致返回冲突。
3. 通过订单服务确认订单存在、归属当前用户，且状态为待服务或已完成。
4. 在同一事务写入状态为 `0` 的投诉与首条 `complaint_track(operator=system, content=Complaint received)`。

### 查询

我的投诉按用户分页；图片 JSON 在读取时反序列化，旧数据格式错误只记录警告并返回空图片数组。进度查询先校验 ID、投诉存在性和归属，然后按 Mapper 顺序返回全部未逻辑删除轨迹。

## 5. 订单事件与评价资格

订单服务支付成功后通过 Kafka 发出 `ORDER_PAID`。`OrderEventConsumer` 只处理事件类型匹配、且 `orderId`、`userId`、`serviceItemId`、`paidAt` 齐全的消息。

- 消费前先写入 `idempotent_record`，范围为 `(ORDER_PAID, orderId, order_paid)`，重复消息直接忽略。
- 首次消费写入 `review_eligibility(order_id, user_id, service_item_id, paid_time)`。
- 因此支付事件尚未消费成功时，即使订单已完成，提交评价也会以 `4002` 被拒绝；事件投递和消费完成后可重试。

## 6. 外部依赖与后台任务

`OrderQueryServiceImpl` 使用配置的 `X-Internal-Token` 调用订单服务。订单查询的非成功响应或网络错误均转换为 HTTP `503`、`code=1009`，避免将依赖异常伪装成订单不存在。

| 任务 | 默认调度 | 行为 |
| --- | --- | --- |
| `IdempotentRecordCleanupJob` | 每小时 | 每批删除最多 1000 条已过期通用幂等记录 |
| `ReviewSnapshotBackfillJob` | 每分钟 | 查询最多 100 条缺少服务/规格快照的评价，批量查询订单服务后补齐；失败保留记录下次重试 |

## 7. 源码对应表

| 职责 | 源码 |
| --- | --- |
| HTTP 入口 | `controller/ReviewController.java`、`ComplaintController.java` |
| 评价流程 | `service/impl/ReviewServiceImpl.java` |
| 投诉流程 | `service/impl/ComplaintServiceImpl.java` |
| 订单依赖 | `integration/impl/OrderQueryServiceImpl.java` |
| 订单事件消费 | `consumer/OrderEventConsumer.java` |
| 后台任务 | `job/IdempotentRecordCleanupJob.java`、`ReviewSnapshotBackfillJob.java` |
| 表结构 | `src/main/resources/db/migration/` |
