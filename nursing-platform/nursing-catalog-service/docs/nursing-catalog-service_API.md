# catalog-service 接口说明

> 基于当前实现整理。本文覆盖 `nursing-catalog-service` 显式定义的业务 HTTP 接口，不包含 Spring Boot Actuator 自动暴露的运维端点。

## 1. 服务约定

| 项目 | 说明 |
| --- | --- |
| 服务名 | `nursing-catalog-service` |
| 默认端口 | `8082` |
| 网关路由 | `/api/v1/categories/**`、`/api/v1/items/**` 转发至本服务 |
| 鉴权 | 上述业务路径位于网关默认白名单，当前实现无需 JWT |
| Content-Type | `application/json` |
| 成功响应 | HTTP `200`，`{ "code": 0, "message": "success", "data": ... }` |

网关地址由部署环境决定。以下路径既可作为网关转发路径使用，也可在服务直连时配合 `http://<host>:8082` 使用。

### 1.1 统一错误响应

```json
{
  "code": 1000,
  "message": "参数错误",
  "data": null
}
```

| HTTP 状态 | `code` | 场景 |
| --- | ---: | --- |
| 400 | 1000 | 关键字、分类 ID、项目 ID、分页游标或分页大小不合法 |
| 404 | 1005 | 指定项目或指定分类不可见、不存在或已逻辑删除 |
| 500 | 1999 | 未被业务层处理的系统异常 |

`categoryId`、`size` 无法转换为数值类型时，没有专门的类型转换异常处理器，当前会落入通用异常处理，表现为 HTTP `500`、`code=1999`。格式错误的 `cursor` 由服务层返回 HTTP `400`、`code=1000`。

### 1.2 共用对象

#### 游标分页对象 `CursorPageResponse<ItemListResponse>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `list` | `ItemListResponse[]` | 本次返回的项目 |
| `size` | `integer` | 实际每页条数 |
| `hasNext` | `boolean` | 是否存在下一页 |
| `nextCursor` | `string` 或 `null` | 下一页游标；`hasNext=false` 时为 `null` |

首次请求不传 `cursor`；存在下一页时，将响应中的 `nextCursor` 原样作为下一次请求的 `cursor`。该接口不返回总数，也不支持跳转至指定页码，仅支持向后连续读取。

#### 项目列表项 `ItemListResponse`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `number` | 项目 ID。Java 字段为 `itemId`，序列化名称为 `id`。 |
| `categoryId` | `number` | 所属叶子分类 ID |
| `categoryName` | `string` | 所属分类名称；分类记录不存在时可为 `null` |
| `name` | `string` | 项目名称 |
| `description` | `string` | 项目描述 |
| `coverImage` | `string` | 封面图 URL |
| `sortOrder` | `integer` | 展示排序值 |
| `status` | `integer` | 项目状态；接口仅返回 `1`（上架）项目 |
| `minPrice` | `number` | 启用规格的最低售价；没有启用规格时为 `null` |
| `specs` | `ServiceSpecResponse[]` | 启用规格列表 |

#### 项目规格 `ServiceSpecResponse`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `number` | 规格 ID。Java 字段为 `specId`，序列化名称为 `id`。 |
| `serviceItemId` | `number` | 所属项目 ID |
| `name` | `string` | 规格名称 |
| `price` | `number` | 售价，数据库精度为 `DECIMAL(10,2)` |
| `originalPrice` | `number` | 原价，可为 `null` |
| `duration` | `integer` | 服务时长，单位为分钟，可为 `null` |
| `status` | `integer` | 规格状态；接口仅返回 `1`（上架）规格 |

## 2. 获取分类树

`GET /api/v1/categories`

返回全部可展示且未逻辑删除的分类，按父子关系组装为树。

### 请求参数

无。

### 成功响应 `data`

