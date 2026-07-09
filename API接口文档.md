# 互联网+智慧护理移动护理平台 — API 接口文档

> **文档版本**：v1.0

> **生成日期**：2026-07-07

> **对应架构版本**：系统架构设计.md v2.0

> **编写依据**：系统架构设计 v2.0、数据库设计 v2.0、业务泳道图

---

## 修订记录

| 版本 | 日期 | 修订人 | 修订说明 |

|------|------|--------|----------|

| v1.0 | 2026-07-07 | 人员 A | 初稿，覆盖全部 4 个微服务接口 |

---

## 一、概述

### 1.1 项目简介

互联网+智慧护理移动护理平台，为居家或社区老人提供专业护理服务的移动平台，涵盖登录注册、服务浏览、下单预约、地址管理、订单管理、评价投诉等模块。前端基于 Uni-app（Vue 3）编译 iOS/Android，后端基于 Java Spring Boot 3.x + Spring Cloud Alibaba 微服务架构。

### 1.2 基础地址（Base URL）

#### 配置方式

API 的 Base URL 不在本文档中写死，而是在前端项目中按环境配置：

| 环境 | 配置文件 | Base URL 值 | 说明 |
|------|----------|-------------|------|
| DEV | `config/dev.js` → `BASE_URL` | `http://localhost:8080` | 本地开发指向 Gateway 端口 |
| PROD | `config/prod.js` → `BASE_URL` | 由 CI/CD 部署时写入 | 生产环境为实际域名或 Nginx 入口 |

#### 调用方式说明

- **统一入口**：App 端所有 API 请求只经过 `Nginx → Spring Cloud Gateway(:8080)`，**不直连任何后端微服务**。
- **完整地址拼接规则**：`{BASE_URL} + 接口路径`
  - 例如登录接口：`{BASE_URL}/api/v1/users/login`
  - 例如订单列表：`{BASE_URL}/api/v1/orders?page=1&size=20`
- `{BASE_URL}` 的值来源于上文配置文件中定义的 `BASE_URL` 常量。
- **禁止直连**：后端各微服务有独立端口（user-service:8081、catalog-service:8082 等），前端**不要**直接访问这些端口。

#### 文档示例说明

- 本文档所有 curl 示例中的 `{BASE_URL}` 均为占位符，实际调用时替换为对应环境的 Base URL。
- 响应示例中出现的 `cdn.nursing.com`（如文件上传返回值）仅为示意数据，实际 CDN 域名由部署配置决定。

### 1.3 鉴权方式

- **JWT Bearer Token**：除标注"免鉴权"的接口外，所有请求必须在 HTTP Header 中携带 `Authorization: Bearer <token>`。

- Token 由登录/注册接口签发，有效期默认 **7 天**。

- Token 过期或登出后会被加入 Redis 黑名单，需重新登录获取。

- 支付宝支付回调接口免 JWT，但**必须校验支付宝公钥 RSA2 签名**。

### 1.4 全局请求头

| 参数名 | 类型 | 必填 | 默认值 | 说明 |

|--------|------|------|--------|------|

| Authorization | string | 鉴权接口必填 | - | `Bearer <token>`，登录接口返回 |

| Content-Type | string | 是 | application/json | 请求体格式 |

 | Idempotent-Key | string | 幂等接口必填 | - | 幂等令牌（下单接口使用 prepay-token 的值） |

 | X-Request-Id | string | 否 | - | 链路追踪 ID，Gateway 自动生成并透传 |

 | X-Date-Format | string | 否 | - | 请求/响应中所有日期时间的格式声明，服务端统一使用 ISO 8601（如 `2026-07-14T20:00:00+08:00`） |

### 1.5 全局响应结构

**成功响应（HTTP 200/201）：**

```json

{

  "code": 0,

  "message": "success",

  "data": { ... }

}

```

**分页响应：**

```json

{

  "code": 0,

  "message": "success",

  "data": {

    "list": [ ... ],

    "total": 120,

    "page": 1,

    "size": 20

  }

}

```

### 1.6 全局错误码

| HTTP 状态码 | code | message | 说明 |

|-------------|------|---------|------|

 | 400 | 1000 | 请求参数校验失败 | 参数格式错误、必填字段缺失，响应中同时返回 errors 数组包含字段级错误详情 |

| 400 | 1001 | 请求体解析失败 | JSON 格式错误 |

| 401 | 1002 | 未授权，请先登录 | Token 缺失或已过期 |

| 401 | 1003 | Token 已被列入黑名单 | 已登出或管理员强制下线 |

| 403 | 1004 | 无权限执行此操作 | 角色权限不足 |

| 404 | 1005 | 资源不存在 | 请求的资源 ID 无效 |

| 409 | 1006 | 数据冲突 | 唯一约束冲突（如重复提交） |

| 422 | 1007 | 业务校验失败 | 不满足业务前置条件 |

| 429 | 1008 | 请求过于频繁，请稍后重试 | 触发限流 |

| 500 | 1999 | 服务器内部错误 | 系统异常，请联系管理员 |

 

 > **字段级错误响应示例（code=1000）：**

 > ```json

 > {

 >   "code": 1000,

 >   "message": "请求参数校验失败",

 >   "data": null,

 >   "errors": [

 >     { "field": "phone", "message": "手机号格式不正确" },

 >     { "field": "smsCode", "message": "验证码不能为空" }

 >   ]

 > }

 > ```

### 1.7 业务错误码范围

| 服务 | 错误码范围 | 说明 |

|------|-----------|------|

| 通用 | 1000-1999 | 全局通用错误码 |

| user-service | 2000-2999 | 用户认证相关 |

| order-service | 3000-3999 | 订单交易相关 |

| feedback-service | 4000-4999 | 评价投诉相关 |

---

## 二、接口目录

| 模块 | 序号 | 接口名称 | 方法 | URL | 鉴权 |

|------|------|----------|------|-----|------|

| **用户与认证** | 1.1 | 发送短信验证码 | POST | /api/v1/users/sms-code | 免 |

| | 1.2 | 用户注册 | POST | /api/v1/users/register | 免 |

| | 1.3 | 用户登录 | POST | /api/v1/users/login | 免 |

| | 1.4 | 用户登出 | POST | /api/v1/users/logout | 需 |

| | 1.5 | 重置密码 | POST | /api/v1/users/password/reset | 免 |

| | 1.6 | 获取个人信息 | GET | /api/v1/users/profile | 需 |

 | | 1.7 | 修改个人信息 | PATCH | /api/v1/users/profile | 需 |

| | 1.8 | 文件上传 | POST | /api/v1/files/upload | 需 |

| **服务目录** | 2.1 | 服务分类列表 | GET | /api/v1/categories | 免 |

| | 2.2 | 服务项目列表 | GET | /api/v1/items | 免 |

| | 2.3 | 服务项目详情 | GET | /api/v1/items/{id} | 免 |

 | | 2.4 | 搜索服务项目 | GET | /api/v1/items | 免 |

| **订单与地址** | 3.1 | 获取下单幂等令牌 | POST | /api/v1/orders/prepay-token | 需 |

 | | 3.2 | 创建订单 | POST | /api/v1/orders | 需 |

| | 3.3 | 订单列表 | GET | /api/v1/orders | 需 |

