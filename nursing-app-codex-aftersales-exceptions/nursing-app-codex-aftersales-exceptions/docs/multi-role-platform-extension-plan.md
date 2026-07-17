# 智慧护理平台多角色与业务闭环扩展方案

> 文档状态：方案草案
> 适用项目：智慧护理平台 Uni-app 客户端及后续管理后台
> 编写日期：2026-07-15

## 1. 建设目标

现有系统主要覆盖顾客浏览服务、预约下单、支付、评价和投诉。下一阶段需要增加商户、护理人员和平台管理员，使平台形成完整业务闭环：

```text
商户入驻并提交服务
→ 管理员审核商户、护理人员和服务
→ 顾客选择服务并预约付款
→ 商户为订单指派护理人员
→ 护理人员接单、上门并记录服务过程
→ 顾客确认完成、评价或投诉
→ 商户处理经营事务，管理员监管异常和售后
```

本方案重点解决：

- 多角色账号、登录入口和权限控制。
- 商户入驻、人员管理和服务发布。
- 订单派单、接单和上门履约。
- 服务审核、订单监管和投诉处理。
- 前端页面、Store、API 和后端数据模型扩展。
- 从当前单一用户模型平滑升级，保留既有功能。

## 2. 总体设计结论

### 2.1 角色定义

| 角色 | 代码 | 主要职责 |
|---|---|---|
| 顾客 | `CUSTOMER` | 浏览服务、预约下单、支付、确认、评价、投诉 |
| 护理人员 | `CAREGIVER` | 接单、查看任务、出发、签到、服务记录、结束服务 |
| 商户成员 | `MERCHANT_MEMBER` | 管理服务、护理人员、订单、派单和经营数据 |
| 平台管理员 | `ADMIN` | 审核、监管、售后、平台配置和审计 |

“商户”是组织，不是自然人账号。实际登录的是商户负责人或员工，因此系统内部建议使用 `MERCHANT_MEMBER`，界面上仍可显示“商户”。

### 2.2 账号与角色原则

采用“一个账号可拥有多个角色，一次会话只激活一个角色”的模型：

```text
一个账号识别一个自然人
→ 账号可以取得多个已审核身份
→ 登录时选择本次准备进入的身份
→ 后端验证账号是否拥有该身份
→ 本次会话只获得该身份的权限
```

这样可支持以下真实场景：

- 护理人员也可以作为顾客为家人下单。
- 商户负责人可以同时具备护理资质并亲自接单。
- 普通顾客后续可以申请成为护理人员。
- 商户员工可以加入或离开商户，而不需要重新注册个人账号。
- 同一个手机号、实名认证和通知渠道不需要重复维护。

角色选择只是登录目标，不能作为授权依据。最终角色资格、审核状态、数据范围和操作权限都必须由后端校验。

### 2.3 客户端划分

建议按以下方式划分产品入口：

| 客户端 | 使用角色 | 建议形态 |
|---|---|---|
| 智慧护理 App | 顾客、护理人员、商户成员 | 当前 Uni-app 工程继续扩展 |
| 平台管理后台 | 管理员 | 独立 Vue 3 Web 管理项目 |

管理员不建议作为 App 登录页的第四个普通身份 Tab。课程演示或第一期资源有限时，可以在登录页底部提供低强调的“平台管理人员入口”，进入独立管理员登录页；正式版本应迁移到独立 Web 后台。

## 3. 登录与身份切换方案

### 3.1 登录页交互

App 登录页提供三个身份选项：

```text
                 智慧护理平台

          [ 顾客 ] [ 护理人员 ] [ 商户 ]

                 手机号
              密码 / 验证码

                   登录

       注册账号    忘记密码    申请入驻

              平台管理人员入口
```

推荐规则：

- 默认选择“顾客”。
- 记住上一次成功登录的身份，下次自动选中。
- 不同身份展示不同说明和辅助入口。
- 登录成功后直接进入所选身份的工作台，不再强制进行第二次选择。
- 登录后可在“我的”页面切换账号已经拥有的其他身份。

### 3.2 登录处理流程

界面上是“先选身份再登录”，系统内部按以下顺序处理：

```text
选择目标身份
→ 提交手机号和密码/验证码
→ 后端验证账号凭证
→ 后端验证账号是否拥有目标身份
→ 验证角色审核状态、启用状态和所属组织
→ 签发仅包含当前身份权限的 Token
→ 进入对应工作台
```

登录请求示例：

```json
{
  "phone": "13800138000",
  "loginMode": "password",
  "password": "123456",
  "targetRole": "CAREGIVER"
}
```

登录响应建议扩展为：

```json
{
  "token": "role-scoped-token",
  "expireTime": "2026-07-22T12:00:00+08:00",
  "user": {
    "userId": 10001,
    "phone": "13800138000",
    "nickname": "测试用户",
    "roles": ["CUSTOMER", "CAREGIVER"],
    "currentRole": "CAREGIVER",
    "merchantId": null,
    "caregiverId": 50001
  },
  "permissions": [
    "caregiver:task:list",
    "caregiver:assignment:accept",
    "caregiver:service:check-in"
  ]
}
```

如果账号没有所选身份，不应自动授权，而应返回明确状态并引导用户：

