# 智慧护理平台后端联调测试假数据

本文档根据 App 当前实际调用的接口、字段和 Mock 数据整理，供后端初始化测试库和联调使用。

## 1. 联调约定

- Base URL：`http://localhost:8080`
- 所有接口统一返回：`{ "code": 0, "message": "success", "data": {} }`
- 需要登录的接口携带：`Authorization: Bearer <token>`
- 下单、支付、评价、投诉需要请求头：`Idempotent-Key: <key>`
- 发送短信验证码当前客户端使用请求头：`Idempotency-Key: <UUID>`（注意与其他接口的单数写法不同）
- 金额使用数字，单位为元，例如 `80`、`216`
- 时间使用 ISO 8601，例如 `2026-07-18T10:00:00+08:00`
- 服务日期格式：`YYYY-MM-DD`

> 地址编辑：App 当前实际调用 `PATCH /api/v1/addresses/{addressId}`，不是 `PUT`。后端应优先支持 PATCH，或同时兼容 PUT。

## 2. 测试账号

### 2.1 已有账号

| userId | 手机号 | 密码 | 昵称 | 状态 | 身份证号 |
|---:|---|---|---|---:|---|
| 10001 | 13800138000 | 123456 | 测试用户 | 0 正常 | 110101199001011234 |

登录验证码固定使用 `123456`。短信验证码接口仍需校验手机号格式和幂等键；测试环境可直接返回验证码已受理，不建议在正式环境返回明文验证码。

### 2.2 注册测试账号

```json
{
  "phone": "13900139001",
  "smsCode": "123456",
  "password": "Test123456",
  "nickname": "新注册用户"
}
```

密码规则：8-32 位，必须同时包含字母和数字。

## 3. 服务分类数据

```json
[
  { "categoryId": 1, "name": "专业护理", "icon": "https://cdn.nursing.com/icons/category/nurse.png", "sortOrder": 1, "status": 1 },
  { "categoryId": 2, "name": "康复理疗", "icon": "https://cdn.nursing.com/icons/category/rehab.png", "sortOrder": 2, "status": 1 },
  { "categoryId": 3, "name": "中医养生", "icon": "https://cdn.nursing.com/icons/category/tcm.png", "sortOrder": 3, "status": 1 },
  { "categoryId": 4, "name": "专项护理", "icon": "https://cdn.nursing.com/icons/category/special.png", "sortOrder": 4, "status": 1 },
  { "categoryId": 5, "name": "心理慰藉", "icon": "https://cdn.nursing.com/icons/category/psych.png", "sortOrder": 5, "status": 1 }
]
```

分类默认 `parentId = 0`，只返回 `status = 1` 的分类。

## 4. 服务项目和规格数据

服务项目共 12 个，全部设置为 `status = 1`。每个项目配置 3 个有效规格：单次服务、3 次套餐、5 次套餐。规格 `duration` 均为 60 分钟，规格状态为 `status = 1`。

| itemId | categoryId | 服务名称 | minPrice | specId / 规格 / price / originalPrice |
|---:|---:|---|---:|---|
| 101 | 1 | 上门输液护理 | 150 | `10101` 单次/150/180；`10102` 3次/405/480；`10103` 5次/645/780 |
| 102 | 1 | 压疮护理 | 180 | `10201` 单次/180/216；`10202` 3次/486/576；`10203` 5次/774/936 |
| 103 | 1 | 日常起居照料 | 120 | `10301` 单次/120/144；`10302` 3次/324/384；`10303` 5次/516/624 |
| 201 | 2 | 康复运动指导 | 220 | `20101` 单次/220/264；`20102` 3次/594/704；`20103` 5次/946/1144 |
| 202 | 2 | 术后康复护理 | 300 | `20201` 单次/300/360；`20202` 3次/810/960；`20203` 5次/1290/1560 |
| 203 | 2 | 推拿按摩 | 168 | `20301` 单次/168/202；`20302` 3次/454/538；`20303` 5次/722/874 |
| 301 | 3 | 艾灸调理 | 128 | `30101` 单次/128/154；`30102` 3次/346/410；`30103` 5次/550/666 |
| 302 | 3 | 拔罐刮痧 | 98 | `30201` 单次/98/118；`30202` 3次/265/314；`30203` 5次/421/510 |
| 401 | 4 | 静脉采血 | 80 | `40101` 单次/80/96；`40102` 3次/216/256；`40103` 5次/344/416 |
| 402 | 4 | PICC维护 | 260 | `40201` 单次/260/312；`40202` 3次/702/832；`40203` 5次/1118/1352 |
| 403 | 4 | 鼻饲护理 | 160 | `40301` 单次/160/192；`40302` 3次/432/512；`40303` 5次/688/832 |
| 501 | 5 | 心理陪伴聊天 | 150 | `50101` 单次/150/180；`50102` 3次/405/480；`50103` 5次/645/780 |