| | 3.4 | 订单详情 | GET | /api/v1/orders/{id} | 需 |

| | 3.5 | 取消订单 | POST | /api/v1/orders/{id}/cancel | 需 |

| | 3.6 | 支付宝支付回调 | POST | /api/v1/orders/pay/callback | 免（验签） |

| | 3.7 | 地址列表 | GET | /api/v1/addresses | 需 |

| | 3.8 | 新增地址 | POST | /api/v1/addresses | 需 |

 | | 3.9 | 编辑地址 | PATCH | /api/v1/addresses/{id} | 需 |

| | 3.10 | 删除地址 | DELETE | /api/v1/addresses/{id} | 需 |

| | 3.11 | 设置默认地址 | PUT | /api/v1/addresses/{id}/default | 需 |

| **评价与投诉** | 4.1 | 提交评价 | POST | /api/v1/reviews | 需 |

| | 4.2 | 评价列表 | GET | /api/v1/reviews | 需 |

| | 4.3 | 提交投诉 | POST | /api/v1/complaints | 需 |

| | 4.4 | 投诉列表 | GET | /api/v1/complaints | 需 |

| | 4.5 | 投诉处理记录 | GET | /api/v1/complaints/{id}/tracks | 需 |

---

## 三、模块一：用户与认证（user-service）

### 3.1 发送短信验证码

| 字段 | 说明 |

|------|------|

| 接口名称 | 发送短信验证码 |

| 接口标识 | sendSmsCode |

| 请求方法 | POST |

| URL 路径 | /api/v1/users/sms-code |

| 接口描述 | 向用户手机号发送短信验证码，用于注册、登录、重置密码等场景。60 秒内不可重复发送。 |

| 所属模块 | 用户与认证 |

| 是否登录鉴权 | 否（免鉴权） |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 嵌套层级 | 说明 | 校验规则 |

|--------|------|------|----------|------|----------|

| phone | string | 是 | 顶层 | 用户手机号 | 11 位数字，以 1 开头 |

| smsType | string | 是 | 顶层 | 短信类型 | 枚举值：`register` / `login` / `reset_password` |

**请求体示例：**

```json

{

  "phone": "13812345678",

  "smsType": "register"

}

```

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "验证码已发送",

  "data": {

    "expireSeconds": 300

  }

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 429 | 2001 | 发送过于频繁，请 60 秒后重试 | 同一手机号 60 秒内重复请求 |

| 429 | 2002 | 今日发送次数已达上限 | 同一手机号每日超过 5 次 |

| 422 | 2003 | 手机号已被注册 | smsType=register 且手机号已存在 |

| 422 | 2004 | 手机号未注册 | smsType=login/reset_password 且手机号不存在 |

| 500 | 2005 | 短信发送失败，请稍后重试 | 短信通道异常 |

#### 调用示例

**curl：**

```bash

curl -X POST '{BASE_URL}/api/v1/users/sms-code' \

  -H 'Content-Type: application/json' \

  -d '{

    "phone": "13812345678",

    "smsType": "register"

  }'

```

**axios（前端）：**

```javascript

import request from '@/utils/request';

export function sendSmsCode(phone, smsType) {

  return request({

    url: '/api/v1/users/sms-code',

    method: 'post',

    data: { phone, smsType }

  });

}

sendSmsCode('13812345678', 'register').then(res => {

  if (res.code === 0) {

    console.log('验证码已发送，有效期：', res.data.expireSeconds, '秒');

  }

}).catch(err => {

  uni.showToast({ title: err.message || '发送失败', icon: 'none' });

});

```

#### 业务规则与注意事项

- 同一手机号 60 秒内不可重复发送，前端按钮应做倒计时处理。

- 日发送上限：同一手机号每天最多 5 条。

- 验证码有效期 5 分钟（300 秒）。

- 验证码使用 BCrypt 哈希后存储于 `sms_record` 表，仅用于安全审计，不存储明文。

- `smsType` 决定验证码的用途，注册/重置密码类短信内容不同。

### 3.2 用户注册

| 字段 | 说明 |

|------|------|

| 接口名称 | 用户注册 |

| 接口标识 | register |

| 请求方法 | POST |

| URL 路径 | /api/v1/users/register |

| 接口描述 | 手机号 + 短信验证码 + 密码完成注册。注册成功后自动签发 Token 并登录。 |

| 所属模块 | 用户与认证 |

| 是否登录鉴权 | 否（免鉴权） |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 嵌套层级 | 说明 | 校验规则 |

|--------|------|------|----------|------|----------|

| phone | string | 是 | 顶层 | 手机号 | 11 位数字，以 1 开头 |

| smsCode | string | 是 | 顶层 | 短信验证码 | 6 位数字 |

| password | string | 是 | 顶层 | 登录密码 | 8-32 位，包含字母和数字 |

| nickname | string | 否 | 顶层 | 昵称 | 2-16 个字符，不传则默认"用户+手机号后四位" |

**请求体示例：**

```json

{

  "phone": "13812345678",

  "smsCode": "123456",

  "password": "abc12345",

  "nickname": "张阿姨"

}

```

#### 响应格式

**成功响应（201）：**

```json

{

  "code": 0,

  "message": "注册成功",

  "data": {

    "token": "eyJhbGciOiJIUzI1NiIs...",

    "expireTime": "2026-07-14T20:00:00+08:00",

    "user": {

      "userId": 10001,

      "phone": "138****5678",

      "nickname": "张阿姨",

      "avatar": null,

      "gender": 0,

      "status": 0

    }

  }

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 400 | 2006 | 验证码错误 | 验证码不匹配 |

| 400 | 2007 | 验证码已过期 | 验证码超过 5 分钟有效期 |

| 409 | 2008 | 该手机号已注册 | 手机号已存在 |

| 422 | 2009 | 密码不符合安全要求 | 密码未包含字母和数字或长度不足 |

#### 调用示例

**curl：**

```bash

curl -X POST '{BASE_URL}/api/v1/users/register' \

  -H 'Content-Type: application/json' \

  -d '{

    "phone": "13812345678",

    "smsCode": "123456",

    "password": "abc12345",

    "nickname": "张阿姨"

  }'

```

**axios：**

```javascript

import request from '@/utils/request';

export function register(data) {

  return request({

    url: '/api/v1/users/register',

    method: 'post',

    data

  });

}

register({

  phone: '13812345678',

  smsCode: '123456',

  password: 'abc12345',

  nickname: '张阿姨'

}).then(res => {

  if (res.code === 0) {

    uni.setStorageSync('token', res.data.token);

    uni.setStorageSync('userInfo', res.data.user);

    uni.switchTab({ url: '/pages/index/index' });

  }

}).catch(err => {

  uni.showToast({ title: err.message, icon: 'none' });

});

