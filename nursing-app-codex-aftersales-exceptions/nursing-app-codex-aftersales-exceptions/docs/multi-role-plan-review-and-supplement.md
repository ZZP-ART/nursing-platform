# 多角色扩展方案 — 评审意见与工程落地补充

> 针对 `docs/multi-role-platform-extension-plan.md` 的评审，重点补充 Uni-app 工程层面缺失的细节。

---

## 一、总体评价

原方案在**业务建模、状态机设计、异常处理、安全隐私**方面质量很高，特别是：

- "一个账号多角色"模型正确覆盖了护理人员也可作为顾客下单等真实场景。
- 订单状态三维拆分（主状态 + 支付 + 派单）比单枚举健壮得多。
- 服务版本化 + 订单快照保证了历史数据完整性。
- 13 个异常场景的覆盖在早期方案里很少见。

以下 7 条建议主要针对**Uni-app 工程落地**缺失的部分。

---

## 二、建议 1：第一阶段简化角色模型

### 问题

原方案的"选身份 → 输手机号密码 → 后端验证身份 → 签发 role-scoped token"要求后端一次性完成完整的角色验证链路。在 Mock 开发和前后端联调早期，这会阻塞进度。

### 建议

**第一阶段用简单模型跑通闭环，第二阶段再加 `switch-role`。**

第一阶段实现：

```
不同角色用不同手机号注册/登录
→ 登录时后端返回 role 字段（单值）
→ 前端按 role 渲染不同界面
→ 不存在身份切换
```

登录请求（简单版）：

```json
{
  "phone": "13800138000",
  "loginMode": "password",
  "password": "123456"
}
```

登录响应（简单版）：

```json
{
  "code": 0,
  "data": {
    "token": "eyJ...",
    "expireTime": "2026-07-22T12:00:00+08:00",
    "role": "CAREGIVER",
    "user": {
      "userId": 10001,
      "phone": "138****8000",
      "nickname": "李护士",
      "role": "CAREGIVER"
    }
  }
}
```

第二阶段再加：

- `targetRole` 请求参数 — 多角色账号选择以哪个身份登录。
- `roles[]` 响应字段 — 账号拥有的全部角色列表。
- `POST /api/v1/auth/switch-role` — 会话中切换角色。
- 登录页的身份 Tab 选择器。

**好处**：Mock 层可以独立跑通完整闭环，Store 改动量也小。

---

## 三、建议 2：Uni-app 自定义 TabBar 方案（关键）

### 问题

原方案只说"Uni-app 原生 TabBar 不适合频繁动态切换，建议使用统一基础导航"，没有给出具体实现方案。这是 Uni-app 多角色开发中最棘手的 UI 问题。

### 需求

| 角色 | Tab 1 | Tab 2 | Tab 3 | Tab 4 |
|------|-------|-------|-------|-------|
| 顾客 | 首页 | 服务 | 我的 | — |
| 护理人员 | 工作台 | 任务 | 排班 | 我的 |
| 商户 | 工作台 | 服务 | 订单 | 我的 |

三种角色底部导航完全不同，Uni-app 原生 `tabBar.list` 在 `pages.json` 中静态配置，运行时无法切换。

### 推荐方案：用户用原生 TabBar + 护士/商户用自定义 TabBar 组件

**核心思路**：用户端保持现有的原生 TabBar（不动），护士和商户放在 subPackages 中，各自用自定义 TabBar 组件。

#### 组件：`src/components/base/role-tab-bar.vue`

