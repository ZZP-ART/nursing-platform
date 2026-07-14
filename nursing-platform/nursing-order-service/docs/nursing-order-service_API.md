# order-service 接口说明

> 基于当前 `nursing-order-service` 实现整理，覆盖业务 API、支付宝回调和受内部令牌保护的服务间接口。

## 1. 服务约定

| 项目 | 说明 |
| --- | --- |
| 服务名 | `nursing-order-service` |
| 默认端口 | `8083` |
| 网关路由 | `/api/v1/orders/**`、`/api/v1/addresses/**` |
| Content-Type | 除回调外为 `application/json` |
| 成功响应 | `{ "code": 0, "message": "success", "data": ... }` |

除 `POST /api/v1/orders/pay/callback` 外，所有公开订单与地址接口均要求网关注入以下请求头：

| Header | 必填 | 说明 |
| --- | --- | --- |
| `X-Gateway-Token` | 是 | 必须等于服务端配置的可信网关令牌 |
| `X-User-Id` | 是 | 正整数登录用户 ID |

缺少或错误的用户 ID 返回 `code=1002`；可信网关令牌不匹配返回 HTTP `403`、`code=1004`。参数校验失败返回 HTTP `400`、`code=1000`；未处理异常返回 HTTP `500`、`code=1999`。

### 1.1 订单状态

| 值 | 状态 |
| ---: | --- |
| `0` | 待支付 |
| `1` | 待服务 |
| `2` | 已完成 |
| `3` | 已取消 |
| `4` | 退款中 |
| `5` | 已退款 |

### 1.2 分页响应

订单列表的 `data` 为 `PageResult<OrderListResponse>`：

```json
{ "list": [], "total": 0, "page": 1, "size": 20 }
```

## 2. 地址管理

### `GET /api/v1/addresses`

返回当前用户全部未逻辑删除地址。`data` 为地址数组，单项字段包括 `addressId`、`receiverName`、`receiverPhone`、`tag`、`province`、`city`、`district`、`detailAddress`、`isDefault`。

### `POST /api/v1/addresses`

创建地址，成功时 `data` 为 `{ "addressId": 123 }`。

### `PATCH /api/v1/addresses/{id}`

更新当前用户地址，成功时 `data=null`。路径 `id` 必须大于 `0`。虽然接口使用 `PATCH`，实现会把请求对象字段直接写入地址记录；调用方应提交完整地址信息，避免遗漏字段被写为 `null`。

### `DELETE /api/v1/addresses/{id}`

逻辑删除当前用户地址，成功时 `data=null`。

### `PUT /api/v1/addresses/{id}/default`

将指定地址设置为默认地址，成功时 `data=null`；同一用户原默认地址会被取消默认标记。

创建时所有下列字段必填。更新时 Bean Validation 仅校验已传字段，但建议仍完整提交。

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `receiverName` | string | 2-16 个字符 |
| `receiverPhone` | string | 中国大陆手机号：`^1\d{10}$` |
| `tag` | string | `家`、`公司`、`学校`、`其他` |
| `province` / `city` / `district` | string | 每项最多 32 个字符 |
| `detailAddress` | string | 5-100 个字符 |
| `isDefault` | integer | 可选，`0` 或 `1`；创建时省略默认为 `0` |

地址不存在或不属于当前用户时返回业务码 `3013`。

## 3. 获取下单令牌

### `POST /api/v1/orders/prepay-token`