```text
该账号尚未开通护理人员身份
[申请成为护理人员] [切换到顾客登录]
```

### 3.3 登录后的身份切换

在“我的”页面提供身份切换入口：

```text
当前身份：护理人员

可切换身份
├── 顾客
└── 护理人员（当前）
```

切换身份时调用后端接口重新签发 Token，不应只修改前端变量：

```http
POST /api/v1/auth/switch-role
Content-Type: application/json

{
  "targetRole": "CUSTOMER"
}
```

如果账号属于多个商户，应在商户身份验证成功后再选择本次进入的商户组织。

### 3.4 管理员登录

管理员账号由平台内部创建，不开放公开注册或普通角色升级。建议采用：

- 独立管理后台域名和登录页面。
- 密码加短信或身份验证器的双因素认证。
- 独立的管理员 Token audience。
- 设备、IP、登录时间和异常登录记录。
- 敏感操作二次确认和完整审计日志。

## 4. 组织和权限模型

### 4.1 商户组织模型

一个商户可以有多个成员，一个账号也可以加入多个商户：

```text
用户账号
→ 商户成员关系
→ 所属商户
→ 商户内岗位
→ 岗位权限
```

商户内部岗位建议包括：

| 岗位 | 代码 | 权限示例 |
|---|---|---|
| 负责人 | `OWNER` | 全部商户权限、成员管理、结算配置 |
| 运营 | `OPERATOR` | 服务管理、订单查看、营销配置 |
| 调度员 | `DISPATCHER` | 候选人员查询、派单、改派 |
| 财务 | `FINANCE` | 账单、退款、结算和数据导出 |

### 4.2 平台权限矩阵

| 功能 | 顾客 | 商户成员 | 护理人员 | 管理员 |
|---|:---:|:---:|:---:|:---:|
| 浏览已上架服务 | 是 | 是 | 是 | 是 |
| 创建和提交服务 |  | 是 |  | 是 |
| 审核服务 |  |  |  | 是 |
| 创建并支付订单 | 是 |  |  |  |
| 查看本人订单 | 是 |  |  |  |
| 查看商户订单 |  | 是 |  | 是 |
| 派单和改派 |  | 是 |  | 是 |
| 接受或拒绝派单 |  |  | 是 |  |
| 签到和填写服务记录 |  |  | 是 |  |
| 确认订单完成 | 是 |  |  | 是 |
| 管理商户护理人员 |  | 是 |  | 是 |
| 评价和投诉 | 是 |  |  |  |
| 协助处理投诉 |  | 是 |  |  |
| 最终处理投诉 |  |  |  | 是 |

除功能权限外，还必须校验数据范围：

- 顾客只能访问自己的地址、订单、评价和投诉。
- 商户成员只能访问当前所属商户的数据，并受商户岗位权限限制。
- 护理人员只能访问派给自己的任务。
- 护理人员仅在接单后和合理履约时间窗口内查看完整地址与联系方式。
- 管理员的审核、强制改派、退款等操作必须记录原因和审计日志。

## 5. 核心业务闭环

### 5.1 商户入驻

```text
创建个人账号
→ 提交商户资料
→ 管理员审核
→ 审核通过
→ 创建商户组织和负责人关系
→ 开通商户工作台
```

商户资料建议包括：

- 商户名称和统一社会信用代码。
- 负责人姓名、手机号和身份证明。
- 营业执照、经营许可等附件。
- 经营地址和服务区域。
- 结算账户。
- 审核结果、审核意见、审核人和审核时间。

商户状态：

```text
DRAFT → PENDING_REVIEW → APPROVED
                       ↘ REJECTED
APPROVED → DISABLED
```

### 5.2 护理人员认证

```text
创建个人账号
→ 提交身份和资质材料
→ 商户确认或邀请加入
→ 管理员认证
→ 配置服务能力、区域和排班
→ 开始接单
```

护理人员资料建议包括：

- 身份证和基础个人资料。
- 护士证、护理员证等资质及有效期。
- 所属商户和劳动/合作关系。
- 可服务项目、服务区域和从业年限。
- 排班、请假和可接单状态。
- 在职、停用、离职状态。
- 评分、完成订单数和异常记录。

### 5.3 服务发布与审核

```text
商户创建服务草稿
→ 设置分类、介绍、规格、价格、时长和适用资质
→ 提交审核
→ 管理员审核
→ 审核通过并上架
```

服务状态：

```text
DRAFT
→ PENDING_REVIEW
→ APPROVED
→ PUBLISHED
→ OFFLINE

PENDING_REVIEW → REJECTED → 修改后重新提交
```

重要内容变更应创建新版本重新审核，不直接覆盖已发布版本。历史订单必须保存服务和规格快照，避免服务改名、改价或下架后影响历史记录。

### 5.4 顾客预约下单

延续当前 `prepay-token → create → pay` 三步流程：

```text
选择服务和规格
→ 选择地址
→ 选择日期与时段
→ 创建预支付令牌
→ 创建订单
→ 支付
→ 进入待派单
```

订单必须保存下单时快照：

