# nursing-user-service 接口实现逻辑

> 本文按 `nursing-catalog-service` 的文档结构说明当前源码的调用链、校验、数据写入与边界行为，并与 `nursing-user-service_API.md` 一一对应。

## 1. 架构与通用规则

```text
Gateway -> Controller -> User/Sms/File Service -> MyBatis Mapper -> user_db
                                      |-> Redis
                                      |-> SMS outbox worker -> SMS provider
                                      |-> local file system
```

匿名接口为短信发送、注册、登录、重置密码；登出、资料和上传接口通过 `UserTokenInterceptor` 取得可信身份。默认模式要求网关注入 `X-Gateway-Token` 与 `X-User-Id`；仅开启 `nursing.auth.allow-local-token-validation` 时，服务才自行验证 Bearer JWT。

所有成功结果通过 `Result.success` 返回。`GlobalExceptionHandler` 将 Bean Validation 错误转换为 HTTP `400`、`code=1000`，将业务异常转换为其声明的状态和业务码。

## 2. `POST /api/v1/users/sms-code`

### 调用链

```text
SmsController.sendSmsCode
  -> SmsService.sendSmsCode
    -> UserMapper / SmsSendRequestMapper
    -> Redis Lua rate limiter
    -> sms_send_request + sms_outbox_event
  -> Result<SmsCodeResponse>
```

### 执行步骤

1. 校验手机号、短信类型和 UUID 形式的 `Idempotency-Key`；`register` 要求用户不存在，`login` 与 `reset_password` 要求用户存在。
2. 解析 `X-Forwarded-For`、`X-Real-IP` 或远端地址作为请求 IP。
3. 同一幂等键且请求内容相同则回放已有请求；内容不同返回冲突。
4. 首次请求在一个事务内创建发送请求和 Outbox 事件，并通过 Redis Lua 原子占用手机号冷却、手机号日额度、IP 小时额度和 IP 日额度。
5. 接口立即返回 `PENDING`。Worker 领取 Outbox 后生成六位验证码、写入 BCrypt 哈希的 `sms_record`，再调用短信发送器。
6. 发送受理成功后才把明文验证码写入 Redis（默认 300 秒）；明确失败释放冷却键；未知结果记为 `UNKNOWN`，不自动重发。

### 边界与可靠性

- 验证码明文不会写入数据库、Outbox 或日志。
- 相同业务重试必须复用幂等键；限流键由 Lua 脚本原子处理，避免并发绕过额度。
- 供应商回执 HTTP 路由当前未实际暴露，不能作为客户端接口调用。

## 3. `GET /api/v1/users/sms-code/requests/{requestId}`

### 调用链

```text
SmsController.querySmsRequest -> SmsService.querySmsRequest -> SmsSendRequestMapper
```

### 执行步骤

1. 用路径中的 `requestId` 和请求头中的 `Idempotency-Key` 查询发送请求。
2. 仅密钥匹配时返回与发送接口相同的任务状态对象。

### 边界与可靠性

- 用于首次成功响应丢失后的恢复，不返回手机号、验证码或供应商内部错误。
- 前端不应轮询该接口作为正常的短信倒计时机制。

## 4. `POST /api/v1/users/register`

### 调用链

```text
UserController.register -> UserService.register -> SmsService.verifyCode -> UserMapper -> JWT service
```

### 执行步骤

1. 校验手机号、六位注册验证码、密码强度和可选昵称。
2. 在事务中确认手机号未注册，消费 `register` 类型验证码。
3. 使用雪花 ID 和 BCrypt 密码创建用户；未提供昵称时使用“用户”加手机号后四位。
4. 签发 HS256 JWT，同时写入 `user_token` 与 Redis 在线 Token 键，返回 HTTP `201`。

### 边界与可靠性

- 数据库手机号唯一键竞争会转换为 `2008`，而不是创建重复账号。
- 注册不写 `register_ip`，也不创建后台角色或 RBAC 数据。

## 5. `POST /api/v1/users/login`

### 调用链

```text
UserController.login -> UserService.login -> UserMapper + BCrypt/SmsService -> JWT service
```

### 执行步骤

1. 根据 `loginMode` 分支处理密码或短信登录，并读取未逻辑删除用户。
2. 检查用户存在且未禁用；密码模式调用 BCrypt，短信模式消费 `login` 验证码。
3. 更新最后登录时间，签发并持久化新 JWT，返回掩码手机号的用户信息。

