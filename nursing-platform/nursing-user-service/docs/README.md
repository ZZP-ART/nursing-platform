# nursing-user-service 业务说明

## 服务定位

`nursing-user-service` 是互联网+智慧护理平台的用户域服务，负责用户注册登录、短信验证码、JWT 登录态、个人资料维护、身份证敏感信息加密、文件上传与上传幂等控制。服务通过网关对外提供用户与文件上传接口，内部使用 MySQL 持久化用户、短信、Token 与上传记录，使用 Redis 承担验证码缓存、短信限流、Token 在线记录和黑名单。

当前模块在仓库中的路径为 `nursing-platform/nursing-user-service`，对应用户所称的 user-service。

## 业务背景

护理平台需要为移动端用户提供账号体系和基础身份资料能力。用户可通过手机号注册、密码或短信验证码登录，登录后维护头像、昵称、性别、身份证号等资料，并上传头像、评价图片、投诉图片等业务文件。由于短信和文件上传容易受到重复提交、重试和并发请求影响，本服务在短信发送、验证码校验、个人资料更新、文件上传等场景中加入了限流、乐观锁、幂等和去重能力。

## 核心功能

- 短信验证码：支持注册、登录、重置密码三类验证码，按手机号和 IP 做 Redis 原子限流。
- 用户注册：手机号 + 注册验证码 + 密码注册，默认昵称为 `用户` + 手机号后四位。
- 用户登录：支持密码登录和短信验证码登录，成功后签发 JWT。
- 用户登出：将 JWT `tokenId` 写入 Redis 黑名单，TTL 与 Token 剩余有效期一致。
- 密码重置：通过重置密码验证码修改密码，并禁止新旧密码相同。
- 个人资料：查询当前用户资料，更新昵称、头像、性别、身份证号。
- 乐观锁：个人资料更新要求传入 `version`，避免并发覆盖。
- 文件上传：支持头像、评价图片、投诉图片上传，校验大小、扩展名和 Content-Type。
- 上传幂等与去重：通过 `Idempotent-Key`、数据库幂等记录和 SHA-256 文件哈希处理重试与重复上传。
- 敏感信息保护：身份证号使用 AES-GCM 加密入库，响应时脱敏。

## 主要业务流程

### 短信验证码

1. 校验手机号和短信类型，短信类型包括 `register`、`login`、`reset_password`。
2. 根据业务类型校验手机号状态：注册验证码要求手机号未注册，登录和重置密码验证码要求手机号已注册。
3. 解析客户端 IP，优先读取 `X-Forwarded-For`，其次 `X-Real-IP`，最后使用远端地址。
4. 通过 Redis Lua 脚本原子检查并占用短信发送额度：同手机号同类型发送间隔、手机号日限、IP 小时限、IP 日限。
5. 生成 6 位数字验证码，向 `sms_record` 写入待发送记录，验证码在数据库中保存 BCrypt 哈希。
6. 调用阿里云短信。成功后将明文验证码写入 Redis，失败或未知结果更新短信记录状态。
7. 校验验证码时读取 Redis 明文验证码，错误次数超过配置后删除验证码，成功后删除验证码和错误计数，并将最近一条短信记录标记为已验证。

### 注册与登录

注册流程先检查手机号是否已存在，再校验 `register` 类型验证码，使用 BCrypt 加密密码，写入 `user` 表并签发 JWT。登录流程先按手机号查询用户并检查账号状态，密码登录使用 BCrypt 校验密码，短信登录校验 `login` 类型验证码，成功后更新最后登录时间并签发 JWT。

JWT 中包含 `jti/tokenId`、`sub/userId`、`userId`、`phone`、签发时间和过期时间。服务会将 Token 审计记录写入 `user_token`，并在 Redis 写入 `user:token:{userId}:{tokenId}`。

### 鉴权与登出

受保护接口包括查询资料、修改资料、登出和文件上传。默认生产模式更偏向由网关鉴权后转发：请求需携带可信网关头 `X-Gateway-Token` 和 `X-User-Id`，服务只使用服务端注入的用户身份。开发环境或显式开启 `nursing.auth.allow-local-token-validation=true` 时，服务也可以直接校验 `Authorization: Bearer <token>`。

登出时服务解析 Authorization 中的 JWT，将 `tokenId` 写入 `jwt:blacklist:{tokenId}`。本地 Token 校验会拒绝已在黑名单中的 Token。

### 个人资料维护

查询个人资料返回脱敏手机号、昵称、头像、性别、状态、资料版本、脱敏身份证号、最后登录时间和创建时间。修改资料时必须提交当前 `version`；服务按 `id + version + is_deleted=0` 条件更新资料，并将 `version` 加 1。若版本不一致但提交内容与当前资料完全相同，视为重复提交并返回当前资料；若内容不同，返回版本冲突。

身份证号入库前会校验 18 位格式和校验位，并使用 AES-GCM 加密，响应时只返回脱敏结果。

### 文件上传