```

#### 业务规则与注意事项

- 注册成功后自动登录，前端需保存 `token` 到本地存储。

- `token` 同时存入 MySQL `user_token` 表和 Redis。

- 手机号返回时脱敏展示（中间四位替换为 `*`）。

- 密码使用 BCrypt 加密存储，不存明文。

### 3.3 用户登录

| 字段 | 说明 |

|------|------|

| 接口名称 | 用户登录 |

| 接口标识 | login |

| 请求方法 | POST |

| URL 路径 | /api/v1/users/login |

| 接口描述 | 手机号 + 密码 或 手机号 + 验证码 两种方式登录，签发 JWT Token。 |

| 所属模块 | 用户与认证 |

| 是否登录鉴权 | 否（免鉴权） |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 嵌套层级 | 说明 | 校验规则 |

|--------|------|------|----------|------|----------|

| phone | string | 是 | 顶层 | 手机号 | 11 位数字 |

| loginMode | string | 是 | 顶层 | 登录方式 | 枚举值：`password` / `sms` |

| password | string | 二选一 | 顶层 | 密码登录 | loginMode=password 时必填 |

| smsCode | string | 二选一 | 顶层 | 验证码登录 | loginMode=sms 时必填 |

**请求体示例（密码登录）：**

```json

{

  "phone": "13812345678",

  "loginMode": "password",

  "password": "abc12345"

}

```

**请求体示例（验证码登录）：**

```json

{

  "phone": "13812345678",

  "loginMode": "sms",

  "smsCode": "123456"

}

```

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "登录成功",

  "data": {

    "token": "eyJhbGciOiJIUzI1NiIs...",

    "expireTime": "2026-07-14T20:00:00+08:00",

    "user": {

      "userId": 10001,

      "phone": "138****5678",

      "nickname": "张阿姨",

      "avatar": "https://cdn.nursing.com/avatars/10001.jpg",

      "gender": 1,

      "status": 0

    }

  }

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 400 | 2010 | 密码错误 | 密码不匹配 |

| 400 | 2006 | 验证码错误 | 验证码不匹配 |

| 400 | 2007 | 验证码已过期 | 验证码超时 |

| 422 | 2011 | 账号已被禁用 | 用户状态 status=1 |

| 422 | 2004 | 手机号未注册 | 手机号不存在 |

#### 调用示例

**curl：**

```bash

curl -X POST '{BASE_URL}/api/v1/users/login' \

  -H 'Content-Type: application/json' \

  -d '{

    "phone": "13812345678",

    "loginMode": "password",

    "password": "abc12345"

  }'

```

**axios：**

```javascript

import request from '@/utils/request';

export function login(data) {

  return request({

    url: '/api/v1/users/login',

    method: 'post',

    data

  });

}

login({ phone: '13812345678', loginMode: 'password', password: 'abc12345' })

  .then(res => {

    if (res.code === 0) {

      uni.setStorageSync('token', res.data.token);

      uni.setStorageSync('userInfo', res.data.user);

      uni.switchTab({ url: '/pages/index/index' });

    }

  })

  .catch(err => {

    uni.showToast({ title: err.message, icon: 'none' });

  });

```

#### 业务规则与注意事项

- 登录后 Token 有效期 7 天。

- 同一用户多次登录会签发新 Token，旧 Token 仍有效（除非调用登出接口）。

- 建议前端在请求拦截器中统一处理 401 响应，自动跳转登录页。

### 3.4 用户登出

| 字段 | 说明 |

|------|------|

| 接口名称 | 用户登出 |

| 接口标识 | logout |

| 请求方法 | POST |

| URL 路径 | /api/v1/users/logout |

| 接口描述 | 将当前 Token 加入 Redis 黑名单，使其立即失效。 |

| 所属模块 | 用户与认证 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求头

| 参数名 | 类型 | 必填 | 说明 |

|--------|------|------|------|

| Authorization | string | 是 | `Bearer <当前 token>` |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "登出成功",

  "data": null

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 401 | 1002 | 未授权，请先登录 | Token 缺失或已过期 |

#### 调用示例

**curl：**

```bash

curl -X POST '{BASE_URL}/api/v1/users/logout' \

  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...'

```

**axios：**

```javascript

import request from '@/utils/request';

export function logout() {

  return request({

    url: '/api/v1/users/logout',

    method: 'post'

  });

}

logout().then(res => {

  if (res.code === 0) {

    uni.removeStorageSync('token');

    uni.removeStorageSync('userInfo');

    uni.reLaunch({ url: '/pages/login/login' });

  }

});

```

#### 业务规则与注意事项

- 后端将 Token 加入 Redis 黑名单（TTL = Token 剩余有效期）。

- 前端登出后必须清除本地存储的 Token 和用户信息。

### 3.5 重置密码

| 字段 | 说明 |

|------|------|

| 接口名称 | 重置密码 |

| 接口标识 | resetPassword |

| 请求方法 | POST |

| URL 路径 | /api/v1/users/password/reset |

| 接口描述 | 通过手机号 + 验证码验证身份后重置登录密码。 |

| 所属模块 | 用户与认证 |

| 是否登录鉴权 | 否（免鉴权） |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 嵌套层级 | 说明 | 校验规则 |

|--------|------|------|----------|------|----------|

| phone | string | 是 | 顶层 | 手机号 | 11 位数字 |

| smsCode | string | 是 | 顶层 | 短信验证码 | 6 位数字 |

| newPassword | string | 是 | 顶层 | 新密码 | 8-32 位，包含字母和数字 |

**请求体示例：**

```json

{

  "phone": "13812345678",

  "smsCode": "123456",

  "newPassword": "newpass678"

}

```

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "密码重置成功",

  "data": null

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 400 | 2006 | 验证码错误 | 验证码不匹配 |

| 400 | 2007 | 验证码已过期 | 验证码超时 |

| 422 | 2004 | 手机号未注册 | 手机号不存在 |

| 422 | 2009 | 密码不符合安全要求 | 新密码不符合规则 |

| 400 | 2012 | 新密码不能与旧密码相同 | 新旧密码相同 |

#### 调用示例

**curl：**

```bash

curl -X POST '{BASE_URL}/api/v1/users/password/reset' \

  -H 'Content-Type: application/json' \

  -d '{

    "phone": "13812345678",

    "smsCode": "123456",

    "newPassword": "newpass678"

  }'

```

#### 业务规则与注意事项

- 重置密码后，该用户的所有已签发 Token **不会自动失效**。

- 新密码同样使用 BCrypt 加密存储。

### 3.6 获取个人信息

| 字段 | 说明 |

|------|------|

| 接口名称 | 获取个人信息 |

| 接口标识 | getUserProfile |

| 请求方法 | GET |

| URL 路径 | /api/v1/users/profile |

| 接口描述 | 获取当前登录用户的个人信息。 |

| 所属模块 | 用户与认证 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求头

| 参数名 | 类型 | 必填 | 说明 |

|--------|------|------|------|

| Authorization | string | 是 | `Bearer <token>` |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "success",

  "data": {

    "userId": 10001,

    "phone": "138****5678",

    "nickname": "张阿姨",

    "avatar": "https://cdn.nursing.com/avatars/10001.jpg",

    "gender": 1,

    "idCard": "110***********1234",

    "status": 0,

    "lastLoginTime": "2026-07-07T19:30:00+08:00",


    "createTime": "2026-07-01T10:00:00+08:00"

  }

}

```

> `idCard` 返回时自动脱敏。

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 401 | 1002 | 未授权，请先登录 | Token 缺失或过期 |

| 401 | 1003 | Token 已被列入黑名单 | 已登出 |

#### 调用示例

**curl：**

```bash

curl -X GET '{BASE_URL}/api/v1/users/profile' \

  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...'

```