```json
{
  "orderId": 30001,
  "userId": 10001,
  "merchantId": 20001,
  "itemId": 101,
  "itemSnapshot": {
    "itemName": "居家基础护理",
    "coverImage": "https://cdn.example.com/item.jpg",
    "specName": "2小时护理",
    "price": 29900,
    "durationMinutes": 120
  },
  "addressSnapshot": {
    "contactName": "张女士",
    "contactPhone": "13800138000",
    "province": "上海市",
    "city": "上海市",
    "district": "浦东新区",
    "detail": "XX路XX号"
  },
  "scheduledStartTime": "2026-07-20T09:00:00+08:00",
  "scheduledEndTime": "2026-07-20T11:00:00+08:00",
  "remark": "老人行动不便"
}
```

新接口建议统一将金额保存为“分”的整数。当前项目 Mock 和联调文档以“元”为单位，迁移时必须统一转换边界，避免新旧接口同时使用不同单位却没有明确字段说明。

### 5.5 派单与接单

第一期建议采用商户人工派单，后续再增加智能推荐或自动派单：

```text
订单支付成功
→ 商户收到待派单通知
→ 查询符合条件的护理人员
→ 商户派单
→ 护理人员接受或拒绝
→ 接受后进入待服务
→ 拒绝或超时后重新派单
```

候选护理人员至少满足：

- 资质有效且账号未停用。
- 具备订单对应服务能力。
- 服务区域覆盖订单地址。
- 预约时间无排班冲突。
- 当前处于可接单状态。
- 未超过每日或同时进行订单上限。

每次派单、拒单、过期、取消和改派都应保存独立记录。护理人员拒单时需要选择原因，接单超时后系统自动恢复为待派单并通知商户。

### 5.6 上门履约

```text
待服务
→ 护理人员出发
→ 到达签到
→ 开始服务
→ 填写服务记录
→ 结束服务
→ 等待顾客确认
→ 订单完成
```

服务记录可以包含：

- 出发、到达、开始和结束时间。
- 签到定位和与服务地址的距离。
- 服务项目清单。
- 护理前后记录和生命体征等结构化数据。
- 图片、附件和异常情况说明。
- 顾客或护理人员签名。

定位校验需要允许异常申诉，例如定位权限关闭、老人临时换址或系统定位漂移。异常完成应由商户或管理员审核，不应让护理人员绕过正常状态直接完成订单。

### 5.7 完成、评价和投诉

```text
护理人员提交服务结束
→ 顾客查看服务记录并确认
→ 订单完成
→ 顾客评价

如有争议：
顾客投诉
→ 商户提交说明和证据
→ 管理员介入处理
→ 退款、补偿、处罚或关闭投诉
```

建议设置自动确认机制，例如护理人员提交结束后 24 小时内顾客未确认且未投诉，系统自动完成订单。具体时长和例外规则应由平台配置。

## 6. 状态模型

当前项目使用单一数字订单状态。扩展后不要继续用一个字段承载支付、派单和履约的全部含义，建议拆分为三个维度。

### 6.1 订单主状态

```text
CREATED
WAITING_DISPATCH
WAITING_SERVICE
IN_SERVICE
WAITING_CONFIRM
COMPLETED
CANCELED
CLOSED
DISPUTED
```

### 6.2 支付状态

```text
UNPAID
PAID
REFUNDING
PARTIALLY_REFUNDED
REFUNDED
```

### 6.3 派单状态

```text
UNASSIGNED
WAITING_ACCEPT
ACCEPTED
REJECTED
EXPIRED
CANCELED
```

前端根据多个状态组合显示用户文案，例如“待付款”“待派单”“待护理人员接单”“待上门”“服务中”“待确认”。后端必须通过状态机限制合法迁移，禁止客户端直接提交任意状态值。

状态动作应使用专用接口：

```http
POST /api/v1/orders/{orderId}/cancel
POST /api/v1/merchant/orders/{orderId}/dispatch
POST /api/v1/caregiver/assignments/{assignmentId}/accept
POST /api/v1/caregiver/assignments/{assignmentId}/reject
POST /api/v1/caregiver/orders/{orderId}/depart
POST /api/v1/caregiver/orders/{orderId}/check-in
POST /api/v1/caregiver/orders/{orderId}/start
POST /api/v1/caregiver/orders/{orderId}/finish
POST /api/v1/orders/{orderId}/confirm
```

下单、取消、支付、派单、接单、签到、完成和退款等动作应支持 `Idempotent-Key`。

## 7. 页面规划

### 7.1 顾客端

现有首页、服务列表、详情、搜索、地址、下单、订单、评价和投诉页面继续保留并增强：

```text
pages/order-create/
pages/address-list/
pages/address-edit/
pages/payment-result/
pages/order-list/
pages/order-detail/
pages/review-submit/
pages/complaint-submit/
pages/complaint-list/
```

订单详情新增：

- 商户信息和护理人员信息。
- 预约时间与服务地址。
- 派单和履约时间线。
- 服务记录和凭证。
- 支付、退款和投诉状态。
- 取消、确认、评价、投诉等上下文操作。

### 7.2 商户工作台

