# nursing-feedback-service 接口实现逻辑

> 本文按 catalog-service 的端点级结构说明评价、投诉和订单事件处理的当前实现。

## 1. 架构与通用规则

```text
Gateway -> ReviewController / ComplaintController -> Service -> Mapper -> feedback_db
                                                   |-> order-service internal API
Kafka order_events -> OrderEventConsumer -> review_eligibility
```

所有 HTTP 接口要求可信 `X-Gateway-Token` 与 `X-User-Id`。核心表为 `review`、`review_image`、`review_eligibility`、`complaint`、`complaint_track`、`idempotent_record` 和 `event_message`。订单查询失败会统一转换为 HTTP `503`、`code=1009`。

## 2. `POST /api/v1/reviews`

### 调用链

```text
ReviewController.submitReview -> ReviewServiceImpl.submitReview
  -> IdempotentRecordMapper
  -> OrderQueryService -> order-service internal API
  -> ReviewMapper + ReviewImageMapper + ReviewEligibilityMapper
```

### 执行步骤
1. 校验 `Idempotent-Key`、评价内容与请求指纹，并删除同范围过期幂等记录。
2. 相同用户、同键且同请求直接回放 `reviewId`；不同请求或正在处理的记录返回冲突。
3. 通过内部订单接口验证订单存在、归属当前用户且状态为已完成。
4. 校验本地 `review_eligibility` 中存在同订单、用户和项目的资格记录，且订单尚未评价。
5. 写入评价快照、过滤空白图片并批量写入图片记录，最后标记幂等记录完成。

### 边界与可靠性
- `review.order_id` 唯一约束确保订单只能评价一次。
- 当前写入的评价状态为 `1`，评价列表 Mapper 也按 `status=1` 查询。
- 支付事件尚未被消费时，即使订单已完成也会返回 `4002`，前端可稍后重试。

## 3. `GET /api/v1/reviews`

### 调用链
`ReviewController.pageReviews -> ReviewServiceImpl.pageReviews -> ReviewMapper + ReviewImageMapper`。

### 执行步骤
1. 校验 `itemId` 大于 0，并将页码、大小规整到有效范围。
2. 查询指定项目的评价页和总数。
3. 以评价 ID 批量查询图片、分组回填，并生成脱敏显示名和快照字段。

### 边界与可靠性
- 空结果直接返回空列表，不发送图片查询。
- 批量图片回填避免评价列表的 N+1 查询。
- Controller 仍要求当前登录身份，但查询本身不按该身份过滤评价。

## 4. `POST /api/v1/complaints`

### 调用链

```text
ComplaintController.submitComplaint -> ComplaintServiceImpl.submitComplaint
  -> OrderQueryService -> order-service internal API
  -> ComplaintMapper + ComplaintTrackMapper
```

### 执行步骤
1. 校验最长 64 的幂等键、非空内容、图片数组和请求指纹。
2. 查询 `(userId, idempotentKey)`；相同指纹回放投诉 ID，不同指纹返回冲突。
3. 查询订单并验证归属，订单状态必须为待服务或已完成。
4. 在同一事务中写入状态 `0` 的投诉和首条 `Complaint received` 系统轨迹。

### 边界与可靠性
- 幂等信息保存在 `complaint` 自身，不使用通用 `idempotent_record`。
- 数据库唯一键处理并发重复提交；图片 JSON 仅保存非空白 URL。

## 5. `GET /api/v1/complaints`

### 调用链
`ComplaintController.pageComplaints -> ComplaintServiceImpl.pageComplaints -> ComplaintMapper`。

### 执行步骤
按当前用户执行分页和总数查询，反序列化图片 JSON，转换投诉类型和状态文本。

### 边界与可靠性
`page < 1` 和超出范围的 `size` 会被规整为 1-100；旧数据图片 JSON 解析失败仅记录警告并返回空数组。

## 6. `GET /api/v1/complaints/{id}/tracks`

### 调用链
`ComplaintController.getComplaintTracks -> ComplaintServiceImpl.getComplaintTracks -> ComplaintMapper + ComplaintTrackMapper`。

### 执行步骤
校验投诉 ID，查询投诉并校验用户归属，再读取全部未逻辑删除轨迹并组装状态文本。

### 边界与可靠性
投诉不存在返回 `4006`，非所有者返回 `4007`；轨迹接口不改变投诉状态。

## 7. 异步订单资格与后台任务

### `ORDER_PAID` Kafka 消费

`OrderEventConsumer` 解析订单支付事件，校验事件类型和必要字段后，先写入 `(ORDER_PAID, orderId, order_paid)` 幂等记录；首次消费写入 `review_eligibility`，重复消息直接忽略。

### 后台任务

| 任务 | 默认频率 | 行为 |
| --- | --- | --- |
| `IdempotentRecordCleanupJob` | 每小时 | 每批删除最多 1000 条过期通用幂等记录 |
| `ReviewSnapshotBackfillJob` | 每分钟 | 读取最多 100 条缺失服务/规格快照的评价，批量查询订单服务后补齐；失败留待下次重试 |

## 8. 源码对应表

| 职责 | 源码 |
| --- | --- |
| HTTP 入口 | `controller/ReviewController.java`、`ComplaintController.java` |
| 评价流程 | `service/impl/ReviewServiceImpl.java` |
| 投诉流程 | `service/impl/ComplaintServiceImpl.java` |
| 内部订单依赖 | `integration/impl/OrderQueryServiceImpl.java` |
| 订单事件消费 | `consumer/OrderEventConsumer.java` |
| 后台任务 | `job/IdempotentRecordCleanupJob.java`、`ReviewSnapshotBackfillJob.java` |
| 表结构 | `deploy/mysql/init/04_feedback_database.sql` |