### 4.1 服务项目基础字段示例

后端每条服务项目至少应能返回以下字段：

```json
{
  "itemId": 401,
  "categoryId": 4,
  "categoryName": "专项护理",
  "name": "静脉采血",
  "description": "专业护士上门进行静脉采血，规范操作、安全无菌，免去老人往返医院奔波。",
  "coverImage": "https://cdn.nursing.com/items/401.jpg",
  "images": [],
  "status": 1,
  "sortOrder": 1,
  "minPrice": 80,
  "createTime": "2026-07-01T10:00:00+08:00",
  "specs": [
    { "specId": 40101, "serviceItemId": 401, "name": "单次服务", "price": 80, "originalPrice": 96, "duration": 60, "status": 1 },
    { "specId": 40102, "serviceItemId": 401, "name": "3次套餐", "price": 216, "originalPrice": 256, "duration": 60, "status": 1 },
    { "specId": 40103, "serviceItemId": 401, "name": "5次套餐", "price": 344, "originalPrice": 416, "duration": 60, "status": 1 }
  ]
}
```

服务列表和详情接口建议统一返回 `itemId`、`specId`。当前 App 也兼容列表接口返回的旧式 `id`，但不要同时返回互相矛盾的 ID。

### 4.2 推荐的服务描述

以下描述用于详情页 Markdown 展示和搜索测试：

| itemId | description |
|---:|---|
| 101 | 由持证执业护士上门，为您提供专业的静脉输液护理服务。包含核对医嘱与药品、评估患者状况、规范输液操作、观察反应并记录。 |
| 102 | 针对长期卧床老人的压疮预防与护理，包括定期翻身、皮肤清洁、敷药换药、压疮风险评估和营养支持建议。 |
| 103 | 协助老人完成起床、洗漱、穿衣、饮食等日常生活起居，确保生活舒适整洁。 |
| 201 | 专业康复师根据老人身体状况制定运动方案，一对一指导康复训练。 |
| 202 | 针对术后恢复期老人的全方位护理，包括切口护理、功能锻炼、营养指导。 |
| 203 | 专业推拿师上门提供全身或局部推拿按摩，缓解肌肉酸痛、改善血液循环。 |
| 301 | 传统艾灸疗法，温经通络，调理脏腑，适用于老年人常见慢性病辅助治疗。 |
| 302 | 中医传统疗法拔罐与刮痧，祛风散寒、活血通络，缓解各种酸痛不适。 |
| 401 | 专业护士上门进行静脉采血，规范操作、安全无菌，免去老人往返医院奔波。 |
| 402 | 专业护士对 PICC 置管进行定期维护护理，包括冲管、更换敷料、观察评估。 |
| 403 | 为需要鼻饲的老人提供专业鼻饲操作及管道维护，确保营养安全摄入。 |
| 501 | 专业心理咨询师上门陪伴聊天，缓解老人孤独感、焦虑情绪，提供心理支持。 |

## 5. 地址数据