**axios：**

```javascript

import request from '@/utils/request';

export function getUserProfile() {

  return request({

    url: '/api/v1/users/profile',

    method: 'get'

  });

}

getUserProfile().then(res => {

  if (res.code === 0) {

    this.userInfo = res.data;

  }

});

```

#### 业务规则与注意事项

- 身份证号 `idCard` 只在涉及医疗实名制场景时返回，且必须脱敏。

- 前端启动 App 时建议通过此接口校验 Token 有效性。

### 3.7 修改个人信息

| 字段 | 说明 |

|------|------|

| 接口名称 | 修改个人信息 |

| 接口标识 | updateUserProfile |

| 请求方法 | PATCH |

| URL 路径 | /api/v1/users/profile |

| 接口描述 | 修改当前登录用户的昵称、头像、性别等个人信息。 |

| 所属模块 | 用户与认证 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 嵌套层级 | 说明 | 校验规则 |

|--------|------|------|----------|------|----------|

| nickname | string | 否 | 顶层 | 昵称 | 2-16 个字符 |

| avatar | string | 否 | 顶层 | 头像 URL | 先调用文件上传接口获取 |

| gender | integer | 否 | 顶层 | 性别 | 0=保密 / 1=男 / 2=女 |

| idCard | string | 否 | 顶层 | 身份证号 | 18 位身份证校验 |

**请求体示例：**

```json

{

  "nickname": "张阿姨（修改）",

  "gender": 2

}

```

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "修改成功",

  "data": {

    "userId": 10001,

    "nickname": "张阿姨（修改）",

    "avatar": "https://cdn.nursing.com/avatars/10001.jpg",

    "gender": 2

  }

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 400 | 1000 | 参数校验失败 | 昵称长度超限等 |

| 422 | 2013 | 身份证号格式不正确 | 身份证号码校验不通过 |

#### 调用示例

**curl：**

```bash

curl -X PUT '{BASE_URL}/api/v1/users/profile' \

  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...' \

  -H 'Content-Type: application/json' \

  -d '{"nickname": "张阿姨（修改）", "gender": 2}'

```

#### 业务规则与注意事项

- 身份证号 `idCard` 提交后服务端使用 AES-256 加密存储，返回时脱敏。

- 手机号不可通过此接口修改。

### 3.8 文件上传

| 字段 | 说明 |

|------|------|

| 接口名称 | 文件上传 |

| 接口标识 | fileUpload |

| 请求方法 | POST |

| URL 路径 | /api/v1/files/upload |

| 接口描述 | 上传用户头像、评价图片、投诉截图等文件，返回可用的文件 URL。 |

| 所属模块 | 用户与认证 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求头

| 参数名 | 类型 | 必填 | 说明 |

|--------|------|------|------|

| Authorization | string | 是 | `Bearer <token>` |

| Content-Type | string | 是 | `multipart/form-data` |

#### 请求体（multipart/form-data）

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |

|--------|------|------|------|----------|

| file | File | 是 | 上传文件 | 支持 jpg/png/gif/mp4，单文件最大 10MB |

| bizType | string | 是 | 业务类型 | 枚举值：`avatar` / `review_image` / `complaint_image` |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "上传成功",

  "data": {

    "fileUrl": "https://cdn.nursing.com/2026/07/07/abc123def.jpg",

    "fileName": "abc123def.jpg",

    "fileSize": 204800,

    "bizType": "avatar"

  }

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 400 | 1000 | 文件格式不支持 | 非 jpg/png/gif/mp4 格式 |

| 400 | 2014 | 文件大小超过限制 | 单文件超过 10MB |

| 400 | 2015 | 文件上传失败 | 文件读写异常 |

#### 调用示例

```bash

curl -X POST '{BASE_URL}/api/v1/files/upload' \

  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...' \

  -F 'file=@/path/to/avatar.jpg' \

  -F 'bizType=avatar'

```

#### 业务规则与注意事项

- MVP 阶段文件上传由 user-service 处理。

- 返回的 `fileUrl` 是 CDN 加速后的完整 URL。

---

## 四、模块二：服务目录（catalog-service）

### 4.1 服务分类列表

| 字段 | 说明 |

|------|------|

| 接口名称 | 服务分类列表 |

| 接口标识 | listCategories |

| 请求方法 | GET |

| URL 路径 | /api/v1/categories |

| 接口描述 | 获取所有上架的服务分类，按 sortOrder 升序排列。 |

| 所属模块 | 服务目录 |

| 是否登录鉴权 | 否（免鉴权） |

| 权限要求 | 无 |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "success",

  "data": [

    {

      "categoryId": 1,

      "name": "专业护理",

      "icon": "https://cdn.nursing.com/icons/category/nurse.png",

      "sortOrder": 1,

      "status": 1

    },

    {

      "categoryId": 2,

      "name": "康复理疗",

      "icon": "https://cdn.nursing.com/icons/category/rehab.png",

      "sortOrder": 2,

      "status": 1

    },

    {

      "categoryId": 3,

      "name": "生活照料",

      "icon": "https://cdn.nursing.com/icons/category/care.png",

      "sortOrder": 3,

      "status": 1

    }

  ]

}

```

**空数据响应：**

```json

{

  "code": 0,

  "message": "success",

  "data": []

}

```

#### 业务错误码

无特定错误码。

#### 调用示例

**curl：**

```bash

curl -X GET '{BASE_URL}/api/v1/categories'

```

**axios：**

```javascript

import request from '@/utils/request';

export function listCategories() {

  return request({

    url: '/api/v1/categories',

    method: 'get'

  });

}

listCategories().then(res => {

  if (res.code === 0) {

    this.categories = res.data;

  }

});

```

#### 业务规则与注意事项

- 仅返回 `status=1`（展示）的分类。

- 返回顺序按 `sortOrder` 升序排列。

- 此接口响应建议前端缓存，避免每次进入页面都重新请求。

- 因服务分类是低频变化数据，后续可接入 Redis 缓存减少 DB 查询。

### 4.2 服务项目列表

| 字段 | 说明 |

|------|------|

| 接口名称 | 服务项目列表 |

| 接口标识 | listItems |

| 请求方法 | GET |

| URL 路径 | /api/v1/items |

| 接口描述 | 根据分类 ID 获取该分类下的服务项目列表，按 sortOrder 升序排列。 |

| 所属模块 | 服务目录 |

| 是否登录鉴权 | 否（免鉴权） |

| 权限要求 | 无 |

#### 查询参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 校验规则 |

|--------|------|------|--------|------|----------|

| categoryId | integer | 否 | - | 分类 ID，不传则返回全部分类项目 | > 0 |

| page | integer | 否 | 1 | 页码 | >= 1 |

| size | integer | 否 | 20 | 每页条数 | 1-50 |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "success",

  "data": {

    "list": [

      {

        "itemId": 101,

        "categoryId": 1,

        "name": "上门输液护理",

        "description": "由专业护士上门进行静脉输液护理服务",

        "coverImage": "https://cdn.nursing.com/items/101.jpg",

        "sortOrder": 1,

        "status": 1,

        "specs": [

          { "specId": 1001, "name": "单次服务", "price": 150.00, "originalPrice": 180.00, "duration": 60 },

          { "specId": 1002, "name": "5 次套餐", "price": 650.00, "originalPrice": 750.00, "duration": 60 }

        ]

      }

    ],

    "total": 12,

    "page": 1,

    "size": 20

  }

}

```

