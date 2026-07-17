# 智慧护理平台

基于 Uni-app、Vue 3、Pinia 和 uView Plus 的智慧护理平台前端项目。

## 项目启动

### 环境要求

- Node.js 20 或更高版本
- npm 或 pnpm
- HBuilderX 或 Uni-app CLI（可选）

### 安装依赖

```bash
npm install
```

### 启动项目

```bash
npx uni
```

也可以使用：

```bash
npm run dev:h5
```

默认开发环境使用 Mock 数据，后端未启动时也可以直接浏览和测试页面。

### 联调真实后端

复制环境变量示例：

```bash
copy .env.example .env.local
```

修改 `.env.local`：

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080
```

不要提交 `.env.local`、Token、密钥或真实用户隐私数据。

## 常用命令

```bash
npm run lint          # ESLint 检查
npm run test:run      # 执行单元测试
npm run build:h5     # 构建 H5
```

Windows PowerShell 如果提示 `npm.ps1` 被禁止，可以使用：

```bash
npm.cmd run lint
npm.cmd run test:run
npm.cmd run build:h5
```

## 主要目录

```text
src/pages/       页面
src/components/ 公共组件
src/store/      Pinia 状态管理
src/utils/      请求和本地存储工具
src/mock/       开发环境 Mock 数据
src/pages.json  页面和 TabBar 配置
docs/           接口联调和测试数据文档
```

## 开发流程

不要直接修改 `main`。开发前先同步主分支并创建自己的功能分支：

```bash
git switch main
git pull --ff-only
git switch -c feat/your-feature
```

完成后：

```bash
git add <修改的文件>
git commit -m "feat: 描述你的修改"
git push -u origin feat/your-feature
```

然后在 GitHub 创建 Pull Request，等待至少一名成员审核后合并。

完整分支、提交、Review 和提交流程见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 测试账号

| 身份 | 手机号 | 密码 | 说明 |
|---|---|---|---|
| 顾客 / 护理人员 | `13800138000` | `Test123456` | 多角色账号，可在登录页选择顾客或护理人员 |
| 护理人员 | `13800138001` | `Test123456` | 仅护理人员身份 |
| 商户 | `13800138002` | `Test123456` | 康宁护理中心商户成员 |
| 护理申请人 | `13800138003` | `Test123456` | 顾客身份，内置一份被驳回的护理人员申请 |
| 商户申请人 | `13800138004` | `Test123456` | 顾客身份，内置一份被驳回的商户入驻申请 |

Mock 验证码统一为 `123456`。公开注册只创建顾客身份，护理人员和商户身份均需提交资料并通过审核。

## 当前功能

- 登录和注册
- 顾客、护理人员、商户多身份登录基础
- 登录后刷新已审核身份，并在顾客、护理人员和商户工作台之间直接切换
- 首页、服务列表、服务详情、搜索
- 地址管理
- 下单预约和模拟支付
- 订单列表、三维订单状态、派单及履约记录
- 护理人员工作台、任务列表、接单、签到、开始和结束服务
- 商户工作台、订单管理、候选护理人员和人工派单
- 商户服务草稿、规格编辑、提交审核、上架和下架
- 护理人员身份资料、资质证书、技能区域、提交认证和审核状态
- 商户企业主体、负责人、经营资质、服务区域、提交入驻和审核状态
- 商户护理团队、内部成员岗位、邀请合作、启用停用和派单候选联动
- 订单取消规则预览、退款进度、接单超时、无人可派和异常签到处理
- 评价、顾客投诉、商户举证、平台仲裁与退款联动
- 按登录身份隔离的消息中心、未读角标、全部已读和业务页面直达
- 护理人员可编辑排班、请假、每日接单上限和服务区域，商户派单展示冲突原因
- 商户订单分账、可结算金额和结算记录，护理人员佣金与提现记录
- 商户经营报表、订单趋势、服务质量指标和待处理风险监控

## 管理后台

独立管理端位于 `admin/`，使用 Vue 3、Vite、Element Plus、Pinia、Vue Router 和 Axios。

```bash
cd admin
npm install
npm run dev
```

Mock 管理员账号：`admin / 123456`。当前支持审核工作台，商户、护理人员和服务审核，异常监管，投诉仲裁与退款裁决，以及平台待办消息。
