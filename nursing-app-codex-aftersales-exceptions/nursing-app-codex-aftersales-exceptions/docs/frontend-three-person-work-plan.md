# 智慧护理平台多角色扩展：前端三人分工方案

> 适用范围：多角色登录、商户工作台、护理人员工作台、平台审核端 MVP  
> 依据文档：[多角色与业务闭环扩展方案](./multi-role-platform-extension-plan.md)  
> 编写日期：2026-07-15

## 1. 分工目标

本方案用于三名前端开发者并行完成智慧护理平台的多角色扩展，目标是跑通以下最小业务闭环：

```text
商户申请并提交服务
→ 管理员审核
→ 顾客预约并支付
→ 商户派单
→ 护理人员接单和上门服务
→ 顾客确认完成、评价或投诉
```

当前项目已经具备顾客端首页、服务发现、地址、预约、支付、订单、评价和投诉页面，因此本轮不重复开发已有页面，重点新增角色基础设施以及商户、护理人员和管理员功能，并升级现有订单展示以适配派单和履约状态。

## 2. 分工原则

- 按业务域分工，每个人完整负责自己领域的页面、Store、Mock 和测试。
- 公共登录、权限和订单状态协议先确定，再并行开发角色工作台。
- 避免三人同时修改 `src/pages.json`、`src/mock/index.js`、`src/store/user.js` 等公共文件。
- 每个业务域通过约定的数据模型和 Store 暴露能力，不直接读取其他业务域内部状态。
- 第一阶段以 Mock 跑通闭环为验收标准，接口结构同时对齐后端协议。
- 管理员正式版本使用独立 Web 后台；本轮先完成审核与订单监管 MVP，不扩展为完整运营平台。

## 3. 三人职责总览

| 人员 | 责任域 | 核心交付 | 建议工作量 |
|---|---|---|---:|
| Person A | 公共基础、身份权限、顾客端整合、项目集成 | 多角色登录、角色切换、权限守卫、订单状态适配、公共组件、最终联调 | 34% |
| Person B | 商户端 | 入驻、服务管理、护理人员管理、订单管理、派单与改派 | 33% |
| Person C | 护理人员端、管理员审核 MVP | 护理认证、任务履约、服务记录、审核列表、投诉与异常监管 | 33% |

Person A 同时担任前端集成负责人，但不代替其他成员完成其业务域。公共协议由三人共同评审，最终代码归 Person A 维护。

## 4. Person A：公共基础、身份权限与顾客端整合

### 4.1 负责范围

Person A 负责所有角色共用的运行基础，以及现有顾客端与新订单状态模型的兼容。

#### 身份与登录

- 登录页增加“顾客 / 护理人员 / 商户”身份选择。
- 登录请求增加 `targetRole`。
- 登录响应接入 `roles`、`currentRole`、`permissions`、`merchantId` 和 `caregiverId`。
- 保存上一次成功登录的身份。
- 没有所选身份时展示申请或切换引导。
- “我的”页面增加当前身份展示和身份切换。
- 切换身份后重新获取 Token、用户资料和角色权限。
- 登出时清理角色、权限和各业务域敏感数据。

#### 权限和导航

- 新增 `role` Store。
- 新增角色、权限和页面访问判断工具。
- 提供未登录、无角色、角色审核中、角色被停用等统一处理。
- 设计统一“工作台”入口，根据当前角色跳转不同首页。
- 维护 `src/pages.json` 和分包配置，集中合并其他成员提交的页面路由。

#### 顾客订单适配

- 将现有订单页面适配为订单主状态、支付状态和派单状态三维模型。
- 订单详情增加商户、护理人员、派单和服务履约时间线。
- 增加“等待派单、等待接单、护理人员出发、已到达、服务中、待确认”等展示状态。
- 根据状态显示取消、确认、评价、投诉等操作。
- 兼容现有数字状态 Mock，建立统一状态映射层，避免页面散落兼容逻辑。

#### 公共能力

- 角色工作台外壳、状态标签、订单时间线和权限空状态组件。
- 统一枚举、状态文案、日期时间和金额格式化工具。
- 公共 API 错误处理和角色失效处理。
- 多角色流程的集成测试和最终构建验证。

### 4.2 建议新增文件