```text
pages/merchant/home/
pages/merchant/apply/
pages/merchant/profile/
pages/merchant/service-list/
pages/merchant/service-edit/
pages/merchant/service-detail/
pages/merchant/order-list/
pages/merchant/order-detail/
pages/merchant/dispatch/
pages/merchant/caregiver-list/
pages/merchant/caregiver-detail/
pages/merchant/member-list/
pages/merchant/statistics/
```

商户首页重点指标：

- 今日待服务、待派单、待接单和服务中订单数。
- 即将超时和异常订单。
- 本月订单量、完成率和营业额。
- 待审核服务和即将到期的人员资质。

### 7.3 护理人员工作台

```text
pages/caregiver/apply/
pages/caregiver/home/
pages/caregiver/order-list/
pages/caregiver/order-detail/
pages/caregiver/check-in/
pages/caregiver/service-record/
pages/caregiver/schedule/
pages/caregiver/profile/
```

护理人员首页重点展示：

- 下一单服务时间和倒计时。
- 今日任务时间线。
- 待接受的新任务。
- 用户脱敏联系方式和地址导航。
- 出发、到达、开始和完成操作。

### 7.4 管理后台

建议独立建立 Vue 3 Web 管理项目，功能模块包括：

```text
admin/
├── dashboard
├── merchant-review
├── caregiver-review
├── service-review
├── order-management
├── dispatch-monitor
├── complaint-management
├── refund-management
├── category-management
├── platform-config
└── operation-log
```

当前已在仓库根目录建立独立 `admin/` Vue 3 Web 工程。第一期先实现管理员登录、审核工作台，以及商户入驻、护理人员认证和服务发布的统一审核闭环；订单监管、投诉仲裁、退款和系统配置继续按后续阶段扩展。

## 8. 前端工程调整

### 8.1 Store 拆分

不要创建包含所有角色业务的超大 Store，建议按领域拆分：

```text
src/store/
├── user.js
├── role.js
├── service.js
├── order.js
├── address.js
├── merchant.js
├── service-manage.js
├── dispatch.js
├── caregiver.js
├── work-order.js
├── review.js
├── complaint.js
└── admin.js
```

职责建议：

| Store | 职责 |
|---|---|
| `role.js` | 可用角色、当前角色、角色切换、权限判断 |
| `merchant.js` | 商户资料、成员和经营数据 |
| `service-manage.js` | 服务草稿、提交审核、上下架和版本 |
| `dispatch.js` | 候选护理人员、派单、改派和派单记录 |
| `caregiver.js` | 护理人员档案、资质、排班和服务能力 |
| `work-order.js` | 护理任务、接单、签到和服务记录 |
| `admin.js` | 审核列表、监管和后台操作；仅管理端使用 |

### 8.2 路由和页面权限

建议增加统一权限工具和页面进入守卫：

```js
export function hasRole(role) {}
export function hasPermission(permission) {}
export function requireRole(roles) {}
export function requirePermission(permission) {}
```

前端权限控制只负责用户体验：隐藏无权限入口、阻止误操作和跳转到正确工作台。后端仍需对每个接口重新鉴权。

Uni-app 原生 TabBar 不适合频繁动态切换，建议使用统一基础导航，将角色功能集中到“工作台”页面，按 `currentRole` 渲染不同内容；角色业务页面可使用分包降低首包体积。

### 8.3 推荐分包

```text
第一阶段主包：保留现有登录、顾客首页、服务发现、地址、订单和售后页面
护理人员分包：subpkg-caregiver（工作台、任务、签到、服务记录、排班）
商户分包：subpkg-merchant（工作台、服务管理、人员管理、派单、经营数据）
```

第一阶段不迁移现有顾客页面，避免同时修改全部页面路径和跳转逻辑。新增角色功能稳定后，再根据主包体积决定是否建立 `subpkg-customer`。

顾客继续使用当前原生 TabBar。护理人员和商户分包使用公共的自定义底部导航组件，分包页面之间使用普通页面导航。`navigationStyle: "custom"` 仅在页面需要自绘顶部导航栏时启用，与自定义底部导航不是同一配置。

角色和目录统一使用 `caregiver`，不使用范围较窄的 `nurse`，以兼容护士、护理员、康复师等服务人员。

### 8.4 第一阶段 Store 边界

第一阶段先增加轻量的 `role.js`，负责角色常量、当前身份、权限判断和工作台入口；业务 Store 仍按领域维护。后续 Store 是否继续拆分，以文件职责和规模为依据：

```text
role.js              当前角色、可用角色、前端权限判断
merchant.js          商户资料和成员关系
service-manage.js    商户服务草稿、审核和上下架
dispatch.js          候选人员、派单、拒单和改派记录
caregiver.js         护理人员档案、资质、能力和排班
work-order.js        护理任务、签到和履约记录
```

不把服务、订单、人员、派单和结算全部合并进单个角色 Store，也不在移动端 Store 中加入管理员业务。管理员状态由独立 Web 项目维护。

### 8.5 Mock 路由约束

Mock 路由正则必须使用明确的路径边界，例如：

```js
/\/api\/v1\/items(?:\?|$)/
/\/api\/v1\/merchants\/items(?:\?|$)/
```

具体模块可以先于通用模块注册以增强可读性，但不能依赖导入顺序解决路径冲突；正确的正则边界和对应的路由测试才是主要保障。

## 9. 后端数据模型