```vue
<template>
  <view class="role-tab-bar">
    <view
      v-for="tab in tabs"
      :key="tab.path"
      class="tab-item"
      :class="{ active: current === tab.path }"
      @click="onTabClick(tab)"
    >
      <u-icon :name="tab.icon" size="22" :color="current === tab.path ? activeColor : inactiveColor" />
      <text class="tab-label">{{ tab.label }}</text>
      <view v-if="tab.badge" class="tab-badge">{{ tab.badge }}</view>
    </view>
  </view>
</template>

<script setup>
defineProps({
  tabs: { type: Array, required: true },
  // [{ path, label, icon, badge? }]
  current: { type: String, required: true },
  activeColor: { type: String, default: '#4A90D9' },
  inactiveColor: { type: String, default: '#999999' },
})
const emit = defineEmits(['switch'])

function onTabClick(tab) {
  emit('switch', tab)
  uni.redirectTo({ url: tab.path })
}
</script>

<style lang="scss" scoped>
.role-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  height: 100rpx;
  padding-bottom: env(safe-area-inset-bottom);
  background: $surface-gradient;
  border-top: 1rpx solid $divider-color;
  z-index: 999;
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
  position: relative;
}
.tab-label {
  font-size: 20rpx;
}
.tab-badge {
  position: absolute;
  top: 4rpx;
  right: 50%;
  transform: translateX(36rpx);
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  border-radius: 16rpx;
  background: #FF4D4F;
  color: #fff;
  font-size: 18rpx;
  line-height: 32rpx;
  text-align: center;
}
</style>
```

#### 护士端使用示例（`subpkg-nurse/home/index.vue`）

```vue
<template>
  <view class="page-shell">
    <!-- 页面内容 -->
    <view style="padding-bottom: 120rpx">
      <!-- 工作台内容 -->
    </view>

    <role-tab-bar
      :tabs="nurseTabs"
      current="/subpkg-nurse/home/index"
      @switch="onTabSwitch"
    />
  </view>
</template>

<script setup>
const nurseTabs = [
  { path: '/subpkg-nurse/home/index', label: '工作台', icon: 'home' },
  { path: '/subpkg-nurse/orders/index', label: '任务', icon: 'list-dot' },
  { path: '/subpkg-nurse/schedule/index', label: '排班', icon: 'calendar' },
  { path: '/subpkg-nurse/profile/index', label: '我的', icon: 'account' },
]
</script>
```

商户端同理，替换 `nurseTabs` 为商户的4个 tab 配置。

#### 注意点

- 各角色首页的 `onShow` 中调用 `requireRole('nurse')` 或 `requireRole('merchant')` 做权限守卫。
- 页面间用 `uni.redirectTo` 切换（不是 `switchTab`），因为 subPackage 页面不走原生 TabBar。
- 如果后续护士/商户也需要微信小程序的原生 TabBar 体验，可以在 `pages.json` 中用条件编译为不同平台配置不同的 `tabBar.list`，但复杂度高且不灵活。

---

## 四、建议 3：API 路径统一规范

### 问题

原方案中混用了单复数：

```
/api/v1/caregiver/profile     ← 单数
/api/v1/caregivers/apply      ← 复数
/api/v1/merchant/orders       ← 单数
/api/v1/merchants/apply       ← 复数
```

### 建议

全部统一为**复数**，与现有 API 一致（`/api/v1/users/`、`/api/v1/orders/`）：

| 原方案（不一致） | 修正后（统一复数） |
|---|---|
| `/api/v1/caregiver/profile` | `/api/v1/caregivers/profile` |
| `/api/v1/caregiver/tasks` | `/api/v1/caregivers/tasks` |
| `/api/v1/caregiver/schedules` | `/api/v1/caregivers/schedules` |
| `/api/v1/merchant/profile` | `/api/v1/merchants/profile` |
| `/api/v1/merchant/dashboard` | `/api/v1/merchants/dashboard` |
| `/api/v1/merchant/orders` | `/api/v1/merchants/orders` |
| `/api/v1/merchant/services` | `/api/v1/merchants/services` |

**例外**：角色切换是个人操作，不是资源集合，可以保持单数：`POST /api/v1/auth/switch-role`。

---

## 五、建议 4：pages.json 配置示例

### 问题