```text
src/store/role.js
src/utils/auth.js
src/utils/permissions.js
src/constants/roles.js
src/constants/order-status.js
src/composables/use-role-guard.js
src/composables/use-order-status.js
src/components/base/role-switcher.vue
src/components/base/status-tag.vue
src/components/base/order-timeline.vue
src/components/base/permission-empty.vue
src/pages/workbench/workbench.vue
src/mock/role.js
```

### 4.3 主要修改文件

```text
src/pages/login/login.vue
src/pages/account/account.vue
src/pages/order/order-list.vue
src/pages/order/order-detail.vue
src/store/user.js
src/store/order.js
src/mock/user.js
src/mock/order.js
src/mock/index.js
src/pages.json
```

### 4.4 交付接口

Person A 需要向 Person B 和 Person C 提供稳定的公共调用方式：

```js
const roleStore = useRoleStore()

roleStore.currentRole
roleStore.roles
roleStore.permissions
roleStore.hasRole('CAREGIVER')
roleStore.hasPermission('merchant:order:dispatch')
roleStore.switchRole('CUSTOMER')
```

并提供统一枚举：

```js
ROLE.CUSTOMER
ROLE.MERCHANT_MEMBER
ROLE.CAREGIVER

ORDER_STATUS.WAITING_DISPATCH
PAYMENT_STATUS.PAID
ASSIGNMENT_STATUS.WAITING_ACCEPT
```

### 4.5 验收标准

- 三种身份均可从同一登录页选择并进入对应工作台。
- 用户不能通过修改本地状态进入未授权角色页面。
- 登录后可在已有身份之间切换，切换后页面和权限立即刷新。
- 现有顾客登录、服务浏览、预约、支付和订单功能不回归。
- 顾客订单详情能完整展示派单和履约时间线。
- 旧订单状态和新订单状态均能正确渲染。
- 公共权限和状态工具具有单元测试。

## 5. Person B：商户工作台

### 5.1 负责范围

Person B 负责商户从入驻到服务发布、人员管理和订单派单的完整前端流程。

#### 商户入驻与资料

- 商户入驻申请表。
- 营业执照和相关资料上传。
- 商户审核状态、驳回原因和重新提交。
- 商户资料查看与编辑。
- 当前商户切换；第一期可仅支持一个商户，但数据结构保留多商户能力。

#### 商户服务管理

- 服务列表、状态筛选和搜索。
- 新建和编辑服务草稿。
- 分类、名称、介绍、封面、轮播图和服务区域设置。
- 服务规格、价格、原价、时长和适用资质配置。
- 提交审核、查看审核意见、重新提交、上架和下架。
- 已发布服务的重要内容修改按版本提交，页面不直接覆盖历史版本。

#### 护理人员管理

- 商户护理人员列表和详情。
- 在职、停用、离职及资质状态筛选。
- 邀请或确认护理人员加入商户。
- 查看技能、服务区域、排班和资质有效期。
- 启用、停用和移出商户；高风险操作二次确认。

#### 商户订单与派单

- 商户订单列表、筛选、详情和状态统计。
- 待派单、待接单、待服务、服务中和异常订单视图。
- 查询候选护理人员。
- 展示候选人的技能匹配、距离、排班和当前任务情况。
- 人工派单、取消派单和改派。
- 查看护理人员拒单原因及全部派单记录。
- 查看护理人员签到和服务记录，但不修改原始记录。

### 5.2 建议新增文件

```text
src/store/merchant.js
src/store/service-manage.js
src/store/dispatch.js
src/mock/merchant.js
src/mock/service-manage.js
src/mock/dispatch.js

src/pages/merchant/home.vue
src/pages/merchant/apply.vue
src/pages/merchant/profile.vue
src/pages/merchant/service-list.vue
src/pages/merchant/service-edit.vue
src/pages/merchant/service-detail.vue
src/pages/merchant/order-list.vue
src/pages/merchant/order-detail.vue
src/pages/merchant/dispatch.vue
src/pages/merchant/caregiver-list.vue
src/pages/merchant/caregiver-detail.vue
src/pages/merchant/member-list.vue

src/components/merchant/service-status-card.vue
src/components/merchant/caregiver-candidate-card.vue
src/components/merchant/dispatch-history.vue
```

