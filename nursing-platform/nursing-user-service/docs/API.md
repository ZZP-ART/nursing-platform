# nursing-user-service 接口文档

## 公共约定

### 基础路径

服务端口默认 `8081`。通过网关访问时以网关路由为准，服务内接口路径如下文所列。

### 统一响应

所有接口返回 `Result<T>`：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

成功时 `code=0`。业务异常由 `UserExceptionHandler` 返回对应 HTTP 状态和业务错误码。

### 鉴权

需要登录的接口包括：

- `POST /api/v1/users/logout`
- `GET /api/v1/users/profile`
- `PATCH /api/v1/users/profile`
- `POST /api/v1/files/upload`

默认模式下服务信任网关注入的请求头：

| Header | 说明 |
|---|---|
| `X-Gateway-Token` | 与 `nursing.gateway.trusted-token` 一致时视为可信网关请求 |
| `X-User-Id` | 网关解析出的用户 ID |

当 `nursing.auth.allow-local-token-validation=true` 时，也支持本服务直接校验：

```http
Authorization: Bearer <jwt>
```

登出接口仍需要 `Authorization` 请求头用于提取要拉黑的 JWT。

### 常见错误码

| HTTP 状态 | code | message | 场景 |
|---:|---:|---|---|
| 400 | 1000 | 参数错误或具体验证消息 | 参数缺失、格式错误、业务类型不正确等 |
| 401 | 1002 | 未授权，请先登录 / Unauthorized | Token 缺失、无效、过期，或网关注入身份缺失 |
| 401 | 1003 | Token 已被列入黑名单 | Token 已登出 |
| 403 | 1004 | Forbidden | 未开启本地 Token 校验且不是可信网关请求 |
| 409 | 2016 | 个人资料已被更新，请刷新后重试 | 个人资料版本冲突 |
| 409 | 2017 | 上传幂等键冲突 | 同一幂等键对应不同上传内容 |
| 409 | 2018 | 文件上传处理中，请稍后重试 | 相同幂等键的首个请求尚未完成 |
| 422 | 2003 | 手机号已被注册 | 注册短信时手机号已存在 |
| 422 | 2004 | 手机号未注册 | 登录/重置密码相关流程中用户不存在 |
| 422 | 2011 | 账号已被禁用 | 用户状态为禁用 |
| 422 | 2013 | 身份证号格式不正确 | 身份证格式或校验位错误 |
| 429 | 2001 | 发送过于频繁，请稍后重试 | 短信发送间隔限制 |
| 429 | 2002 | 今日发送次数已达上限 / 请求过于频繁，请稍后重试 | 手机号或 IP 额度超限 |
| 500 | 2005 | 短信发送失败，请稍后重试 | 阿里云短信失败或结果未知 |

Validation 框架产生的参数错误响应形态需确认；源码中未看到 user-service 专用的 `MethodArgumentNotValidException` 处理器。

## 1. 发送短信验证码

| 项目 | 内容 |
|---|---|
| 接口名称 | 发送短信验证码 |
| 请求方法 | `POST` |
| 请求路径 | `/api/v1/users/sms-code` |
| 权限要求 | 匿名 |
| Controller | `SmsController.sendSmsCode` |

接口说明：发送注册、登录或重置密码验证码。验证码有效期默认 300 秒。

请求参数：无 Query 参数。

请求体：

| 字段 | 类型 | 必填 | 规则 | 说明 |
|---|---|---|---|---|
| `phone` | string | 是 | `^1\d{10}$` | 手机号 |
| `smsType` | string | 是 | `register`、`login`、`reset_password` | 短信类型 |

请求体示例：

```json
{
  "phone": "13812345678",
  "smsType": "register"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "验证码已发送",
  "data": {
    "expireSeconds": 300,
    "retryAfterSeconds": null
  }
}
```

状态码/错误码：

