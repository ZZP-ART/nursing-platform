# 智慧护理平台核心版后端调整说明

> 交付对象：后端开发  
> 编写日期：2026-07-17  
> 联调环境：`http://192.168.57.85`（80 端口）  
> 依据文档：`docs/API接口文档.md`，如有冲突，以双方确认后的最新版接口文档为准。

## 1. 调整背景

当前后端已经优先修复 P0 主流程，但完整规划中的部分扩展功能无法在本期完成。为了保证项目能够稳定演示和验收，本期缩减为以下四个核心角色闭环：

1. 顾客浏览服务、下单、支付、查看订单、确认完成、评价和投诉。
2. 商户提交服务供平台审核，查看订单并完成必要派单，查看并回复顾客投诉。
3. 护理人员接收任务并完成接单、出发、签到、开始和结束服务。
4. 管理员审核护理人员和商户服务，处理投诉最终结果。

本说明只列出后端需要保留、确认或新增的最小能力。本期不再要求实现完整经营系统。

---

## 2. 本期最终业务边界

### 2.1 顾客端

保留：

- 注册、登录、退出和身份切换；
- 服务分类、服务列表、搜索和服务详情；
- 地址增删改查和默认地址；
- 创建订单、支付、订单列表、订单详情、取消和确认完成；
- 提交评价、查看服务评价；
- 提交投诉、查看投诉记录和处理进度。

### 2.2 护理人员端

保留：

- 护理人员申请和管理员审核；
- 任务列表和任务详情；
- 接单、拒单；
- 出发、签到、开始服务、结束服务。

本期不做：排班、收益、佣金和提现。

### 2.3 商户端

主要保留：

- 简化工作台；
- 服务列表、新建服务、编辑草稿、提交审核、查看审核结果；
- 顾客投诉列表、投诉详情、填写商户回复；
- 商户账号基本信息、切换身份和退出登录。

为保持订单闭环，继续保留 P0 已完成的：

- 商户订单列表和详情；
- 派单和改派。

本期不做：商户入驻申请、团队管理、商户成员岗位、异常工单、消息中心、经营结算、提现和经营报表。

### 2.4 管理后台

保留：

- 管理员登录；
- 护理人员申请审核；
- 商户服务审核；
- 投诉最终处理；
- 评价审核可保留现有能力，但不作为本次新增重点。

本期不做：商户入驻审核、异常监管、平台消息、复杂经营仪表盘和财务功能。

---

## 3. P0 已有接口：请继续保留并确认可用

以下能力不要求重新设计，但必须在缩减前端后继续可用。

### 3.1 公共和顾客主流程

- `POST /api/v1/users/login`
- `POST /api/v1/users/logout`
- `GET /api/v1/users/profile`
- `POST /api/v1/auth/switch-role`
- `POST /api/v1/files/upload`
- `GET /api/v1/categories`
- `GET /api/v1/items`
- `GET /api/v1/items/{id}`
- 地址相关接口；
- 下单令牌、创建订单、支付、订单列表、订单详情、取消、确认完成；
- 顾客评价和投诉相关接口。

### 3.2 护理人员履约

- `GET /api/v1/caregivers/tasks`
- `GET /api/v1/caregivers/tasks/{orderId}`
- `POST /api/v1/caregivers/assignments/{assignmentId}/accept`
- `POST /api/v1/caregivers/assignments/{assignmentId}/reject`
- `POST /api/v1/caregivers/orders/{orderId}/{action}`

其中 `action` 为：`depart`、`check-in`、`start`、`finish`。

### 3.3 商户订单和派单

- `GET /api/v1/merchants/dashboard`
- `GET /api/v1/merchants/orders`
- `GET /api/v1/merchants/orders/{orderId}`
- `GET /api/v1/merchants/orders/{orderId}/candidates`
- `POST /api/v1/merchants/orders/{orderId}/dispatch`
- `POST /api/v1/merchants/orders/{orderId}/redispatch`

如果上述 P0 接口的实际路径或字段与这里不同，请后端在联调前更新 `API接口文档.md`，不要仅通过口头说明修改。

---

## 4. 必须新增一：商户服务提交与管理员审核

### 4.1 业务简化规则

本期采用以下职责：

- 商户负责新建、编辑并提交服务；
- 管理员负责通过或驳回；
- 管理员审核通过后，服务自动上架到顾客端；
- 商户本期不能自行上架或下架；
- 商户只能修改 `DRAFT` 或 `REJECTED` 状态的服务；
- `PENDING_REVIEW` 和 `PUBLISHED` 状态不可编辑；
- 被驳回的服务修改后可以重新提交。