实际可按现有项目习惯将每个页面放入独立目录，以上路径用于表达归属。

### 5.3 依赖的公共能力

Person B 使用 Person A 提供的：

- `role` Store 和权限判断。
- 订单、支付和派单状态枚举。
- 公共请求层、上传接口、状态标签、时间线和空状态组件。
- 页面路由登记流程。

Person B 不直接修改 `user` Store 的登录逻辑，也不自行定义另一套角色或订单状态常量。

### 5.4 对外提供能力

Person B 需要向 Person C 提供一致的派单对象结构：

```json
{
  "assignmentId": 60001,
  "orderId": 30001,
  "merchantId": 20001,
  "caregiverId": 50001,
  "status": "WAITING_ACCEPT",
  "assignedAt": "2026-07-20T08:00:00+08:00",
  "acceptDeadline": "2026-07-20T08:10:00+08:00",
  "rejectReason": null
}
```

### 5.5 验收标准

- 未审核商户只能查看审核进度，不能发布服务或派单。
- 商户可以完成服务草稿、提交审核、查看驳回原因和重新提交。
- 审核通过的服务可以上架和下架。
- 商户只能查看当前商户的护理人员和订单。
- 待派单订单能显示符合条件的护理人员并成功派单。
- 护理人员拒单或超时后，订单恢复为可改派状态。
- 每次派单和改派都能在订单详情中追溯。
- 商户端关键 Store 和状态操作具有单元测试。

## 6. Person C：护理人员工作台与管理员审核 MVP

### 6.1 负责范围

Person C 负责护理人员从身份申请到完成服务的履约流程，同时交付平台管理员审核和监管的最小可用版本。

#### 护理人员认证与档案

- 护理人员申请表。
- 身份资料、护理资质和证书有效期维护。
- 服务技能、服务区域和从业年限设置。
- 审核状态、驳回原因和重新提交。
- 个人排班、请假和可接单状态设置。

#### 护理任务

- 护理工作台首页和今日任务时间线。
- 新派单提醒、派单详情和接单倒计时。
- 接受或拒绝派单，拒绝时必须填写原因。
- 待服务、进行中和已完成任务列表。
- 订单详情展示预约时间、服务内容、联系信息和地址导航。
- 未接单前隐藏完整地址，接单后按规则展示。

#### 履约流程

- 出发、到达签到、开始服务和结束服务。
- 签到时间、定位、距离和异常原因展示。
- 服务项目清单、文字记录、图片附件和异常情况表单。
- 提交结束前校验必填服务记录。
- 服务结束后进入等待顾客确认状态。
- 定位失败或异常签到进入待审核状态，不直接跳过流程。

#### 管理员审核与监管 MVP

- 独立管理员登录页或演示入口。
- 商户审核列表和详情。
- 护理人员资质审核列表和详情。
- 服务审核列表和详情。
- 审核通过、驳回及审核意见输入。
- 全平台订单查询和异常订单详情。
- 投诉详情、商户说明、处理轨迹和处理结果。
- 管理员操作历史的只读展示。

管理员正式后台建议拆为独立 Web 工程。本轮如果仍在 Uni-app 工程实现，页面和 Store 必须隔离在 `admin` 目录，避免与 App 普通角色页面耦合。

### 6.2 建议新增文件

```text
src/store/caregiver.js
src/store/work-order.js
src/store/admin.js
src/mock/caregiver.js
src/mock/work-order.js
src/mock/admin.js

src/pages/caregiver/apply.vue
src/pages/caregiver/home.vue
src/pages/caregiver/order-list.vue
src/pages/caregiver/order-detail.vue
src/pages/caregiver/check-in.vue
src/pages/caregiver/service-record.vue
src/pages/caregiver/schedule.vue
src/pages/caregiver/profile.vue

src/pages/admin/login.vue
src/pages/admin/home.vue
src/pages/admin/merchant-review-list.vue
src/pages/admin/merchant-review-detail.vue
src/pages/admin/caregiver-review-list.vue
src/pages/admin/caregiver-review-detail.vue
src/pages/admin/service-review-list.vue
src/pages/admin/service-review-detail.vue
src/pages/admin/order-list.vue
src/pages/admin/order-detail.vue
src/pages/admin/complaint-detail.vue

src/components/caregiver/task-card.vue
src/components/caregiver/service-stepper.vue
src/components/admin/audit-actions.vue
```