原方案列出了所有页面路径，但没有给出 `pages.json` 中的 subPackages 配置。实际开发时这是必须的。

### 建议配置

```jsonc
{
  "pages": [
    // ===== 主包：登录 + 用户端核心页面（保持现有）=====
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "智慧护理" } },
    { "path": "pages/login/login", "style": { "navigationBarTitleText": "登录" } },
    { "path": "pages/register/register", "style": { "navigationBarTitleText": "注册" } },
    { "path": "pages/service/service", "style": { "navigationBarTitleText": "服务项目" } },
    { "path": "pages/service-detail/service-detail", "style": { "navigationBarTitleText": "服务详情" } },
    { "path": "pages/search/search", "style": { "navigationBarTitleText": "搜索" } },
    { "path": "pages/account/account", "style": { "navigationBarTitleText": "我的" } }
    // 注意：booking、address、order、review、complaint 等可移入用户分包
  ],

  "subPackages": [
    {
      "root": "subpkg-user",
      "name": "user",
      "pages": [
        { "path": "booking/index", "style": { "navigationBarTitleText": "确认预约" } },
        { "path": "address-list/index", "style": { "navigationBarTitleText": "服务地址" } },
        { "path": "address-edit/index", "style": { "navigationBarTitleText": "编辑地址" } },
        { "path": "payment-result/index", "style": { "navigationBarTitleText": "支付结果" } },
        { "path": "order-list/index", "style": { "navigationBarTitleText": "我的订单" } },
        { "path": "order-detail/index", "style": { "navigationBarTitleText": "订单详情" } },
        { "path": "review-submit/index", "style": { "navigationBarTitleText": "评价服务" } },
        { "path": "complaint-submit/index", "style": { "navigationBarTitleText": "提交投诉" } },
        { "path": "complaint-list/index", "style": { "navigationBarTitleText": "投诉记录" } },
        { "path": "complaint-detail/index", "style": { "navigationBarTitleText": "投诉进度" } }
      ]
    },
    {
      "root": "subpkg-nurse",
      "name": "nurse",
      "pages": [
        { "path": "home/index", "style": { "navigationBarTitleText": "工作台", "navigationStyle": "custom" } },
        { "path": "orders/index", "style": { "navigationBarTitleText": "我的任务", "navigationStyle": "custom" } },
        { "path": "order-detail/index", "style": { "navigationBarTitleText": "任务详情" } },
        { "path": "task-start/index", "style": { "navigationBarTitleText": "开始服务" } },
        { "path": "task-complete/index", "style": { "navigationBarTitleText": "完成服务" } },
        { "path": "schedule/index", "style": { "navigationBarTitleText": "排班管理", "navigationStyle": "custom" } },
        { "path": "earnings/index", "style": { "navigationBarTitleText": "收入明细" } },
        { "path": "profile/index", "style": { "navigationBarTitleText": "个人中心", "navigationStyle": "custom" } }
      ]
    },
    {
      "root": "subpkg-merchant",
      "name": "merchant",
      "pages": [
        { "path": "home/index", "style": { "navigationBarTitleText": "工作台", "navigationStyle": "custom" } },
        { "path": "service-list/index", "style": { "navigationBarTitleText": "服务管理", "navigationStyle": "custom" } },
        { "path": "service-edit/index", "style": { "navigationBarTitleText": "编辑服务" } },
        { "path": "spec-manage/index", "style": { "navigationBarTitleText": "规格管理" } },
        { "path": "nurse-assign/index", "style": { "navigationBarTitleText": "分配护理人员" } },
        { "path": "order-list/index", "style": { "navigationBarTitleText": "订单管理", "navigationStyle": "custom" } },
        { "path": "order-detail/index", "style": { "navigationBarTitleText": "订单详情" } },
        { "path": "dispatch/index", "style": { "navigationBarTitleText": "派单" } },
        { "path": "revenue/index", "style": { "navigationBarTitleText": "收入结算" } },
        { "path": "nurse-list/index", "style": { "navigationBarTitleText": "人员管理" } },
        { "path": "nurse-detail/index", "style": { "navigationBarTitleText": "人员详情" } },
        { "path": "profile/index", "style": { "navigationBarTitleText": "商户资料" } }
      ]
    }
  ],

  "preloadRule": {
    "pages/index/index": {
      "network": "wifi",
      "packages": ["subpkg-user"]
    },
    "subpkg-nurse/home/index": {
      "network": "all",
      "packages": ["subpkg-nurse"]
    },
    "subpkg-merchant/home/index": {
      "network": "all",
      "packages": ["subpkg-merchant"]
    }
  },

  "tabBar": {
    "color": "#999999",
    "selectedColor": "#4A90D9",
    "backgroundColor": "#F9FBFF",
    "borderStyle": "white",
    "list": [
      { "pagePath": "pages/index/index", "text": "首页" },
      { "pagePath": "pages/service/service", "text": "服务" },
      { "pagePath": "pages/account/account", "text": "我的" }
    ]
  }
}
```