**空列表响应：**

```json

{

  "code": 0,

  "message": "success",

  "data": { "list": [], "total": 0, "page": 1, "size": 20 }

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 404 | 1005 | 分类不存在 | categoryId 对应的分类不存在或已删除 |

#### 调用示例

```bash

curl -X GET '{BASE_URL}/api/v1/items?categoryId=1&page=1&size=20'

```

```javascript

import request from '@/utils/request';

export function listItems(params) {

  return request({ url: '/api/v1/items', method: 'get', params });

}

listItems({ categoryId: 1, page: 1, size: 20 }).then(res => {

  if (res.code === 0) { this.items = res.data.list; this.total = res.data.total; }

});

```

#### 业务规则与注意事项

- 仅返回 `status=1`（上架）的项目及规格。

- 每个服务项目下挂其所有上架的规格（`specs` 数组），规格包含售价、原价、服务时长。

- 价格是下单时的实时参考价，最终以下单时 catalog-service 返回的价格为准。

### 4.3 服务项目详情

| 字段 | 说明 |

|------|------|

| 接口名称 | 服务项目详情 |

| 接口标识 | getItemDetail |

| 请求方法 | GET |

| URL 路径 | /api/v1/items/{id} |

| 接口描述 | 获取单个服务项目的完整信息，包括图文详情描述和所有规格。 |

| 所属模块 | 服务目录 |

| 是否登录鉴权 | 否（免鉴权） |

| 权限要求 | 无 |

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |

|--------|------|------|------|----------|

| id | integer | 是 | 服务项目 ID | > 0 |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "success",

  "data": {

    "itemId": 101,

    "categoryId": 1,

    "categoryName": "专业护理",

    "name": "上门输液护理",

    "description": "由持证执业护士上门，为您提供专业的静脉输液护理服务。",

    "coverImage": "https://cdn.nursing.com/items/101.jpg",

    "images": [ "https://cdn.nursing.com/items/101-1.jpg" ],

    "sortOrder": 1,

    "status": 1,

    "specs": [

      { "specId": 1001, "name": "单次服务", "price": 150.00, "duration": 60, "status": 1 },

      { "specId": 1002, "name": "5 次套餐", "price": 650.00, "duration": 60, "status": 1 }

    ],

    "createTime": "2026-07-07T20:00:00+08:00"

  }

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 404 | 1005 | 服务项目不存在 | itemId 无效或已删除 |

#### 调用示例

```bash

curl -X GET '{BASE_URL}/api/v1/items/101'

```

```javascript

import request from '@/utils/request';

export function getItemDetail(id) {

  return request({ url: '/api/v1/items/${id}', method: 'get' });

}

getItemDetail(101).then(res => {

  if (res.code === 0) { this.item = res.data; }

});

```

#### 业务规则与注意事项

- `description` 为 Markdown 格式的图文详情。

- `images` 数组是服务项目的多张展示图片。

- 规格 `specs` 中的 `duration` 单位为分钟。

### 4.4 搜索服务项目

| 字段 | 说明 |

|------|------|

| 接口名称 | 搜索服务项目 |

| 接口标识 | searchItems |

| 请求方法 | GET |

| URL 路径 | /api/v1/items |

| 接口描述 | 根据关键词模糊搜索服务项目名称和描述，支持按分类筛选和分页。 |

| 所属模块 | 服务目录 |

| 是否登录鉴权 | 否（免鉴权） |

| 权限要求 | 无 |

#### 查询参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 校验规则 |

|--------|------|------|--------|------|----------|

| keyword | string | 是 | - | 搜索关键词 | 1-50 个字符 |

| categoryId | integer | 否 | - | 分类筛选（可选） | > 0 |

| page | integer | 否 | 1 | 页码 | >= 1 |

| size | integer | 否 | 20 | 每页条数 | 1-50 |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "success",

  "data": {

    "list": [

      {

        "itemId": 101, "categoryId": 1, "categoryName": "专业护理",

        "name": "上门输液护理", "coverImage": "https://cdn.nursing.com/items/101.jpg",

        "status": 1, "minPrice": 150.00,

        "specs": [ { "specId": 1001, "name": "单次服务", "price": 150.00, "duration": 60 } ]

      }

    ],

    "total": 1, "page": 1, "size": 20

  }

}

```

**空结果响应：**

```json

{ "code": 0, "message": "success", "data": { "list": [], "total": 0, "page": 1, "size": 20 } }

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 400 | 1000 | 关键词不能为空 | keyword 为空白字符串 |

#### 调用示例

```bash

curl -X GET '{BASE_URL}/api/v1/items?keyword=%E8%BE%93%E6%B6%B2&page=1&size=20'

```

```javascript

import request from '@/utils/request';

export function searchItems(params) {

  return request({ url: '/api/v1/items', method: 'get', params });

}

searchItems({ keyword: '输液', categoryId: 1, page: 1 }).then(res => {

  if (res.code === 0) { this.searchResults = res.data.list; }

});

```

#### 业务规则与注意事项

- 搜索匹配范围：服务项目名称 + 服务描述。

- 仅搜索 `status=1`（上架）的项目。

- `minPrice` 字段返回该服务项目下所有规格的最低售价。

- 建议前端对搜索输入做防抖处理（300-500ms）。

---

## 五、模块三：订单与地址（order-service）

### 5.1 获取下单幂等令牌

| 字段 | 说明 |

|------|------|

| 接口名称 | 获取下单幂等令牌 |

| 接口标识 | getPrepayToken |

| 请求方法 | POST |

| URL 路径 | /api/v1/orders/prepay-token |

| 接口描述 | 下单前获取后端发放的幂等令牌，用于下单时的幂等校验，防止重复下单。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0, "message": "success",

  "data": { "prepayToken": "pt_7a8b9c0d1e2f3a4b5c6d7e8f", "expireTime": "2026-07-14T20:00:00+08:00" }

}

```

#### 调用示例

```bash

curl -X POST '{BASE_URL}/api/v1/orders/prepay-token' \

  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...'

```

```javascript

import request from '@/utils/request';

export function getPrepayToken() {

  return request({ url: '/api/v1/orders/prepay-token', method: 'post' });

}

getPrepayToken().then(res => {

  if (res.code === 0) { uni.setStorageSync('prepayToken', res.data.prepayToken); }

});

```

#### 业务规则与注意事项

- `prepayToken` 有效期 10 分钟，过期后需重新获取。

- 前端收到 prepay-token 后立即存入 localStorage，作为后续下单请求头 `Idempotent-Key` 的值。

- 此接口是下单流程的第一步，必须先调用。

### 5.2 创建订单

| 字段 | 说明 |

|------|------|

| 接口名称 | 创建订单 |

| 接口标识 | createOrder |

| 请求方法 | POST |

| URL 路径 | /api/v1/orders |

| 接口描述 | 提交订单创建请求，需携带幂等令牌。同一 prepay-token 重复调用幂等返回相同 orderId。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |

|--------|------|------|------|----------|

| serviceItemId | integer | 是 | 服务项目 ID | > 0 |