### 6.3 依赖的公共能力

Person C 使用 Person A 提供的：

- 当前角色、权限守卫和角色失效处理。
- 订单状态、派单状态、状态标签和时间线组件。
- 上传、日期时间、定位结果展示和错误处理约定。

Person C 使用 Person B 确定的派单对象结构，但护理人员的接单、拒单和履约 Store 由 Person C 维护。

### 6.4 对外提供能力

护理人员完成履约后，需要向 Person A 的顾客订单详情和 Person B 的商户订单详情提供统一时间线数据：

```json
[
  {
    "eventType": "CAREGIVER_DEPARTED",
    "operatorType": "CAREGIVER",
    "operatorName": "李护士",
    "eventTime": "2026-07-20T08:30:00+08:00",
    "remark": "已出发"
  },
  {
    "eventType": "SERVICE_FINISHED",
    "operatorType": "CAREGIVER",
    "operatorName": "李护士",
    "eventTime": "2026-07-20T11:00:00+08:00",
    "remark": "服务记录已提交"
  }
]
```

### 6.5 验收标准

- 未认证或审核中的护理人员不能接单。
- 护理人员只能查看派给自己的任务。
- 护理人员可以接受、拒绝并查看响应时限。
- 履约操作严格按出发、签到、开始、结束顺序执行。
- 服务记录未填写完整时不能结束服务。
- 异常签到不会直接将订单置为服务中。
- 管理员能完成商户、护理人员和服务的通过或驳回。
- 审核结果能同步反映到商户或护理人员页面。
- 管理员能查看异常订单和投诉处理轨迹。
- 护理人员及管理员关键 Store 具有单元测试。

## 7. 公共协议先行

三人开始页面开发前，必须共同确定以下协议，并记录在接口文档或常量文件中。

### 7.1 角色和权限

```js
CUSTOMER
MERCHANT_MEMBER
CAREGIVER
ADMIN
```

权限采用明确字符串，不在页面中使用模糊数字：

```text
merchant:service:create
merchant:service:submit
merchant:order:dispatch
caregiver:assignment:accept
caregiver:service:check-in
admin:service:audit
```

### 7.2 核心状态

```text
订单：CREATED / WAITING_DISPATCH / WAITING_SERVICE / IN_SERVICE /
      WAITING_CONFIRM / COMPLETED / CANCELED / CLOSED / DISPUTED

支付：UNPAID / PAID / REFUNDING / PARTIALLY_REFUNDED / REFUNDED

派单：UNASSIGNED / WAITING_ACCEPT / ACCEPTED / REJECTED /
      EXPIRED / CANCELED
```

### 7.3 基础数据结构

至少先固定：

- 登录用户和当前角色结构。
- 商户和商户成员结构。
- 护理人员档案与资质结构。
- 服务、服务规格和审核版本结构。
- 订单详情与下单快照结构。
- 派单记录结构。
- 服务签到和服务记录结构。
- 订单时间线结构。
- 审核记录和投诉处理结构。

### 7.4 通用约定

- 金额统一使用“分”的整数；兼容旧接口时由适配层转换。
- 时间统一使用带时区的 ISO 8601 字符串。
- ID 使用后端提供的数字 ID，不生成业务伪 ID。
- 分页响应统一为 `{ list, total, page, size }`，如继续使用 cursor 分页需按接口单独注明。
- 写操作统一展示加载状态并防止重复点击。
- 下单、支付、取消、派单、接单、签到、结束和审核等动作携带幂等键。
- 所有接口使用 `{ code, message, data }` 响应包装。

## 8. 公共文件所有权和冲突控制

### 8.1 文件所有权