建议新增或调整以下核心实体：

```text
users
roles
permissions
user_roles

merchants
merchant_members

caregivers
caregiver_certificates
caregiver_skills
caregiver_schedules

service_items
service_specs
service_versions
service_audit_records

orders
order_status_logs
order_assignments
payments
refunds

service_checkins
service_records
service_record_attachments

reviews
complaints
complaint_tracks
notifications
operation_logs
```

关键关系：

- `user_roles` 保存一个账号具备的平台注册身份。
- `merchant_members` 保存账号与商户的关系、岗位和成员状态。
- `caregivers` 保存护理人员业务档案，不与基础账号字段混在一起。
- `caregiver_skills` 关联护理人员可执行的服务或服务分类。
- `service_versions` 保存服务审核版本和历史版本。
- `order_assignments` 保存每次派单、拒单、过期和改派。
- `order_status_logs` 保存完整订单时间线。
- `operation_logs` 保存管理员和商户的敏感操作审计记录。

## 10. API 扩展建议

### 10.1 认证与身份

```http
POST /api/v1/users/login
POST /api/v1/auth/switch-role
GET  /api/v1/profile
GET  /api/v1/profile/roles
GET  /api/v1/profile/permissions
```

### 10.2 商户入驻和资料

```http
POST /api/v1/merchants/apply
GET  /api/v1/merchant/profile
PUT  /api/v1/merchant/profile
GET  /api/v1/merchant/dashboard
GET  /api/v1/merchant/members
POST /api/v1/merchant/members/invite
```

### 10.3 商户服务管理

```http
GET  /api/v1/merchant/services
POST /api/v1/merchant/services
GET  /api/v1/merchant/services/{itemId}
PUT  /api/v1/merchant/services/{itemId}
POST /api/v1/merchant/services/{itemId}/submit
POST /api/v1/merchant/services/{itemId}/publish
POST /api/v1/merchant/services/{itemId}/offline
```

### 10.4 商户订单和派单

```http
GET  /api/v1/merchant/orders
GET  /api/v1/merchant/orders/{orderId}
GET  /api/v1/merchant/orders/{orderId}/candidates
POST /api/v1/merchant/orders/{orderId}/dispatch
POST /api/v1/merchant/orders/{orderId}/redispatch
GET  /api/v1/merchant/orders/{orderId}/assignments
```

### 10.5 护理人员

```http
POST /api/v1/caregivers/apply
GET  /api/v1/caregiver/profile
PUT  /api/v1/caregiver/profile
GET  /api/v1/caregiver/tasks
GET  /api/v1/caregiver/tasks/{orderId}
POST /api/v1/caregiver/assignments/{assignmentId}/accept
POST /api/v1/caregiver/assignments/{assignmentId}/reject
POST /api/v1/caregiver/orders/{orderId}/depart
POST /api/v1/caregiver/orders/{orderId}/check-in
POST /api/v1/caregiver/orders/{orderId}/start
POST /api/v1/caregiver/orders/{orderId}/finish
GET  /api/v1/caregiver/schedules
PUT  /api/v1/caregiver/schedules
```

### 10.6 管理员

```http
GET  /api/v1/admin/merchants
POST /api/v1/admin/merchants/{merchantId}/approve
POST /api/v1/admin/merchants/{merchantId}/reject

GET  /api/v1/admin/caregivers
POST /api/v1/admin/caregivers/{caregiverId}/approve
POST /api/v1/admin/caregivers/{caregiverId}/reject

GET  /api/v1/admin/services/pending
POST /api/v1/admin/services/{itemId}/approve
POST /api/v1/admin/services/{itemId}/reject

GET  /api/v1/admin/orders
GET  /api/v1/admin/complaints
POST /api/v1/admin/complaints/{complaintId}/process
POST /api/v1/admin/orders/{orderId}/force-redispatch
```

## 11. 通知设计

建议建立统一通知中心，并根据事件发送站内信、App 推送或短信：

| 事件 | 接收方 |
|---|---|
| 商户、护理人员或服务审核结果 | 申请人、商户负责人 |
| 新订单待派单 | 商户调度员 |
| 新派单 | 被指派护理人员 |
| 接单、拒单或超时 | 商户调度员 |
| 即将开始服务 | 顾客、护理人员 |
| 护理人员出发或到达 | 顾客 |
| 服务结束待确认 | 顾客 |
| 投诉提交和处理进展 | 顾客、商户、管理员 |
| 退款进展 | 顾客、商户财务 |

通知事件与业务状态变更应解耦。即使推送失败，也不能回滚已经成功的订单状态操作。

## 12. 安全与隐私要求

- Token 必须包含或关联当前激活角色、账号状态和组织范围。
- 所有接口按角色和数据归属双重校验，不能只依赖前端菜单隐藏。
- 手机号、详细地址、身份证和资质材料按最小必要原则展示。
- 护理人员未接单前不展示完整地址；履约窗口结束后再次脱敏。
- 下单时后端重新校验服务、规格、地址、库存/排班和价格。
- 客户端不得提交可信金额、商户 ID、护理人员 ID 或订单状态作为最终依据。
- 资质文件使用受控访问地址，避免永久公开 URL。
- 管理员和商户敏感操作记录操作者、时间、IP、对象、变更前后值及原因。
- 登录、接单、派单、签到、完成、退款等接口需要防重放和幂等控制。
- 服务记录可能涉及健康信息，应设置访问期限、下载权限和数据保留策略。