### 注意点

- 护士和商户首页使用 `"navigationStyle": "custom"` 来隐藏原生导航栏，因为需要配合自定义 TabBar。
- `pages/booking/`、`pages/order/` 等页面建议移入 `subpkg-user` 分包，减小主包体积。
- `preloadRule` 让用户在 WiFi 下预加载分包，首次进入不卡顿。

---

## 六、建议 5：Store 按角色合并

### 问题

原方案建议拆成 `merchant.js`、`service-manage.js`、`dispatch.js`、`caregiver.js`、`work-order.js` 共 5 个 Store。考虑到现有项目总共只有 6 个 Store 且每个都简洁清晰，这个粒度太细。

### 建议

按角色合并为 3 个新 Store，与现有模式一致：

```
src/store/
├── user.js          # 🔧 修改：增加 role、角色判断、navigateByRole()
├── service.js       # 🔧 修改：增加 auditStatus、merchantId、服务管理方法
├── order.js         # 🔧 修改：扩展状态、护士字段、角色相关方法
├── address.js       # 不变
├── review.js        # 🔧 修改：增加 nurseRating
├── complaint.js     # 🔧 修改：增加管理员处理方法
├── nurse.js         # 🆕 护士全部状态：任务/接单/履约/排班/收益
├── merchant.js      # 🆕 商户全部状态：服务/订单/派单/人员/结算
└── admin.js         # 🆕 管理员全部状态：审核/监管（Web 子项目复用）
```

**理由**：

- 当前 Store 模式是"一个领域一个 Store"（user / service / order / address / review / complaint），每个约 80-150 行。
- 按角色组织，开发护士端时只需关注 `nurse.js`，不用在 5 个 Store 间跳转。
- `dispatch.js`（派单）属于商户的操作流程，放在 `merchant.js` 里更自然。
- `work-order.js`（履约）是护士的核心流程，放在 `nurse.js` 里更自然。

---

## 七、建议 6：Mock 注册顺序

### 问题

原方案没有提到 Mock 注册顺序。Mock.js 按 `Mock.mock()` 调用顺序匹配 URL，如果通用模式先注册，具体路径后注册的会被前者拦截。这是实际开发中必踩的坑。

### 建议

`src/mock/index.js` 中，**具体路径在前，通用路径在后**：

```js
import Mock from 'mockjs'

// ⚠️ 顺序至关重要！admin/merchant/caregiver 的具体前缀必须在通用模块之前
// 否则 /api/v1/merchant/items 会被 /api/v1/items 的泛匹配拦截

import './admin.js'       // /api/v1/admin/*
import './merchant.js'    // /api/v1/merchants/*
import './caregiver.js'   // /api/v1/caregivers/*

// 原有通用模块
import './user.js'        // /api/v1/users/*
import './service.js'     // /api/v1/items/*, /api/v1/categories/*
import './address.js'
import './order.js'       // /api/v1/orders/*
import './review.js'
import './complaint.js'

Mock.setup({ timeout: '200-500' })

console.log('[Mock] 全部模块已启用 — 多角色扩展版')
```