| serviceSpecId | integer | 是 | 规格 ID | > 0，且属于 serviceItemId |

| addressId | integer | 是 | 地址 ID | > 0，且属于当前用户 |

| serviceDate | string | 是 | 预约日期 | 格式 yyyy-MM-dd，不得早于明天 |

| serviceTimeSlot | string | 是 | 预约时段 | MORNING / AFTERNOON / EVENING |

| remark | string | 否 | 用户备注 | 最多 200 字符 |

**预约时段：** MORNING=08:00-12:00，AFTERNOON=13:00-17:00，EVENING=18:00-21:00

**请求体示例：**

```json

{

  "serviceItemId": 101, "serviceSpecId": 1001, "addressId": 5001,

  "serviceDate": "2026-07-08", "serviceTimeSlot": "MORNING", "remark": "请提前电话联系"

}

```

#### 响应格式

**成功响应（201）：**

```json

{ "code": 0, "message": "下单成功", "data": { "orderId": 20001, "orderNo": "2026070820001" } }

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 400 | 3001 | 幂等令牌不存在或已过期 | prepay-token 无效或超时 |

| 409 | 3002 | 该时段已被预约 | 同一用户在同一时段已预约相同服务 |

| 422 | 3003 | 服务项目已下架 | serviceItem 状态不为 1 |

| 422 | 3004 | 规格已下架 | serviceSpec 状态不为 1 |

| 422 | 3005 | 价格已变动，请重新确认 | 实时价格与页面价格不一致 |

| 422 | 3006 | 地址不存在或已被删除 | 地址 ID 无效 |

#### 调用示例

```bash

curl -X POST '{BASE_URL}/api/v1/orders' \

  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...' \

  -H 'Content-Type: application/json' \

  -H 'Idempotent-Key: pt_7a8b9c0d1e2f3a4b5c6d7e8f' \

  -d '{"serviceItemId": 101, "serviceSpecId": 1001, "addressId": 5001, "serviceDate": "2026-07-08", "serviceTimeSlot": "MORNING"}'

```

#### 业务规则与注意事项

- **幂等机制**：后端根据 Idempotent-Key 查 `idempotent_record`，已处理则直接返回已有 orderId。

- **价格实时性**：下单时 order-service 通过 Feign 同步调用 catalog-service 获取实时价格。

- **时段唯一约束**：同一用户同一天同一时段不可重复预约同一服务。

### 5.3 订单列表

| 字段 | 说明 |

|------|------|

| 接口名称 | 订单列表 |

| 接口标识 | listOrders |

| 请求方法 | GET |

| URL 路径 | /api/v1/orders |

| 接口描述 | 获取当前用户的订单列表，支持按状态筛选和分页。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 查询参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 校验规则 |

|--------|------|------|--------|------|----------|

| status | integer | 否 | - | 订单状态筛选 | 0-5 |

| page | integer | 否 | 1 | 页码 | >= 1 |

| size | integer | 否 | 20 | 每页条数 | 1-50 |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0, "message": "success",

  "data": {

    "list": [

      { "orderId": 20001, "orderNo": "2026070820001", "serviceItemName": "上门输液护理", "specName": "单次服务",

        "specPrice": 150.00, "totalAmount": 150.00, "status": 0,

        "serviceDate": "2026-07-08", "serviceTimeSlot": "MORNING",

        "receiverName": "张三", "receiverPhone": "13812345678",

        "addressDetail": "北京市朝阳区建国路 88 号 6 栋 301",

        "createTime": "2026-07-07T20:00:00+08:00" }

    ],

    "total": 5, "page": 1, "size": 20

  }

}

```

#### 调用示例

```bash

curl -X GET '{BASE_URL}/api/v1/orders?status=0&page=1&size=20' \

  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...'

```

#### 业务规则与注意事项

- **数据可见性**：仅返回当前用户的订单。

- 订单列表中的服务名、地址、价格均为**快照信息**，不受后续数据变更影响。

- 排序规则：按 `create_time` 倒序排列（最新订单在前）。

### 5.4 订单详情

| 字段 | 说明 |

|------|------|

| 接口名称 | 订单详情 |

| 接口标识 | getOrderDetail |

| 请求方法 | GET |

| URL 路径 | /api/v1/orders/{id} |

| 接口描述 | 获取单个订单的完整详情，包含服务信息、地址、金额、状态时间线等。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0, "message": "success",

  "data": {

    "orderId": 20001, "orderNo": "2026070820001",

    "serviceItemName": "上门输液护理", "specName": "单次服务", "specPrice": 150.00,

    "totalAmount": 150.00, "status": 0,

    "receiverName": "张三", "addressDetail": "北京市朝阳区建国路 88 号 6 栋 301",

    "serviceDate": "2026-07-08", "serviceTimeSlot": "MORNING",

    "operationLogs": [

      { "action": "create", "fromStatus": null, "toStatus": 0, "remark": "用户创建订单", "createTime": "2026-07-07T20:00:00+08:00" }

    ],

    "createTime": "2026-07-07T20:00:00+08:00"

  }

}

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 404 | 3007 | 订单不存在 | orderId 无效 |

| 403 | 3008 | 无权查看该订单 | 订单不属于当前用户 |

#### 调用示例

```bash

curl -X GET '{BASE_URL}/api/v1/orders/20001' \

  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...'

```

#### 业务规则与注意事项

- 只有订单所属用户可查看详情。

- `operationLogs` 数组按时间倒序排列展示完整的订单生命周期。

### 5.5 取消订单

| 字段 | 说明 |

|------|------|

| 接口名称 | 取消订单 |

| 接口标识 | cancelOrder |

| 请求方法 | POST |

| URL 路径 | /api/v1/orders/{id}/cancel |

| 接口描述 | 用户主动取消订单。待支付直接取消；待服务取消后触发退款。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |

|--------|------|------|------|----------|

| cancelReason | string | 否 | 取消原因 | 最多 200 字符 |

#### 响应格式

```json

{ "code": 0, "message": "订单已取消", "data": { "orderId": 20001, "status": 3, "refundStatus": "NO_REFUND" } }

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 422 | 3009 | 当前状态不可取消 | 订单状态不是待支付或待服务 |

| 404 | 3007 | 订单不存在 | 订单 ID 无效 |

| 403 | 3008 | 无权操作该订单 | 订单不属于当前用户 |

#### 调用示例

```bash

curl -X POST '{BASE_URL}/api/v1/orders/20001/cancel' \

  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...' \

  -H 'Content-Type: application/json' \

  -d '{"cancelReason": "临时有事，无法接待"}'

