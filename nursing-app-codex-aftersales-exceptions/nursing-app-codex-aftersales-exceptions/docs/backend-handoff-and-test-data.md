# 智慧护理平台后端交接与测试数据说明

> 适用对象：后端开发、测试、前端联调人员  
> 项目位置：`E:\nursing-app`  
> 文档日期：2026-07-16  
> 前端基线提交：`e318332`  
> 当前范围：App 前端（Uni-app）+ 独立管理后台（Vue 3）+ Mock 接口  
> 交接原则：Mock 用于确认前端需要的接口形状和业务场景，不代表最终生产规则。

> **契约更新说明（2026-07-16）：** HTTP 联调的唯一有效标准是 [`API接口文档.md`](./API接口文档.md)。本文后续列出的扩展接口和 Mock 路径仅代表前端产品规划，凡与权威契约冲突均不得用于真实后端调用。

## 1. 交接目标

前端已经完成顾客、护理人员、商户和管理员的主要页面与 Mock 闭环。后端需要逐步使用真实接口替换 Mock，并负责：

- 数据库存储与事务一致性；
- JWT 鉴权、角色校验和商户数据隔离；
- 订单、支付、派单、履约、退款、结算等状态机；
- 幂等、并发控制、审计日志和安全保护；
- 文件存储、消息推送、支付渠道、财务系统和报表聚合。

前端扩展总方案见 [`multi-role-platform-extension-plan.md`](./multi-role-platform-extension-plan.md)。真实 HTTP 请求以 [`API接口文档.md`](./API接口文档.md) 为准。

## 2. 当前系统边界

### 2.1 客户端

| 客户端 | 技术栈 | 使用角色 |
|---|---|---|
| App/H5 | Uni-app + Vue 3 + Pinia | 顾客、护理人员、商户成员 |
| 管理后台 | Vue 3 + Vite + Element Plus | 平台管理员 |

### 2.2 角色模型

| 角色 | 枚举 | 说明 |
|---|---|---|
| 顾客 | `CUSTOMER` | 浏览服务、下单、支付、确认完成、评价、投诉 |
| 护理人员 | `CAREGIVER` | 认证、接单、履约、排班、佣金提现 |
| 商户成员 | `MERCHANT_MEMBER` | 服务、团队、订单、派单、售后、结算、报表 |
| 管理员 | `ADMIN` | 审核、仲裁、异常监管、平台消息和后续风控 |

正式账号模型采用“一账号可拥有多个角色”：

```text
user
  -> roles[]
  -> currentRole
  -> caregiverId（开通护理人员身份时）
  -> merchantId + memberId + position（加入商户时）
```

登录请求传 `targetRole`，后端必须验证账号是否拥有该角色。身份切换也必须由后端重新校验，建议签发包含当前角色的新 Token，不能只依赖前端修改 `currentRole`。

## 3. 统一 API 规范

### 3.1 地址与环境

```text
开发环境 Gateway：http://192.168.57.85（80 端口）
H5 联调代理前缀：/backend
API 前缀：/api/v1
```

生产地址由环境变量 `VITE_API_BASE_URL` 注入。

### 3.2 响应格式

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败：

```json
{
  "code": 3007,
  "message": "订单不存在",
  "data": null
}
```