**商户服务管理的 URL 冲突示例**：

```
注册顺序正确时：
  Mock.mock(/\/api\/v1\/merchants\/items/, 'get', handler)  → 先匹配
  Mock.mock(/\/api\/v1\/items/, 'get', handler)             → 后匹配
  请求 GET /api/v1/merchants/items → 命中第一个 ✓

注册顺序错误时：
  Mock.mock(/\/api\/v1\/items/, 'get', handler)             → 先匹配
  Mock.mock(/\/api\/v1\/merchants\/items/, 'get', handler)  → 后匹配
  请求 GET /api/v1/merchants/items → 被第一个拦截 ✗
```

---

## 八、建议 7：管理后台子项目结构

### 问题

原方案第 7.4 节只列了一行目录名，完全没展开。管理后台是独立项目，需要明确技术选型、目录结构、与主项目的代码共享策略。

### 建议

#### 技术选型

| 项 | 选择 | 理由 |
|----|------|------|
| 框架 | Vue 3 + Vite | 与主项目一致 |
| UI 库 | Element Plus | 后台表格/表单/对话框的标准方案 |
| 状态管理 | Pinia | 与主项目一致 |
| 路由 | Vue Router 4 | 标准 SPA 路由 |
| HTTP | Axios（复用错误码映射逻辑） | 非 Uni-app 环境不能用 `uni.request` |
| Mock | Mock.js（复用主项目 mock 逻辑） | 或独立 mock-server |

#### 目录结构

```
admin/
├── package.json
├── vite.config.js
├── index.html
├── .env.development
├── .env.production
└── src/
    ├── main.js
    ├── App.vue
    ├── router/
    │   └── index.js
    ├── views/                          # 页面
    │   ├── login/index.vue             #   管理员登录
    │   ├── dashboard/index.vue         #   数据看板
    │   ├── service-audit/
    │   │   ├── index.vue              #   待审核列表
    │   │   └── detail.vue             #   审核详情（通过/驳回）
    │   ├── categories/
    │   │   ├── index.vue              #   分类列表（树形）
    │   │   └── form.vue               #   分类表单
    │   ├── users/
    │   │   ├── index.vue              #   用户列表
    │   │   └── detail.vue             #   用户详情+订单历史
    │   ├── nurses/
    │   │   ├── index.vue              #   护士列表
    │   │   ├── detail.vue             #   护士详情+资质+排班
    │   │   └── verify.vue             #   资质审核
    │   ├── merchants/
    │   │   ├── index.vue              #   商户列表
    │   │   ├── detail.vue             #   商户详情+服务+人员
    │   │   └── verify.vue             #   入驻审核
    │   ├── orders/
    │   │   ├── index.vue              #   全量订单
    │   │   └── detail.vue             #   订单详情+纠纷处理
    │   ├── complaints/
    │   │   ├── index.vue              #   投诉列表
    │   │   └── handle.vue             #   处理投诉
    │   └── settings/
    │       └── index.vue              #   系统设置
    ├── components/                     # 管理端组件
    │   ├── sidebar-nav.vue            #   侧边导航
    │   ├── stat-card.vue              #   KPI 卡片
    │   ├── data-table.vue             #   通用表格（排序/分页/筛选）
    │   ├── status-badge.vue           #   状态标签
    │   └── audit-dialog.vue           #   审核弹窗（通过/驳回原因）
    ├── stores/
    │   └── admin.js                   #   管理端 Store
    ├── api/
    │   └── request.js                 #   Axios 封装（复用主项目错误码映射）
    ├── mock/
    │   └── index.js                   #   管理端独立 Mock
    └── styles/
        └── index.scss                 #   Element Plus 主题变量 + 品牌色
```

