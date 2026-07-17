# 智慧护理平台管理后台

独立 Vue 3 Web 管理项目，用于平台管理员审核商户、护理人员和服务。

## 启动

```bash
npm install
npm run dev
```

`npm run dev` 默认连接本地网关；需要 Mock 时设置 `VITE_USE_MOCK=true`。

连接真实后端时运行：

```bash
npm run dev:api
```

默认 API 地址为 `http://localhost:8080`，也可以复制 `.env.example` 后通过 `VITE_API_BASE_URL` 覆盖。

## 验证

```bash
npm test
npm run build
```
