# 智慧护理平台 API 联调契约

生成日期：2026-07-16。本文是当前工作区唯一的前后端 HTTP 契约。前端只能依据本文调用 API；后端修改 Controller、DTO、错误码、鉴权或 JSON 序列化时，必须在同一变更中更新本文和契约测试。

## 1. 基础规则

- Base URL：`http://{gateway-host}:8080`。
- 所有公开 API 返回 `{"code":number,"message":string,"data":T|null}`。`code=0` 是成功；任何非 0 都是失败，即使 HTTP 为 2xx。
- 成功 HTTP 状态：注册为 `201`；其余 `Result` 成功响应为 `200`。校验失败 `400/1000`，未认证 `401/1002|1003`，无权限 `403/1004`，不存在 `404/1005`，冲突 `409/1006`，业务规则不满足 `422/1007`，限流 `429/1008`，未处理异常 `500/1999`。
- 所有响应和请求中的 `id` 或 `*Id` 都是十进制 **string**。前端不得把它们转换为 JavaScript number。金额、分页 `page/size/total`、状态、时长、评分均为 JSON number。
- 日期是 `yyyy-MM-dd`；时间是 ISO-8601 本地时间字符串；金额为十进制 JSON number。
- 除匿名接口外，发送 `Authorization: Bearer <token>`。前端绝不发送 `X-User-Id`、`X-User-Roles`、`X-Gateway-Token`、`X-Internal-Token`；这些是网关或服务网络可信头。
- 所有副作用公开接口必须发送 `Idempotency-Key`。短信键是 UUID；其余键为 1-128 字符。网络超时必须原键、原请求体重试；同键不同请求或处理中均为 `409/1006`。
- 网关匿名接口：短信发送、注册、登录、重置密码、管理员登录、分类查询、项目查询、支付回调。管理员路径要求 `ADMIN`；商家派单要求 `MERCHANT_MEMBER`；护工任务要求 `CAREGIVER`。

## 2. 公共数据模型

| 模型 | 字段 |
|---|---|
| `AuthResponse` | `token:string, expireTime:string, user:UserInfo, roles:string[], currentRole:CUSTOMER|CAREGIVER|MERCHANT_MEMBER|ADMIN, permissions:string[]` |
| `UserInfo` | `userId:string, phone:string, nickname:string, avatar:string|null, gender:0|1|2|null, idCard:string|null, status:number, version:number, lastLoginTime:string|null, createTime:string` |
| `Category` | `categoryId:string, parentId:string, name:string, icon:string|null, sortOrder:number, status:0|1, children:Category[]` |
| `Spec` | `id:string, serviceItemId:string, name:string, price:number, originalPrice:number|null, duration:number|null, status:0|1` |
| `ItemList` | `id:string, categoryId:string, categoryName:string, name:string, description:string|null, coverImage:string|null, sortOrder:number, status:0|1, minPrice:number|null, specs:Spec[]` |
| `ItemDetail` | `id:string, categoryId:string, categoryName:string, name:string, description:string|null, coverImage:string|null, images:string[], sortOrder:number, status:0|1, specs:Spec[], createTime:string` |
| `Address` | `addressId:string, receiverName:string, receiverPhone:string, tag:家|公司|学校|其他, province:string, city:string, district:string, detailAddress:string, isDefault:0|1` |
| `OrderList` | `orderId:string, orderNo:string, serviceItemName:string, specName:string, specPrice:number, totalAmount:number, status:number, serviceDate:string, serviceTimeSlot:MORNING|AFTERNOON|EVENING, receiverName:string, receiverPhone:string, addressDetail:string, createTime:string` |
| `OrderDetail` | `orderId:string, orderNo:string, serviceItemName:string, specName:string, specPrice:number, totalAmount:number, status:number, receiverName:string, receiverPhone:string, addressDetail:string, serviceDate:string, serviceTimeSlot:MORNING|AFTERNOON|EVENING, payStatus:number|null, operationLogs:OrderLog[], createTime:string` |
| `OrderLog` | `action:string, fromStatus:number|null, toStatus:number|null, remark:string|null, createTime:string` |
| `OrderDTO` | `orderId:string, orderNo:string, status:number, serviceItemId:string, serviceItemName:string|null, specName:string|null, totalAmount:number, userId:string` |
| `Review` | `reviewId:string, orderId:string, serviceItemId:string, rating:1|2|3|4|5, content:string, images:string[], userNickname:string|null, serviceItemName:string|null, specName:string|null, createTime:string` |
| `Complaint` | `complaintId:string, orderId:string, type:1|2|3|4, content:string, images:string[], status:0|1|2|3, createTime:string` |
| `ComplaintTrack` | `trackId:string, operator:string, content:string, createTime:string` |
| `Application` | `id:string, userId:string, realName:string, phone:string, serviceDistrict:string, skills:string, status:0|1|2, reviewRemark:string|null, createTime:string, updateTime:string` |
| `Assignment` | `assignmentId:string, orderId:string, merchantId:string, caregiverUserId:string, status:0|1|2, acceptedTime:string|null, rejectedTime:string|null` |
| `AdminCategory` | `id:string, parentId:string, name:string, icon:string|null, sortOrder:number, status:0|1, createTime:string, updateTime:string` |
| `AdminItem` | `id:string, categoryId:string, name:string, description:string|null, coverImage:string|null, sortOrder:number, status:0|1, createTime:string, updateTime:string` |
| `AdminSpec` | `id:string, serviceItemId:string, name:string, price:number, originalPrice:number|null, duration:number|null, status:0|1, createTime:string, updateTime:string` |
| `AdminReview` | `id:string, orderId:string, userId:string, serviceItemId:string, serviceItemName:string|null, specName:string|null, rating:number, content:string, status:number, createTime:string, updateTime:string` |
| `AdminComplaint` | `id:string, orderId:string, userId:string, type:1|2|3|4, content:string, images:string|null, status:0|1|2|3, createTime:string, updateTime:string` |