## 13. 异常场景和处理规则

第一期至少覆盖以下场景：

| 场景 | 建议处理 |
|---|---|
| 派单后护理人员拒绝 | 记录原因，订单恢复待派单，通知商户 |
| 护理人员超时未响应 | 派单过期，自动恢复待派单并提醒调度员 |
| 护理人员临时请假 | 商户改派，保留原派单和改派记录 |
| 长时间无人可派 | 升级为异常订单，允许改期、退款或管理员介入 |
| 顾客取消未支付订单 | 直接关闭订单 |
| 顾客取消已支付订单 | 根据时间和履约阶段计算退款 |
| 护理人员到达但顾客不在 | 上传签到和联系凭证，由商户确认后处理 |
| 定位权限关闭或定位漂移 | 允许提交异常签到并进入人工审核 |
| 服务实际超时 | 记录真实时间，按配置决定是否补费或仅作异常记录 |
| 护理人员结束后顾客不确认 | 到期自动确认，投诉中的订单除外 |
| 顾客质疑服务未完成 | 订单进入争议状态，冻结自动结算 |
| 资质到期 | 禁止接受新派单，通知本人和商户续期 |

取消、退款、自动确认和派单超时规则应配置化，不要硬编码在客户端。

## 14. 与现有项目的兼容和迁移

### 14.1 第一阶段兼容原则

- 现有账号默认授予 `CUSTOMER` 角色。
- 现有登录接口可先将 `targetRole` 设为可选，缺省值为 `CUSTOMER`。
- 现有顾客页面、Store 和 Mock 保持可用。
- 先新增角色和组织字段，再逐步接入商户及护理人员功能。
- 管理后台与 App 共用 API 规范，但使用独立路由前缀和安全策略。

### 14.2 订单状态迁移建议

当前状态：

```text
0 待支付
1 待服务
2 已完成
3 已取消
4 退款中
5 已退款
```

迁移时可以先做兼容映射：

| 旧状态 | 新订单状态 | 新支付状态 | 新派单状态 |
|---:|---|---|---|
| 0 | `CREATED` | `UNPAID` | `UNASSIGNED` |
| 1 | `WAITING_DISPATCH` | `PAID` | `UNASSIGNED` |
| 2 | `COMPLETED` | `PAID` | `ACCEPTED` 或历史未知 |
| 3 | `CANCELED` | `UNPAID` 或 `PAID` | `CANCELED` |
| 4 | `CANCELED` | `REFUNDING` | `CANCELED` |
| 5 | `CLOSED` | `REFUNDED` | `CANCELED` |

历史订单缺少派单数据时，不应虚构具体护理人员。可标记为“历史数据”或“派单信息未知”。新旧状态并行期间，应由 API 适配层完成映射，页面不要同时散落两套判断逻辑。

### 14.3 Mock 扩展顺序

```text
src/mock/role.js
src/mock/merchant.js
src/mock/caregiver.js
src/mock/service-manage.js
src/mock/dispatch.js
src/mock/admin.js
```

同时扩展 `user.js` 登录响应和 `order.js` 的状态、商户、护理人员、派单及履约记录。

## 15. 推荐实施路线

### 第一阶段：身份和基础权限

- [ ] 扩展登录请求和用户响应，增加 `targetRole`、`roles`、`currentRole`。
- [x] 新增角色切换接口、`role` Store 和路由权限工具。
- [ ] 登录页增加顾客、护理人员、商户三个身份选项。
- [ ] 现有用户自动兼容为顾客。
- [x] 建立商户、商户成员和护理人员基础档案（前端 Mock 档案）。

### 第二阶段：最小业务闭环

- [x] 商户入驻资料和 Mock 管理员审核；正式管理员后台仍待开发。
- [x] 护理人员认证资料、证书、服务能力和审核状态；真实排班接口仍待开发。
- [x] 商户服务创建、规格编辑、提交审核和上下架前端流程。
- [x] Mock 模拟管理员审核结果，正式管理员审核后台仍待开发。
- [x] 前端订单状态升级为主状态、支付状态和派单状态，并兼容旧数字状态。
- [x] 前端 Mock 建立派单记录、服务记录和合法状态迁移。
- [x] 建立商户派单与护理人员履约 Store/API 契约。
- [x] 商户工作台、订单列表、订单详情、候选人员和人工派单。
- [x] 护理人员工作台、任务列表和任务详情。
- [x] 护理人员接单、拒单、出发、签到、开始和结束服务的前端闭环。
- [x] 顾客订单详情展示商户、护理人员、派单记录和履约时间线。
- [x] 顾客仅在护理人员结束服务后确认完成。

### 第三阶段：售后和异常处理

- [x] 取消和退款规则（前端规则预览与 Mock 退款进度）。
- [x] 改派、拒单、接单超时和无人可派处理（前端异常工单）。
- [x] 服务记录和异常签到审核；真实附件上传和证据存储仍待后端。
- [x] 投诉、商户举证、管理员仲裁和退款联动（前端 Mock 闭环；真实支付退款待后端）。
- [x] 通知中心和状态提醒（按当前身份隔离、未读角标、分类筛选和业务页面跳转）。