### 边界与可靠性

- 密码错误、验证码错误、账号不存在和禁用账号使用不同业务码。
- 登录会新增 Token，不会撤销同一用户的其他有效 Token。

## 6. `POST /api/v1/users/logout`

### 调用链

```text
UserController.logout -> UserService.logout -> JWT parser -> Redis blacklist
```

### 执行步骤

1. 从 `Authorization: Bearer <JWT>` 提取并验证 JWT。
2. 将 `jti` 写入 `jwt:blacklist:{tokenId}`，TTL 为 Token 剩余有效期。
3. 返回成功结果。

### 边界与可靠性

- 登出不删除 `user_token` 审计记录，也不撤销其他设备 Token。
- 过期 Token 没有可用 TTL，格式或签名错误均返回未授权。

## 7. `POST /api/v1/users/password/reset`

### 调用链

```text
UserController.resetPassword -> UserService.resetPassword -> SmsService.verifyCode -> UserMapper
```

### 执行步骤

1. 校验手机号、重置验证码与新密码格式。
2. 查询用户并消费 `reset_password` 验证码。
3. 拒绝与 BCrypt 旧密码匹配的新密码，随后更新密码哈希和更新时间。

### 边界与可靠性

- 该流程不会拉黑历史 JWT；已签发且未过期的会话仍可继续使用。

## 8. `GET /api/v1/users/profile`

### 调用链

```text
UserController.getProfile -> UserService.getProfile -> UserMapper
```

### 执行步骤

1. 从拦截器注入的 `userId` 查询未逻辑删除用户并检查账号状态。
2. 组装资料响应，掩码手机号和身份证号。
3. 加密身份证号以 `enc:v1:` 开头时，先用 AES-256-GCM 解密再脱敏。

### 边界与可靠性

- 用户不存在按未授权处理；禁用账号返回 `2011`。
- 响应不包含密码、原始 JWT 或明文身份证号。

## 9. `PATCH /api/v1/users/profile`

### 调用链

```text
UserController.updateProfile -> UserService.updateProfile -> UserMapper optimistic update
```

### 执行步骤

1. 校验 `version` 与可选资料字段；身份证号还会校验日期和校验位。
2. 对身份证号使用随机 IV 的 AES-256-GCM 加密后入库。
3. 以 `id + version + is_deleted=0` 条件更新，并将版本加一。
4. 发生版本竞争时重新读取：请求实际提供的字段与当前值一致则视为幂等重试，否则返回冲突。

### 边界与可靠性

- 生产环境没有 32 字节身份证加密密钥时服务不能启动。
- 同版本同内容不会重复写库；不同内容的陈旧版本返回 `2016`。

## 10. `POST /api/v1/files/upload`

### 调用链

```text
FileController.upload -> FileStorageService.upload
  -> temporary file + SHA-256
  -> IdempotentRecordMapper / FileUploadRecordMapper
  -> permanent file move
```

### 执行步骤

1. 校验受保护身份、`Idempotent-Key`、业务类型、大小、扩展名和可选 Content-Type。
2. 写入临时目录并计算 SHA-256，锁定用户级上传幂等记录。
3. 同键同文件回放原结果；同键不同文件或槽位返回冲突；处理中返回处理中冲突。
4. 按 `(userId, bizType, hash, extension)` 复用已有上传记录，或移动文件至哈希目录并写入新记录。
5. 将幂等记录完成并返回文件 URL。

### 边界与可靠性

- 文件系统移动与数据库事务不能组成原子提交；流程会尽力删除临时文件。
- 文件只支持上传、幂等和去重，未实现删除、替换、引用计数和过期幂等记录清理。

## 11. 源码对应表

| 职责 | 源码 |
| --- | --- |
| 用户 HTTP 入口 | `controller/UserController.java` |
| 短信 HTTP 入口 | `controller/SmsController.java` |
| 上传 HTTP 入口 | `controller/FileController.java` |
| 用户、认证和资料 | `service/UserService.java` 及实现类 |
| 短信发送、校验和 Worker | `service/SmsService.java`、`SmsOutboxWorker.java` |
| 上传和幂等 | `service/FileStorageService.java` 及 Mapper |
| 通用响应与异常 | `nursing-common/.../result/`、`exception/GlobalExceptionHandler.java` |