| HTTP 状态 | code | 说明 |
|---:|---:|---|
| 200 | 0 | 发送成功 |
| 400 | 1000 | 参数校验失败，具体响应需确认 |
| 422 | 2003 | 注册短信但手机号已注册 |
| 422 | 2004 | 登录或重置密码短信但手机号未注册 |
| 429 | 2001 | 发送过于频繁，`data.retryAfterSeconds` 返回剩余等待秒数 |
| 429 | 2002 | 手机号日限或 IP 限流 |
| 500 | 2005 | 短信发送失败 |

备注：数据库 `sms_record.code` 保存 BCrypt 哈希；Redis `sms:code:{smsType}:{phone}` 保存明文验证码。

## 2. 用户注册

| 项目 | 内容 |
|---|---|
| 接口名称 | 用户注册 |
| 请求方法 | `POST` |
| 请求路径 | `/api/v1/users/register` |
| 权限要求 | 匿名 |
| Controller | `UserController.register` |

接口说明：使用手机号、注册验证码和密码创建用户，成功后直接签发 JWT。

请求体：

| 字段 | 类型 | 必填 | 规则 | 说明 |
|---|---|---|---|---|
| `phone` | string | 是 | `^1\d{10}$` | 手机号 |
| `smsCode` | string | 是 | `^\d{6}$` | 注册验证码 |
| `password` | string | 是 | 8-32 位，至少包含字母和数字 | 登录密码 |
| `nickname` | string | 否 | 2-16 个字符 | 昵称；不传时使用默认昵称 |

请求体示例：

```json
{
  "phone": "13812345678",
  "smsCode": "123456",
  "password": "abc123456",
  "nickname": "张三"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expireTime": "2026-07-17T10:00:00",
    "user": {
      "userId": 123456789012345678,
      "phone": "138****5678",
      "nickname": "张三",
      "avatar": null,
      "gender": 0,
      "status": 0,
      "version": 0
    }
  }
}
```

状态码/错误码：

| HTTP 状态 | code | 说明 |
|---:|---:|---|
| 201 | 0 | 注册成功 |
| 400 | 1000 | 参数校验失败，具体响应需确认 |
| 400 | 2006 | 验证码错误 |
| 400 | 2007 | 验证码已过期 |
| 409 | 2008 | 该手机号已注册 |

备注：成功后会写入 `user`、`user_token`，并写入 Redis Token 记录。

## 3. 用户登录

| 项目 | 内容 |
|---|---|
| 接口名称 | 用户登录 |
| 请求方法 | `POST` |
| 请求路径 | `/api/v1/users/login` |
| 权限要求 | 匿名 |
| Controller | `UserController.login` |

接口说明：支持密码登录和短信验证码登录。

请求体：

| 字段 | 类型 | 必填 | 规则 | 说明 |
|---|---|---|---|---|
| `phone` | string | 是 | `^1\d{10}$` | 手机号 |
| `loginMode` | string | 是 | `password`、`sms` | 登录方式 |
| `password` | string | 条件必填 | 密码登录时必填 | 登录密码 |
| `smsCode` | string | 条件必填 | `^\d{6}$` | 短信登录时必填 |

密码登录示例：

```json
{
  "phone": "13812345678",
  "loginMode": "password",
  "password": "abc123456"
}
```

短信登录示例：

```json
{
  "phone": "13812345678",
  "loginMode": "sms",
  "smsCode": "123456"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expireTime": "2026-07-17T10:00:00",
    "user": {
      "userId": 123456789012345678,
      "phone": "138****5678",
      "nickname": "张三",
      "avatar": "http://gateway:8080/uploads/avatar/...",
      "gender": 0,
      "status": 0,
      "version": 1
    }
  }
}
```

状态码/错误码：

| HTTP 状态 | code | 说明 |
|---:|---:|---|
| 200 | 0 | 登录成功 |
| 400 | 1000 | 登录方式、密码或验证码参数错误 |
| 400 | 2006 | 验证码错误 |
| 400 | 2007 | 验证码已过期 |
| 400 | 2010 | 密码错误 |
| 422 | 2004 | 手机号未注册 |
| 422 | 2011 | 账号已被禁用 |