投诉闭环第一期实现说明：顾客提交投诉后订单进入 `DISPUTED`，商户可提交处理说明和凭证并选择协商解决或升级平台；管理员独立后台可核验双方材料，裁定不退款、部分退款或全额退款。退款继续使用独立退款状态，Mock 约 8 秒完成，真实支付渠道和附件存储由后端实现。

通知中心第一期实现说明：顾客、护理人员、商户按当前登录身份读取独立消息，支持分类筛选、单条已读、全部已读、未读角标，并携带受控业务目标跳转订单、任务、审核、异常或投诉详情；管理员后台增加平台待办消息。App 推送、短信和消息模板编排仍由真实后端与推送服务实现。

### 第四阶段：平台运营

- [ ] 智能推荐或自动派单。
- [x] 商户结算和护理人员佣金（前端分账快照、冻结金额、提现/结算记录；真实财务系统待后端）。
- [x] 护理人员可编辑排班、请假、每日接单上限和服务区域；商户派单展示时间、区域和上限冲突。真实地图路程仍待后端。
- [x] 经营报表、服务质量指标和异常订单监控（前端 Mock 统计口径与风险事项跳转；真实数仓和报表服务待后端）。
- [ ] 风控、黑名单、资质到期和账号风险控制。

## 16. 第一批开发任务建议

在开始大量页面开发前，优先确定以下基础协议：

1. 账号、角色、商户成员和护理人员档案的数据关系。
2. 登录时选择身份、后端验证身份、登录后切换身份的接口协议。
3. 新订单、支付和派单状态及其合法迁移表。
4. 商户人工派单和护理人员接单的最小 API。
5. 服务、地址、价格和护理人员信息的订单快照结构。
6. 金额单位、时间格式、幂等请求头和错误码规范。

建议第一批代码按以下顺序落地：

```text
角色模型和登录协议
→ 登录页身份选择与 role Store
→ 护理人员/商户分包和自定义角色导航
→ 订单状态模型、派单记录和履约记录升级
→ 护理人员任务与履约最小闭环
→ 商户服务管理和人工派单
→ 顾客订单时间线与确认页
→ 独立管理后台审核和异常处理
```

该顺序先稳定权限和状态模型，再开发各角色工作台，可以减少后续因接口、状态和数据归属变化造成的返工。

第一阶段保留完整的多角色账号契约：`targetRole`、`roles[]` 和 `currentRole`。为了方便 Mock 测试，可以准备不同角色的测试账号，但这只是测试数据安排，不把正式账号模型降级为“一手机号固定一个角色”。登录后的 `switch-role` 已实现；一个账号加入多个商户、复杂岗位权限和结算能力仍可暂缓实现。

公开注册只创建顾客身份。护理人员身份通过资质申请获得，商户身份通过商户入驻或成员邀请获得，管理员身份仅由平台内部授予。

### 16.1 本轮前端开发范围

当前仓库本轮只实现前端能力：

- Uni-app 页面、组件、Store、本地存储和前端权限体验。
- Mock.js 测试账号、角色校验和业务模拟。
- 为真实后端预留的请求参数、响应字段和 API 契约。

本轮不实现数据库、后端鉴权、真实 API、消息推送服务、部署和结算服务。管理员独立 Web 后台暂保留架构方案，待移动端最小闭环稳定后再单独搭建。

### 16.2 第二阶段前端实现记录

第二阶段已完成业务状态和接口基础，不包含商户、护理人员的完整操作页面：

- 新增订单主状态、支付状态、派单状态和合法迁移定义。
- 保留旧 `status=0..5` 映射，兼容现有顾客订单筛选、评价和投诉流程。
- Mock 支持候选护理人员、派单/改派、接单/拒单、出发、签到、开始和结束服务。
- 新增 `dispatch` 和 `work-order` Store，供下一阶段角色页面直接调用。
- 顾客订单详情增加服务方、派单记录、服务记录和新状态时间线。
- 顾客确认接口调整为 `POST /api/v1/orders/{orderId}/confirm`，Mock 暂时兼容旧 `/complete` 路径。

### 16.3 护理人员端页面实现记录

- 新增工作台、任务列表、任务详情、完成服务、排班和个人中心页面。
- 新增护理人员分包自定义底部导航，不影响顾客原生 TabBar。
- 任务列表支持待接单、今日、进行中和已完成筛选。
- 任务详情根据派单和履约状态只显示合法操作按钮。
- 到达签到优先读取定位；定位不可用时允许提交异常签到记录。
- 开始服务前必须已经签到，结束服务前必须填写服务摘要。
- 当前排班和个人数据为前端演示，后续接入护理人员资料及排班 API。

### 16.4 商户端页面实现记录