这样不需要实现商户发布、下架和多版本发布系统。

### 4.2 状态定义

建议统一为字符串枚举：

| 状态 | 含义 | 商户可编辑 | 顾客端可见 |
|---|---|---:|---:|
| `DRAFT` | 草稿 | 是 | 否 |
| `PENDING_REVIEW` | 待管理员审核 | 否 | 否 |
| `REJECTED` | 审核驳回 | 是 | 否 |
| `PUBLISHED` | 审核通过并已上架 | 否 | 是 |

### 4.3 商户接口

#### 4.3.1 查询商户自己的服务

```http
GET /api/v1/merchants/services?status=&page=1&size=20
Authorization: Bearer <merchant-token>
```

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "itemId": "501",
        "name": "居家生活照护",
        "categoryId": "101",
        "categoryName": "生活照护",
        "coverImage": "https://example.com/service.jpg",
        "description": "提供居家日常生活照护服务",
        "status": "PENDING_REVIEW",
        "rejectReason": null,
        "minPrice": 120.00,
        "specs": [
          {
            "specId": "601",
            "name": "2小时服务",
            "price": 120.00,
            "originalPrice": 150.00,
            "duration": 120
          }
        ],
        "createTime": "2026-07-17T10:00:00+08:00",
        "updateTime": "2026-07-17T10:10:00+08:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

#### 4.3.2 新建服务草稿

```http
POST /api/v1/merchants/services
Authorization: Bearer <merchant-token>
Idempotency-Key: <unique-key>
Content-Type: application/json
```

请求体：

```json
{
  "name": "居家生活照护",
  "categoryId": "101",
  "coverImage": "https://example.com/service.jpg",
  "description": "提供居家日常生活照护服务",
  "specs": [
    {
      "name": "2小时服务",
      "price": 120.00,
      "originalPrice": 150.00,
      "duration": 120
    }
  ]
}
```

成功 `data` 返回完整服务对象，初始状态必须为 `DRAFT`。

#### 4.3.3 查询服务详情

```http
GET /api/v1/merchants/services/{itemId}
Authorization: Bearer <merchant-token>
```

返回完整服务对象，并包含最近一次 `rejectReason`。商户只能查看本商户创建的服务。

#### 4.3.4 编辑服务草稿

```http
PUT /api/v1/merchants/services/{itemId}
Authorization: Bearer <merchant-token>
Idempotency-Key: <unique-key>
Content-Type: application/json
```

请求体与创建接口一致。已有规格可额外提交 `specId`；没有 `specId` 的规格按新增处理；本次请求未包含的旧规格可逻辑删除。

只允许编辑 `DRAFT` 或 `REJECTED`。编辑 `REJECTED` 服务后，状态可以保持 `REJECTED`，提交审核时再进入 `PENDING_REVIEW`。

#### 4.3.5 提交管理员审核

```http
POST /api/v1/merchants/services/{itemId}/submit
Authorization: Bearer <merchant-token>
Idempotency-Key: <unique-key>
```

无请求体。成功后状态变为 `PENDING_REVIEW`，返回完整服务对象。

### 4.4 管理员服务审核接口

#### 4.4.1 审核列表

```http
GET /api/v1/admin/merchant-services?status=PENDING_REVIEW&page=1&size=20
Authorization: Bearer <admin-token>
```

返回分页服务列表，至少包含服务基础信息、规格、商户名称、提交时间和审核状态。

#### 4.4.2 审核详情

```http
GET /api/v1/admin/merchant-services/{itemId}
Authorization: Bearer <admin-token>
```

#### 4.4.3 审核通过

```http
POST /api/v1/admin/merchant-services/{itemId}/approve
Authorization: Bearer <admin-token>
Idempotency-Key: <unique-key>
Content-Type: application/json

{
  "remark": "服务资料完整，审核通过"
}
```

审核通过必须在同一事务中：

1. 把服务状态改为 `PUBLISHED`；
2. 使服务可被 `GET /api/v1/items` 和 `GET /api/v1/items/{id}` 查询；
3. 写入管理员、时间和审核备注。

#### 4.4.4 审核驳回

```http
POST /api/v1/admin/merchant-services/{itemId}/reject
Authorization: Bearer <admin-token>
Idempotency-Key: <unique-key>
Content-Type: application/json

{
  "remark": "服务说明缺少风险提示，请补充后重新提交"
}
```