```json
[
  {
    "addressId": 5001,
    "userId": 10001,
    "receiverName": "张三",
    "receiverPhone": "13800138000",
    "tag": "家",
    "province": "北京市",
    "city": "北京市",
    "district": "朝阳区",
    "detailAddress": "建国路88号6栋301",
    "isDefault": 1,
    "isDeleted": 0
  },
  {
    "addressId": 5002,
    "userId": 10001,
    "receiverName": "张三",
    "receiverPhone": "13800138000",
    "tag": "公司",
    "province": "北京市",
    "city": "北京市",
    "district": "海淀区",
    "detailAddress": "中关村大街1号5层",
    "isDefault": 0,
    "isDeleted": 0
  },
  {
    "addressId": 5003,
    "userId": 10001,
    "receiverName": "李四",
    "receiverPhone": "13900139000",
    "tag": "其他",
    "province": "上海市",
    "city": "上海市",
    "district": "浦东新区",
    "detailAddress": "张江高科技园区碧波路690号",
    "isDefault": 0,
    "isDeleted": 0
  }
]
```

地址列表应只返回当前用户且 `isDeleted != 1` 的记录，默认地址优先返回。

新增地址请求示例：

```json
{
  "receiverName": "王五",
  "receiverPhone": "13700137000",
  "tag": "家",
  "province": "广东省",
  "city": "深圳市",
  "district": "南山区",
  "detailAddress": "科技南十二路8号",
  "isDefault": 0
}
```

## 6. 订单数据

订单状态：`0 待支付`、`1 待服务`、`2 已完成`、`3 已取消`、`4 退款中`、`5 已退款`。

请至少初始化以下两个订单。它们用于覆盖支付、确认完成、评价和投诉流程：

| orderId | orderNo | item/spec | addressId | serviceDate | timeSlot | status | 用途 |
|---:|---|---|---:|---|---|---:|---|
| 20000 | 2026070120000 | 401 / 40101 | 5001 | 2026-07-02 | MORNING | 2 | 已完成；已有评价和投诉详情 |
| 19999 | 2026070119999 | 301 / 30103 | 5001 | 2026-07-18 | AFTERNOON | 1 | 待服务；可确认完成、提交投诉 |

订单 20000：

```json
{
  "orderId": 20000,
  "orderNo": "2026070120000",
  "userId": 10001,
  "serviceItemId": 401,
  "serviceSpecId": 40101,
  "serviceItemName": "静脉采血",
  "serviceCoverImage": "https://cdn.nursing.com/items/401.jpg",
  "specName": "单次服务",
  "specPrice": 80,
  "totalAmount": 80,
  "status": 2,
  "addressId": 5001,
  "receiverName": "张三",
  "receiverPhone": "13800138000",
  "addressDetail": "北京市北京市朝阳区建国路88号6栋301",
  "serviceDate": "2026-07-02",
  "serviceTimeSlot": "MORNING",
  "remark": "老人需要协助，请提前电话联系。",
  "createTime": "2026-07-01T10:00:00+08:00",
  "operationLogs": [
    { "action": "create", "fromStatus": null, "toStatus": 0, "remark": "用户创建订单", "createTime": "2026-07-01T10:00:00+08:00" },
    { "action": "pay", "fromStatus": 0, "toStatus": 1, "remark": "支付宝支付成功", "createTime": "2026-07-01T10:05:00+08:00" },
    { "action": "complete", "fromStatus": 1, "toStatus": 2, "remark": "服务完成", "createTime": "2026-07-02T10:00:00+08:00" }
  ]
}
```

订单 19999：

```json
{
  "orderId": 19999,
  "orderNo": "2026070119999",
  "userId": 10001,
  "serviceItemId": 301,
  "serviceSpecId": 30103,
  "serviceItemName": "艾灸调理",
  "serviceCoverImage": "https://cdn.nursing.com/items/301.jpg",
  "specName": "5次套餐",
  "specPrice": 550,
  "totalAmount": 550,
  "status": 1,
  "addressId": 5001,
  "receiverName": "张三",
  "receiverPhone": "13800138000",
  "addressDetail": "北京市北京市朝阳区建国路88号6栋301",
  "serviceDate": "2026-07-18",
  "serviceTimeSlot": "AFTERNOON",
  "remark": "首次服务，请提前电话确认。",
  "createTime": "2026-07-01T15:00:00+08:00",
  "operationLogs": [
    { "action": "create", "fromStatus": null, "toStatus": 0, "remark": "用户创建订单", "createTime": "2026-07-01T15:00:00+08:00" },
    { "action": "pay", "fromStatus": 0, "toStatus": 1, "remark": "支付宝支付成功", "createTime": "2026-07-01T15:03:00+08:00" }
  ]
}
```