备注：登录成功会更新 `last_login_time` 并签发新 JWT。

## 4. 用户登出

| 项目 | 内容 |
|---|---|
| 接口名称 | 用户登出 |
| 请求方法 | `POST` |
| 请求路径 | `/api/v1/users/logout` |
| 权限要求 | 登录用户 |
| Controller | `UserController.logout` |

接口说明：将当前 JWT 的 `tokenId` 写入 Redis 黑名单。

请求头：

| Header | 必填 | 说明 |
|---|---|---|
| `Authorization` | 是 | `Bearer <jwt>` |
| `X-Gateway-Token` | 条件必填 | 默认网关模式下需要 |
| `X-User-Id` | 条件必填 | 默认网关模式下需要 |

请求体：无。

成功响应：

```json
{
  "code": 0,
  "message": "登出成功",
  "data": null
}
```

状态码/错误码：

| HTTP 状态 | code | 说明 |
|---:|---:|---|
| 200 | 0 | 登出成功 |
| 401 | 1002 | 未授权、Token 缺失、过期或格式错误 |
| 403 | 1004 | 非可信网关且未开启本地 Token 校验 |

备注：登出不会修改 JWT 本身；黑名单 key 为 `jwt:blacklist:{tokenId}`。

## 5. 重置密码

| 项目 | 内容 |
|---|---|
| 接口名称 | 重置密码 |
| 请求方法 | `POST` |
| 请求路径 | `/api/v1/users/password/reset` |
| 权限要求 | 匿名 |
| Controller | `UserController.resetPassword` |

接口说明：通过重置密码验证码设置新密码。

请求体：

| 字段 | 类型 | 必填 | 规则 | 说明 |
|---|---|---|---|---|
| `phone` | string | 是 | `^1\d{10}$` | 手机号 |
| `smsCode` | string | 是 | `^\d{6}$` | 重置密码验证码 |
| `newPassword` | string | 是 | 8-32 位，至少包含字母和数字 | 新密码 |

请求体示例：

```json
{
  "phone": "13812345678",
  "smsCode": "123456",
  "newPassword": "newabc123456"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "密码重置成功",
  "data": null
}
```

状态码/错误码：

| HTTP 状态 | code | 说明 |
|---:|---:|---|
| 200 | 0 | 重置成功 |
| 400 | 1000 | 参数校验失败，具体响应需确认 |
| 400 | 2006 | 验证码错误 |
| 400 | 2007 | 验证码已过期 |
| 400 | 2012 | 新密码不能与旧密码相同 |
| 422 | 2004 | 手机号未注册 |

备注：成功后只更新密码，不主动拉黑历史 Token；是否需要重置密码后强制下线需确认。

## 6. 查询个人资料

| 项目 | 内容 |
|---|---|
| 接口名称 | 查询个人资料 |
| 请求方法 | `GET` |
| 请求路径 | `/api/v1/users/profile` |
| 权限要求 | 登录用户 |
| Controller | `UserController.getProfile` |

接口说明：查询当前登录用户资料。

请求头：见公共鉴权说明。

请求参数：无。