```

#### 业务规则与注意事项

- **待支付（status=0）**：直接取消，无需退款。

- **待服务（status=1）**：取消后进入退款流程，资金原路返回。

- **已完成（status=2）**：不可取消，需通过投诉流程申请退款。

- 取消操作需幂等处理：重复点击不产生副作用。
- 建议前端在 `Idempotent-Key` 请求头中传入 UUID，后端依据该键进行幂等校验。

### 5.6 支付宝支付回调

| 字段 | 说明 |

|------|------|

| 接口名称 | 支付宝支付回调 |

| 接口标识 | alipayCallback |

| 请求方法 | POST |

| URL 路径 | /api/v1/orders/pay/callback |

| 接口描述 | 支付宝支付成功后的异步通知回调地址。免 JWT，但必须校验支付宝公钥 RSA2 签名。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 否（免 JWT，强制验签） |

| 权限要求 | 无 |

#### 请求体（application/x-www-form-urlencoded）

| 参数名 | 类型 | 必填 | 说明 |

|--------|------|------|------|

| notify_id | string | 是 | 支付宝通知 ID（用于幂等校验） |

| trade_no | string | 是 | 支付宝交易流水号 |

| out_trade_no | string | 是 | 商户订单号（orderNo） |

| total_amount | string | 是 | 交易金额 |

| trade_status | string | 是 | 成功时为 TRADE_SUCCESS |

| sign | string | 是 | RSA2 签名 |

| sign_type | string | 是 | 固定为 RSA2 |

#### 响应格式

```

success

```

#### 业务规则与注意事项

1. **验签**：收到回调后先验证 RSA2 签名，验签失败直接返回 403。

2. **幂等**：验签通过后 INSERT `payment_record`。唯一键冲突说明已处理，返回 `success`。

3. **状态更新**：更新 `order_header` 状态为 `1=待服务`，写入 `order_operation_log`。

4. **消息发送**：使用 Outbox 模式发送 `ORDER_PAID` 事件到 Kafka。

5. **前端无需对接此接口**，前端通过轮询或支付结果页获取支付状态。

### 5.7 发起支付

| 字段 | 说明 |
|------|------|
| 接口名称 | 发起支付 |
| 接口标识 | initiatePayment |
| 请求方法 | POST |
| URL 路径 | /api/v1/orders/{id}/pay |
| 接口描述 | 用户确认支付后，后端返回支付参数，前端用于唤起支付客户端。 |
| 所属模块 | 订单与地址 |
| 是否登录鉴权 | 是 |

#### 请求体

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| payChannel | string | 是 | 支付渠道，固定为 alipay |

### 5.7 地址列表

| 字段 | 说明 |

|------|------|

| 接口名称 | 地址列表 |

| 接口标识 | listAddresses |

| 请求方法 | GET |

| URL 路径 | /api/v1/addresses |

| 接口描述 | 获取当前用户的所有收货地址列表，按默认地址优先、创建时间倒序排列。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 响应格式

**成功响应（200）：**

```json

{

  "code": 0,

  "message": "success",

  "data": [

    { "addressId": 5001, "receiverName": "张三", "receiverPhone": "13812345678", "tag": "家",

      "province": "北京市", "city": "北京市", "district": "朝阳区",

      "detailAddress": "建国路 88 号 6 栋 301", "isDefault": 1 },

    { "addressId": 5002, "receiverName": "张三", "receiverPhone": "13812345678", "tag": "公司",

      "province": "北京市", "city": "北京市", "district": "海淀区",

      "detailAddress": "中关村大街 1 号 8 层", "isDefault": 0 }

  ]

}

```

#### 业务规则与注意事项

- 仅返回当前用户的地址（is_deleted=0）。

- 排序规则：默认地址排首位，其余按 `create_time` 倒序。

### 5.9 新增地址

| 字段 | 说明 |

|------|------|

| 接口名称 | 新增地址 |

| 接口标识 | createAddress |

| 请求方法 | POST |

| URL 路径 | /api/v1/addresses |

| 接口描述 | 为当前用户新增一个收货地址。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |

|--------|------|------|------|----------|

| receiverName | string | 是 | 收件人姓名 | 2-16 个字符 |

| receiverPhone | string | 是 | 联系电话 | 11 位手机号 |

| tag | string | 是 | 地址标签 | 家/公司/学校/其他 |

| province | string | 是 | 省 | - |

| city | string | 是 | 市 | - |

| district | string | 是 | 区/县 | - |

| detailAddress | string | 是 | 详细地址 | 5-100 个字符 |

| isDefault | integer | 否 | 是否默认 | 0=非默认 / 1=默认 |

#### 响应格式

```json

{ "code": 0, "message": "地址添加成功", "data": { "addressId": 5003 } }

```

#### 业务规则与注意事项

- 省市区建议前端使用三级联动选择器。

- 如果 `isDefault=1`，原有默认地址自动置 0。

### 5.10 编辑地址

| 字段 | 说明 |

|------|------|

| 接口名称 | 编辑地址 |

| 接口标识 | updateAddress |

| 请求方法 | PUT |

| URL 路径 | /api/v1/addresses/{id} |

| 接口描述 | 编辑当前用户的指定地址，可修改部分字段。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求体

参数同新增地址，均非必填。幂等操作。

#### 响应格式

```json

{ "code": 0, "message": "地址修改成功", "data": null }

```

### 5.11 删除地址

| 字段 | 说明 |

|------|------|

| 接口名称 | 删除地址 |

| 接口标识 | deleteAddress |

| 请求方法 | DELETE |

| URL 路径 | /api/v1/addresses/{id} |

| 接口描述 | 逻辑删除当前用户的指定地址（is_deleted 置 1）。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 响应格式

```json

{ "code": 0, "message": "地址已删除", "data": null }

```

#### 业务规则与注意事项

- 采用**逻辑删除**，数据不物理删除。

### 5.12 设置默认地址

| 字段 | 说明 |

|------|------|

| 接口名称 | 设置默认地址 |

| 接口标识 | setDefaultAddress |

| 请求方法 | PUT |

| URL 路径 | /api/v1/addresses/{id}/default |

| 接口描述 | 将指定地址设为当前用户的默认地址。 |

| 所属模块 | 订单与地址 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 响应格式

```json

{ "code": 0, "message": "默认地址设置成功", "data": null }

```

#### 业务规则与注意事项

- 将指定地址的 `isDefault` 置为 1，并将该用户其他地址的 `isDefault` 全部置为 0。

---

## 六、模块四：评价与投诉（feedback-service）

### 6.1 提交评价

| 字段 | 说明 |

|------|------|

| 接口名称 | 提交评价 |

| 接口标识 | submitReview |

| 请求方法 | POST |

| URL 路径 | /api/v1/reviews |

| 接口描述 | 对已完成的服务订单提交评价（一单一评，不可重复评价）。 |

| 所属模块 | 评价与投诉 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |

|--------|------|------|------|----------|

| orderId | integer | 是 | 订单 ID | > 0，且属于当前用户 |

| rating | integer | 是 | 评分 | 1-5 |

| content | string | 否 | 评价内容 | 最多 500 字符 |

| images | string[] | 否 | 图片 URL 列表 | 最多 6 张 |

#### 响应格式

```json

{ "code": 0, "message": "评价提交成功", "data": { "reviewId": 30001 } }

```

#### 业务错误码

| HTTP 状态码 | code | message | 触发条件 |

|-------------|------|---------|----------|

| 422 | 4002 | 订单状态不可评价 | 订单状态不是已完成 |

| 409 | 4003 | 该订单已评价 | 一单一评，重复提交 |

#### 业务规则与注意事项

- **一单一评**：每个订单只能评价一次。

- **评价前提**：订单状态必须为 `2=已完成`。

- **幂等推荐**：前端在 localStorage 中持久化 UUID 作为幂等键。

### 6.2 评价列表

| 字段 | 说明 |

|------|------|

| 接口名称 | 评价列表 |

| 接口标识 | listReviews |

| 请求方法 | GET |

| URL 路径 | /api/v1/reviews |

| 接口描述 | 获取指定服务项目的公开评价列表，支持分页。 |

| 所属模块 | 评价与投诉 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 查询参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 |

|--------|------|------|--------|------|

| itemId | integer | 是 | - | 服务项目 ID |

| page | integer | 否 | 1 | 页码 |

| size | integer | 否 | 20 | 每页条数 | 1-50

#### 响应格式

```json