分页：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 120,
    "page": 1,
    "size": 20
  }
}
```

### 3.3 请求头

```http
Authorization: Bearer <token>
Content-Type: application/json
Idempotent-Key: <unique-key>
```

下单、取消、支付、派单、接单、提交审核、仲裁、退款和提现等写操作必须支持 `Idempotent-Key`。同一用户、接口和幂等键重复请求应返回同一业务结果，不得生成重复记录。

### 3.4 金额与时间

- 数据库金额使用 `DECIMAL` 或整数分，不使用浮点数持久化金额；
- 接口若返回元，统一保留两位小数，并在 API 文档标明单位；
- 时间统一使用 ISO 8601，例如 `2026-07-16T10:30:00+08:00`；
- 所有列表排序、超时、退款完成和结算周期由后端时间决定。

### 3.5 鉴权和数据隔离

后端每次请求都需要验证：

1. Token 有效且未被撤销；
2. Token 当前角色允许访问该接口；
3. `merchantId`、`caregiverId`、`userId` 从认证上下文获取，不能信任客户端提交；
4. 商户成员只能操作所属商户数据；
5. 护理人员只能操作自己的任务；
6. 顾客只能访问自己的地址、订单、评价和投诉；
7. 敏感操作需要权限点和审计日志。

## 4. 登录和角色接口契约

登录请求示例：

```json
{
  "phone": "13800138000",
  "loginMode": "password",
  "password": "123456",
  "targetRole": "CAREGIVER"
}
```

登录返回至少包含：

```json
{
  "token": "jwt-token",
  "expireTime": "2026-07-23T10:00:00+08:00",
  "roles": ["CUSTOMER", "CAREGIVER"],
  "currentRole": "CAREGIVER",
  "permissions": ["caregiver:task:view", "caregiver:task:operate"],
  "user": {
    "userId": 10001,
    "nickname": "测试用户",
    "phone": "138****8000",
    "caregiverId": 50001,
    "merchantId": null
  }
}
```

关键接口：

| 方法 | 路径 | 用途 |
|---|---|---|
| `POST` | `/api/v1/users/sms-code` | 获取验证码 |
| `POST` | `/api/v1/users/register` | 注册顾客账号 |
| `POST` | `/api/v1/users/login` | 按 `targetRole` 登录 |
| `POST` | `/api/v1/users/logout` | 退出并撤销 Token |
| `POST` | `/api/v1/users/password/reset` | 重置密码 |
| `GET/PATCH` | `/api/v1/users/profile` | 获取或修改资料 |
| `GET` | `/api/v1/profile/roles` | 刷新账号已开通角色 |
| `POST` | `/api/v1/auth/switch-role` | 切换当前角色并返回新会话 |
| `POST` | `/api/v1/files/upload` | 上传附件 |
| `POST` | `/api/v1/admin/login` | 管理员登录 |

公开注册只创建 `CUSTOMER`。护理人员和商户身份必须通过申请、审核后授权。

## 5. 订单核心状态模型

订单、支付和派单必须分开维护，不能使用一个数字状态同时表达三类含义。

### 5.1 订单状态 `orderStatus`

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

主流程：

```text
CREATED
  -> WAITING_DISPATCH
  -> WAITING_SERVICE
  -> IN_SERVICE
  -> WAITING_CONFIRM
  -> COMPLETED
