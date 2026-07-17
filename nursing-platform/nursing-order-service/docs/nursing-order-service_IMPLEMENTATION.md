# nursing-order-service 接口实现逻辑

> 本文使用 catalog-service 的“逐接口调用链、执行步骤、边界”结构，说明订单服务当前实现。

## 1. 架构与通用规则

```text
Gateway -> Order/Address Controller -> Service -> Mapper -> order_db
                                      |-> catalog-service (OpenFeign)
                                      |-> Kafka outbox -> feedback-service
                                      |-> payment/refund gateway
```

公开接口先验证 `X-Gateway-Token` 和 `X-User-Id`；内部订单查询使用 `X-Internal-Token`。核心表是 `user_address`、`order_header`、`payment_record`、`payment_intent`、`refund_record`、`order_operation_log`、`idempotent_record` 与 `event_message`。订单会保存项目、规格、价格和地址快照。

## 2. `GET /api/v1/addresses`

### 调用链
`AddressController.list -> AddressServiceImpl.listAddresses -> UserAddressMapper.selectByUserId`。

### 执行步骤
按当前用户查询未逻辑删除地址，将实体转为 `AddressResponse` 数组并包装返回。

### 边界与可靠性
没有地址时返回空数组；不会返回其他用户地址。

## 3. `POST /api/v1/addresses`

### 调用链
`AddressController.create -> AddressServiceImpl.createAddress -> UserAddressMapper`。

### 执行步骤
1. 校验创建地址的必填字段。
2. `isDefault=1` 时先清除该用户其他默认标记。
3. 使用雪花 ID 插入地址，未传 `isDefault` 时写入 `0`。

### 边界与可靠性
清除旧默认地址和插入新地址在同一事务中完成。

## 4. `PATCH /api/v1/addresses/{id}`

### 调用链
`AddressController.update -> AddressServiceImpl.updateAddress -> UserAddressMapper`。

### 执行步骤
先按 `(addressId, userId)` 验证归属；请求标记默认时清除旧默认地址；随后直接复制请求字段并更新。

### 边界与可靠性
虽然方法为 PATCH，服务使用字段覆盖更新，调用方遗漏字段可能写入 `null`；不存在或无权地址返回 `3013`。

## 5. `DELETE /api/v1/addresses/{id}`

### 调用链
`AddressController.delete -> AddressServiceImpl.deleteAddress -> UserAddressMapper.logicalDelete`。

### 执行步骤
先校验当前用户归属，再设置逻辑删除标记。

### 边界与可靠性
已创建订单持有地址快照，删除地址不会影响历史订单。

## 6. `PUT /api/v1/addresses/{id}/default`

### 调用链
`AddressController.setDefault -> AddressServiceImpl.setDefaultAddress -> UserAddressMapper`。

### 执行步骤
验证地址归属，清除用户现有默认地址，再设置目标地址默认标记。

### 边界与可靠性
两个更新在同一事务中执行，避免用户出现两个默认地址。

## 7. `POST /api/v1/orders/prepay-token`

### 调用链
`OrderController.prepayToken -> IdempotentService.issuePrepayToken -> IdempotentRecordMapper.insert`。

### 执行步骤
生成 `pt_` 加 UUID 的令牌，写入业务类型为 `CREATE_ORDER`、状态为处理中且十分钟过期的幂等记录。

### 边界与可靠性
UUID 冲突最多重试三次。令牌绑定用户，不能用于其他用户或不同下单内容。

## 8. `POST /api/v1/orders`

### 调用链

```text
OrderController.create -> OrderServiceImpl.createOrder
  -> IdempotentService lock/bind
  -> CatalogServiceFeignClient.getItemDetail
  -> UserAddressMapper / OrderHeaderMapper
  -> OrderOperationLogMapper
```

### 执行步骤
1. 锁定预支付令牌并比较请求 SHA-256 指纹；完成的相同请求直接回放订单。
2. 调用 catalog-service 校验项目与规格可用。
3. 校验日期晚于当天、地址归属和用户项目时段占用。
4. 创建状态为 `0` 的订单，保存目录与地址快照，并写 `create` 操作日志。
5. 完成幂等记录并返回订单 ID、订单号。

### 边界与可靠性
`uk_user_service_slot` 唯一键处理并发抢占时段；项目、规格、地址或令牌无效均返回对应业务码。

## 9. `GET /api/v1/orders`

### 调用链
`OrderController.list -> OrderServiceImpl.listOrders -> OrderHeaderMapper.selectPage/countPage`。

### 执行步骤
按当前用户、可选状态与页码查询订单，再查询总数并组装 `PageResult`。

### 边界与可靠性
分页大小由 DTO 限制为 1-50；列表只包含当前用户订单。

