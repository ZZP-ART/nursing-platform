# 智慧护理平台演示数据与后端接口补充清单

> 文档日期：2026-07-17  
> 使用环境：仅 `dev/test`，禁止在生产环境保留固定密码、固定验证码或演示身份证信息。  
> 当前网关：`http://192.168.57.85`（80 端口）  
> 基础契约：[`API接口文档.md`](./API接口文档.md)  
> 本文用途：补齐前端演示所需的 Seed 数据和当前契约缺少的角色端接口。后端实现后，需要同步更新基础契约和契约测试。

---

## 1. 当前联调问题摘要

当前顾客端的分类、服务列表、服务详情、地址和订单列表已有数据，但护理人员端和商户端大量页面只能显示状态或数量，业务字段为空。

已确认的主要原因：

1. `GET /api/v1/caregivers/tasks` 只返回 `Assignment[]`，没有服务、预约、联系人、地址和订单状态。
2. 护理人员使用 `GET /api/v1/orders/{orderId}` 补查订单时返回 `403/3008`，前端无法自行补齐。
3. 当前公开契约没有商户工作台、商户订单列表、商户订单详情和派单候选人员接口。
4. 商户资料、护理人员业务档案、排班、消息、结算和经营报表接口尚未提供。
5. 后端实际仍读取旧请求头 `Idempotent-Key`，但文档和前端使用 `Idempotency-Key`。
6. 后端把 Snowflake `Long` ID 序列化为 JSON number，JavaScript 会发生精度丢失。
7. 文档中的固定密码与数据库实际密码不一致；密码登录不可用，开发短信验证码 `123456` 可用。
8. 待支付订单约 20 秒后自动取消，正常页面跳转和支付操作来不及完成。

---

## 2. 演示账号

建议后端在每次初始化开发环境时幂等写入以下账号。账号已存在时应重置密码、角色和关联关系，不重复创建。

| 场景 | 登录账号 | 建议密码 | userId | 角色 | 说明 |
|---|---|---:|---:|---|---|
| 普通顾客 | `13800138000` | `Test123456` | `10001` | `CUSTOMER` | 浏览、下单、支付、确认、评价、投诉 |
| 护理人员 | `13800138001` | `Test123456` | `10002` | `CAREGIVER` | 查看任务、接单、履约 |
| 商户负责人 | `13800138002` | `Test123456` | `10003` | `MERCHANT_MEMBER` | 工作台、订单、派单、服务与团队 |
| 平台管理员 | `admin` | `Admin123456` | `90001` | `ADMIN` | 护理申请审核、评价审核、投诉处理 |

开发环境短信规则：

- 登录、注册、重置密码固定验证码：`123456`。
- 短信发送后应在 1 秒内可用于登录，避免异步任务长时间处于 `PENDING`。
- `GET /api/v1/users/sms-code/requests/{requestId}` 应按文档保持匿名访问。

建议用户资料：

```json
[
  {
    "userId": "10001",
    "phone": "13800138000",
    "nickname": "安心顾客",
    "gender": 2,
    "roles": ["CUSTOMER"]
  },
  {
    "userId": "10002",
    "phone": "13800138001",
    "nickname": "王护理员",
    "gender": 2,
    "roles": ["CAREGIVER"]
  },
  {
    "userId": "10003",
    "phone": "13800138002",
    "nickname": "康宁护理中心",
    "gender": 0,
    "roles": ["MERCHANT_MEMBER"]
  }
]
```

---

## 3. 组织、人员和地址 Seed 数据

### 3.1 商户

```json
{
  "merchantId": "20001",
  "merchantName": "康宁居家护理中心",
  "shortName": "康宁护理",
  "contactName": "李经理",
  "contactPhone": "13800138002",
  "businessLicenseNo": "DEV-MERCHANT-20001",
  "servicePhone": "400-800-2026",
  "province": "北京市",
  "city": "北京市",
  "district": "朝阳区",
  "address": "健康路88号护理服务中心",
  "status": "ACTIVE",
  "auditStatus": "APPROVED",
  "rating": 4.9,
  "completedOrders": 328,
  "createTime": "2026-01-10T09:00:00"
}
```

商户成员关系：

```json
{
  "memberId": "21001",
  "merchantId": "20001",
  "userId": "10003",
  "position": "OWNER",
  "status": "ACTIVE",
  "permissions": ["*"]
}
```