`Page<T>` 为 `{list:T[],total:number,page:number,size:number}`；`CursorPage<T>` 为 `{list:T[],size:number,hasNext:boolean,nextCursor:string|null}`。

## 3. 公开 API 清单

请求模型中 `!` 为必填，`?` 为可选。所有未列出的 Header、Query 或 Body 字段均不得发送。

### 3.1 认证、用户与文件

| 方法和路径 | 权限 | 请求 | 成功 data | 业务失败 |
|---|---|---|---|---|
| `POST /api/v1/users/sms-code` | 匿名 | H:`Idempotency-Key`; B:`phone:string!,smsType:register|login|reset_password!` | `expireSeconds:number,retryAfterSeconds:number|null,requestId:string,status:string` | `429/2001|2002`,`409/2019` |
| `GET /api/v1/users/sms-code/requests/{requestId}` | 匿名 | H:`Idempotency-Key`; P:`requestId:string!` | 同短信响应 | `404/2020`,`409/2019` |
| `POST /api/v1/users/register` | 匿名 | B:`phone:string!,smsCode:string!,password:string!,nickname:string?`; phone 11 位，验证码 6 位，密码 8-32 且含字母数字，昵称 2-16 | `AuthResponse` | `400/2006|2007`,`409/2008` |
| `POST /api/v1/users/login` | 匿名 | B:`phone:string!,loginMode:password|sms!,password:string?,smsCode:string?,targetRole:CUSTOMER|CAREGIVER|MERCHANT_MEMBER|ADMIN?` | `AuthResponse` | `400/2006|2007|2010`,`422/2004|2011` |
| `POST /api/v1/users/logout` | 登录 | 无 Body | `null` | `401/1002|1003` |
| `POST /api/v1/users/password/reset` | 匿名 | B:`phone:string!,smsCode:string!,newPassword:string!`；密码规则同注册 | `null` | `400/2006|2007|2012`,`422/2004` |
| `GET /api/v1/users/profile` | 登录 | 无 | `UserInfo` | `401/1002|1003` |
| `PATCH /api/v1/users/profile` | 登录 | B:`version:number!,nickname:string?,avatar:string?,gender:0|1|2?,idCard:string?` | `userId:string,nickname:string|null,avatar:string|null,gender:number|null,version:number` | `400/2013`,`409/2016` |
| `POST /api/v1/auth/switch-role` | 登录 | B:`targetRole:CUSTOMER|CAREGIVER|MERCHANT_MEMBER|ADMIN!` | `AuthResponse` | `403/1004` |
| `POST /api/v1/admin/login` | 匿名 | B:`username:string!,password:string!`；用户名 <=20，密码 6-72 | `AuthResponse` | `400/1000` |
| `POST /api/v1/files/upload` | 登录 | H:`Idempotency-Key`; Form:`file!,bizType:string!` | `fileUrl:string,fileName:string,fileSize:number,bizType:string` | `413/2014`,`400/2015`,`409/2017|2018` |