#### 代码共享策略

主项目（Uni-app）和管理后台之间有两层可以共享：

| 共享内容 | 方式 | 说明 |
|----------|------|------|
| API 错误码映射 | 复制 `src/utils/request.js` 中的 `ERROR_MESSAGES` 对象 | 体积小，复制比引入整个 Uni-app 依赖简单 |
| 设计 Token | 复制 `src/uni.scss` 中的 SCSS 变量到 `admin/src/styles/variables.scss` | 保持品牌色一致 |
| 枚举常量 | 如果主项目提取了 `src/utils/constants.js`，可以复制或建 shared 包 | 目前没有，建议先建 |
| Pinia Store | **不建议共享** — admin 的数据结构和操作与移动端差异大 | 各自维护 |

如果后续觉得复制太多，可以在仓库根目录建 `packages/shared/` 放共享常量、类型定义和错误码映射，主项目和 admin 分别引用。但第一期不建议引入 monorepo 的复杂度。

#### 启动与 Mock

```bash
# 开发环境（使用 Mock）
cd admin && npm run dev
# VITE_USE_MOCK=true → 加载 admin/src/mock/index.js

# 开发环境（连后端）
cd admin && npm run dev:api
# VITE_USE_MOCK=false → 全部请求发往 http://localhost:8080
```

管理员测试账号在 Mock 中预设：手机号 `13800138003`，密码 `123456`，不支持自主注册。

---

## 九、补充：建议的第一批实施顺序

原方案第 15 节的阶段划分整体合理，但第一期可以更聚焦：

```
Phase 0: 角色模型 + 登录改造（3-4 天）
├── storage.js 增加 ROLE 键
├── user store 增加 role 字段和 navigateByRole()
├── mock/user.js 预设 4 角色测试账号
├── login.vue 增加角色 Tab 选择（顾客/护理人员/商户）
├── register.vue 增加角色选择步骤
└── permission.js 路由守卫

Phase 1: 订单状态升级（2-3 天）
├── constants.js 枚举定义
├── order store 扩展 STATUS_MAP + 护士字段
├── mock/order.js 扩展状态流
├── order-detail.vue 增加护士信息 + 时间线
└── review-submit.vue 增加护士评分

Phase 2: 护士端 subPackages（5-7 天）
├── role-tab-bar.vue 组件
├── nurse store + mock
├── 8 个页面（home → orders → order-detail → task-start → task-complete → schedule → earnings → profile）
└── pages.json subpkg-nurse 配置

Phase 3: 商户端 subPackages（5-7 天）
├── merchant store + mock
├── 12 个页面
└── pages.json subpkg-merchant 配置

Phase 4: 管理员 Web（独立项目，5-7 天）
├── 项目搭建
├── sidebar + router
├── 10 个视图
└── admin mock
```

---

## 十、总结

原方案在业务架构层面几乎没有需要修改的地方。以上 7 条建议全部针对工程落地：

| # | 建议 | 影响 |
|---|------|------|
| 1 | 第一阶段简化角色模型 | 降低 Mock 和前端开发阻塞 |
| 2 | 自定义 TabBar 组件方案 | Uni-app 多角色导航的核心实现 |
| 3 | API 路径统一复数 | 与现有代码风格一致 |
| 4 | pages.json subPackages 配置 | 实际开发必需的配置文件 |
| 5 | Store 按角色合并 | 减少文件跳转，对齐现有模式 |
| 6 | Mock 注册顺序 | 避免 URL 拦截错误（必踩坑） |
| 7 | 管理后台项目结构 | 原方案完全缺失的部分 |

如果原方案是"做什么"，这份补充就是"在 Uni-app 里具体怎么做"。两份文档配合使用即可开始编码。