请求体：无。

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 123456789012345678,
    "phone": "138****5678",
    "nickname": "张三",
    "avatar": "http://gateway:8080/uploads/avatar/...",
    "gender": 0,
    "idCard": "110***********1234",
    "status": 0,
    "version": 1,
    "lastLoginTime": "2026-07-10T10:00:00",
    "createTime": "2026-07-01T09:00:00"
  }
}
```

状态码/错误码：

| HTTP 状态 | code | 说明 |
|---:|---:|---|
| 200 | 0 | 查询成功 |
| 401 | 1002 | 未授权或用户不存在 |
| 401 | 1003 | Token 已被列入黑名单 |
| 403 | 1004 | 非可信网关且未开启本地 Token 校验 |
| 422 | 2011 | 账号已被禁用 |

备注：`idCard` 仅在存在时返回，且为脱敏值。

## 7. 修改个人资料

| 项目 | 内容 |
|---|---|
| 接口名称 | 修改个人资料 |
| 请求方法 | `PATCH` |
| 请求路径 | `/api/v1/users/profile` |
| 权限要求 | 登录用户 |
| Controller | `UserController.updateProfile` |

接口说明：按非空字段更新当前用户资料，使用 `version` 做乐观锁。

请求头：见公共鉴权说明。

请求体：

| 字段 | 类型 | 必填 | 规则 | 说明 |
|---|---|---|---|---|
| `version` | integer | 是 | `>=0` | 当前资料版本号 |
| `nickname` | string | 否 | 2-16 个字符 | 昵称 |
| `avatar` | string | 否 | 最长 256 个字符 | 头像 URL |
| `gender` | integer | 否 | 0、1、2 | 性别，0 保密、1 男、2 女 |
| `idCard` | string | 否 | 18 个字符且校验位正确 | 身份证号 |

请求体示例：

```json
{
  "version": 1,
  "nickname": "张三",
  "avatar": "http://gateway:8080/uploads/avatar/123.png",
  "gender": 1,
  "idCard": "110101199001011234"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "修改成功",
  "data": {
    "userId": 123456789012345678,
    "nickname": "张三",
    "avatar": "http://gateway:8080/uploads/avatar/123.png",
    "gender": 1,
    "version": 2
  }
}
```

状态码/错误码：

| HTTP 状态 | code | 说明 |
|---:|---:|---|
| 200 | 0 | 修改成功，或重复提交相同内容 |
| 400 | 1000 | 参数校验失败，具体响应需确认 |
| 401 | 1002 | 未授权或用户不存在 |
| 401 | 1003 | Token 已被列入黑名单 |
| 403 | 1004 | 非可信网关且未开启本地 Token 校验 |
| 409 | 2016 | 资料版本冲突且提交内容不同 |
| 422 | 2011 | 账号已被禁用 |
| 422 | 2013 | 身份证号格式不正确 |

备注：身份证号入库前加密；响应不返回身份证号。

## 8. 上传文件

| 项目 | 内容 |
|---|---|
| 接口名称 | 上传文件 |
| 请求方法 | `POST` |
| 请求路径 | `/api/v1/files/upload` |
| 权限要求 | 登录用户 |
| Controller | `FileController.upload` |
| Content-Type | `multipart/form-data` |

接口说明：上传头像、评价图片或投诉图片。接口要求幂等键，用于处理前端重试和重复点击。

请求头：

| Header | 必填 | 说明 |
|---|---|---|
| `Idempotent-Key` | 是 | 幂等键，最长 64 个字符；同一次上传重试应复用 |
| `Authorization` | 条件必填 | 本地 Token 校验或登出场景需要 |
| `X-Gateway-Token` | 条件必填 | 默认网关模式下需要 |
| `X-User-Id` | 条件必填 | 默认网关模式下需要 |

表单参数：

| 字段 | 类型 | 必填 | 规则 | 说明 |
|---|---|---|---|---|
| `file` | file | 是 | 非空，最大 10MB | 上传文件 |
| `bizType` | string | 是 | `avatar`、`review_image`、`complaint_image` | 业务类型 |

支持文件类型：

| 扩展名 | Content-Type |
|---|---|
| `jpg` | `image/jpeg` |
| `jpeg` | `image/jpeg` |
| `png` | `image/png` |
| `gif` | `image/gif` |
| `mp4` | `video/mp4` |

请求示例：

```bash
curl -X POST "http://localhost:8081/api/v1/files/upload" \
  -H "Authorization: Bearer <jwt>" \
  -H "Idempotent-Key: avatar-slot-001" \
  -F "bizType=avatar" \
  -F "file=@avatar.png;type=image/png"
