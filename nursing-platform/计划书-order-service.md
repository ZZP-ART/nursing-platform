# nursing-order-service 开发计划书

> **版本**：v1.0
> **日期**：2026-07-08
> **状态**：已评审
> **对应分支**：`codex/order-service`

---

## 一、概述

nursing-order-service 是智慧护理平台的订单核心服务，负责下单预约、订单生命周期管理、地址管理、支付回调处理、支付成功事件投递等业务。服务注册名为 `nursing-order-service`，默认端口 `8083`。

### 1.1 服务核心职责

| 模块 | 职责 |
|------|------|
| 订单 | 创建订单、订单列表、订单详情、取消订单、状态机流转 |
| 支付 | 发起支付、支付回调、金额校验、幂等控制 |
| 地址 | 用户收货地址 CRUD、默认地址切换 |
| 幂等 | 预下单令牌发放、`Idempotent-Key` 校验 |
| 消息 | 支付成功后通过 Outbox 投递 `ORDER_PAID` 事件 |

### 1.2 服务依赖

| 方向 | 服务 / 组件 | 方式 | 场景 |
|------|-------------|------|------|
| 调用 | catalog-service | Feign 同步 | 下单时查询服务项和实时价格 |
| 被调 | feedback-service | 内部接口 / 事件 | 订单完成后可评价、投诉链路 |
| 发出 | Kafka `order_events` | Outbox 模式 | 支付成功后发送 `ORDER_PAID` |

---

## 二、项目结构

### 2.1 代码结构

```text
nursing-order-service/
+-- pom.xml
+-- src/main/java/com/nursing/order/
    +-- OrderApplication.java
    +-- controller/
    |   +-- OrderController.java
    |   +-- AddressController.java
    |   +-- InternalOrderController.java
    +-- service/
    |   +-- IOrderService.java
    |   +-- IAddressService.java
    |   +-- PaymentService.java
    |   +-- IdempotentService.java
    |   +-- OrderTimeoutScheduler.java
    |   +-- impl/
    |       +-- OrderServiceImpl.java
    |       +-- AddressServiceImpl.java
    +-- repository/
    |   +-- OrderHeaderMapper.java
    |   +-- PaymentRecordMapper.java
    |   +-- UserAddressMapper.java
    |   +-- OrderOperationLogMapper.java
    |   +-- OrderSequenceMapper.java
    |   +-- IdempotentRecordMapper.java
    |   +-- EventMessageMapper.java
    +-- entity/
    |   +-- OrderHeader.java
    |   +-- PaymentRecord.java
    |   +-- UserAddress.java
    |   +-- OrderOperationLog.java
    |   +-- OrderSequence.java
    |   +-- IdempotentRecord.java
    |   +-- EventMessage.java
    +-- dto/
    |   +-- request/
    |   |   +-- OrderCreateRequest.java
    |   |   +-- OrderPageQuery.java
    |   |   +-- CancelOrderRequest.java
    |   |   +-- PayRequest.java
    |   |   +-- AddressRequest.java
    |   +-- response/
    |       +-- PrepayTokenResponse.java
    |       +-- OrderCreateResponse.java
    |       +-- OrderListResponse.java
    |       +-- OrderDetailResponse.java
    |       +-- OrderOperationLogResponse.java
    |       +-- PayResponse.java
    |       +-- CancelResponse.java
    |       +-- AddressResponse.java
    +-- event/
        +-- OrderPaidEvent.java
        +-- OrderEventPublisher.java
```

### 2.2 common 模块协作

`nursing-common` 中需提供：

```text
nursing-common/src/main/java/com/nursing/common/feign/
+-- CatalogServiceFeignClient.java

nursing-common/src/main/java/com/nursing/common/dto/
+-- ServiceItemDTO.java
+-- ServiceSpecDTO.java
+-- OrderDTO.java
```

---

## 三、数据库实体与映射

### 3.1 表清单

| 表名 | Entity | 说明 |
|------|--------|------|
| `order_header` | `OrderHeader` | 订单主表，含服务快照、地址快照、乐观锁版本 |
| `payment_record` | `PaymentRecord` | 支付记录，防止重复回调入账 |
| `user_address` | `UserAddress` | 用户地址 |
| `order_operation_log` | `OrderOperationLog` | 订单状态流转日志 |
| `order_sequence` | `OrderSequence` | 生成订单号的序列表 |
| `idempotent_record` | `IdempotentRecord` | 幂等令牌记录 |
| `event_message` | `EventMessage` | Outbox 本地消息表 |

### 3.2 关键索引与约束