建议额外增加以下订单，用于状态筛选和取消测试：

```json
[
  { "orderId": 19998, "userId": 10001, "orderNo": "2026070119998", "serviceItemId": 101, "serviceSpecId": 10101, "status": 0, "totalAmount": 150, "serviceDate": "2026-07-20", "serviceTimeSlot": "EVENING", "addressId": 5002 },
  { "orderId": 19997, "userId": 10001, "orderNo": "2026063019997", "serviceItemId": 201, "serviceSpecId": 20101, "status": 3, "totalAmount": 220, "serviceDate": "2026-07-01", "serviceTimeSlot": "MORNING", "addressId": 5001 },
  { "orderId": 19996, "userId": 10001, "orderNo": "2026062919996", "serviceItemId": 102, "serviceSpecId": 10201, "status": 4, "totalAmount": 180, "serviceDate": "2026-06-30", "serviceTimeSlot": "AFTERNOON", "addressId": 5001 },
  { "orderId": 19995, "userId": 10001, "orderNo": "2026062819995", "serviceItemId": 501, "serviceSpecId": 50101, "status": 5, "totalAmount": 150, "serviceDate": "2026-06-28", "serviceTimeSlot": "MORNING", "addressId": 5001 }
]
```

## 7. 评价数据

评价列表接口按 `itemId` 查询，只有 `status = 2` 的评价展示给用户。建议初始化以下数据：

```json
[
  { "reviewId": 30001, "orderId": 20000, "itemId": 401, "userId": 10001, "rating": 5, "content": "非常满意！护士技术娴熟，态度也很好，妈妈完全没感到疼。", "userNickname": "张阿姨", "status": 2, "createTime": "2026-07-02T15:00:00+08:00" },
  { "reviewId": 30002, "orderId": 20000, "itemId": 401, "userId": 10002, "rating": 4, "content": "整体不错，就是预约时间稍微延迟了一些，希望能改进。", "userNickname": "李叔叔", "status": 2, "createTime": "2026-07-02T16:00:00+08:00" },
  { "reviewId": 30003, "orderId": 20000, "itemId": 401, "userId": 10003, "rating": 5, "content": "已经是老客户了，平台的服务一直很稳定，会继续推荐给朋友。", "userNickname": "王女士", "status": 2, "createTime": "2026-07-03T09:00:00+08:00" },
  { "reviewId": 30004, "orderId": 20000, "itemId": 101, "userId": 10001, "rating": 5, "content": "护士非常专业，上门输液操作规范，还耐心教了日常注意事项。", "userNickname": "张阿姨", "status": 2, "createTime": "2026-07-01T11:00:00+08:00" },
  { "reviewId": 30005, "orderId": 20000, "itemId": 101, "userId": 10004, "rating": 3, "content": "服务本身不错，但价格偏高了，希望多一些优惠活动。", "userNickname": "赵先生", "status": 2, "createTime": "2026-07-01T14:00:00+08:00" },
  { "reviewId": 30006, "orderId": 20000, "itemId": 301, "userId": 10005, "rating": 5, "content": "艾灸师傅手法很好，老伴的关节疼缓解了不少，会继续坚持治疗。", "userNickname": "刘奶奶", "status": 2, "createTime": "2026-07-04T10:00:00+08:00" },
  { "reviewId": 30007, "orderId": 20000, "itemId": 201, "userId": 10006, "rating": 4, "content": "康复师经验丰富，给了很多实用的康复建议，已经能看到效果了。", "userNickname": "陈女士", "status": 2, "createTime": "2026-07-03T16:00:00+08:00" },
  { "reviewId": 30008, "orderId": 20000, "itemId": 501, "userId": 10007, "rating": 5, "content": "陪聊的小伙子很贴心，我爸跟他聊得特别开心，心情好了很多。", "userNickname": "孙阿姨", "status": 2, "createTime": "2026-07-05T09:00:00+08:00" }
]
```