| 文件或目录 | 主要维护人 | 其他人修改方式 |
|---|---|---|
| `src/pages.json` | Person A | 提交页面路径清单，由 A 集中登记 |
| `src/store/user.js` | Person A | 通过 PR 请求 A 修改 |
| `src/store/role.js` | Person A | 公共评审后由 A 修改 |
| `src/mock/index.js` | Person A | B/C 提供导出函数，A 集中注册 |
| `src/constants/` | Person A | 三人评审，A 维护公共枚举 |
| `src/pages/merchant/` | Person B | B 独占维护 |
| `src/store/merchant.js` 等商户 Store | Person B | B 独占维护 |
| `src/pages/caregiver/` | Person C | C 独占维护 |
| `src/pages/admin/` | Person C | C 独占维护 |
| `src/store/caregiver.js`、`work-order.js`、`admin.js` | Person C | C 独占维护 |

### 8.2 公共文件提交规则

- B 和 C 新增页面时，不在长期功能分支反复修改 `pages.json`。
- B 和 C 在 PR 描述中提供需要新增的页面路径、标题和分包归属。
- Mock 模块各自独立导出，`mock/index.js` 由 A 在集成分支一次性注册。
- 公共组件如果只服务一个角色，先放角色组件目录；两人以上复用后再提升到 `components/base`。
- 不在业务页面复制角色、订单状态和权限常量。
- 对公共接口的破坏性修改必须先在三人群组中确认，再提交代码。

## 9. 开发阶段与依赖顺序

建议按四个阶段推进，每个阶段结束都必须有可运行版本。

### 阶段 0：协议冻结

三人共同完成：

- [ ] 确认角色、权限和组织模型。
- [ ] 确认订单、支付和派单状态机。
- [ ] 确认登录、切换身份、派单、接单和履约 API。
- [ ] 建立 Mock 测试账号和固定测试订单。
- [ ] 确认页面清单、分包和公共文件所有权。

阶段 0 未完成前，可以搭建静态页面，但不应各自定义不同数据结构。

### 阶段 1：基础设施与页面骨架

| Person A | Person B | Person C |
|---|---|---|
| 多角色登录、role Store、权限守卫 | 商户申请、商户首页、商户 Store | 护理申请、护理首页、护理 Store |
| 公共状态枚举和组件 | 服务管理页面骨架 | 任务列表和详情骨架 |
| 工作台入口和路由集成 | 商户 Mock 基础数据 | 护理和管理员 Mock 基础数据 |

阶段验收：三个角色都能登录并进入各自工作台，页面使用统一角色模型。

### 阶段 2：服务审核和派单闭环

| Person A | Person B | Person C |
|---|---|---|
| 顾客订单新状态适配 | 服务创建、提交和上下架 | 管理员服务审核 |
| 订单时间线公共组件 | 商户订单列表和候选人员 | 护理人员接单和拒单 |
| 集成服务审核结果 | 派单、改派和派单记录 | 任务状态同步 |

阶段验收：商户提交服务后管理员可审核；顾客支付订单后商户可派单，护理人员可接受或拒绝。

### 阶段 3：履约和顾客确认闭环

| Person A | Person B | Person C |
|---|---|---|
| 顾客查看履约进度和确认完成 | 商户查看履约记录和异常订单 | 出发、签到、开始、服务记录、结束 |
| 评价和投诉入口状态适配 | 改派和异常协助处理 | 异常签到和服务记录附件 |
| 跨角色时间线联调 | 商户订单统计 | 管理员异常订单监管 |

阶段验收：一笔订单可从支付完成一直流转到顾客确认完成，并可进入评价或投诉。

### 阶段 4：质量和交付

三人分别完成本业务域：

- [ ] 空状态、加载状态、错误状态和重复提交保护。
- [ ] 权限越界、角色停用和审核驳回场景。
- [ ] H5 构建和至少一个目标小程序端构建。
- [ ] Store 单元测试和关键流程手工测试。
- [ ] 联调问题修复和接口文档更新。
- [ ] 页面截图、演示账号和演示流程整理。

## 10. 测试账号和演示数据建议

| 场景 | 手机号 | 可用角色 | 说明 |
|---|---|---|---|
| 顾客 | `13800138000` | `CUSTOMER` | 保留现有测试账号 |
| 多角色人员 | `13800138001` | `CUSTOMER`, `CAREGIVER` | 测试角色切换 |
| 商户负责人 | `13800138002` | `CUSTOMER`, `MERCHANT_MEMBER` | 商户 `OWNER` |
| 商户调度员 | `13800138003` | `MERCHANT_MEMBER` | 仅订单和派单权限 |
| 待审核护理人员 | `13800138004` | `CUSTOMER` | 已提交护理申请 |
| 管理员 | `admin` | `ADMIN` | 仅管理员入口可登录 |