### 3.2 目录与目录管理

| 方法和路径 | 权限 | 请求 | 成功 data |
|---|---|---|---|
| `GET /api/v1/categories` | 匿名 | 无 | `Category[]` |
| `GET /api/v1/items` | 匿名 | Q:`categoryId:string?,keyword:string?,cursor:string?,size:number?` | `CursorPage<ItemList>` |
| `GET /api/v1/items/{id}` | 匿名 | P:`id:string!` | `ItemDetail` |
| `POST /api/v1/admin/catalog/categories` | ADMIN | B:`parentId:string?,name:string!,icon:string?,sortOrder:number?,status:0|1?` | `AdminCategory` |
| `PUT /api/v1/admin/catalog/categories/{id}` | ADMIN | P:`id:string!`; B:同创建 | `AdminCategory` |
| `POST /api/v1/admin/catalog/categories/{id}/publish` | ADMIN | P:`id:string!` | `null` |
| `POST /api/v1/admin/catalog/categories/{id}/unpublish` | ADMIN | P:`id:string!` | `null` |
| `POST /api/v1/admin/catalog/items` | ADMIN | B:`categoryId:string!,name:string!,description:string?,coverImage:string?,sortOrder:number?,status:0|1?` | `AdminItem` |
| `PUT /api/v1/admin/catalog/items/{id}` | ADMIN | P:`id:string!`; B:同创建 | `AdminItem` |
| `POST /api/v1/admin/catalog/items/{id}/publish` | ADMIN | P:`id:string!` | `null` |
| `POST /api/v1/admin/catalog/items/{id}/unpublish` | ADMIN | P:`id:string!` | `null` |
| `POST /api/v1/admin/catalog/specs` | ADMIN | B:`serviceItemId:string!,name:string!,price:number!,originalPrice:number?,duration:number?,status:0|1?`; price >=0.01,duration >=1 | `AdminSpec` |
| `PUT /api/v1/admin/catalog/specs/{id}` | ADMIN | P:`id:string!`; B:同创建 | `AdminSpec` |
| `POST /api/v1/admin/catalog/specs/{id}/publish` | ADMIN | P:`id:string!` | `null` |
| `POST /api/v1/admin/catalog/specs/{id}/unpublish` | ADMIN | P:`id:string!` | `null` |

目录管理失败统一为 `400/1000`、`403/1004`、`404/1005` 或 `409/1006`。

### 3.3 地址、订单与支付