### 3.2 护理人员业务档案

```json
{
  "caregiverId": "50002",
  "userId": "10002",
  "realName": "王芳",
  "phone": "13800138001",
  "avatar": null,
  "idCard": "110101199001011234",
  "workYears": 6,
  "introduction": "持证养老护理员，擅长术后照护、老年康复和生活护理。",
  "serviceDistrict": "北京市朝阳区、海淀区",
  "skills": ["生活照护", "术后护理", "老年康复"],
  "auditStatus": "APPROVED",
  "merchantId": "20001",
  "merchantName": "康宁居家护理中心",
  "cooperationStatus": "ACTIVE",
  "rating": 4.9,
  "completedOrders": 126,
  "punctualityRate": 98,
  "maxDailyOrders": 4,
  "status": "AVAILABLE"
}
```

排班演示数据：

```json
{
  "caregiverId": "50002",
  "enabled": true,
  "maxDailyOrders": 4,
  "serviceAreas": ["朝阳区", "海淀区"],
  "days": [
    { "date": "2026-07-17", "slots": ["MORNING", "AFTERNOON"], "onLeave": false },
    { "date": "2026-07-18", "slots": ["MORNING", "AFTERNOON", "EVENING"], "onLeave": false },
    { "date": "2026-07-19", "slots": ["AFTERNOON"], "onLeave": false }
  ]
}
```

### 3.3 顾客地址

```json
[
  {
    "addressId": "40001",
    "userId": "10001",
    "receiverName": "张女士",
    "receiverPhone": "13800138000",
    "tag": "家",
    "province": "北京市",
    "city": "北京市",
    "district": "朝阳区",
    "detailAddress": "望京花园东区8号楼1202室",
    "isDefault": 1
  },
  {
    "addressId": "40002",
    "userId": "10001",
    "receiverName": "张先生",
    "receiverPhone": "13800138000",
    "tag": "公司",
    "province": "北京市",
    "city": "北京市",
    "district": "海淀区",
    "detailAddress": "中关村健康大厦A座806室",
    "isDefault": 0
  }
]
```

注意：地址 `tag` 应按契约返回 `家|公司|学校|其他`，不要返回 `HOME`。

---

## 4. 服务目录 Seed 数据

可以继续使用现有服务 ID，确保商户、规格和状态关系完整：

| itemId | categoryId | 服务名称 | specId | 规格 | 价格 | 时长 |
|---:|---:|---|---:|---|---:|---:|
| `201` | `113` | 上门康复推拿 | `301` | 单次体验 | 198.00 | 60 |
| `201` | `113` | 上门康复推拿 | `302` | 5次套餐 | 880.00 | 60 |
| `202` | `111` | 术后康复护理 | `303` | 单次护理 | 258.00 | 90 |
| `203` | `112` | 老年康复训练 | `304` | 单次训练 | 168.00 | 45 |
| `204` | `121` | 常规体检套餐 | `306` | 基础套餐 | 99.00 | 30 |
| `206` | `131` | 老人陪护服务 | `309` | 日常陪护4小时 | 128.00 | 240 |

所有演示服务应满足：

- `merchantId = "20001"`。
- 项目和规格均为已发布、可下单状态。
- 封面图可为空，但名称、说明、价格和时长不可为空。
- 所有 `id/*Id` 在 HTTP JSON 中必须序列化为 string。

---

## 5. 订单、派单和履约 Seed 数据

以普通顾客 `10001`、商户 `20001`、护理人员 `10002` 为固定关系，准备以下状态样本。

| orderId | orderNo | 服务 | 日期/时段 | status | 场景 |
|---:|---|---|---|---:|---|
| `31001` | `DEV-PAY-31001` | 上门康复推拿 | 2026-07-18 上午 | `0` | 顾客待支付 |
| `31002` | `DEV-DISPATCH-31002` | 术后康复护理 | 2026-07-18 下午 | `1` | 商户待派单 |
| `31003` | `DEV-OFFER-31003` | 老年康复训练 | 2026-07-19 上午 | `6` | 已派单，护理人员待接单 |
| `31004` | `DEV-ACCEPT-31004` | 常规体检套餐 | 2026-07-17 下午 | `7` | 已接单，护理人员待上门 |
| `31005` | `DEV-SERVICE-31005` | 老人陪护服务 | 2026-07-17 上午 | `8` | 服务中 |
| `31006` | `DEV-CONFIRM-31006` | 上门康复推拿 | 2026-07-16 下午 | `9` | 待顾客确认 |
| `31007` | `DEV-COMPLETE-31007` | 术后康复护理 | 2026-07-15 上午 | `2` | 已完成，可评价/投诉 |
| `31008` | `DEV-CANCEL-31008` | 常规体检套餐 | 2026-07-20 上午 | `3` | 已取消 |
| `31009` | `DEV-REFUND-31009` | 上门康复推拿 | 2026-07-14 下午 | `5` | 已退款 |