`CategoryTreeResponse[]`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `categoryId` | `number` | 分类 ID |
| `parentId` | `number` | 父分类 ID；`0` 或 `null` 表示顶级节点 |
| `name` | `string` | 分类名称 |
| `icon` | `string` | 图标 URL，可为 `null` |
| `sortOrder` | `integer` | 同级排序值 |
| `status` | `integer` | 分类状态；接口仅返回 `1` |
| `children` | `CategoryTreeResponse[]` | 子分类。没有子分类时该字段不输出。 |

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "categoryId": 101,
      "parentId": 0,
      "name": "康复护理",
      "icon": null,
      "sortOrder": 1,
      "status": 1,
      "children": [
        {
          "categoryId": 111,
          "parentId": 101,
          "name": "术后康复",
          "icon": null,
          "sortOrder": 1,
          "status": 1
        }
      ]
    }
  ]
}
```

## 3. 游标浏览或搜索项目

`GET /api/v1/items`

同一路径支持普通浏览和关键字搜索：请求中没有 `keyword` 参数时执行普通游标查询；只要 `keyword` 参数存在且不为 `null`，即使值为空白，也执行搜索分支。

### 请求参数

| 参数 | 位置 | 类型 | 必填 | 默认值 | 规则 |
| --- | --- | --- | --- | --- | --- |
| `categoryId` | query | `long` | 否 | - | 省略时跨分类查询；传入时必须大于 `0` 且分类及其祖先必须可见。传入父分类时，结果包含全部可见后代分类下的项目。 |
| `keyword` | query | `string` | 否 | - | 存在时启用搜索；去除首尾空白后必须非空。匹配项目名称或描述。 |
| `cursor` | query | `string` | 否 | - | 首次请求省略；后续请求传入上一次响应的 `nextCursor`，不可自行拼接或修改。 |
| `size` | query | `integer` | 否 | `20` | 必须在 `1` 到 `50` 之间。 |

### 成功响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 201,
        "categoryId": 113,
        "categoryName": "康复理疗",
        "name": "上门康复推拿",
        "description": "专业康复师上门推拿服务。",
        "coverImage": null,
        "sortOrder": 1,
        "status": 1,
        "minPrice": 198.00,
        "specs": [
          {
            "id": 301,
            "serviceItemId": 201,
            "name": "单次体验",
            "price": 198.00,
            "originalPrice": 298.00,
            "duration": 60,
            "status": 1
          }
        ]
      }
    ],
    "size": 20,
    "hasNext": true,
    "nextCursor": "MToyMDE"
  }
}
```

### 调用示例

```text
GET /api/v1/items?categoryId=101&size=20
GET /api/v1/items?categoryId=101&size=20&cursor=MToyMDE
GET /api/v1/items?keyword=%E6%8E%A8%E6%8B%BF&categoryId=101&size=10
```

## 4. 获取项目详情

`GET /api/v1/items/{id}`

### 路径参数

| 参数 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `id` | `long` | 是 | 必须大于 `0`，且对应项目必须已上架、未逻辑删除。 |

### 成功响应 `data`

`ItemDetailResponse`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `number` | 项目 ID |
| `categoryId` | `number` | 所属叶子分类 ID |
| `categoryName` | `string` | 所属分类名称，可为 `null` |
| `name` | `string` | 项目名称 |
| `description` | `string` | 项目详情描述 |
| `coverImage` | `string` | 封面图 URL |
| `images` | `string[]` | 当前 SQL 不读取独立图片表；初始为空，若 `coverImage` 非空则仅补入该封面图。 |
| `sortOrder` | `integer` | 展示排序值 |
| `status` | `integer` | 项目状态；成功返回时为 `1` |
| `specs` | `ServiceSpecResponse[]` | 启用规格列表，按规格 ID 升序 |
| `createTime` | `string` | 项目创建时间，JSON 时间格式由 Spring 全局 Jackson 配置决定。 |

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 201,
    "categoryId": 113,
    "categoryName": "康复理疗",
    "name": "上门康复推拿",
    "description": "专业康复师上门推拿服务。",
    "coverImage": "/assets/default-service-cover.png",
    "images": ["/assets/default-service-cover.png"],
    "sortOrder": 1,
    "status": 1,
    "specs": [
      {
        "id": 301,
        "serviceItemId": 201,
        "name": "单次体验",
        "price": 198.00,
        "originalPrice": 298.00,
        "duration": 60,
        "status": 1
      }
    ],
    "createTime": "2026-07-01T10:00:00"
  }
}
```

## 5. 兼容性说明

- `id` 是项目和规格在 JSON 中的正式字段名；调用方不应依赖 Java 内部名称 `itemId` 或 `specId`。
- 共享模块中的 `CatalogServiceFeignClient` 也请求 `GET /api/v1/items/{id}`。变更该响应字段时，需要同时验证其 `ServiceItemDTO` 反序列化兼容性。
- 当前服务只读。分类、项目与规格的创建、修改、上下架、删除均没有 HTTP 实现，不能视为可调用接口。