| 方法和路径 | 权限 | 请求 | 成功 data | 业务失败 |
|---|---|---|---|---|
| `GET /api/v1/addresses` | 登录 | 无 | `Address[]` | `401/1002|1003`,`403/1004` |
| `POST /api/v1/addresses` | 登录 | B:`receiverName:string!,receiverPhone:string!,tag:家|公司|学校|其他!,province:string!,city:string!,district:string!,detailAddress:string!,isDefault:0|1?` | `addressId:string` | `400/1000` |
| `PATCH /api/v1/addresses/{id}` | 登录 | P:`id:string!`; B:地址字段均可选，提供时须满足创建校验 | `null` | `404/3013` |
| `DELETE /api/v1/addresses/{id}` | 登录 | P:`id:string!` | `null` | `404/3013` |
| `PUT /api/v1/addresses/{id}/default` | 登录 | P:`id:string!` | `null` | `404/3013` |
| `POST /api/v1/orders/prepay-token` | 登录 | 无 | `prepayToken:string,expireTime:string` | `401/1002|1003` |
| `POST /api/v1/orders` | 登录 | H:`Idempotency-Key`=prepayToken; B:`serviceItemId:string!,serviceSpecId:string!,addressId:string!,serviceDate:string!,serviceTimeSlot:MORNING|AFTERNOON|EVENING!,remark:string?` | `orderId:string,orderNo:string` | `400/3001`,`409/3002`,`422/3003|3004|3006` |
| `GET /api/v1/orders` | 登录 | Q:`status:number?,page:number=1,size:number=20`；status 0-5，size 1-50 | `Page<OrderList>` | `400/1000` |
| `GET /api/v1/orders/{id}` | 登录 | P:`id:string!` | `OrderDetail` | `404/3007`,`403/3008` |
| `POST /api/v1/orders/{id}/cancel` | 登录 | P:`id:string!`; B:`cancelReason:string?` <=200 | `orderId:string,status:number,refundStatus:string|null` | `422/3009` |
| `POST /api/v1/orders/{id}/confirm` | 登录 | P:`id:string!` | `OrderDTO` | `422/3010` |
| `POST /api/v1/orders/{id}/complete` | 登录 | P:`id:string!` | `OrderDTO` | `422/3010` |
| `POST /api/v1/orders/{id}/pay` | 登录 | P:`id:string!`; H:`Idempotency-Key`; B:`payChannel:alipay!` | `orderId:string,orderNo:string,payChannel:alipay,payAmount:number,payStatus:string,mock:boolean,payParams:object` | `409/1006`,`422/3010` |
| `POST /api/v1/orders/pay/callback` | 支付渠道 | Form:支付宝签名参数 | plain `success` | 非前端接口 |

### 3.4 评价、投诉与反馈管理

| 方法和路径 | 权限 | 请求 | 成功 data | 业务失败 |
|---|---|---|---|---|
| `POST /api/v1/reviews` | 登录 | H:`Idempotency-Key`; B:`orderId:string!,rating:1..5!,content:string!,images:string[]?`; content <=500, images <=6 | `reviewId:string` | `400/4001|4002`,`409/4003` |
| `GET /api/v1/reviews` | 登录 | Q:`itemId:string!,page:number=1,size:number=20` | `Page<Review>` | `400/1000` |
| `POST /api/v1/complaints` | 登录 | H:`Idempotency-Key`; B:`orderId:string!,type:1..4!,content:string!,images:string[]?`; content <=1000, images <=6 | `complaintId:string` | `400/4004|4005` |
| `GET /api/v1/complaints` | 登录 | Q:`page:number=1,size:number=20` | `Page<Complaint>` | `400/1000` |
| `GET /api/v1/complaints/{id}/tracks` | 登录 | P:`id:string!` | `complaintId:string,status:number,statusText:string,tracks:ComplaintTrack[]` | `404/4006`,`403/4007` |
| `GET /api/v1/admin/feedback/reviews` | ADMIN | Q:`status:number?,page:number=1,size:number=20` | `Page<AdminReview>` | `400/1000` |
| `POST /api/v1/admin/feedback/reviews/{id}/moderate` | ADMIN | P:`id:string!`; B:`status:2|3!` | `null` | `404/1005`,`409/1006` |
| `GET /api/v1/admin/feedback/complaints` | ADMIN | Q:`status:number?,page:number=1,size:number=20` | `Page<AdminComplaint>` | `400/1000` |
| `POST /api/v1/admin/feedback/complaints/{id}/handle` | ADMIN | P:`id:string!`; B:`status:1|2|3!,content:string!` | `null` | `404/4006`,`409/1006` |

### 3.5 护工申请、派单与履约

