# feedback-service 接口说明

> 基于当前 `nursing-feedback-service` 实现整理，覆盖评价、投诉与投诉进度查询接口。

## 1. 服务约定

| 项目 | 说明 |
| --- | --- |
| 服务名 | `nursing-feedback-service` |
| 默认端口 | `8084` |
| 网关路由 | `/api/v1/reviews/**`、`/api/v1/complaints/**` |
| Content-Type | `application/json` |
| 成功响应 | `{ "code": 0, "message": "success", "data": ... }` |

所有公开接口均需要可信网关注入：

| Header | 必填 | 说明 |
| --- | --- | --- |
| `X-Gateway-Token` | 是 | 必须与服务端配置匹配 |
| `X-User-Id` | 是 | 登录用户 ID |

可信网关令牌不匹配时返回 HTTP `403`、`code=1004`；用户身份缺失或不能转换为数字时返回 `code=1002`。所有接口实际都校验登录身份，包括公开评价列表接口。

### 1.1 统一分页响应

评价与投诉列表的 `data` 格式：

```json
{ "list": [], "total": 0, "page": 1, "size": 20 }
```

服务会将 `page` 小于 1 修正为 1，并将 `size` 限制在 1-100；不会因超出区间返回参数错误。

## 2. 提交评价

### `POST /api/v1/reviews`

请求头 `Idempotent-Key` 必填，去除首尾空白后长度不得超过 128。同一用户以相同幂等键提交相同内容会回放原 `reviewId`；用于不同内容或请求仍在处理时返回 HTTP `409`、`code=1006`。

请求体：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `orderId` | number | 是 | 非空；业务上必须为当前用户已完成订单 |
| `rating` | integer | 是 | `1-5` |
| `content` | string | 是 | 非空白，最多 500 字符 |
| `images` | string[] | 否 | 最多 6 项；空白项会被忽略 |

```json
{
  "orderId": 123,
  "rating": 5,
  "content": "服务专业且准时。",
  "images": ["/uploads/reviews/1.jpg"]
}
```

成功响应：

```json
{ "code": 0, "message": "评价提交成功", "data": { "reviewId": 456 } }
```

仅订单归属当前用户、状态为已完成，且反馈服务已收到该订单支付事件并建立评价资格时可提交。订单已评价返回 `code=4003`；订单状态或评价资格不满足返回 `code=4002`；订单不存在返回 `code=1005`；订单服务不可用返回 HTTP `503`、`code=1009`。

## 3. 查询项目评价

### `GET /api/v1/reviews`

| 参数 | 类型 | 默认值 | 规则 |
| --- | --- | --- | --- |
| `itemId` | number | - | 必填且大于 0 |
| `page` | integer | `1` | 小于 1 时按 1 查询 |
| `size` | integer | `20` | 限制为 `1-100` |

返回指定服务项目中符合当前发布查询条件的评价。单项字段：

| 字段 | 说明 |
| --- | --- |
| `reviewId`、`orderId`、`serviceItemId` | 评价、订单和项目 ID |
| `rating`、`content`、`images` | 用户提交的评价内容 |
| `userNickname` | 由用户 ID 派生的脱敏显示名，如 `user1234` |
| `serviceItemName`、`specName` | 订单快照中的服务与规格名称 |
| `createTime` | 创建时间 |

`itemId` 缺失或小于等于 0 返回 HTTP `400`、`code=1000`。

## 4. 提交投诉

### `POST /api/v1/complaints`

请求头 `Idempotent-Key` 必填，最长 64 字符。相同用户相同键且请求指纹一致时回放原投诉 ID；同键不同内容返回 HTTP `409`、`code=1006`。

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `orderId` | number | 是 | 必须为当前用户订单 |
| `type` | integer | 是 | `1-4` |
| `content` | string | 是 | 非空白，最多 1000 字符 |
| `images` | string[] | 否 | 最多 6 项；持久化为 JSON 数组 |

投诉类型：`1=service_quality`、`2=service_attitude`、`3=overcharging`、`4=other`。

订单必须为待服务或已完成状态。成功响应：

```json
{ "code": 0, "message": "投诉提交成功，我们将在24小时内处理", "data": { "complaintId": 789 } }
```

## 5. 查询我的投诉

### `GET /api/v1/complaints`

| 参数 | 类型 | 默认值 | 规则 |
| --- | --- | --- | --- |
| `page` | integer | `1` | 小于 1 时按 1 查询 |
| `size` | integer | `20` | 限制为 `1-100` |

仅返回当前用户投诉。单项字段为 `complaintId`、`orderId`、`type`、`typeText`、`content`、`images`、`status`、`statusText`、`createTime`。

投诉状态：`0=pending`、`1=processing`、`2=resolved`、`3=closed`。

## 6. 查询投诉进度

### `GET /api/v1/complaints/{id}/tracks`

返回投诉当前状态与轨迹：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "complaintId": 789,
    "status": 0,
    "statusText": "pending",
    "tracks": [
      { "trackId": 1, "operator": "system", "content": "Complaint received", "createTime": "2026-07-14T10:00:00" }
    ]
  }
}
```

`id` 必须大于 0。投诉不存在返回 `code=4006`；不属于当前用户返回 `code=4007`。