`remark` 必填，建议 5-500 字。成功后服务状态变为 `REJECTED`，商户详情必须返回该驳回原因。

### 4.5 图片上传

继续复用已有接口：

```http
POST /api/v1/files/upload
```

建议商户服务封面使用 `bizType=merchant_service_cover`。返回的 `fileUrl` 存入服务 `coverImage`，本期不新增独立图片服务。

---

## 5. 必须新增二：商户查看并回复投诉

### 5.1 业务简化规则

- 顾客继续使用现有接口提交投诉；
- 后端根据投诉关联订单确定所属商户，不能让前端传入可信 `merchantId`；
- 商户只能查看属于本商户订单的投诉；
- 商户只能提交核查说明和附件，不直接决定退款，也不直接关闭投诉；
- 管理员继续使用现有管理端投诉处理接口给出最终结果；
- 商户回复、管理员处理结果都要进入顾客可见的投诉进度记录。

### 5.2 投诉状态

继续沿用现有数值状态，避免修改顾客端和管理员端：

| 状态 | 含义 |
|---|---|
| `0` | 待商户响应 |
| `1` | 商户已响应，待平台处理 |
| `2` | 已解决 |
| `3` | 已关闭 |

商户回复后由 `0 -> 1`。管理员使用现有处理接口将 `0/1 -> 2/3`。

### 5.3 商户投诉接口

#### 5.3.1 投诉列表

```http
GET /api/v1/merchant/complaints?status=&page=1&size=20
Authorization: Bearer <merchant-token>
```

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "complaintId": "801",
        "complaintNo": "CP202607170001",
        "orderId": "701",
        "orderNo": "NO202607170001",
        "serviceItemName": "居家生活照护",
        "type": 1,
        "typeText": "服务质量",
        "content": "服务开始时间晚于预约时间",
        "status": 0,
        "statusText": "待商户响应",
        "createTime": "2026-07-17T10:00:00+08:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

#### 5.3.2 投诉详情

```http
GET /api/v1/merchant/complaints/{complaintId}
Authorization: Bearer <merchant-token>
```

成功 `data` 至少包含：

```json
{
  "complaintId": "801",
  "complaintNo": "CP202607170001",
  "type": 1,
  "typeText": "服务质量",
  "content": "服务开始时间晚于预约时间",
  "images": [],
  "status": 0,
  "statusText": "待商户响应",
  "merchantReply": null,
  "merchantReplyImages": [],
  "order": {
    "orderId": "701",
    "orderNo": "NO202607170001",
    "serviceItemName": "居家生活照护",
    "specName": "2小时服务",
    "receiverName": "张女士",
    "totalAmount": 120.00,
    "serviceDate": "2026-07-18",
    "serviceTimeSlot": "MORNING"
  },
  "tracks": [
    {
      "trackId": "901",
      "operator": "顾客",
      "content": "提交投诉",
      "createTime": "2026-07-17T10:00:00+08:00"
    }
  ],
  "createTime": "2026-07-17T10:00:00+08:00"
}
```

手机号和详细地址不是投诉处理的必要字段，本接口建议不返回，避免扩大隐私暴露。

#### 5.3.3 提交商户回复

```http
POST /api/v1/merchant/complaints/{complaintId}/respond
Authorization: Bearer <merchant-token>
Idempotency-Key: <unique-key>
Content-Type: application/json

{
  "content": "已核对护理人员签到和沟通记录，迟到情况属实，商户已向顾客致歉并提交平台处理。",
  "images": [
    "https://example.com/evidence-1.jpg"
  ]
}
```

校验建议：

- `content` 必填，10-1000 字；
- `images` 可选，最多 6 张；
- 只允许处理本商户投诉；
- 只允许在状态 `0` 时首次提交；
- 重复提交同一个 `Idempotency-Key` 必须返回第一次结果；
- 成功后状态变为 `1`，并新增一条顾客和管理员可见的处理记录。

### 5.4 管理员投诉处理

继续复用现有接口：

```http
GET  /api/v1/admin/feedback/complaints
POST /api/v1/admin/feedback/complaints/{id}/handle
```

管理员列表和详情数据需要能看到商户回复及附件。处理完成后，顾客的：

```http
GET /api/v1/complaints/{id}/tracks
```

必须能看到商户回复和管理员最终处理说明。