```

成功响应：

```json
{
  "code": 0,
  "message": "上传成功",
  "data": {
    "fileUrl": "http://gateway:8080/uploads/avatar/123456789012345678/ab/abcdef....png",
    "fileName": "abcdef....png",
    "fileSize": 204800,
    "bizType": "avatar"
  }
}
```

状态码/错误码：

| HTTP 状态 | code | 说明 |
|---:|---:|---|
| 200 | 0 | 上传成功；重复提交同一文件时返回同一结果 |
| 400 | 1000 | `bizType` 不正确、`Idempotent-Key` 缺失或过长、文件格式不支持 |
| 400 | 2014 | 文件大小超过限制 |
| 400 | 2015 | 文件为空或保存失败 |
| 401 | 1002 | 未授权 |
| 401 | 1003 | Token 已被列入黑名单 |
| 403 | 1004 | 非可信网关且未开启本地 Token 校验 |
| 409 | 2017 | 上传幂等键冲突 |
| 409 | 2018 | 文件上传处理中，请稍后重试 |

备注：

- 服务内部幂等键为 `fu:{userId}:{Idempotent-Key}`。
- 文件实际相对路径格式为 `{bizType}/{userId}/{hashPrefix}/{sha256}.{ext}`。
- `fileUrl` 的外部可访问性依赖 `nursing.file.public-base-url` 对应的静态资源或网关配置，源码中未看到静态资源映射，需确认。

## Redis Key 参考

| Key | 用途 |
|---|---|
| `sms:rate:{smsType}:{phone}` | 同手机号同短信类型发送间隔限制 |
| `sms:quota:phone:day:{phone}:{yyyyMMdd}` | 手机号每日短信额度 |
| `sms:quota:ip:hour:{ip}:{yyyyMMddHH}` | IP 每小时短信额度 |
| `sms:quota:ip:day:{ip}:{yyyyMMdd}` | IP 每日短信额度 |
| `sms:code:{smsType}:{phone}` | 短信验证码缓存 |
| `sms:verify:fail:{smsType}:{phone}` | 验证码错误次数 |
| `user:token:{userId}:{tokenId}` | 已签发 Token 在线记录 |
| `jwt:blacklist:{tokenId}` | 登出 Token 黑名单 |

## 需确认事项

- Validation 参数错误的统一响应格式需确认。
- 网关到 user-service 的生产鉴权请求头规范需确认。
- 文件 URL 的静态访问或网关映射规则需确认。
- 重置密码后是否需要强制历史 Token 失效需确认。
- `idempotent_record.expire_time` 的清理任务或保留周期需确认。

## 文档不足与后续补充清单

本接口文档已覆盖 `nursing-user-service` 当前 Controller 暴露的全部接口，并尽量根据 DTO、Service、异常处理和 Mapper 给出请求/响应/错误码说明。以下内容属于跨服务或部署层面的接口语义，单看 user-service 无法完全确认，后续补齐后可将“需确认”改为明确约定。

| 待补充内容 | 当前接口文档处理方式 | 后续确认来源 |
|---|---|---|
| 参数校验失败响应 | 已按 `1000` 和“具体响应需确认”标注 | `nursing-common`、Spring 全局异常处理、接口联调响应 |
| 生产鉴权 Header | 已说明网关模式和本地 Bearer Token 模式 | `nursing-gateway` 鉴权 Filter、生产配置、网关转发头约定 |
| 上传文件访问 URL | 已说明 `fileUrl` 由 `nursing.file.public-base-url` 拼接，访问映射需确认 | 网关路由、Nginx、静态资源服务或对象存储配置 |
| 重置密码后的 Token 处理 | 已标注当前接口不主动拉黑历史 Token | 产品安全规则、账号会话管理设计 |
| 幂等记录清理 | 已说明接口会写入 `idempotent_record`，但未声明自动清理 | 定时任务代码、运维脚本、数据库维护策略 |
| 短信验证码过期审计 | 已说明 Redis TTL 控制验证码有效期，未承诺 DB 过期状态自动更新 | 短信审计需求、定时任务或运营后台实现 |
