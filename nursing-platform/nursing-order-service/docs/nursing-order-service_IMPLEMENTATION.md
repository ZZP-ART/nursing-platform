# order-service 接口实现逻辑

> 本文描述当前源码中的调用链、状态转换、数据写入和可靠性处理，并与 `API.md` 对应。

## 1. 架构与数据

```text
Gateway -> Controller -> Service -> MyBatis Mapper -> order_db
                              |-> catalog-service (OpenFeign)
                              |-> Kafka outbox -> feedback-service
                              |-> payment/refund gateway
```

核心表包括：`user_address`、`order_header`、`payment_record`、`payment_intent`、`refund_record`、`order_operation_log`、`idempotent_record`、`event_message`。订单保存项目、规格、价格、地址和收件人快照，后续目录或地址变更不会改变已创建订单。

所有公开 Controller 均先校验 `X-Gateway-Token`，再校验正整数 `X-User-Id`。全局异常处理器将 `BusinessException` 转为其 HTTP 状态与 `Result.error`；`@Valid` 校验错误为 HTTP 400 / `code=1000`。

## 2. 地址接口

`AddressController -> AddressServiceImpl -> UserAddressMapper`。

- 地址查询按用户读取未逻辑删除记录。
- 创建、更新、设置默认均在事务中执行；设置 `isDefault=1` 时先清除当前用户所有默认标记。
- 删除为逻辑删除。所有修改操作先通过 `selectByIdAndUserId` 验证归属，因此不会跨用户修改地址。
- 更新使用 `copyRequest` 覆盖实体字段，不是数据库层的非空字段合并更新；这也是 API 文档要求完整提交更新内容的原因。

## 3. 下单与幂等

```text
POST /prepay-token
  -> IdempotentService.issuePrepayToken
  -> idempotent_record(status=0, expire_time=now+10m)

POST /orders
  -> 锁定幂等令牌并绑定请求 SHA-256 指纹
  -> catalog-service 查询项目和规格
  -> 校验日期、地址、时段
  -> 写入 order_header 与 create 操作日志
  -> 将幂等记录标记完成并绑定 orderId
```

1. 预支付令牌以 `pt_` 加 UUID 生成，业务类型为 `CREATE_ORDER`。
2. 创建订单前先比较幂等记录的用户、类型与请求指纹。已完成的同请求会回放原订单；未完成令牌过期或用于不同内容均失败。
3. 通过 `CatalogServiceFeignClient.getItemDetail` 读取项目并确认项目、规格状态均为上架。
4. 预约日期必须严格晚于当前日期；地址必须属于当前用户。服务先查询时段占用，再依赖 `uk_user_service_slot` 唯一键处理并发竞争。
5. `order_header` 初始状态为 `0`、数量为 `1`，使用雪花 ID，订单号由日期加 `order_sequence` 自增序列组成。

## 4. 查询与订单状态转换

```text
0 待支付 --支付成功--> 1 待服务 --用户完成--> 2 已完成
     |                                  
     +--用户/超时取消--> 3 已取消
1 待服务 --用户取消或迟到支付--> 4 退款中 --退款成功--> 5 已退款
```

- 列表查询使用用户 ID、可选状态和 `offset/size`；详情同时读取支付记录和操作日志。
- 取消操作使用 `id + status + version` 乐观锁更新。待支付取消直接进入 `3`，已支付订单进入 `4` 并提交退款任务。
- 完成操作仅允许 `1 -> 2`，并写入 `complete` 操作日志。
- `OrderTimeoutScheduler` 每分钟扫描创建超过默认 30 分钟的待支付订单，按乐观锁转为 `3`，同时写 `timeout_cancel` 日志。

## 5. 支付、回调与退款

### 支付意图

`PaymentService.initiatePayment` 以 `(user_id, idempotent_key)` 和 `order_id` 双唯一约束保护支付意图。相同请求回放 `READY` 或 `SUCCESS` 结果；相同键但订单或渠道不同返回冲突。

模拟支付在开发/测试 profile 中直接完成支付，写入 `payment_record`、`pay` 日志、支付意图状态和订单支付事件。非模拟支付返回调起参数，实际入账由回调完成。

### 回调

非模拟回调会校验 RSA2 签名、应用/卖家信息、订单号和金额。`notify_id` 与 `trade_no` 在数据库中唯一，确保重放安全。正常入账以条件更新执行 `0 -> 1`，写入支付记录与日志；支付超时取消后迟到到账时转换 `3 -> 4` 并创建退款记录。

### 退款

`RefundService` 为已支付订单创建每订单唯一的 `refund_record`。`RefundWorker` 每 5 秒领取到期任务，使用租约防止多实例重复处理；失败采用指数退避，达到默认 5 次后标为人工处理。退款成功时更新支付记录为已退款、订单 `4 -> 5`，并写入 `refund_complete` 日志。

## 6. 支付事件 Outbox

支付成功时 `OrderEventPublisher.saveOrderPaidEvent` 在订单事务内写入唯一 `event_message`，事件键为 `order_paid:{orderId}`。定时任务每 5 秒批量发送待投递消息到配置的 Kafka topic；发送失败记录错误，并以最多 300 秒的指数退避重试。反馈服务消费该事件以建立评价资格。

## 7. 内部接口与源码对应

`InternalOrderController` 通过独立的 `X-Internal-Token` 保护订单查询，供反馈等内部服务使用。批量查询最多 100 个订单。

| 职责 | 源码 |
| --- | --- |
| HTTP 入口 | `controller/OrderController.java`、`AddressController.java`、`InternalOrderController.java` |
| 下单、状态与查询 | `service/impl/OrderServiceImpl.java` |
| 地址管理 | `service/impl/AddressServiceImpl.java` |
| 支付与回调 | `service/PaymentService.java` |
| 超时、退款 | `service/OrderTimeoutScheduler.java`、`RefundService.java`、`RefundWorker.java` |
| Outbox 投递 | `event/OrderEventPublisher.java` |
| 表结构 | `src/main/resources/db/migration/` |