| 方法和路径 | 权限 | 请求 | 成功 data | 业务失败 |
|---|---|---|---|---|
| `POST /api/v1/caregiver/applications` | 登录 | B:`realName:string!,phone:string!,serviceDistrict:string!,skills:string!`; max 32/20/64/256 | `Application` | `409/1006` |
| `GET /api/v1/caregiver/applications/me` | 登录 | 无 | `Application|null` | `401/1002|1003` |
| `GET /api/v1/admin/caregiver-applications` | ADMIN | 无 | `Application[]` | `403/1004` |
| `POST /api/v1/admin/caregiver-applications/{userId}/approve` | ADMIN | P:`userId:string!`; Q:`remark:string?` | `null` | `409/1006` |
| `POST /api/v1/admin/caregiver-applications/{userId}/reject` | ADMIN | P:`userId:string!`; Q:`remark:string?` | `null` | `409/1006` |
| `POST /api/v1/merchants/orders/{orderId}/dispatch` | MERCHANT_MEMBER | P:`orderId:string!`; H:`Idempotency-Key`; B:`caregiverUserId:string!,remark:string?` | `Assignment` | `400/1000`,`403/1004`,`409/1006` |
| `POST /api/v1/merchants/orders/{orderId}/redispatch` | MERCHANT_MEMBER | P:`orderId:string!`; H:`Idempotency-Key`; B:`caregiverUserId:string!,remark:string?` | `Assignment` | `400/1000`,`403/1004`,`409/1006` |
| `POST /api/v1/caregivers/assignments/{assignmentId}/accept` | CAREGIVER | P:`assignmentId:string!`; H:`Idempotency-Key` | `Assignment` | `400/1000`,`403/1004`,`409/1006` |
| `POST /api/v1/caregivers/assignments/{assignmentId}/reject` | CAREGIVER | P:`assignmentId:string!`; H:`Idempotency-Key`; B:`remark:string?` | `Assignment` | `400/1000`,`403/1004`,`409/1006` |
| `POST /api/v1/caregivers/orders/{orderId}/{action}` | CAREGIVER | P:`orderId:string!,action:depart|check-in|start|finish`; H:`Idempotency-Key`; B:`remark:string?` | `Assignment` | `400/1000`,`403/1004`,`409/1006` |
| `GET /api/v1/merchants/assignments` | MERCHANT_MEMBER | 无 | `Assignment[]` | `403/1004` |
| `GET /api/v1/caregivers/tasks` | CAREGIVER | 无 | `Assignment[]` | `403/1004` |

## 4. 内部服务 API

内部接口不经过网关，禁止浏览器调用，必须携带 `X-Internal-Token`。

| 方法和路径 | 调用方 | 请求 | 成功 data |
|---|---|---|---|
| `POST /internal/v1/users/{id}/roles/{roleCode}` | 运营服务 | P:`id:string!,roleCode:CUSTOMER|CAREGIVER|MERCHANT_MEMBER|ADMIN!` | `null` |
| `GET /internal/v1/orders/{id}` | 反馈、运营服务 | P:`id:string!` | `OrderDTO` |
| `POST /internal/v1/orders/batch` | 反馈、运营服务 | B:`string[]`，长度 1-100 | `OrderDTO[]` |
| `POST /internal/v1/orders/{id}/transition` | 运营服务 | P:`id:string!`; B:`fromStatus:number!,toStatus:number!,reason:string?` | `OrderDTO` |

## 5. 状态机

订单：`0 待支付 -> 1 待派单 -> 6 已派单 -> 7 已接单 -> 8 服务中 -> 9 待客户确认 -> 2 已完成`。取消仅从 `0|1` 到 `3 已取消`；退款为 `4 退款中 -> 5 已退款`。护工动作严格为 `depart -> check-in -> start -> finish`；start 使订单 `7->8`，finish 使订单 `8->9`。护工申请：`0 待审,1 通过,2 驳回`，仅 `2` 可重新申请。评价：`1 待审,2 通过,3 驳回`。投诉：`0 待处理,1 处理中,2 已解决,3 已关闭`。

## 6. 覆盖核对

源码有 63 个 Mapping 注解；其中两个注解各声明两个路径，共 65 条具体路径：61 条公开 API、4 条内部 API。网关 Nacos 路由覆盖所有公开路径前缀；`/internal/v1/**` 不由网关暴露。
