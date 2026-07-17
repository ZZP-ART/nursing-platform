# nursing-order-service 接口说明

> 本文遵循 catalog-service 的接口文档结构，覆盖订单、地址、支付回调及内部订单查询接口。

## 1. 服务约定

| 项目 | 说明 |
| --- | --- |
| 服务名 | `nursing-order-service` |
| 默认端口 | `8083` |
| 网关路由 | `/api/v1/orders/**`、`/api/v1/addresses/**` |
| Content-Type | 业务接口为 `application/json`；支付回调为表单参数 |
| 成功响应 | `{ "code": 0, "message": "success", "data": ... }` |

除支付回调外，公开接口均由网关注入 `X-Gateway-Token` 和正整数 `X-User-Id`。前端应通过网关调用，不应伪造这两个头。缺失用户身份为 `1002`，网关令牌不可信为 HTTP `403`、`1004`；Bean Validation 失败为 HTTP `400`、`1000`。

### 1.1 共用对象

| 对象 | 字段 | 说明 |
| --- | --- | --- |
| `PageResult<T>` | `list`、`total`、`page`、`size` | 订单和反馈列表分页对象 |
| `AddressRequest` | 收件人、电话、标签、省市区、详细地址、`isDefault` | 创建地址必填；更新建议完整提交 |
| `OrderStatus` | `0-5` | 依次为待支付、待服务、已完成、已取消、退款中、已退款 |

## 2. 获取地址列表

`GET /api/v1/addresses`

### 请求参数
无。

### 成功响应 `data`
`AddressResponse[]`，字段为 `addressId`、`receiverName`、`receiverPhone`、`tag`、`province`、`city`、`district`、`detailAddress`、`isDefault`。

## 3. 创建地址

`POST /api/v1/addresses`

### 请求体

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `receiverName` | string | 2-16 字符 |
| `receiverPhone` | string | `^1\d{10}$` |
| `tag` | string | `家`、`公司`、`学校`、`其他` |
| `province`、`city`、`district` | string | 每项最多 32 字符 |
| `detailAddress` | string | 5-100 字符 |
| `isDefault` | integer | 可选，`0` 或 `1` |

### 成功响应
`{ "data": { "addressId": 123 } }`。

## 4. 更新地址

`PATCH /api/v1/addresses/{id}`

### 路径与请求参数
`id` 必须大于 0；请求体使用第 3 节地址对象。实现为字段覆盖更新，尽管请求方法为 PATCH，调用方仍应完整提交业务字段。

### 成功响应
`data=null`。地址不存在或不属于当前用户时返回 `3013`。

## 5. 删除地址

`DELETE /api/v1/addresses/{id}`

### 路径参数
`id` 必须大于 0。

### 成功响应
`data=null`；删除为逻辑删除。地址不存在或无权操作返回 `3013`。

## 6. 设置默认地址

`PUT /api/v1/addresses/{id}/default`

### 路径参数与响应
`id` 必须大于 0；成功 `data=null`。同一用户的原默认地址会被取消默认状态。

## 7. 获取下单令牌

`POST /api/v1/orders/prepay-token`

### 成功响应 `data`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `prepayToken` | string | 以 `pt_` 开头的下单幂等令牌 |
| `expireTime` | string | 十分钟后过期时间 |

下单时必须将该值原样传入 `Idempotent-Key`。

## 8. 创建订单

`POST /api/v1/orders`

### 请求头
`Idempotent-Key` 必填，必须是第 7 节获取且属于当前用户的未过期令牌。

### 请求体

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `serviceItemId` / `serviceSpecId` / `addressId` | number | 正整数 |
| `serviceDate` | string | 必须晚于当天 |
| `serviceTimeSlot` | string | `MORNING`、`AFTERNOON`、`EVENING` |
| `remark` | string | 可选，最多 200 字符 |

### 成功响应
`{ "data": { "orderId": 123, "orderNo": "202607141" } }`。

同一用户、服务项目、日期和时段只能有一个占用订单。令牌无效、时段冲突、项目/规格不可用或地址无效分别使用 `3001`、`3002`、`3003/3004`、`3006`。

## 9. 查询我的订单

`GET /api/v1/orders`

### 请求参数

| 参数 | 类型 | 默认值 | 规则 |
| --- | --- | --- | --- |
| `status` | integer | - | 可选，`0-5` |
| `page` | integer | `1` | 最小 1 |
| `size` | integer | `20` | `1-50` |

### 成功响应 `data`
`PageResult<OrderListResponse>`；列表项包含 `orderId`、`orderNo`、项目/规格/价格快照、金额、状态、预约日期与时段、收件快照和 `createTime`。

## 10. 查询订单详情

`GET /api/v1/orders/{id}`

### 路径参数
`id` 必须大于 0。

### 成功响应 `data`
订单列表字段之外还包括 `payStatus` 与 `operationLogs`；日志项为 `action`、`fromStatus`、`toStatus`、`remark`、`createTime`。订单不存在返回 `3007`，无权访问返回 `3008`。

## 11. 取消订单

`POST /api/v1/orders/{id}/cancel`

### 请求体
可省略；提供时仅包含最长 200 字符的 `cancelReason`。

### 成功响应 `data`
`CancelResponse(orderId, status, refundStatus)`。待支付订单变为已取消且 `refundStatus=NO_REFUND`；待服务订单变为退款中且 `refundStatus=REFUNDING`。

## 12. 完成订单

`POST /api/v1/orders/{id}/complete`

仅待服务订单可完成。成功响应为内部订单对象：`orderId`、`orderNo`、`userId`、`status`、`serviceItemId`、`serviceItemName`、`specName`、`totalAmount`。

## 13. 发起支付

`POST /api/v1/orders/{id}/pay`

### 请求头与请求体
`Idempotent-Key` 必填，最长 128。请求体为 `{ "payChannel": "alipay" }`，当前仅支持支付宝。

### 成功响应 `data`
`PayResponse` 含 `orderId`、`orderNo`、`payChannel`、`payAmount`、`payStatus`（`READY` 或 `SUCCESS`）、`mock`、`payParams`。同一订单只能有一个支付意图。

## 14. 支付回调

`POST /api/v1/orders/pay/callback`

支付宝服务端以请求参数回调，不使用 `Result` 包装。服务验证 RSA2 签名、应用/卖家信息、金额和交易状态；成功或可安全重放时返回文本 `success`，冲突时返回 `failure`。

## 15. 查询一笔内部订单

`GET /internal/v1/orders/{id}`

### 请求参数
请求头 `X-Internal-Token` 必填；路径 `id` 为订单 ID。

### 成功响应 `data`
`OrderDTO`，字段为 `orderId`、`orderNo`、`userId`、`status`、`serviceItemId`、`serviceItemName`、`specName`、`totalAmount`。内部令牌不匹配返回 HTTP `403`、`1004`。

## 16. 批量查询内部订单

`POST /internal/v1/orders/batch`

### 请求头与请求体
请求头 `X-Internal-Token` 必填；请求体为 `Long[]`，长度必须为 1-100。

### 成功响应 `data`
`OrderDTO[]`。空数组或超过 100 项返回 HTTP `400`、`1000`。