订单通用字段示例：

```json
{
  "orderId": "31004",
  "orderNo": "DEV-ACCEPT-31004",
  "userId": "10001",
  "merchantId": "20001",
  "merchantName": "康宁居家护理中心",
  "serviceItemId": "204",
  "serviceItemName": "常规体检套餐",
  "serviceSpecId": "306",
  "specName": "基础套餐",
  "specPrice": 99.00,
  "totalAmount": 99.00,
  "status": 7,
  "payStatus": 1,
  "serviceDate": "2026-07-17",
  "serviceTimeSlot": "AFTERNOON",
  "receiverName": "张女士",
  "receiverPhone": "13800138000",
  "addressDetail": "北京市北京市朝阳区望京花园东区8号楼1202室",
  "remark": "老人行动不便，请提前电话联系。",
  "createTime": "2026-07-16T10:30:00"
}
```

### 5.1 派单关系

```json
[
  {
    "assignmentId": "95003",
    "orderId": "31003",
    "merchantId": "20001",
    "caregiverUserId": "10002",
    "status": 0,
    "acceptedTime": null,
    "rejectedTime": null
  },
  {
    "assignmentId": "95004",
    "orderId": "31004",
    "merchantId": "20001",
    "caregiverUserId": "10002",
    "status": 1,
    "acceptedTime": "2026-07-16T11:00:00",
    "rejectedTime": null
  },
  {
    "assignmentId": "95005",
    "orderId": "31005",
    "merchantId": "20001",
    "caregiverUserId": "10002",
    "status": 1,
    "acceptedTime": "2026-07-16T09:00:00",
    "rejectedTime": null
  },
  {
    "assignmentId": "95006",
    "orderId": "31006",
    "merchantId": "20001",
    "caregiverUserId": "10002",
    "status": 1,
    "acceptedTime": "2026-07-15T09:00:00",
    "rejectedTime": null
  },
  {
    "assignmentId": "95007",
    "orderId": "31007",
    "merchantId": "20001",
    "caregiverUserId": "10002",
    "status": 1,
    "acceptedTime": "2026-07-14T09:00:00",
    "rejectedTime": null
  }
]
```

### 5.2 履约记录

为 `31005` 准备：

```json
[
  {
    "recordId": "97001",
    "orderId": "31005",
    "action": "DEPART",
    "content": "护理人员已出发",
    "remark": "预计30分钟后到达",
    "createTime": "2026-07-17T08:00:00"
  },
  {
    "recordId": "97002",
    "orderId": "31005",
    "action": "CHECK_IN",
    "content": "护理人员已到达并签到",
    "remark": "已与家属确认身份",
    "createTime": "2026-07-17T08:30:00"
  },
  {
    "recordId": "97003",
    "orderId": "31005",
    "action": "START",
    "content": "护理服务已开始",
    "remark": "生命体征正常",
    "createTime": "2026-07-17T08:35:00"
  }
]
```

为 `31006` 和 `31007` 准备完整 `DEPART -> CHECK_IN -> START -> FINISH` 记录。

---

## 6. 管理后台展示数据

### 6.1 护理人员申请

至少准备三种状态：

```json
[
  {
    "id": "71001",
    "userId": "10002",
    "realName": "王芳",
    "phone": "13800138001",
    "serviceDistrict": "北京市朝阳区、海淀区",
    "skills": "生活照护、术后护理、老年康复",
    "status": 1,
    "reviewRemark": "资质核验通过",
    "createTime": "2026-07-10T09:00:00",
    "updateTime": "2026-07-11T10:00:00"
  },
  {
    "id": "71002",
    "userId": "10004",
    "realName": "赵敏",
    "phone": "13800138003",
    "serviceDistrict": "北京市丰台区",
    "skills": "生活照护、陪诊陪护",
    "status": 0,
    "reviewRemark": null,
    "createTime": "2026-07-16T14:30:00",
    "updateTime": "2026-07-16T14:30:00"
  },
  {
    "id": "71003",
    "userId": "10005",
    "realName": "陈丽",
    "phone": "13800138004",
    "serviceDistrict": "北京市东城区",
    "skills": "康复护理",
    "status": 2,
    "reviewRemark": "请补充有效护理员资格证明",
    "createTime": "2026-07-15T11:00:00",
    "updateTime": "2026-07-16T09:20:00"
  }
]
```