文件上传接口要求登录身份、`Idempotent-Key`、`file` 和 `bizType`。服务先校验业务类型、幂等键、文件大小、扩展名和 Content-Type，然后将文件暂存到上传目录的 `.tmp/{userId}`，计算 SHA-256。幂等记录键为 `fu:{userId}:{Idempotent-Key}`，状态为处理中或已完成。

同一用户、同一幂等键、同一文件内容重复请求会返回同一上传结果；同一幂等键但文件内容或业务类型不同会返回冲突；同一用户、同一业务类型、同一 SHA-256 和扩展名的文件会复用已有文件记录，避免重复落盘。

## 角色与权限说明

源码中未定义后台角色或 RBAC 权限模型。当前权限边界为“匿名接口”和“登录用户接口”：

| 权限类别 | 接口范围 | 说明 |
|---|---|---|
| 匿名可访问 | 发送验证码、注册、登录、重置密码 | 不要求登录态 |
| 登录用户 | 登出、查询资料、修改资料、文件上传 | 需要网关注入身份或本服务校验 Bearer Token |

## 关键数据模型

| 表 | 说明 | 关键字段 |
|---|---|---|
| `user` | 用户账号与资料 | `id`、`phone`、`password`、`nickname`、`avatar`、`gender`、`id_card`、`status`、`last_login_time`、`is_deleted`、`version` |
| `user_token` | 已签发 Token 审计 | `id`、`user_id`、`token`、`expire_time`、`is_deleted`、`create_time` |
| `sms_record` | 短信发送与验证审计 | `phone`、`sms_type`、`code`、`status`、`request_ip`、`provider`、`provider_request_id`、`failure_reason`、`send_time`、`expire_time`、`verify_time` |
| `idempotent_record` | 上传幂等控制 | `idempotent_key`、`biz_type`、`biz_id`、`status`、`expire_time` |
| `file_upload_record` | 文件上传结果与去重索引 | `user_id`、`idempotent_key`、`biz_type`、`file_hash`、`file_name`、`file_url`、`relative_path`、`file_size`、`content_type`、`file_ext` |

主要状态值：

| 字段 | 值 | 含义 |
|---|---:|---|
| `user.status` | 0 | 正常 |
| `user.status` | 1 | 禁用 |
| `sms_record.status` | 0 | 待发送 |
| `sms_record.status` | 1 | 已发送 |
| `sms_record.status` | 2 | 发送失败 |
| `sms_record.status` | 3 | 已验证 |
| `sms_record.status` | 5 | 结果未知 |
| `idempotent_record.status` | 0 | 处理中 |
| `idempotent_record.status` | 1 | 已完成 |

## 外部依赖

| 依赖 | 用途 |
|---|---|
| MySQL | 保存用户、短信、Token、幂等和文件上传记录 |
| Redis | 短信验证码缓存、短信限流计数、验证码错误计数、Token 在线记录、Token 黑名单 |
| Nacos Discovery / Config | 服务发现与配置中心 |
| Gateway | 统一入口、路由转发、可信身份头注入 |
| 阿里云短信服务 | 发送短信验证码 |
| 本地文件系统 | 保存上传文件，默认目录由 `nursing.file.upload-dir` 指定 |

## 配置说明

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` | `8081` | 服务端口 |
| `spring.datasource.url` | `jdbc:mysql://mysql:3306/user_db...` | 用户库连接 |
| `spring.data.redis.host` | `redis` | Redis 主机 |
| `spring.cloud.nacos.discovery.server-addr` | `nacos:8848` | Nacos 注册中心地址 |
| `nursing.auth.allow-local-token-validation` | `false` | 是否允许服务本地校验 Bearer Token |
| `nursing.gateway.trusted-token` | 空 | 网关注入可信令牌 |
| `nursing.jwt.secret` | 空 | JWT HS256 密钥，至少 32 bytes |
| `nursing.jwt.expire-seconds` | `604800` | JWT 有效期，默认 7 天 |
| `nursing.security.id-card-key` | 空 | 身份证 AES-GCM 密钥，需 32 bytes 或 `base64:` 格式 |
| `nursing.sms.rate-limit-seconds` | `60` | 同手机号同类型短信发送间隔 |
| `nursing.sms.phone-daily-limit` | `10` | 同手机号每日发送上限 |
| `nursing.sms.ip-hourly-limit` | `60` | 同 IP 每小时发送上限 |
| `nursing.sms.ip-daily-limit` | `300` | 同 IP 每日发送上限 |
| `nursing.sms.verify-max-attempts` | `5` | 同一验证码最大错误次数 |
| `nursing.sms.expire-seconds` | `300` | 验证码有效期 |
| `nursing.sms.sign-name` | 环境变量 | 阿里云短信签名 |
| `nursing.sms.templates.register/login/reset-password` | 环境变量 | 阿里云短信模板 |
| `nursing.sms.aliyun.*` | 环境变量 | 阿里云短信 AccessKey、Endpoint 与超时 |
| `nursing.file.upload-dir` | `uploads` | 上传文件落盘目录 |
| `nursing.file.public-base-url` | `http://gateway:8080/uploads` | 对外访问 URL 前缀 |
| `nursing.snowflake.worker-id` | `1` | 雪花算法 workerId |
| `nursing.snowflake.datacenter-id` | `1` | 雪花算法 datacenterId |