提交评价请求示例：

```json
{
  "orderId": 20000,
  "rating": 5,
  "content": "护理人员很专业，服务态度很好。",
  "images": ["https://cdn.nursing.com/2026/07/review-001.jpg"]
}
```

评价规则：订单必须为 `status = 2`，同一订单只能评价一次，`rating` 范围为 1-5。

## 8. 投诉数据

投诉类型：`1 服务质量`、`2 服务态度`、`3 乱收费`、`4 其他`。

投诉状态：`0 待处理`、`1 处理中`、`2 已处理`、`3 已关闭`。

```json
[
  {
    "complaintId": 40001,
    "orderId": 20000,
    "userId": 10001,
    "type": 1,
    "typeText": "服务质量",
    "status": 2,
    "statusText": "已处理",
    "content": "护士上门时间比预约晚了将近一小时，而且操作不太规范。",
    "images": [],
    "createTime": "2026-07-04T14:00:00+08:00",
    "tracks": [
      { "trackId": 1, "operator": "客服小王", "content": "已收到投诉，正在核实情况", "createTime": "2026-07-04T14:30:00+08:00" },
      { "trackId": 2, "operator": "客服小王", "content": "已联系护士核实，确认迟到属实，向您致歉。已安排补偿方案", "createTime": "2026-07-04T16:00:00+08:00" }
    ]
  },
  {
    "complaintId": 40002,
    "orderId": 20000,
    "userId": 10001,
    "type": 3,
    "typeText": "乱收费",
    "status": 1,
    "statusText": "处理中",
    "content": "预约时说好150元一次，但上门后要求额外收取材料费，未提前告知。",
    "images": [],
    "createTime": "2026-07-06T10:00:00+08:00",
    "tracks": [
      { "trackId": 1, "operator": "客服小李", "content": "投诉已受理，正在核实收费明细", "createTime": "2026-07-06T10:30:00+08:00" }
    ]
  }
]
```

提交投诉请求示例：

```json
{
  "orderId": 19999,
  "type": 2,
  "content": "护理人员服务态度需要改进，希望平台协助核实。",
  "images": ["https://cdn.nursing.com/2026/07/complaint-001.jpg"]
}
```

投诉允许提交的订单状态为 `1 待服务` 或 `2 已完成`。订单 19999 可用于新建投诉测试；订单 20000 可用于查看既有投诉和处理轨迹。

## 9. 文件上传测试数据

接口：`POST /api/v1/files/upload`，使用 `multipart/form-data`，文件字段名为 `file`。

| 场景 | bizType | 建议限制 |
|---|---|---|
| 头像 | `avatar` | jpg/png，建议不超过 5 MB |
| 评价图片 | `review_image` | jpg/png，最多 6 张 |
| 投诉截图 | `complaint_image` | jpg/png，最多 6 张 |

成功返回示例：

```json
{
  "code": 0,
  "message": "上传成功",
  "data": {
    "fileUrl": "https://cdn.nursing.com/2026/07/review-001.jpg",
    "fileName": "review-001.jpg",
    "fileSize": 102400,
    "bizType": "review_image"
  }
}
```

## 10. 关键接口联调样例

### 10.1 登录

```http
POST /api/v1/users/login
Content-Type: application/json

{
  "phone": "13800138000",
  "loginMode": "password",
  "password": "123456"
}
```

验证码登录：

```json
{
  "phone": "13800138000",
  "loginMode": "sms",
  "smsCode": "123456"
}
```