### 6.2 评价审核

至少准备待审核、已通过和已驳回各一条：

```json
[
  {
    "id": "81001",
    "orderId": "31007",
    "userId": "10001",
    "serviceItemId": "202",
    "serviceItemName": "术后康复护理",
    "specName": "单次护理",
    "rating": 5,
    "content": "护理人员专业耐心，服务过程很细致。",
    "status": 1,
    "createTime": "2026-07-16T10:00:00",
    "updateTime": "2026-07-16T10:00:00"
  }
]
```

### 6.3 投诉处理

```json
[
  {
    "id": "82001",
    "orderId": "31007",
    "userId": "10001",
    "type": 2,
    "content": "护理人员到达时间比预约时间晚了二十分钟。",
    "images": "[]",
    "status": 0,
    "createTime": "2026-07-16T15:00:00",
    "updateTime": "2026-07-16T15:00:00"
  },
  {
    "id": "82002",
    "orderId": "31006",
    "userId": "10001",
    "type": 1,
    "content": "希望平台核实服务内容是否完整。",
    "images": "[]",
    "status": 1,
    "createTime": "2026-07-15T16:00:00",
    "updateTime": "2026-07-16T09:00:00"
  }
]
```

---

## 7. P0：必须修复的现有契约问题

这些问题会直接阻断登录、下单和支付，应优先于新增页面接口处理。

### 7.1 ID 必须序列化为 string

错误：

```json
{ "orderId": 2077931204807634944 }
```

JavaScript 实际解析为 `2077931204807635000`，随后支付会访问错误订单。

正确：

```json
{ "orderId": "2077931204807634944" }
```

要求覆盖所有 `id` 和 `*Id`，包括用户、分类、服务、规格、地址、订单、商户、派单、投诉、评价和审核记录。

### 7.2 幂等请求头统一为 `Idempotency-Key`

当前后端运行版本仍读取旧的：

```http
Idempotent-Key
```

必须按文档统一读取：

```http
Idempotency-Key
```

覆盖下单、支付、地址写操作、取消、确认、评价、投诉、申请、派单、接拒单、履约和管理员处理接口。

### 7.3 修复测试账号

- 重置三个 App 账号和管理员密码。
- 确保密码哈希与登录服务使用相同算法和盐配置。
- 密码登录失败必须稳定返回标准 `{code,message,data}`。

### 7.4 延长待支付超时

当前约 20 秒自动取消，页面无法完成支付。

建议：

- 开发/测试环境：15 分钟。
- 生产环境：至少 15 分钟，具体由产品确认。

---

## 8. P0：护理人员任务接口扩展

### 8.1 扩展任务列表

当前：

```http
GET /api/v1/caregivers/tasks
```

