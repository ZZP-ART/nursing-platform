# 智慧护理平台 — 开发备忘

> 给 Codex 看的恢复指南。新开终端后把这份文件发给我即可继续。

---

## 项目位置
E:\nursing-app\

## 启动命令
```bash
cd /e/nursing-app
npx uni
```

## 技术栈
- Uni-app (Vue 3) + Vite
- uView Plus 3.x（UI 组件库）
- Pinia 2.x（状态管理）
- SCSS（样式）
- Mock.js（后端未就绪时的模拟数据）

## 当前进度

### ✅ 已完成（Person B — App基础 + 用户与服务发现）
- [x] App 工程搭建（脚手架、路由、状态管理、网络层）
- [x] 登录/注册页（密码+验证码双模式，对齐 API v1.0）
- [x] 首页（分类导航 + 服务列表 + 骨架屏 + 空状态 + 分页加载）
- [x] 服务列表页（分类Tab筛选，支持取消，按API分页）
- [x] 服务详情页（规格选择、Markdown介绍、图片展示、评价）
- [x] 搜索页（关键词搜索 + 搜索历史 + 分页结果）
- [x] 基础组件库（service-card、empty-state、skeleton-card、verify-code-btn）
- [x] Mock 数据（用户/服务/地址/订单/评价/投诉 — 全模块 v1.0）
- [x] 设计 Token（品牌色系、字体、间距、圆角、阴影）
- [x] ESLint + Prettier 配置
- [x] API 对齐：全部接口路径、字段名、响应格式已对齐 API 文档 v1.0
- [x] Stores：user / service / address / order / review / complaint 全部就绪
- [x] 代码已上传 GitHub: https://github.com/velvetkr/nursing-app

### 🆕 API v1.0 对齐完成（2026-07-08）
以下基础设施已全面对齐后端 API 文档：

| 层 | 文件 | 状态 |
|----|------|------|
| 网络层 | `src/utils/request.js` | ✅ code=0 成功，错误码映射，PATCH/DELETE，Idempotent-Key，环境 BASE_URL |
| 环境配置 | `config/dev.js` / `config/prod.js` | ✅ |
| Mock | `src/mock/user.js` | ✅ 8个接口（sms-code, register, login, logout, password/reset, profile GET/PUT, file upload） |
| Mock | `src/mock/service.js` | ✅ 分类+列表(分页)+详情+搜索，specs规格结构 |
| Mock | `src/mock/address.js` | ✅ CRUD + 默认地址 |
| Mock | `src/mock/order.js` | ✅ prepay-token + 创建 + 列表/详情 + 取消 + 支付 |
| Mock | `src/mock/review.js` | ✅ 提交评价 + 列表 |
| Mock | `src/mock/complaint.js` | ✅ 提交 + 列表 + 处理记录 |
| Store | `src/store/user.js` | ✅ login/logout/register/profile/upload |
| Store | `src/store/service.js` | ✅ categories/items(分页)/detail/search |
| Store | `src/store/address.js` | ✅ CRUD + default |
| Store | `src/store/order.js` | ✅ prepay-token → create → pay 三步流程 |
| Store | `src/store/review.js` | ✅ submit + list |
| Store | `src/store/complaint.js` | ✅ submit + list + tracks |

### ❌ 待完成（Person C — 交易转化页面）
- [ ] 下单预约页（选规格、选地址、日期时段、订单确认）
- [ ] 地址管理页（列表、新增/编辑、省市区级联）
- [ ] 支付结果页

### ❌ 待完成（Person D — 售后与账户页面）
- [ ] 我的订单页（列表、详情、操作、状态时间线）
- [ ] 评价提交页（星级+文字+图片）
- [ ] 投诉模块页（表单+记录跟踪）
- [ ] UI 资源包（图标、启动图、插画）

## 测试账号
- 手机号：13800138000
- 密码：123456
- 验证码（任意账号）：123456
- Mock 注册可用任意未注册手机号 + 验证码 123456

## API 规范速查

| 项 | 规则 |
|----|------|
| Base URL (DEV) | `http://localhost:8080` |
| 响应格式 | `{ code: 0, message: "success", data: {...} }` |
| 分页响应 | `{ list: [...], total: 120, page: 1, size: 20 }` |
| 鉴权 | `Authorization: Bearer <token>` |
| 幂等 | `Idempotent-Key` 头（下单/取消等） |
| 成功码 | `code === 0` |
| 鉴权失败 | HTTP 401 或 code=1002/1003 |

### 关键字段对照（旧 → 新）

| 概念 | 旧字段 | 新字段 |
|------|--------|--------|
| 用户ID | `id` (string) | `userId` (number) |
| 分类ID | `id` (string) | `categoryId` (number) |
| 服务ID | `id` (string) | `itemId` (number) |
| 服务价格 | `price` | `minPrice` + `specs[].price` |
| 服务封面 | `cover` | `coverImage` |
| 登录方式 | `mode` | `loginMode` |
| 验证码 | `code` | `smsCode` |
| 用户信息 | `userInfo` | `user` |

## 项目结构
```
src/
├── api/              # API 接口（待扩展）
├── components/base/  # 公共组件
│   ├── service-card.vue
│   ├── empty-state.vue
│   ├── skeleton-card.vue
│   └── verify-code-btn.vue
├── composables/      # 组合函数（待扩展）
├── mock/             # Mock 数据（全模块就绪）
│   ├── index.js      # 入口
│   ├── user.js       # 用户认证
│   ├── service.js    # 服务目录
│   ├── address.js    # 地址管理
│   ├── order.js      # 订单
│   ├── review.js     # 评价
│   └── complaint.js  # 投诉
├── pages/            # 页面
│   ├── index/         # 首页
│   ├── login/         # 登录
│   ├── register/      # 注册
│   ├── service/       # 服务列表
│   ├── service-detail/# 服务详情
│   └── search/        # 搜索
├── store/            # Pinia stores（全部就绪）
│   ├── user.js        # 用户状态
│   ├── service.js     # 服务状态
│   ├── address.js     # 地址管理
│   ├── order.js       # 订单管理
│   ├── review.js      # 评价管理
│   └── complaint.js   # 投诉管理
├── utils/            # 工具函数
│   ├── request.js     # 网络请求（对齐 API v1.0）
│   └── storage.js     # 本地存储
└── uni.scss          # 全局设计 Token
config/
├── dev.js            # 开发环境 BASE_URL
└── prod.js           # 生产环境 BASE_URL
```