登录成功 `data` 至少返回 `token`、`expireTime`、`user`，其中 `user` 至少包含 `userId`、`phone`、`nickname`、`avatar`、`gender`、`status`、`version`。

### 10.2 获取服务列表

```http
GET /api/v1/items?categoryId=4&size=20
Authorization: Bearer <token>
```

响应数据：

```json
{
  "list": [],
  "size": 20,
  "hasNext": false,
  "nextCursor": null
}
```

搜索使用同一接口：`GET /api/v1/items?keyword=采血&size=20`。分页使用 `cursor` 和 `nextCursor`，不要只实现 `page` 分页。

### 10.3 下单、支付

下单是三步流程：

```http
POST /api/v1/orders/prepay-token
Authorization: Bearer <token>
```

```http
POST /api/v1/orders
Authorization: Bearer <token>
Idempotent-Key: <prepayToken>
Content-Type: application/json

{
  "serviceItemId": 401,
  "serviceSpecId": 40101,
  "addressId": 5001,
  "serviceDate": "2026-07-20",
  "serviceTimeSlot": "MORNING",
  "remark": "请提前电话联系。"
}
```

```http
POST /api/v1/orders/{orderId}/pay
Authorization: Bearer <token>
Idempotent-Key: <payment-key>
Content-Type: application/json

{ "payChannel": "alipay" }
```

支付成功响应至少包含：`orderId`、`orderNo`、`payAmount`、`payStatus`、`payChannel`、`payParams`、`tradeNo`。Mock 环境返回 `payStatus = SUCCESS`、`mock = true`。

### 10.4 订单状态操作

```http
POST /api/v1/orders/{orderId}/complete
Authorization: Bearer <token>
```

```http
POST /api/v1/orders/{orderId}/cancel
Authorization: Bearer <token>
Idempotent-Key: <cancel-key>
Content-Type: application/json

{ "cancelReason": "用户主动取消" }
```

状态转换建议：`0 -> 1` 支付成功，`1 -> 2` 确认完成，`0 -> 3` 未支付取消，`1 -> 4` 已支付取消并退款中，`4 -> 5` 退款完成。

## 11. 必测异常场景

后端至少准备或支持以下校验，便于前端联调：

| 场景 | 期望错误码 |
|---|---:|
| 未登录或 Token 失效 | HTTP 401 或 `1002/1003` |
| 手机号未注册 | `2004` |
| 密码错误 | `2010` |
| 验证码错误/过期 | `2006/2007` |
| 缺少幂等键或重复提交 | `1000/1006` |
| 服务不存在或已下架 | `3003` |
| 规格不存在或已下架 | `3004` |
| 价格发生变化 | `3005` |
| 地址不存在、已删除或无权限 | `3006/3013` |
| 订单不存在 | `3007` |
| 当前状态不可支付/取消 | `3010/3009` |
| 待服务订单提交评价 | `4002` |
| 同一订单重复评价 | `4003` |
| 投诉记录不存在 | `4006` |

## 12. 后端交付前检查清单

- [ ] 初始化 `userId=10001`、地址 `5001-5003`、服务 `101-501`、规格和订单 `19995-20000`
- [ ] 确认订单、评价、投诉均按 `userId` 做数据隔离，不能只按 ID 查询
- [ ] 服务列表支持 `categoryId`、`keyword`、`size`、`cursor`
- [ ] 服务详情和列表返回稳定的 `itemId/specId` 字段
- [ ] 地址编辑支持 App 当前使用的 PATCH 方法
- [ ] 下单时重新校验服务、规格、地址和当前价格，不信任客户端金额
- [ ] 幂等键重复请求返回同一业务结果，不重复创建订单、评价或投诉
- [ ] 订单状态变更写入 `operationLogs` 或等价的订单状态流水
- [ ] 评价列表只返回审核通过的数据，并支持按 `itemId` 分页
- [ ] 投诉列表和处理记录可按 `complaintId` 查询，图片字段返回 URL 数组
- [ ] 所有成功和失败响应保持统一 `code/message/data` 包装格式