只返回 `Assignment[]`，建议改为 `CaregiverTask[]`：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "assignmentId": "95003",
      "orderId": "31003",
      "orderNo": "DEV-OFFER-31003",
      "merchantId": "20001",
      "merchantName": "康宁居家护理中心",
      "caregiverUserId": "10002",
      "assignmentStatus": 0,
      "orderStatus": 6,
      "serviceItemId": "203",
      "serviceItemName": "老年康复训练",
      "specName": "单次训练",
      "serviceDate": "2026-07-19",
      "serviceTimeSlot": "MORNING",
      "receiverName": "张女士",
      "receiverPhone": "13800138000",
      "addressDetail": "北京市北京市朝阳区望京花园东区8号楼1202室",
      "remark": "老人行动不便，请提前联系",
      "createTime": "2026-07-16T10:30:00"
    }
  ]
}
```

### 8.2 新增任务详情

```http
GET /api/v1/caregivers/tasks/{orderId}
```

权限：仅当前有效派单对应的护理人员可查看。

详情在列表字段基础上增加：

```json
{
  "serviceRecords": [],
  "operationLogs": [],
  "availableActions": ["accept", "reject", "depart", "check-in", "start", "finish"]
}
```

后端根据当前派单状态、订单状态和履约记录计算 `availableActions`，防止前端自行猜测状态机。

---

## 9. P0：商户工作台、订单与派单接口

当前前端商户端页面已经存在，但基础契约没有对应只读接口。

### 9.1 商户工作台

```http
GET /api/v1/merchants/dashboard
```

```json
{
  "merchantId": "20001",
  "merchantName": "康宁居家护理中心",
  "waitingDispatch": 1,
  "waitingAccept": 1,
  "todayServices": 2,
  "inService": 1,
  "totalOrders": 9,
  "completed": 1,
  "monthRevenue": 2680.00,
  "exceptionCount": 0
}
```

### 9.2 商户订单列表

```http
GET /api/v1/merchants/orders?page=1&size=20&status=1&keyword=
```

返回 `Page<MerchantOrderList>`，至少包含：

```text
orderId, orderNo, orderStatus, assignmentStatus,
serviceItemName, specName, totalAmount,
serviceDate, serviceTimeSlot,
receiverName, receiverPhone, addressDetail,
currentAssignment, createTime
```

### 9.3 商户订单详情

```http
GET /api/v1/merchants/orders/{orderId}
```

在订单详情基础上增加：

```text
assignments[], currentAssignment, serviceRecords[], operationLogs[]
```

### 9.4 派单候选人员

```http
GET /api/v1/merchants/orders/{orderId}/candidates
```

```json
{
  "list": [
    {
      "caregiverUserId": "10002",
      "caregiverId": "50002",
      "name": "王芳",
      "rating": 4.9,
      "completedOrders": 126,
      "distanceKm": 2.4,
      "dailyTaskCount": 2,
      "maxDailyOrders": 4,
      "serviceAreas": ["朝阳区", "海淀区"],
      "skills": ["生活照护", "术后护理", "老年康复"],
      "eligible": true,
      "conflictReasons": []
    }
  ]
}
```

候选查询应由后端校验合作关系、审核状态、排班、服务区域、技能和每日接单上限。

### 9.5 按订单查询派单记录

```http
GET /api/v1/merchants/orders/{orderId}/assignments
```

虽然当前已有 `GET /api/v1/merchants/assignments`，但按订单查询可以减少前端全量过滤和数据泄露风险。

---

## 10. P1：护理人员和商户资料接口

### 10.1 护理人员业务档案

```http
GET /api/v1/caregiver/profile
```

返回第 3.2 节护理人员档案，用于“我的”页面显示评分、完成单量、准时率和合作商户。

### 10.2 护理人员排班

```http
GET /api/v1/caregiver/schedule
PUT /api/v1/caregiver/schedule
```

### 10.3 商户资料

```http
GET /api/v1/merchant/profile
```

### 10.4 商户团队

```http
GET  /api/v1/merchant/caregivers
GET  /api/v1/merchant/caregivers/{relationId}
POST /api/v1/merchant/caregivers/invite
POST /api/v1/merchant/caregivers/{relationId}/suspend
POST /api/v1/merchant/caregivers/{relationId}/resume

