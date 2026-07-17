# GPT-5.6 Terra 提示词：生成完整前后端 API 接口文档

```text
你是一名资深后端架构师和 API 技术写作专家。请基于当前工作区中的真实 Spring Cloud Java 源码，为“互联网+智慧护理平台”生成一份可直接供前端与后端联调使用的完整 API 接口文档。

## 任务目标

只生成文档，不修改 Java、YAML、SQL、测试或部署配置。将最终文档写入：

`nursing-platform/docs/API接口文档.md`

文档必须使用 UTF-8 编码、简体中文、Markdown，并作为单一、完整、可交付的前后端接口契约。不要只给摘要、目录或生成计划。

## 代码库与事实优先级

项目根目录：`nursing-platform`

这是一个 Spring Boot 3.5 / Spring Cloud 微服务项目，模块包括：

- `nursing-gateway`：统一入口、路由、JWT 鉴权和跨域配置。
- `nursing-user-service`：注册、登录、短信、用户资料、文件上传、角色切换。
- `nursing-catalog-service`：服务分类、服务项目、规格与后台目录管理。
- `nursing-order-service`：地址、订单、支付、取消、确认、回调及内部订单接口。
- `nursing-feedback-service`：评价、投诉、后台审核与处理。
- `nursing-operations-service`：护工申请、审核、派单、接单、服务履约与任务查询。
- `nursing-common`：统一响应、异常、错误码、枚举和跨服务 DTO。

请严格按下列事实优先级核对，低优先级内容不得覆盖高优先级内容：

1. `src/main/java/**/controller/**/*Controller.java` 的 Spring Mapping 与方法签名：接口是否存在、HTTP 方法、路径、请求来源、返回类型的最高依据。
2. Controller 调用的 request/response DTO、实体、Service、Feign Client、网关 Filter、拦截器、全局异常处理与 `nursing-common`：字段、校验、鉴权、业务状态、错误码和实际语义的依据。
3. `nursing-gateway/src/main/resources/application.yml`、`docker-compose/nacos/config/nursing-gateway*.yaml`：前端可访问路由、网关端口和路由边界的依据。
4. `deploy/mysql/**`、Mapper XML：必要时用于补充枚举值、状态和持久化约束，不能仅凭表字段虚构 HTTP 接口。
5. 已有文档仅作交叉校验，不能作为接口事实来源：
   - `docs/接口与数据流实现说明.md`
   - `docs/address-order-frontend-api-contract.md`
   - `nursing-*-service/docs/*_API.md`
   - `nursing-*-service/docs/*_IMPLEMENTATION.md`

若源码、网关配置和已有契约冲突，正文必须以“当前代码实际行为”为准；在文末“待确认与风险”表中精确列出冲突，包含文件路径、冲突内容、对前端的影响和建议处理方式。不得悄悄选择旧文档，也不得编造尚未实现的接口、字段、枚举或 HTTP 状态。

## 必须覆盖的接口范围

逐一枚举所有带 Spring MVC Mapping 的 Controller 方法，不能遗漏多路径 Mapping、内部接口或管理端接口。至少检索以下目录：

- `nursing-user-service/src/main/java/**/controller/`
- `nursing-catalog-service/src/main/java/**/controller/`
- `nursing-order-service/src/main/java/**/controller/`
- `nursing-feedback-service/src/main/java/**/controller/`
- `nursing-operations-service/src/main/java/**/controller/`

将接口分成两个明确区域：

1. **前端公开 API（经网关访问）**：仅限网关配置中 `Path=` 能匹配的 `/api/v1/**` 接口。统一以 `http://{gateway-host}:8080` 为示例 Base URL。
2. **内部服务 API（非前端调用）**：所有 `/internal/v1/**` 接口，以及只应由网关/服务调用的接口。必须标明“禁止前端直接调用”、调用方和信任边界；不要把它们混入前端接口列表。

管理端 `/api/v1/admin/**` 和角色端（商家、护工）接口属于公开 API，但必须明确其所需角色。把匿名、登录用户、管理员、商家、护工的访问权限分开说明。

## 文档结构（必须完整输出）

按以下顺序组织 `API接口文档.md`：

1. 标题、生成日期、源码基线说明和适用范围。
2. 服务与网关概览：网关 Base URL、五个业务服务、每个网关路由前缀及目标服务。提供一张“路径前缀 -> 服务 -> 调用方”表。
3. 通用调用约定：
   - 统一成功/失败响应信封，依据 `Result<T>` 和 `PageResult<T>` 给出 JSON 示例。
   - HTTP 状态、业务 `code`、`message`、`data` 的关系；仅列出源码能确认的错误码含义。
   - JWT `Authorization: Bearer <token>`、匿名端点、角色边界。
   - `X-Gateway-Token`、`X-User-Id` 等下游可信请求头的来源和安全规则。前端不能伪造或手动发送由网关注入的头；若当前代码存在例外，按代码准确说明。
   - JSON 时间格式、日期格式、枚举表达、分页（页码分页与游标分页分别说明）、文件上传 `multipart/form-data`。
   - Snowflake ID 的前端精度风险：检查所有 ID 的实际序列化类型；若 `Long` 仍会序列化为 JSON number，而现有契约要求字符串，正文按实际代码描述并在风险表中醒目标注，不能把未实现的字符串序列化写成既有事实。
   - `Idempotent-Key` 的适用端点、生成/重试规则、同键不同请求体的行为和不可重试错误。
4. 前端公开 API：按“认证与用户、文件、服务目录、地址、订单与支付、评价与投诉、运营与履约、管理后台”分组。每个实际端点都必须有独立小节，不能用“同上”省略字段。
5. 内部服务 API：逐个端点列出路径、方法、调用方、鉴权/可信头、请求字段、响应字段、错误与幂等规则。
6. 前端关键业务流程：至少覆盖注册/登录、下单与支付、地址管理、评价、投诉、护工申请与派单履约。每个流程用编号步骤说明应调用的接口、前端状态变化、重复提交/网络超时处理和关键错误的 UI 处理。只引用已实现接口。
7. 错误码总表：按通用、用户、订单、反馈、运营/目录分组。每条写业务 code、HTTP 状态（仅在源码可确定时）、含义、前端动作；不确定项标记“待确认”。
8. 接口清单与覆盖核对：一张表列出全部 Controller 方法（服务、Controller#method、方法、路径、公开/内部、文档章节）。多路径 Mapping 必须分行列出，作为无遗漏自检。
9. 待确认与风险：仅列出从源码或文档对比中发现的真实问题、未暴露能力、路由不可达、序列化不一致、鉴权边界或配置依赖。不要把猜测写成事实。

## 每个端点的强制模板

对每一个端点，严格给出以下信息。无内容时写“无”，不要省略。

- 接口名称与业务目的。
- 方法、完整网关访问路径（内部接口则写服务内路径）、所属服务、Controller#method。
- 访问权限：匿名/登录用户/管理员/商家/护工/内部服务，并解释服务端如何实际判定。
- `Content-Type` 与所有请求 Header：名称、必填性、示例、用途、由谁设置。
- Path、Query、Body 或 Multipart 参数表：字段名、JSON 类型、Java 类型（必要时）、必填性、校验规则/取值范围、默认值、业务说明。嵌套对象与数组必须展开。
- 一份可直接使用的请求示例：JSON 或 `curl`。示例 ID 采用字符串时须与当前实际响应类型保持一致；不得把大整数当作 JavaScript 安全 number 使用。
- 成功响应：说明 `data` 的字段表（含所有嵌套字段、类型、可空性、枚举含义），再给一份真实信封格式的 JSON 示例。
- 失败响应：列出此接口实际可能的校验、鉴权、状态冲突、资源不存在、幂等和依赖故障；每项包含 HTTP 状态、业务 code、触发条件、前端处理建议。无法从代码确认 HTTP 状态时明确标注“待确认”，不要猜测。
- 幂等、并发、状态转换、数据脱敏、重试限制和前端注意事项（如适用）。

## 质量要求

- 以字段级契约为目标，不要把 Java 类名、数据库内部字段或实现细节直接丢给前端，除非它们影响调用行为。
- 所有 JSON 示例都必须符合对应 DTO 的字段名、类型、校验规则和统一响应信封。
- 每个枚举、状态码、日期格式和分页字段都必须可追溯到源码；不确定时写“待确认”。
- 明确区分 HTTP 200 但业务 `code != 0` 的场景，与真正的 HTTP 4xx/5xx。
- 不输出密钥、JWT、手机号实名信息、身份证、数据库密码或任何环境变量实际值；使用脱敏或占位符。
- 不将网关下游的可信 Header 设计为前端接口参数；同时准确记录当前实现若依赖这些头的事实。
- 不改变接口设计，不生成 OpenAPI/Swagger 文件，不新增依赖，不修改源码。
- 完成后自检：扫描全部 Controller Mapping，与“接口清单与覆盖核对”逐行比对；确认每个端点都在正文有字段、示例和错误处理说明。

## 输出要求

完成写入后，在回复中只汇报：生成文件路径、公开 API 与内部 API 的端点数、发现的关键待确认项数量，以及是否发现网关不可达的 `/api/v1/**` Controller 路由。不要输出冗长的工作过程。
```

## 使用方式

将上方代码块完整复制给 GPT-5.6 Terra，并让其在当前工作区执行。提示词已按本项目的网关路由、微服务划分、统一响应和既有接口契约定制。