## 10. `GET /api/v1/orders/{id}`

### 调用链
`OrderController.detail -> OrderServiceImpl.getOrderDetail -> OrderHeaderMapper + PaymentRecordMapper + OrderOperationLogMapper`。

### 执行步骤
验证订单存在与归属，读取支付记录和操作日志，组装订单详情及状态轨迹。

### 边界与可靠性
订单不存在返回 `3007`，非所有者返回 `3008`；没有支付记录时 `payStatus` 为 `null`。

## 11. `POST /api/v1/orders/{id}/cancel`

### 调用链
`OrderController.cancel -> OrderServiceImpl.cancelOrder -> OrderHeaderMapper + RefundService`。

### 执行步骤
1. 读取并验证归属。
2. 待支付订单以乐观锁转为 `3`；待服务订单转为 `4`。
3. 写取消或退款操作日志；已支付订单创建退款记录。

### 边界与可靠性
已取消、退款中和已退款订单会返回当前状态，其他不可取消状态返回 `3009`。退款记录按订单唯一，重复创建安全。

## 12. `POST /api/v1/orders/{id}/complete`

### 调用链
`OrderController.complete -> OrderServiceImpl.completeOrder -> OrderHeaderMapper + OrderOperationLogMapper`。

### 执行步骤
验证归属后，以版本条件把状态从 `1` 更新为 `2`，写入 `complete` 日志并返回内部订单 DTO。

### 边界与可靠性
非待服务状态或并发状态变化均返回 `3010`，不会重复写完成日志。

## 13. `POST /api/v1/orders/{id}/pay`

### 调用链
`OrderController.pay -> PaymentService.initiatePayment -> PaymentIntentMapper + OrderHeaderMapper`。

### 执行步骤
1. 校验支付渠道和最长 128 的幂等键。
2. 以 `(userId, idempotentKey)` 和 `orderId` 双唯一约束创建或回放支付意图。
3. 模拟支付直接完成入账；真实支付返回渠道调起参数，等待回调。

### 边界与可靠性
同键不同订单或渠道、以及订单已有不同支付意图均返回冲突；仅待支付订单可新建支付。

## 14. `POST /api/v1/orders/pay/callback`

### 调用链
`OrderController.payCallback -> PaymentService.handleAlipayCallback -> payment/order/outbox mappers`。

### 执行步骤
1. 非模拟模式校验 RSA2 签名、应用、卖家、交易号、金额和交易状态。
2. 对成功交易以条件更新将订单 `0 -> 1`，写支付记录和 `pay` 日志。
3. 同事务写 `ORDER_PAID` Outbox 事件，并标记支付意图完成。

### 边界与可靠性
`notify_id` 与 `trade_no` 唯一保证回调重放安全；金额或标识冲突写审计日志并返回 `failure`。超时取消后的迟到支付会转为退款中并创建退款任务。

## 15. `GET /internal/v1/orders/{id}`

### 调用链
`InternalOrderController.getOrder -> OrderServiceImpl.getInternalOrder -> OrderHeaderMapper.selectById`。

### 执行步骤
校验内部令牌，读取订单并映射为跨服务 `OrderDTO`。

### 边界与可靠性
不进行用户归属校验，因为该接口仅供可信服务调用；订单不存在返回 `3007`。

## 16. `POST /internal/v1/orders/batch`

### 调用链
`InternalOrderController.getOrders -> OrderServiceImpl.getInternalOrders -> OrderHeaderMapper.selectByIds`。

### 执行步骤
校验内部令牌和 1-100 个订单 ID，批量查询后映射为 DTO 列表。

### 边界与可靠性
空列表或超过 100 个 ID 返回 HTTP `400`、`code=1000`。

## 17. 后台流程与源码对应

`OrderTimeoutScheduler` 默认每分钟取消超时待支付订单。`RefundWorker` 每五秒领取退款任务、使用租约和指数退避，成功后将订单 `4 -> 5`。`OrderEventPublisher` 每五秒将 `ORDER_PAID` Outbox 事件投递 Kafka，失败以最多 300 秒的指数退避重试。

| 职责 | 源码 |
| --- | --- |
| HTTP 入口 | `controller/OrderController.java`、`AddressController.java`、`InternalOrderController.java` |
| 订单与地址 | `service/impl/OrderServiceImpl.java`、`AddressServiceImpl.java` |
| 支付与退款 | `service/PaymentService.java`、`RefundService.java`、`RefundWorker.java` |
| 超时与 Outbox | `service/OrderTimeoutScheduler.java`、`event/OrderEventPublisher.java` |
| 表结构 | `deploy/mysql/init/03_order_schema.sql` |