GET  /api/v1/merchant/members
POST /api/v1/merchant/members/invite
POST /api/v1/merchant/members/{memberId}/disable
POST /api/v1/merchant/members/{memberId}/enable
```

### 10.5 商户服务管理

如果商户负责创建并提交服务，需要新增：

```http
GET  /api/v1/merchants/services
POST /api/v1/merchants/services
GET  /api/v1/merchants/services/{itemId}
PUT  /api/v1/merchants/services/{itemId}
POST /api/v1/merchants/services/{itemId}/submit
POST /api/v1/merchants/services/{itemId}/publish
POST /api/v1/merchants/services/{itemId}/offline
```

如果平台管理员统一维护目录，则应隐藏商户服务编辑入口，不要同时保留两套职责模型。

---

## 11. P1：管理后台接口补充

当前契约已有管理员登录、护理申请、评价和投诉接口。为了让后台工作台不为空，建议新增：

### 11.1 管理后台概览

```http
GET /api/v1/admin/dashboard
```

```json
{
  "pendingTotal": 3,
  "pending": {
    "caregiver": 1,
    "review": 1,
    "complaint": 1
  },
  "processedToday": 5,
  "priorityQueue": []
}
```

### 11.2 管理员目录查询

当前只有目录创建、修改、发布和下架接口，后台管理列表还需要：

```http
GET /api/v1/admin/catalog/categories
GET /api/v1/admin/catalog/items
GET /api/v1/admin/catalog/items/{id}
GET /api/v1/admin/catalog/specs?serviceItemId={id}
```

### 11.3 商户申请审核（如果保留商户入驻）

```http
GET  /api/v1/admin/merchant-applications
POST /api/v1/admin/merchant-applications/{merchantId}/approve
POST /api/v1/admin/merchant-applications/{merchantId}/reject
```

---

## 12. P2：完整演示增强接口

以下功能不阻塞订单履约主闭环，可以后续实现：

### 12.1 消息中心

```http
GET   /api/v1/notifications
GET   /api/v1/notifications/unread-count
PATCH /api/v1/notifications/{id}/read
PATCH /api/v1/notifications/read-all
```

### 12.2 取消预览和售后详情

```http
GET /api/v1/orders/{orderId}/cancel-preview
GET /api/v1/orders/{orderId}/aftersales
```

### 12.3 商户投诉协同与异常工单

```http
GET  /api/v1/merchant/complaints
GET  /api/v1/merchant/complaints/{id}
POST /api/v1/merchant/complaints/{id}/resolve
POST /api/v1/merchant/complaints/{id}/escalate

GET  /api/v1/merchant/exceptions
GET  /api/v1/merchant/exceptions/{id}
POST /api/v1/merchant/exceptions/{id}/{action}
```

### 12.4 收益、结算与报表

```http
GET  /api/v1/caregiver/earnings
POST /api/v1/caregiver/withdrawals
GET  /api/v1/merchant/settlements
POST /api/v1/merchant/withdrawals
GET  /api/v1/merchant/reports/operations?range=7d|30d|90d
```

---

## 13. 接口统一要求

所有新增和修改接口必须满足：

1. 返回统一格式：`{ "code": 0, "message": "success", "data": ... }`。
2. 所有 `id` 和 `*Id` 为十进制 string。
3. 金额、状态、评分、时长和分页字段保持 JSON number。
4. 所有副作用接口使用 `Idempotency-Key`。
5. 角色权限由后端验证，前端不发送可信身份头。
6. 顾客只能查看自己的订单；商户只能查看本商户订单；护理人员只能查看有效派单关联订单。
7. 列表页不得返回身份证号等非必要敏感信息；手机号和身份证按权限脱敏。
8. Seed 脚本必须可重复执行，不产生重复用户、订单或派单关系。
9. Seed 订单不要在几十秒内自动过期，保证现场演示时间。
10. 新增接口必须同步更新 [`API接口文档.md`](./API接口文档.md) 和后端契约测试。

---

## 14. 后端交付验收清单

### P0 主闭环

- [ ] 四个演示账号可使用固定密码登录。
- [ ] 顾客能看到地址和九种订单状态样本。
- [ ] 新订单返回 string 类型 `orderId`。
- [ ] 使用 `Idempotency-Key` 可正常创建和支付订单。
- [ ] 护理人员任务卡片能显示服务、日期、联系人和地址。
- [ ] 护理人员可查看任务详情并完成接单、出发、签到、开始和结束。
- [ ] 商户工作台有统计数据。
- [ ] 商户能看到订单详情、候选人员并完成派单。
- [ ] 管理员可以登录并查看护理申请、评价和投诉待办。

### P1 展示完整度

- [ ] 护理人员“我的”页面有评分、完成单量和准时率。
- [ ] 护理人员排班页面可读取和保存。
- [ ] 商户资料、团队和服务管理页面有数据。
- [ ] 管理后台概览和目录列表有数据。

### P2 扩展能力

- [ ] 消息中心可显示各角色待办。
- [ ] 售后、异常和投诉协同可形成闭环。
- [ ] 护理人员收益、商户结算和经营报表有演示数据。

---

## 15. 推荐后端实施顺序

```text
1. 修复 ID 字符串序列化、Idempotency-Key、测试密码和支付超时
2. 幂等 Seed 四个账号、商户、护理人员、地址、服务和九种订单
3. 扩展护理人员 tasks + task detail
4. 增加商户 dashboard + order list/detail + candidates
5. 验证派单、接单、履约、顾客确认闭环
6. 补管理后台 Seed 和 dashboard
7. 再实现资料、团队、排班、消息、售后、结算和报表
```