```

投诉或履约争议可使 `IN_SERVICE`、`WAITING_CONFIRM` 或已完成订单进入 `DISPUTED`。争议解决后由后端根据裁决恢复、完成、取消或关闭。

### 5.2 支付状态 `paymentStatus`

```text
UNPAID
PAID
REFUNDING
PARTIALLY_REFUNDED
REFUNDED
```

### 5.3 派单状态 `assignmentStatus`

```text
UNASSIGNED
WAITING_ACCEPT
ACCEPTED
REJECTED
EXPIRED
CANCELED
```

### 5.4 履约动作

```text
DEPART
CHECK_IN
START
FINISH
```

后端必须校验动作顺序，例如未签到不能开始，未开始不能结束；所有动作写入不可覆盖的服务记录与操作日志。

## 6. 接口模块清单

以下是前端当前实际依赖的主要接口。详细请求字段和返回结构可以直接参考对应 Store 与 Mock。

本节及后续模块表格为简洁起见省略统一前缀 `/api/v1`；例如 `/orders` 的完整路径为 `/api/v1/orders`。

### 6.1 服务与地址

| 模块 | 主要接口 |
|---|---|
| 服务发现 | `GET /categories`、`GET /items`、`GET /items/{id}` |
| 地址 | `GET/POST /addresses`、`PATCH/DELETE /addresses/{id}`、`PUT /addresses/{id}/default` |
| 商户服务 | `GET/POST /merchants/services`、`GET/PUT /merchants/services/{id}`、`POST /submit`、`POST /publish`、`POST /offline` |

### 6.2 下单、支付与顾客订单

| 方法 | 路径 |
|---|---|
| `POST` | `/api/v1/orders/prepay-token` |
| `POST` | `/api/v1/orders` |
| `GET` | `/api/v1/orders` |
| `GET` | `/api/v1/orders/{orderId}` |
| `POST` | `/api/v1/orders/{orderId}/pay` |
| `POST` | `/api/v1/orders/{orderId}/cancel` |
| `POST` | `/api/v1/orders/{orderId}/confirm` |
| `GET` | `/api/v1/orders/{orderId}/cancel-preview` |
| `GET` | `/api/v1/orders/{orderId}/aftersales` |

创建订单时后端应保存服务名称、规格、价格、商户、地址等订单快照，避免后续服务资料修改影响历史订单。

### 6.3 商户订单与派单

| 方法 | 路径 |
|---|---|
| `GET` | `/api/v1/merchants/dashboard` |
| `GET` | `/api/v1/merchants/orders` |
| `GET` | `/api/v1/merchants/orders/{orderId}` |
| `GET` | `/api/v1/merchants/orders/{orderId}/candidates` |
| `GET` | `/api/v1/merchants/orders/{orderId}/assignments` |
| `POST` | `/api/v1/merchants/orders/{orderId}/dispatch` |
| `POST` | `/api/v1/merchants/orders/{orderId}/redispatch` |

候选人员接口应返回不可派原因，例如时间冲突、服务区域不符、当日接单上限、合作关系失效、资质过期或账号停用。

### 6.4 护理人员认证、排班与履约

| 模块 | 主要接口 |
|---|---|
| 认证 | `GET/PUT /caregivers/application`、`POST /caregivers/apply`、`GET /caregiver/profile` |
| 排班 | `GET/PUT /caregiver/schedule` |
| 任务 | `GET /caregivers/tasks`、`GET /caregivers/tasks/{orderId}` |
| 接拒单 | `POST /caregivers/assignments/{id}/accept`、`POST /reject` |
| 履约 | `POST /caregivers/orders/{orderId}/{depart|check-in|start|finish}` |

### 6.5 商户入驻与团队

| 模块 | 主要接口 |
|---|---|
| 入驻 | `GET/PUT /merchants/application`、`POST /merchants/apply`、`GET /merchant/profile` |
| 护理团队 | `GET /merchant/caregivers`、`GET /merchant/caregivers/{id}`、`POST /invite`、`POST /{id}/{suspend|resume}` |
| 商户成员 | `GET /merchant/members`、`POST /merchant/members/invite`、`POST /merchant/members/{id}/{disable|enable}` |

建议商户内部岗位至少包括：

```text
OWNER
OPERATOR
DISPATCHER
FINANCE
```

具体权限必须由后端权限表控制，不能只靠岗位名称判断。

### 6.6 评价、投诉、异常与退款

| 模块 | 主要接口 |
|---|---|
| 评价 | `POST /reviews`、`GET /reviews` |
| 顾客投诉 | `POST /complaints`、`GET /complaints`、`GET /complaints/{id}/tracks` |
| 商户投诉 | `GET /merchant/complaints`、`GET /merchant/complaints/{id}`、`POST /{id}/{resolve|escalate}` |
| 商户异常 | `GET /merchant/exceptions`、`GET /merchant/exceptions/{id}`、`POST /{id}/{action}` |
| 管理员投诉 | `GET /admin/complaints`、`GET /admin/complaints/{id}`、`POST /{id}/arbitrate` |
| 管理员异常 | `GET /admin/exceptions`、`GET /admin/exceptions/{id}`、`POST /{id}/{resolve|approve-refund}` |

退款必须是独立状态。投诉进入处理中后应冻结相关结算；管理员裁定部分或全额退款时，由后端事务或可靠事件驱动退款及结算调整。

### 6.7 通知

App 与管理员分别使用：

```text
GET   /notifications
GET   /notifications/unread-count
PATCH /notifications/{id}/read
PATCH /notifications/read-all