- 新增商户工作台、订单列表、订单详情、派单、服务概览和商户中心页面。
- 商户工作台展示待派单、待接单、今日服务、服务中和模拟经营统计。
- 商户订单支持待派单、待接单、今日、待服务、服务中和已完成筛选。
- 候选护理人员由 Mock 按资质和可用状态返回，支持首次派单和拒单后的重新派单。
- 商户订单、详情、候选人员和派单接口按当前 Token 的 `merchantId` 做数据隔离。
- 服务管理当前为状态概览，完整服务草稿、规格编辑和审核流程留到下一阶段。

### 16.5 商户服务发布实现记录

- 商户可创建服务草稿，设置分类、服务说明、封面地址和最多 6 个服务规格。
- 草稿及已驳回服务允许编辑，审核中的服务锁定编辑。
- 提交后进入 `PENDING_REVIEW`；Mock 约 8 秒后在下一次刷新时模拟管理员审核通过。
- 审核通过后商户可主动上架，已上架服务可下架。
- 公开服务目录始终只返回已上架服务，草稿、审核中和驳回服务只在商户端可见。
- 审核记录保存提交、通过或驳回的操作者、说明和时间。
- 当前审核通过由 Mock 模拟，正式管理员独立 Web 后台仍未开发。

### 16.6 护理人员认证页面实现记录

- 登录页和顾客“我的”页可进入护理人员认证，未登录时先引导登录或注册个人账号。
- 申请资料覆盖真实姓名、身份证、从业年限、个人简介、服务区域、服务技能和多项资质证书。
- 支持保存草稿、驳回后修改、重新提交、审核记录和审核结果展示。
- 提交后进入 `PENDING_REVIEW`；Mock 约 8 秒后在下一次刷新时模拟管理员审核通过并授予护理人员角色。
- 已认证护理人员可查看完整业务档案；真实管理员审核、证书 OCR 与实名认证仍待后端接入。

### 16.7 商户入驻页面实现记录

- 登录页和顾客“我的”页可进入商户入驻，未登录时先引导登录或注册负责人个人账号。
- 入驻资料覆盖企业主体、统一社会信用代码、法定代表人、负责人、营业执照、经营许可、经营地址和服务区域。
- 支持保存草稿、驳回后修改、重新提交、审核记录和审核结果展示；结算字段仅作为前端契约演示。
- 提交后进入 `PENDING_REVIEW`；Mock 约 8 秒后在下一次刷新时模拟审核通过、创建商户组织并将当前账号设为负责人。
- 已认证商户中心接入商户档案；真实企业认证、附件存储、结算账户加密和管理员审核仍待后端接入。

### 16.8 独立管理后台第一期实现记录

- 新增独立 `admin/` 工程，采用 Vue 3、Vite、Element Plus、Vue Router、Pinia 和 Axios，不依赖 Uni-app 运行时。
- 管理员使用独立登录页和会话存储，Mock 账号为 `admin / 123456`，不开放自主注册。
- 审核工作台汇总待审核商户、护理人员和服务，并展示按提交时间排序的优先处理队列。
- 三类审核均支持筛选、搜索、详情查看、通过、驳回原因和操作时间线。
- 管理端 Mock 独立维护审核队列；正式接入后统一调用 `/api/v1/admin/*` 接口并由后端执行权限和审计校验。

### 16.9 登录后身份切换实现记录

- 新增统一身份切换页，只展示当前账号已获批的顾客、护理人员和商户身份。
- 进入页面时调用 `GET /api/v1/profile/roles` 刷新角色资格，解决审核通过后本地角色列表未及时更新的问题。
- 切换时调用 `POST /api/v1/auth/switch-role`，由后端或 Mock 重新签发 Token、当前角色和权限集合，不仅修改前端变量。
- 顾客“我的”、护理人员中心和商户中心均提供切换入口；审核通过后也可直接进入切换页，无需退出登录。
- 未开通的护理人员和商户身份在切换页显示申请入口，但不能直接进入对应工作台。

### 16.10 商户团队与成员管理实现记录

- 商户团队管理将护理人员合作关系与商户内部成员岗位分开建模，避免护理履约身份和运营岗位混淆。
- 护理团队支持合作中、待确认、已停用筛选，展示技能、服务区域、证书、排班概览和当前任务数。
- 商户可以按手机号邀请已认证护理人员，并查看合作记录、暂停或恢复合作；待确认邀请不会进入派单候选列表。
- 商户内部成员支持负责人、运营、调度员和财务岗位，当前实现邀请、启用和停用的前端流程。
- 商户派单候选人改为从当前商户“合作中且可接单”的护理人员生成，团队状态会直接影响订单调度。

### 16.11 售后与异常处理第一期实现记录

- 顾客取消前先获取规则预览，分别展示未支付直接关闭、未派单全额退款和已接单扣除调度成本的前端示例。
- 退款状态与订单主状态分离，顾客订单详情展示退款金额、扣除金额、申请记录和 Mock 退款进度。
- 派单响应到期后自动标记 `EXPIRED` 并生成接单超时异常；没有可用护理人员时生成无人可派异常。
- 护理人员提交定位异常签到后生成独立异常工单，商户可确认有效或驳回并升级平台复核。
- 商户新增异常中心和处理详情；管理员 Web 新增异常监管、结案和批准退款的演示流程。
- 当前规则、退款完成、超时扫描和跨端异常数据均由 Mock 模拟，真实环境必须由后端状态机、定时任务和支付渠道执行。