## 启动与部署说明

本项目为 Maven 多模块工程，父项目使用 Spring Boot 3.5.0 和 Java 21。用户服务模块依赖 `nursing-common`。

本地开发常用方式：

```bash
cd nursing-platform
mvn -pl nursing-user-service -am spring-boot:run -Dspring-boot.run.profiles=dev
```

打包方式：

```bash
cd nursing-platform
mvn -pl nursing-user-service -am -DskipTests package
```

Docker 镜像运行时基于 Eclipse Temurin 21 JRE，暴露 `8081`，健康检查地址为 `/actuator/health`。Dockerfile 默认将上传目录设为 `/app/data/uploads`。docker-compose 中 `user-service` 依赖 MySQL、Redis、Nacos，并通过网关对外访问。

数据库初始化脚本位于 `deploy/mysql/init/01_user_schema.sql`，会创建 `user_db` 及用户服务所需表。

## 开发与维护注意事项

- 接口文档应以 Controller、DTO、Service、Mapper 和 SQL 脚本为准，旧计划文档不能直接作为实现依据。
- 生产环境必须配置 `NURSING_JWT_SECRET`，且长度不少于 32 bytes。
- 生产环境使用身份证加密时必须配置 32 bytes 的 `NURSING_ID_CARD_ENCRYPTION_KEY`，否则 `prod` profile 启动会失败。
- 阿里云短信签名、模板和 AccessKey 为空时，`AliyunSmsSender` 初始化会失败。
- 默认模式下受保护接口依赖网关注入身份；若绕过网关直接调用，需要开启本地 Token 校验。
- 文件上传必须携带 `Idempotent-Key`。前端重试同一次上传应复用同一个 key，不同文件或不同上传槽位应使用不同 key。
- 上传文件按 SHA-256 和业务类型去重，删除或替换文件的业务语义目前不在本服务内实现。
- `idempotent_record.expire_time` 当前用于记录过期时间，但源码未看到自动清理任务，需由运维或后续任务处理历史记录清理。
- `user.register_ip` 表字段存在，但当前注册流程未写入注册 IP。

## 已知限制或需确认事项

- 需确认生产环境中网关是否已统一完成 JWT 校验并注入 `X-Gateway-Token`、`X-User-Id`。
- 需确认上传文件的静态资源映射或网关转发规则；本服务生成 `fileUrl`，但源码中未看到静态文件访问 Controller。
- 需确认短信模板参数是否统一为 `{"code":"验证码"}`，当前阿里云调用固定只传 `code`。
- 需确认是否需要短信发送记录过期状态自动维护；当前源码未看到将过期短信记录更新为 `status=4` 的任务。
- 需确认 `idempotent_record` 和历史上传临时文件的清理策略。
- 需确认是否需要多端登录、Token 主动失效、账号注销、后台禁用用户等更完整账号生命周期能力。

## 文档不足与后续补充清单

本文档已基于 `nursing-user-service` 当前源码、模块配置、数据库脚本和现有 docs 整理完成，但以下内容无法仅通过 user-service 单模块源码得到最终答案。后续如果要形成跨服务级别的最终结论，需要补充核对相关微服务、部署配置或产品规则。

| 待补充内容 | 当前 user-service 可确认的信息 | 还需核对的来源 |
|---|---|---|
| Validation 参数错误响应格式 | DTO 使用 Jakarta Validation 注解，但 user-service 内未看到专用参数异常处理器 | `nursing-common` 全局异常处理、网关异常包装、实际接口联调结果 |
| 生产网关鉴权与身份注入 | `UserTokenInterceptor` 支持可信网关头 `X-Gateway-Token`、`X-User-Id`，也支持本地 Bearer Token 校验开关 | `nursing-gateway` JWT Filter、路由配置、生产环境变量 |
| 文件 URL 访问方式 | 上传成功后生成 `fileUrl`，并将文件保存到本地上传目录 | 网关 `/uploads/**` 路由、Nginx/静态资源配置、对象存储或文件服务配置 |
| 重置密码后的会话策略 | 当前只更新密码，不主动拉黑历史 Token | 产品安全策略、账号体系设计、是否存在其他会话管理服务 |
| 幂等记录和临时文件清理 | `idempotent_record` 有 `expire_time`，上传临时文件按流程尽力删除 | 定时任务、运维脚本、数据库归档策略、异常中断后的临时文件清理方案 |
| 短信过期记录维护 | `sms_record.status` 定义了过期状态，但当前流程主要依赖 Redis TTL 判断验证码过期 | 是否存在定时任务或运营审计需求 |
| 上传文件生命周期 | 当前支持上传、幂等、去重，不包含删除、替换、引用计数 | 业务服务对文件的引用关系、文件清理策略、对象存储策略 |