GET   /admin/notifications
GET   /admin/notifications/unread-count
PATCH /admin/notifications/{id}/read
PATCH /admin/notifications/read-all
```

消息必须按 `userId + currentRole` 隔离，并使用受控的业务类型和资源 ID，不能接受任意外部 URL 跳转。

### 6.8 结算与佣金

```text
GET  /caregiver/earnings
POST /caregiver/withdrawals
GET  /merchant/settlements
POST /merchant/withdrawals
```

当前 Mock 演示比例：

```text
平台 10%
护理人员 70%
商户 20%
```

该比例不是生产规则。后端需确认：比例配置层级、生效时间、结算周期、税费、退款调整、提现审核、手续费和对账规则。

建议在订单完成时生成不可变的分账快照：

```text
grossAmount
refundedAmount
netAmount
platformFee
caregiverCommission
merchantIncome
splitRuleSnapshot
settlementStatus
```

争议、退款中和未完成订单不可提现。处理中和已成功提现都要占用可用余额，避免重复提现。

### 6.9 商户经营报表

```text
GET /merchant/reports/operations?range=7d|30d|all
```

当前页面需要：订单量、支付订单量、完成订单量、订单净额、平均客单价、完成率、平均评分、好评率、投诉率、异常率、热门服务、日趋势和待处理风险事项。

Mock 口径仅用于前端展示，正式环境建议由数据库聚合、只读副本或报表服务提供，并明确各指标的时间字段、时区、退款口径和去重规则。

## 7. 管理员审核接口

管理后台当前使用：

```text
GET  /admin/dashboard
GET  /admin/reviews/{merchant|caregiver|service}
GET  /admin/reviews/{type}/{id}
POST /admin/reviews/{type}/{id}/{approve|reject}
```

后端需要保存：申请版本、提交记录、审核人、审核时间、审核意见、附件快照和状态迁移记录。同一待办只允许处理一次，并处理并发审核冲突。

## 8. 主要状态枚举

### 8.1 审核状态

商户：

```text
NOT_APPLIED DRAFT PENDING_REVIEW APPROVED REJECTED DISABLED
```

护理人员：

```text
NOT_APPLIED DRAFT PENDING_REVIEW APPROVED REJECTED
```

服务审核：

```text
DRAFT PENDING_REVIEW APPROVED REJECTED
```

服务发布：

```text
OFFLINE PUBLISHED
```

### 8.2 售后状态

退款：

```text
NONE NOT_REQUIRED PENDING PROCESSING SUCCESS REJECTED
```

异常类型：

```text
ASSIGNMENT_TIMEOUT NO_CAREGIVER ABNORMAL_CHECK_IN
```

异常状态：

```text
OPEN MERCHANT_PROCESSING ADMIN_REVIEW RESOLVED CLOSED
```

投诉状态：

```text
0 SUBMITTED
1 MERCHANT_PROCESSING
2 RESOLVED
3 CLOSED
4 ADMIN_REVIEW
```

投诉裁决：

```text
FULL_REFUND PARTIAL_REFUND NO_REFUND
```

### 8.3 结算状态

```text
FROZEN AVAILABLE SETTLING SETTLED
```

提现状态：

```text
PROCESSING SUCCESS REJECTED
```

## 9. 开发与测试账号

以下账号仅用于 `dev/test` 环境，生产环境严禁保留固定密码或万能验证码。

| 场景 | 账号 | 密码 | 固定标识 | 说明 |
|---|---|---|---|---|
| 多角色账号 | `13800138000` | `123456` | `userId=10001`、`caregiverId=50001` | 可选择顾客或护理人员 |
| 单角色护理人员 | `13800138001` | `123456` | `userId=10002`、`caregiverId=50002` | 护理人员任务测试 |
| 商户负责人 | `13800138002` | `123456` | `userId=10003`、`merchantId=20001` | 康宁护理中心 |
| 护理申请人 | `13800138003` | `123456` | `userId=10004` | 内置驳回申请场景 |
| 商户申请人 | `13800138004` | `123456` | `userId=10005` | 内置驳回申请场景 |
| 管理员 | `admin` | `123456` | `adminId=1` | 平台管理员 |

Mock 短信验证码统一为 `123456`。真实短信服务在开发环境可以配置固定验证码，但必须限制环境、手机号白名单和使用频率。

固定 ID 只为联调排查方便，正式业务代码不得硬编码这些 ID。

## 10. Seed 数据最低要求

后端建议提供可重复执行的 Seeder 或 `seed-data.sql`，至少初始化以下数据。

### 10.1 用户、角色和组织

- 上表全部测试账号；
- 一个多角色账号；
- 一个已认证商户 `merchantId=20001`；
- 两名已认证且与商户有效合作的护理人员；
- 商户负责人、运营、调度、财务和一名停用成员；
- 待确认、正常合作、暂停合作、已解除的护理人员关系。

### 10.2 服务数据

- 至少 3 个分类、5 个已上架服务；
- 单规格、多规格服务各一项；
- 草稿、待审核、驳回、审核通过未上架、已上架、已下架各一项；
- 至少两个商户的数据，用于验证租户隔离；
- 服务时长、技能要求、区域、图片和价格完整。

### 10.3 订单状态样本

每个状态至少一单：

| 场景 | `orderStatus` | `paymentStatus` | `assignmentStatus` |
|---|---|---|---|
| 待支付 | `CREATED` | `UNPAID` | `UNASSIGNED` |
| 待派单 | `WAITING_DISPATCH` | `PAID` | `UNASSIGNED` |
| 等待接单 | `WAITING_DISPATCH` | `PAID` | `WAITING_ACCEPT` |
| 待上门 | `WAITING_SERVICE` | `PAID` | `ACCEPTED` |
| 服务中 | `IN_SERVICE` | `PAID` | `ACCEPTED` |
| 待确认 | `WAITING_CONFIRM` | `PAID` | `ACCEPTED` |
| 已完成 | `COMPLETED` | `PAID` | `ACCEPTED` |
| 已取消 | `CANCELED` | `UNPAID` 或 `REFUNDING` | `CANCELED` |
| 已退款 | `CLOSED` | `REFUNDED` | `CANCELED` |
| 争议中 | `DISPUTED` | `PAID` 或 `REFUNDING` | `ACCEPTED` |

额外准备：拒单、接单超时、无人可派、排班冲突、区域不匹配、当日上限、正常签到、异常签到、部分退款和投诉冻结。

### 10.4 售后与财务

- 未派单取消并全额退款；
- 已接单取消并按测试规则扣费；
- 退款处理中、成功、驳回；
- 投诉待响应、商户处理、平台仲裁、已裁决；
- 不退款、部分退款、全额退款；
- 可结算、未完成冻结、投诉冻结、退款冻结；
- 护理人员和商户提现的处理中、成功、驳回；
- 一笔可用于实际发起提现的剩余余额。

## 11. 联调验收场景

### 11.1 最小主闭环

```text
顾客登录
-> 浏览服务和规格
-> 选择地址、日期、时段
-> 创建订单并支付
-> 商户查看订单并派单
-> 护理人员接单
-> 出发、签到、开始、结束
-> 顾客确认完成
-> 生成评价资格、分账快照和通知
```

### 11.2 异常闭环

```text
商户派单
-> 护理人员拒单或超时
-> 创建异常工单
-> 商户重新派单
-> 新护理人员完成履约
```

### 11.3 投诉退款闭环

```text
顾客提交投诉
-> 订单进入 DISPUTED
-> 结算冻结
-> 商户提交说明和证据
-> 升级平台仲裁
-> 管理员裁决
-> 退款完成
-> 订单、支付、投诉、结算和通知同步更新
```

### 11.4 多角色与隔离

- `13800138000` 以顾客和护理人员分别登录；
- 切换角色后旧角色接口不可继续访问；
- 商户不能查看其他商户服务、订单、成员和财务；
- 护理人员不能操作其他人的任务；
- 顾客不能查看其他用户地址和订单；
- 管理员 Token 不能直接作为 App 商户 Token 使用。

## 12. 边界、安全与并发测试

后端和测试至少覆盖：

- 无 Token、Token 过期、Token 被撤销、角色不匹配；
- 越权访问其他用户、护理人员或商户数据；
- 重复下单、重复取消、重复派单、重复接单、重复仲裁、重复提现；
- 同一订单被两个商户成员同时派单；
- 同一护理人员同一时间被多个订单占用；
- 金额为 `0`、负数、非数字、超过余额或精度异常；
- 非法状态跳转和动作乱序；
- 退款与提现并发，投诉与结算并发；
- 分页为空、最后一页、页码越界和最大 `size`；
- 文件为空、超限、扩展名伪造、MIME 不匹配和恶意文件；
- 身份证、手机号、银行卡、地址、医疗信息脱敏；
- 审核、派单、履约、退款、提现和管理员操作的审计日志。

## 13. 需要产品、前端和后端共同确认的规则

以下 Mock 行为不能直接作为生产规则：

1. 分账比例、结算周期、税费和提现手续费；
2. 取消时的扣款比例和可取消时间；
3. 接单超时时长和自动改派策略；
4. 签到距离阈值、定位异常处理和隐私授权；
5. 投诉受理、商户响应和平台仲裁时限；
6. 资质有效期、到期提醒和过期停用规则；
7. 商户岗位权限和敏感操作复核；
8. 自动派单或推荐算法；
9. 支付、退款、分账、提现渠道及对账方式；
10. 报表指标定义、统计时间和退款归属；
11. 消息渠道、模板、重试、去重和已读同步；
12. 文件存储、病毒扫描、访问权限和保存周期。

## 14. 后端建议交付物

后端每个模块建议同时交付：

- OpenAPI/Swagger 或 Apifox 接口定义；
- 数据库迁移和可重复执行的 Seeder；
- 开发环境测试账号；
- Postman/Apifox 联调集合；
- 枚举、错误码和状态迁移说明；
- 幂等、事务和并发策略说明；
- 文件、支付、消息等外部依赖的本地替代方案；
- 自动化接口测试和最小闭环测试报告。

## 15. 推荐实现与联调顺序

| 优先级 | 模块 | 验收目标 |
|---|---|---|
| P0 | 登录、角色、权限、用户资料 | 不同角色可登录，越权被拒绝 |
| P0 | 服务、地址、下单、订单查询 | 顾客可创建真实订单 |
| P0 | 商户订单、派单、护理履约 | 完成核心履约闭环 |
| P1 | 认证、商户服务审核、管理员审核 | 身份和服务可真实开通 |
| P1 | 取消、退款、异常、投诉仲裁 | 完成售后闭环 |
| P1 | 通知 | 各角色收到业务状态提醒 |
| P2 | 结算、佣金、提现 | 完成财务账本与冻结逻辑 |
| P2 | 报表 | 后端提供统一统计口径 |
| P3 | 风控、黑名单、资质到期、自动派单 | 完成运营增强能力 |

## 16. 代码参考位置

| 内容 | 路径 |
|---|---|
| 网络层与错误码 | `src/utils/request.js` |
| 角色模型 | `src/constants/roles.js` |
| 订单状态 | `src/constants/order-status.js` |
| 售后状态 | `src/constants/aftersales-status.js` |
| 结算状态 | `src/constants/settlement.js` |
| App 请求调用 | `src/store/` |
| App Mock | `src/mock/` |
| 管理端请求调用 | `admin/src/stores/` |
| 管理端 Mock | `admin/src/mock/index.js` |
| 路由和页面范围 | `src/pages.json`、`admin/src/router/index.js` |
| 完整扩展规划 | `docs/multi-role-platform-extension-plan.md` |

后端接口字段与前端不一致时，应先更新 OpenAPI 契约并同步前端，不建议在双方代码中分别维护隐式字段转换。