本期不做复杂仲裁、退款裁决或商户多轮协商接口。涉及退款时，管理员仅记录处理说明；实际退款仍按现有订单退款能力处理，不能由商户投诉回复直接触发。

---

## 6. 本期明确不需要开发的后端接口

为避免继续扩大范围，以下接口本期可以不实现，前端会移除或隐藏对应入口：

- `/api/v1/notifications/**`
- `/api/v1/caregiver/schedule`
- `/api/v1/caregiver/earnings`
- `/api/v1/caregiver/withdrawals`
- `/api/v1/merchant/caregivers/**`
- `/api/v1/merchant/members/**`
- `/api/v1/merchant/exceptions/**`
- `/api/v1/merchant/settlements`
- `/api/v1/merchant/withdrawals`
- `/api/v1/merchant/reports/**`
- 商户自行 `publish`、`offline` 的服务接口；
- 商户入驻申请和管理员商户入驻审核接口；
- 复杂投诉仲裁、自动退款和多轮协商接口。

如果上述接口已经实现，可以保留，但不纳入本期联调和验收，前端也不会提供主要入口。

---

## 7. 通用契约要求

所有保留和新增接口必须遵守：

1. 统一响应：`{ "code": 0, "message": "success", "data": ... }`。
2. 所有 `id` 和 `*Id` 必须序列化为十进制 `string`，不能返回超出 JavaScript 安全范围的 number。
3. 所有副作用接口使用请求头 `Idempotency-Key`，不要再读取旧的 `Idempotent-Key`。
4. `merchantId`、`userId`、`caregiverId` 必须从登录 Token 和后端授权上下文取得，不能信任前端传值。
5. 商户只能访问本商户服务、订单和投诉；护理人员只能访问自己的任务；顾客只能访问自己的订单和投诉。
6. 所有状态变更必须在后端校验当前状态，非法跳转返回明确业务错误，不能只依赖前端隐藏按钮。
7. 列表接口统一返回 `{ list, total, page, size }`；空列表返回 `list: []`，不能返回 `null`。
8. 图片字段统一使用可访问的完整 URL，或提供与当前网关一致的静态资源相对路径。
9. 新接口完成后同步更新 `docs/API接口文档.md`，包括权限、请求参数、响应字段、状态和错误码。

---

## 8. 最小演示数据

请继续保留以下账号：

| 角色 | 账号 | 密码 |
|---|---|---|
| 顾客 | `13800138000` | `123456` |
| 护理人员 | `13800138001` | `123456` |
| 商户 | `13800138002` | `123456` |
| 管理员 | `admin` | `123456` |

商户账号至少准备：

- 1 个草稿服务；
- 1 个待审核服务；
- 1 个被驳回服务，带明确驳回原因；
- 1 个已审核上架服务；
- 1 条待商户回复投诉；
- 1 条商户已回复、待管理员处理投诉；
- P0 订单和派单演示数据。

服务审核通过后，应能够使用顾客账号从公开服务列表中看到该服务。

---

## 9. 联调验收流程

### 9.1 服务提交与审核

```text
商户登录
-> 新建服务并保存草稿
-> 编辑规格并提交审核
-> 管理员登录后台查看待审核服务
-> 管理员驳回，商户能看到驳回原因
-> 商户修改并重新提交
-> 管理员审核通过
-> 顾客端服务列表和详情可以查询该服务
```

### 9.2 投诉处理

```text
顾客从订单提交投诉
-> 商户投诉列表出现该投诉
-> 商户查看详情并提交回复
-> 顾客投诉进度出现商户回复
-> 管理员查看投诉和商户回复
-> 管理员标记已解决或已关闭
-> 顾客和商户都能看到最终状态与处理说明
```

### 9.3 订单履约主闭环

```text
顾客下单并支付
-> 商户查看订单并派单
-> 护理人员接单
-> 出发、签到、开始、结束服务
-> 顾客确认完成
-> 顾客评价或投诉
```

---

## 10. 建议后端交付顺序

1. 确认现有 P0 登录、下单、支付、派单和履约接口没有回归。
2. 新增商户服务列表、创建、详情、编辑和提交审核接口。
3. 新增管理员服务审核列表、详情、通过和驳回接口。
4. 新增商户投诉列表、详情和回复接口。
5. 让管理员投诉接口和顾客投诉进度包含商户回复。
6. 补齐四个演示账号和最小演示数据。
7. 更新 API 文档后通知前端开始最终接口对齐。