{

  "code": 0, "message": "success",

  "data": { "list": [{ "reviewId": 30001, "rating": 5, "content": "非常满意！", "userNickname": "张阿姨", "createTime": "2026-07-08T11:00:00+08:00" }], "total": 3, "page": 1, "size": 20 }

}

```

#### 业务规则与注意事项

- 仅返回 `status=2`（已审核展示）的评价。

- 排序规则：按 `create_time` 倒序排列。

### 6.3 提交投诉

| 字段 | 说明 |

|------|------|

| 接口名称 | 提交投诉 |

| 接口标识 | submitComplaint |

| 请求方法 | POST |

| URL 路径 | /api/v1/complaints |

| 接口描述 | 用户对已完成或服务中的订单提交投诉，选择投诉类型并描述问题。 |

| 所属模块 | 评价与投诉 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 请求体

| 参数名 | 类型 | 必填 | 说明 |

|--------|------|------|------|

| orderId | integer | 是 | 关联的订单 ID |

| type | integer | 是 | 1=服务质量 / 2=服务态度 / 3=乱收费 / 4=其他 |

| content | string | 否 | 投诉内容描述，最多 1000 字符 |

| images | string[] | 否 | 投诉截图 URL 列表，最多 6 张 |

#### 响应格式

```json

{ "code": 0, "message": "投诉提交成功，我们将在 24 小时内处理", "data": { "complaintId": 40001 } }

```

#### 业务规则与注意事项

- 投诉表有 `idempotent_key` 唯一约束，防止重复提交。

- 投诉提交初始状态为 `0=待处理`。

### 6.4 投诉列表

| 字段 | 说明 |

|------|------|

| 接口名称 | 投诉列表 |

| 接口标识 | listComplaints |

| 请求方法 | GET |

| URL 路径 | /api/v1/complaints |

| 接口描述 | 获取当前用户的所有投诉记录列表，按时间倒序排列。 |

| 所属模块 | 评价与投诉 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 响应格式

```json

{

  "code": 0, "message": "success",

  "data": { "list": [{ "complaintId": 40001, "type": 1, "typeText": "服务质量", "status": 0, "statusText": "待处理", "createTime": "2026-07-08T12:00:00+08:00" }], "total": 1, "page": 1, "size": 20 }

}

```

#### 业务规则与注意事项

- 仅返回当前用户的投诉记录。

- 排序规则：按 `create_time` 倒序排列。

### 6.5 投诉处理记录

| 字段 | 说明 |

|------|------|

| 接口名称 | 投诉处理记录 |

| 接口标识 | getComplaintTracks |

| 请求方法 | GET |

| URL 路径 | /api/v1/complaints/{id}/tracks |

| 接口描述 | 查看指定投诉的处理过程记录，包括客服回复、处理意见等。 |

| 所属模块 | 评价与投诉 |

| 是否登录鉴权 | 是 |

| 权限要求 | 无 |

#### 响应格式

```json

{

  "code": 0, "message": "success",

  "data": {

    "complaintId": 40001, "status": 1, "statusText": "处理中",

    "tracks": [

      { "trackId": 1, "operator": "客服小王", "content": "已收到投诉，正在核实", "createTime": "2026-07-08T12:00:00+08:00" }

    ]

  }

}

```

#### 业务规则与注意事项

- 仅投诉发起人可查看处理记录。

- `tracks` 数组按 `create_time` 升序排列（处理时间线）。

---

## 附录 A：通用枚举值表

### 订单状态

| 枚举值 | 含义 | 说明 |

|--------|------|------|

| PENDING_PAYMENT | 待支付 | 订单已创建，等待支付 |

| WAITING_SERVICE | 待服务 | 已支付，等待护士上门 |

| COMPLETED | 已完成 | 服务完成 |

| CANCELLED | 已取消 | 用户取消或超时取消 |

| REFUNDING | 退款中 | 申请退款，等待审核 |

| REFUNDED | 已退款 | 退款审核通过 |

### 投诉类型：SERVICE_QUALITY=服务质量，SERVICE_ATTITUDE=服务态度，OVERCHARGING=乱收费，OTHER=其他
### 投诉状态：PENDING=待处理，PROCESSING=处理中，RESOLVED=已处理，CLOSED=已关闭
### 评价状态：PENDING_REVIEW=待审核，APPROVED=已展示，HIDDEN=隐藏
### 预约时段：MORNING=上午 08:00-12:00，AFTERNOON=下午 13:00-17:00，EVENING=晚上 18:00-21:00
### 用户性别：SECRET=保密，MALE=男，FEMALE=女
### 订单来源：APP=App，MINI_PROGRAM=小程序，ADMIN=后台

### 短信类型：register=注册验证码，login=登录验证码，reset_password=重置密码

## 附录 B：数据字典

### 通用响应结构

```json

{ "code": 0, "message": "success", "data": { ... } }

```

### 通用分页结构

```json

{ "list": [ ... ], "total": 120, "page": 1, "size": 20 }

```

### 订单状态流转图

```mermaid

stateDiagram-v2

    [*] --> 待支付: 创建订单

    待支付 --> 待服务: 支付成功

    待支付 --> 已取消: 用户取消/超时

    待服务 --> 已完成: 服务完成

    待服务 --> 退款中: 服务前取消（退款）

    已完成 --> 退款中: 申请退款

    退款中 --> 已退款: 审核通过

    退款中 --> 已完成: 审核拒绝

    已取消 --> [*]

    已完成 --> [*]

    已退款 --> [*]

```

## 附录 C：核心业务泳道图（接口调用时序）

```mermaid

sequenceDiagram

    participant App as App（前端）

    participant Gateway as API Gateway

    participant Auth as user-service

    participant Catalog as catalog-service

    participant Order as order-service

    participant Pay as 支付宝

    participant Feedback as feedback-service

    Note over App,Feedback: === 登录注册 ===

    App->>Gateway: POST sms-code / register / login

    Gateway->>Auth: JWT 签发

    Auth-->>App: Token

    Note over App,Feedback: === 浏览服务 ===

    App->>Gateway: GET categories / items

    Gateway->>Catalog: 查询分类/项目

    Catalog-->>App: 数据

    Note over App,Feedback: === 下单流程 ===

    App->>Gateway: POST prepay-token

    App->>Gateway: POST create（Idempotent-Key）

    Order->>Catalog: Feign 查价格

    Order-->>App: orderId

    Note over App,Feedback: === 支付 ===

    App->>Pay: 唤起支付宝

    Pay-->>Order: 异步回调（验签+幂等）

    Note over App,Feedback: === 评价投诉 ===

    App->>Gateway: POST reviews / complaints

    Feedback->>Order: Feign 查订单信息

    App->>Gateway: GET complaints/{id}/tracks

    Feedback-->>App: 处理时间线

```
