# 团队开发规范

## 分支策略

- `main` 是稳定分支，禁止直接 push。
- 新功能从最新 `main` 创建 `feat/<name>` 分支。
- 缺陷修复使用 `fix/<name>`；工程维护使用 `chore/<name>`；紧急线上修复使用 `hotfix/<name>`。
- 一个分支只处理一个主题，完成后通过 Pull Request 合并。
- 合并方式统一使用 Squash merge，合并后删除远程分支。
- 只有仓库管理员可以修改分支保护和合并规则。

示例：

```text
feat/booking-page
fix/order-payment-status
chore/update-api-doc
```

## 开发流程

```bash
git switch main
git pull --ff-only
git switch -c feat/your-feature

# 开发并提交
git add <files>
git commit -m "feat: add your feature"
git push -u origin feat/your-feature
```

发起 Pull Request 前优先执行：

```bash
npm.cmd run lint
npm.cmd run test:run
npm.cmd run build:h5
```

课程项目阶段，CI 暂不作为合并硬门槛；如果本地环境无法完整执行全部命令，请在 PR 中说明原因，并至少手动验证本次修改涉及的页面流程。

如果使用 pnpm，请使用项目锁定的 pnpm 版本并执行对应脚本。

## Commit 规范

提交信息使用 Conventional Commits：

```text
feat: 新增地址管理页面
fix: 修复订单支付状态
refactor: 优化服务 Store
test: 补充订单流程测试
docs: 更新接口联调文档
chore: 更新依赖
```

## Pull Request 规范

- PR 标题遵循 Commit 规范。
- 每个 PR 只解决一个问题，避免混入无关格式化和重命名。
- 页面改动附运行截图或录屏。
- 接口改动同步更新 API 文档和 Mock。
- 修改公共文件 `src/utils/request.js`、`src/uni.scss`、`src/pages.json`、`src/main.js`、`package.json` 时，PR 描述中说明影响范围。
- 至少通过 1 名成员 Review 后才能合并。
- CI 如果正常运行，应优先修复红色检查；课程项目阶段不因 CI 环境问题阻塞合并。

## 代码约定

- 页面只负责展示和交互，接口调用、数据转换优先放在 Store 或 composable。
- API 字段以 API 文档为准，禁止在页面中散落旧字段兼容逻辑。
- 订单、支付、评价、投诉等写操作必须使用幂等键。
- 不提交 `.env`、真实 Token、密钥、生产账号和真实用户隐私数据。
- 手机号、身份证、地址等敏感数据只能使用测试数据，并避免写入日志。
- 生产环境关闭 Mock 和调试日志。

## Review 重点

- 是否破坏登录态、订单状态流转或支付幂等性。
- 是否正确处理 loading、空状态、错误状态和重复点击。
- 是否存在跨用户数据访问风险。
- 是否补充了接口异常和关键流程测试。
- 是否兼容 H5 和目标小程序平台。