| 表 | 约束名 | 类型 | 作用 |
|----|--------|------|------|
| `order_header` | `uk_order_no` | UNIQUE | 订单号唯一 |
| `order_header` | `uk_user_service_slot` | UNIQUE | 同用户同服务同时间槽防重复下单 |
| `order_header` | `idx_user_id` | INDEX | 用户订单列表查询 |
| `order_header` | `idx_status` | INDEX | 状态筛选 |
| `payment_record` | `uk_order_pay_type` | UNIQUE | 防止支付回调重复入账 |
| `idempotent_record` | `uk_key` | UNIQUE | 预下单幂等键唯一 |
| `event_message` | `uk_event` | UNIQUE | Outbox 事件幂等 |

---

## 四、幂等性设计（四层防御）

### L1：预下单令牌发放

```text
POST /api/v1/orders/prepay-token
-> 生成 UUID
-> INSERT idempotent_record(status=0, expire=30min)
```

- 前端拿到后暂存本地
- 创建订单时通过 `Idempotent-Key` 头回传

### L2：`Idempotent-Key` 校验

```text
Header: Idempotent-Key = <prepay_token>
```

- 查询 `idempotent_record`
  - 不存在：拒绝请求
  - `status=1`：直接返回已有 `biz_id`
  - `status=0`：执行创建逻辑并更新为已完成

### L3：数据库唯一约束兜底

```text
UNIQUE KEY uk_user_service_slot
(user_id, service_item_id, service_date, service_time_slot)
```

- 即便应用层幂等失效，数据库仍可兜底防重

### L4：支付回调幂等

```text
UNIQUE KEY uk_order_pay_type (order_no, pay_type)
```

- 支付宝重复通知时，命中唯一约束即可视为已处理，直接返回 `success`

---

## 五、订单状态机

```text
待支付(0) --支付成功--> 待服务(1) --服务完成--> 已完成(2)
    |                         |
    | 用户取消                | 服务前取消
    v                         v
已取消(3)                已取消(3, 需退款)

已完成(2) --申请退款--> 退款中(4) --退款成功--> 已退款(5)
```

### 5.1 状态值定义

| 值 | 含义 | 可操作 | 说明 |
|----|------|--------|------|
| 0 | 待支付 | 取消、支付 | 超时自动取消 |
| 1 | 待服务 | 取消（触发退款） | 服务开始前可取消 |
| 2 | 已完成 | 申请退款、评价 | 服务完成后的终态前置状态 |
| 3 | 已取消 | 无 | 终态 |
| 4 | 退款中 | 无 | 客服 / 支付处理中 |
| 5 | 已退款 | 无 | 终态 |

### 5.2 关键规则

- 每次状态变更都写 `order_operation_log`
- 取消待支付订单可直接完成
- 取消待服务订单需进入退款链路
- `order_header.version` 负责乐观锁并发控制

---

## 六、API 设计

### 6.1 订单接口（OrderController）

| 序号 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|------|
| 1 | POST | `/api/v1/orders/prepay-token` | JWT | 发放幂等令牌 |
| 2 | POST | `/api/v1/orders` | JWT | 创建订单 |
| 3 | GET | `/api/v1/orders` | JWT | 订单分页列表 |
| 4 | GET | `/api/v1/orders/{id}` | JWT | 订单详情 |
| 5 | POST | `/api/v1/orders/{id}/cancel` | JWT | 取消订单 |
| 6 | POST | `/api/v1/orders/{id}/pay` | JWT | 发起支付 |
| 7 | POST | `/api/v1/orders/pay/callback` | 免 JWT，需验签 | 支付回调 |

### 6.2 内部订单接口（InternalOrderController）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/internal/orders/{id}` | 供 feedback-service 等内部服务查询可信订单数据 |

### 6.3 地址接口（AddressController）

| 序号 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|------|
| 1 | GET | `/api/v1/addresses` | JWT | 地址列表 |
| 2 | POST | `/api/v1/addresses` | JWT | 新增地址 |
| 3 | PATCH | `/api/v1/addresses/{id}` | JWT | 编辑地址 |
| 4 | DELETE | `/api/v1/addresses/{id}` | JWT | 逻辑删除 |
| 5 | PUT | `/api/v1/addresses/{id}/default` | JWT | 设置默认地址 |

### 6.4 错误码（order-service 3000-3999）

| HTTP | code | message | 触发条件 |
|------|------|---------|----------|
| 400 | 3000 | 幂等令牌无效或已过期 | 预下单令牌不存在或过期 |
| 409 | 3001 | 重复下单 | 幂等键已处理 |
| 422 | 3002 | 服务项目不存在 | catalog-service 返回空 |
| 422 | 3003 | 服务规格已下架 | 规格状态不可用 |
| 422 | 3005 | 该时段已被预约 | 命中唯一约束 |
| 404 | 3007 | 订单不存在 | 无效订单 ID |
| 403 | 3008 | 无权操作此订单 | 非订单所属用户 |
| 422 | 3009 | 当前状态不可取消 | 非待支付 / 待服务 |
| 422 | 3010 | 当前状态不可支付 | 非待支付 |
| 422 | 3011 | 支付已处理，请勿重复提交 | 命中支付幂等 |
| 400 | 3012 | 支付宝验签失败 | 签名校验失败 |
| 422 | 3013 | 地址不存在 | 地址无效或已删除 |
| 500 | 3999 | 订单服务内部错误 | 系统异常 |