Mock 至少准备以下订单：

- 一笔待支付订单。
- 一笔已支付待派单订单。
- 一笔等待护理人员接单订单。
- 一笔护理人员已拒绝、需要改派订单。
- 一笔待上门订单。
- 一笔服务中订单。
- 一笔待顾客确认订单。
- 一笔已完成可评价订单。
- 一笔投诉处理中订单。

## 11. Git 协作建议

### 11.1 分支

每人从最新主分支创建独立功能分支：

```text
codex/role-auth-foundation
codex/merchant-workbench
codex/caregiver-admin-workbench
```

如果团队已有 `feat/` 分支规范，可继续使用现有规范，重点是三条开发线互相隔离。

### 11.2 提交粒度

建议按可评审功能提交，不要将整个工作台压成一个提交：

```text
feat(auth): add target role login
feat(merchant): add service submission flow
feat(dispatch): add caregiver assignment
feat(caregiver): add service check-in flow
feat(admin): add service audit actions
test(order): cover multi-status display
```

### 11.3 PR 顺序

推荐合并顺序：

1. Person A 的角色常量、role Store 和登录基础 PR。
2. Person B、Person C 的页面骨架与独立 Store PR。
3. Person A 的统一路由和 Mock 注册 PR。
4. 服务审核与派单闭环 PR。
5. 履约与顾客确认闭环 PR。
6. 最终集成、回归和文档 PR。

每个 PR 必须说明：

- 功能范围和未包含内容。
- 新增或修改的 API。
- 页面路径和测试账号。
- 手工验证步骤。
- 是否修改公共文件。
- 已知限制和后续任务。

## 12. Definition of Done

每项功能只有同时满足以下条件才算完成：

- 页面、Store、Mock/API 调用和异常状态完整，不是只有静态界面。
- 当前角色和权限校验正确，不能越权访问其他商户或护理人员数据。
- 加载、空数据、错误和重复提交状态可见。
- 关键写操作有确认提示、成功反馈和失败恢复。
- 枚举、接口字段和金额/时间格式符合公共协议。
- ESLint、单元测试和 H5 构建通过。
- 至少完成一次真实操作路径的手工验证。
- PR 已由至少一名非作者成员 Review。
- 相关接口或使用说明已经更新到 `docs/`。

建议每次集成执行：

```bash
npm run lint
npm run test:run
npm run build:h5
```

## 13. 三人联合验收场景

最终必须使用同一组 Mock 或联调环境完成以下端到端测试：

### 场景一：正常履约

```text
商户创建服务
→ 管理员审核通过
→ 顾客下单并支付
→ 商户派给护理人员
→ 护理人员接单
→ 出发、签到、开始和结束服务
→ 顾客确认完成并评价
```

### 场景二：拒单和改派

```text
商户首次派单
→ 护理人员拒绝并填写原因
→ 商户查看拒绝记录
→ 商户改派另一名护理人员
→ 新护理人员接单
```

### 场景三：异常签到和投诉

```text
护理人员定位签到失败
→ 提交异常签到凭证
→ 商户和管理员查看异常
→ 服务结束后顾客提交投诉
→ 管理员查看证据并处理
→ 顾客查看处理进度
```

### 场景四：身份和数据隔离

```text
多角色账号以护理人员身份登录
→ 只能看到本人护理任务
→ 切换为顾客身份
→ 只能看到本人顾客订单
→ 商户 A 成员无法查看商户 B 的订单
→ 普通用户无法进入管理员页面
```

## 14. 暂不纳入本轮的内容

为控制三人首期范围，以下功能放到后续迭代：

- 自动派单和复杂推荐算法。
- 路径规划与实时位置跟踪。
- 商户结算、护理人员佣金和发票。
- 多级商户组织和复杂审批流。
- 完整运营报表和 BI 大屏。
- 在线客服、即时聊天和音视频。
- 高级风控、黑名单和设备指纹。
- 管理员后台的完整响应式 Web 设计。

首期只需把角色、服务审核、下单、人工派单、接单、履约、确认和售后监管串成稳定闭环，再进入运营能力建设。