为当前用户创建一次性下单令牌：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "prepayToken": "pt_...",
    "expireTime": "2026-07-14T10:10:00"
  }
}
```

令牌有效期为十分钟。下单时必须将 `prepayToken` 原样传入 `Idempotent-Key`；令牌仅能由创建它的用户使用，且不能用于不同请求内容。

## 4. 创建订单

### `POST /api/v1/orders`

请求头：`Idempotent-Key` 必填，取第 3 节返回的下单令牌。

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `serviceItemId` | number | 是 | 正整数 |
| `serviceSpecId` | number | 是 | 正整数，且属于可用项目 |
| `addressId` | number | 是 | 当前用户有效地址 |
| `serviceDate` | string | 是 | 日期，必须晚于当天 |
| `serviceTimeSlot` | string | 是 | `MORNING`、`AFTERNOON`、`EVENING` |
| `remark` | string | 否 | 最多 200 个字符 |

成功响应：

```json
{ "code": 0, "message": "success", "data": { "orderId": 123, "orderNo": "202607141" } }
```

同一用户、项目、日期和时段只能创建一个占用时段的订单。常见业务码：`3001` 幂等令牌无效、过期或请求内容不一致；`3002` 时段已预约；`3003` 项目下架；`3004` 规格下架；`3006` 地址无效。

## 5. 查询订单

### `GET /api/v1/orders`

| 参数 | 类型 | 默认值 | 规则 |
| --- | --- | --- | --- |
| `status` | integer | - | 可选，`0-5` |
| `page` | integer | `1` | 最小为 `1` |
| `size` | integer | `20` | `1-50` |

仅返回当前用户订单，按服务端 Mapper 的默认顺序分页。列表项包括订单号、项目/规格快照、金额、状态、预约时间、收件快照和创建时间。

### `GET /api/v1/orders/{id}`

返回当前用户订单详情。`data` 包含列表字段，以及 `payStatus` 和 `operationLogs`。日志项为 `action`、`fromStatus`、`toStatus`、`remark`、`createTime`。

订单不存在返回 `3007`；订单不属于当前用户返回 `3008`。

## 6. 取消与完成订单

### `POST /api/v1/orders/{id}/cancel`

请求体可省略；提供时仅支持：

```json
{ "cancelReason": "行程变更" }
```

`cancelReason` 最多 200 个字符。待支付订单取消后变为 `3`，返回 `refundStatus=NO_REFUND`；已支付待服务订单变为 `4` 并创建退款任务，返回 `refundStatus=REFUNDING`。对已取消、退款中或已退款订单重复调用会返回当前状态；其他状态不可取消，业务码为 `3009`。

### `POST /api/v1/orders/{id}/complete`

仅当前用户的待服务订单可完成，状态从 `1` 变为 `2`。成功 `data` 为内部订单对象，含 `orderId`、`orderNo`、`userId`、`status`、`serviceItemId`、`serviceItemName`、`specName`、`totalAmount`。

## 7. 发起支付与支付回调

### `POST /api/v1/orders/{id}/pay`

请求头 `Idempotent-Key` 必填，长度不超过 128。请求体：

```json
{ "payChannel": "alipay" }
```

同一用户同一幂等键只能用于相同订单和支付渠道；一个订单只能创建一个支付意图。成功响应字段为：

| 字段 | 说明 |
| --- | --- |
| `orderId` / `orderNo` | 订单标识 |
| `payChannel` | 当前仅 `alipay` |
| `payAmount` | 支付金额 |
| `payStatus` | `READY` 或 `SUCCESS` |
| `mock` | 是否模拟支付 |
| `payParams` | 非模拟支付时用于调起渠道的参数 |

模拟支付仅允许开发或测试 profile，调用即将订单支付成功。非模拟模式下，订单必须为待支付状态；无效状态返回 `3010`。

### `POST /api/v1/orders/pay/callback`

支付宝服务端回调，不使用统一 `Result` 包装，响应文本为 `success` 或 `failure`。生产调用必须携带并通过 RSA2 验签的 `notify_id`、`trade_no`、`out_trade_no`、`total_amount`、`trade_status`、`app_id`、`sign`、`sign_type`（配置卖家 ID 时还要求 `seller_id`）。

服务仅对 `TRADE_SUCCESS` 与 `TRADE_FINISHED` 执行入账，并校验订单金额、应用和卖家信息。重复且内容一致的回调可安全确认；冲突回调记录审计日志并返回 `failure`。

## 8. 内部订单接口

仅服务间调用。请求头 `X-Internal-Token` 必须等于 `nursing.internal.token`，否则返回 HTTP `403`、`code=1004`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/internal/v1/orders/{id}` | 返回一笔订单的内部 DTO |
| `POST` | `/internal/v1/orders/batch` | 请求体为 `Long[]`，查询多笔订单 |

批量接口限制 1-100 个 ID；不符合时返回 HTTP `400`、`code=1000`。