---

## 七、核心处理流程

### 7.1 创建订单

1. 校验 `Idempotent-Key`
2. 校验服务项、规格、地址合法性
3. 调用 catalog-service 获取服务和规格实时信息
4. 生成订单快照和金额快照
5. 生成 `orderId` 与 `orderNo`
6. 写入 `order_header`
7. 写入 `order_operation_log`
8. 更新幂等记录状态
9. 返回 `orderId`、`orderNo`

### 7.2 支付回调

1. 校验 RSA2 签名
2. 校验 appId、sellerId、orderNo、amount、tradeStatus
3. 写入 `payment_record`
4. 乐观锁更新订单状态为待服务
5. 记录操作日志
6. 同事务写入 `event_message`
7. 返回纯文本 `success`

### 7.3 地址 CRUD

- 新增地址时，若 `isDefault=1`，需先清空该用户其他默认地址
- 编辑地址为 PATCH 语义
- 删除地址采用逻辑删除
- 设置默认地址需在事务内完成“清空旧默认 + 设置新默认”

---

## 八、Feign 与 Kafka

### 8.1 Feign 集成 catalog-service

```java
@FeignClient(name = "nursing-catalog-service", path = "/api/v1/items")
public interface CatalogServiceFeignClient {
    @GetMapping("/{id}")
    Result<ServiceItemDTO> getItemDetail(@PathVariable("id") Long id);
}
```

调用场景：

1. 下单时查服务项详情
2. 从 `specs` 中找到目标规格
3. 校验规格状态
4. 取当前价格写入订单快照

### 8.2 Kafka Outbox

```text
事务内：
1. 更新 order_header
2. INSERT event_message(status=0)

事务提交后：
1. 定时扫描 pending event_message
2. kafkaTemplate.send(topic, eventKey, payload)
3. 成功后更新 status=1
4. 失败则 retry_count+1
5. 达到阈值后标记失败待人工处理
```

事件体示例：

```json
{
  "eventType": "ORDER_PAID",
  "orderId": 20001,
  "orderNo": "20260708123456",
  "userId": 10001,
  "paidAt": "2026-07-08T14:30:00+08:00"
}
```

---

## 九、配置补充

| 配置项 | 说明 |
|--------|------|
| `nursing.snowflake.worker-id=3` | 订单服务雪花 ID 节点 |
| `nursing.snowflake.datacenter-id=1` | 数据中心 ID |
| `nursing.payment.mock=true` | DEV 环境模拟支付 |
| `nursing.payment.alipay.*` | 支付宝配置项 |

---

## 十、实施顺序

### Phase 1：基础骨架
- Entity + Mapper + XML
- common 模块 Feign DTO
- SnowflakeConfig

### Phase 2：地址 CRUD
- Address DTO
- `IAddressService` / `AddressServiceImpl`
- `AddressController`

### Phase 3：幂等 + 下单闭环
- `IdempotentService`
- 创建订单 DTO
- `IOrderService` / `OrderServiceImpl#createOrder`
- `OrderController` 基础接口

### Phase 4：订单查询 + 取消
- 分页查询 DTO
- 订单详情 DTO
- 取消订单状态流转

### Phase 5：支付 + Outbox
- 支付 DTO
- `PaymentService`
- `OrderPaidEvent` / `OrderEventPublisher`
- 回调处理与支付发起

---

## 十一、测试要点

| 类型 | 覆盖内容 |
|------|---------|
| 幂等测试 | 同一 `Idempotent-Key` 重复创建订单应返回相同结果 |
| 状态机测试 | 各状态下允许 / 禁止的操作是否符合预期 |
| 乐观锁测试 | 并发更新版本号冲突处理 |
| Outbox 测试 | 本地消息写入与定时投递 |
| 地址权限测试 | 用户 A 不得操作用户 B 地址 |

---

## 十二、已确认的决策

| # | 问题 | 决策 |
|---|------|------|
| 1 | 创建订单路由 | `POST /api/v1/orders` |
| 2 | 编辑地址方法 | `PATCH /api/v1/addresses/{id}` |
| 3 | `uk_user_service_slot` | 补充唯一索引兜底 |
| 4 | 分页方案 | 使用清晰可控的分页实现，保持接口稳定 |
| 5 | `order_item` 是否拆表 | MVP 阶段不拆，先使用主表冗余快照 |
| 6 | 支付回调响应 | 返回纯文本 `"success"` |

---

## 附录：参考文档

- `系统架构设计.md`
- `API接口文档.md`
- `03_order_schema.sql`
