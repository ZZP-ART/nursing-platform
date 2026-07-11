# 开发计划书：nursing-catalog-service

> **文档版本**：v1.0
> **日期**：2026-07-08
> **编写依据**：系统架构设计.md v2.0、API 接口文档.md v1.0、数据库 DDL：02_catalog_schema.sql
> **对应分支**：codex/catalog-service（基于 develop）→ 开发完成后 squash merge 回 develop

---

## 一、概述
### 1.1 服务定位

nursing-catalog-service（服务目录服务）是智慧护理平台的服务浏览核心，负责管理服务分类、服务项目及规格价格的 CRUD 和查询。所有对外 API **免鉴权**（Gateway 直接放行），供前端用户在未登录状态下浏览服务分类和项目。

### 1.2 服务信息

| 项目 | 值 |
|------|-----|
| Maven artifactId | nursing-catalog-service |
| 服务注册名 | nursing-catalog-service |
| 端口 | 8082 |
| 数据库 | catalog_db |
| Snowflake worker-id | 2 |
| Snowflake datacenter-id | 1 |

### 1.3 当前进展

骨架已完成：

```text
nursing-catalog-service/
+-- pom.xml                          # 依赖就绪：mybatis, mysql, common, web, nacos, feign, redis
+-- src/main/
    +-- java/com/nursing/catalog/
    |   +-- CatalogApplication.java  # @SpringBootApplication + @EnableDiscoveryClient
    +-- resources/
        +-- application.yml          # 端口、数据源、Redis、Nacos、MyBatis 已配置
        +-- application-dev.yml      # 日志 DEBUG 级别
        +-- bootstrap.yml            # Nacos 配置中心
```

公共模块（nursing-common）已提供：Result、PageResult、BusinessException、GlobalExceptionHandler、SnowflakeIdWorker、ApiCode。

---

## 二、需要完成的工作

### 2.1 SQL Schema 更新（DDL 补丁）

当前 `02_catalog_schema.sql` 中 `service_category` 表 **缺少 `parent_id` 字段**，无法支持多级树形结构。需要在 DDL 中补上该字段，并调整种子数据。

**变更内容：**

1. `service_category` 表新增字段 `parent_id BIGINT DEFAULT 0 COMMENT '父分类ID(0表示顶级)'`
2. `service_category` 新增索引 `INDEX idx_parent (parent_id)`
3. 种子数据补充 `parent_id` 值（当前 4 个一级分类的 `parent_id = 0`），并可追加二级分类示例

> **设计约定**：使用 `parent_id = 0` 表示顶级分类。最多支持 3 级，由业务层递归构建树，不做深度限制。

### 2.2 实体层（entity）

每个实体对应一张业务表，使用 `SnowflakeIdWorker` 生成 ID（所有 DDL 显式指定 `BIGINT NOT NULL`，无 `AUTO_INCREMENT`）。

| 类名 | 对应表 | 关键字段 |
|------|--------|---------|
| ServiceCategory | service_category | id, parentId, name, icon, sortOrder, status, isDeleted, createTime, updateTime |

所有实体加 `@Data`（Lombok）。

### 2.3 DTO 层（dto）

| 类名 | 用途 | 说明 |
|------|------|------|
| CategoryTreeResponse | 分类树节点 | id, name, icon, children（递归 List） |
| ItemListResponse | 项目列表项 | id, name, coverImage, categoryId, minPrice, status |
| ItemDetailResponse | 项目详情 | id, name, description, coverImage, categoryId, specs |
| ServiceSpecResponse | 规格信息 | id, name, price, originalPrice, duration |
| CursorPageResponse | 游标分页响应 | list, size, hasNext, nextCursor |

### 2.4 Mapper 层（MyBatis XML）

3 个 Mapper 接口 + 3 个 XML 文件。

**ServiceCategoryMapper**

| 方法 | SQL 说明 |
|------|---------|
| selectListVisible() | 查询所有 `status=1 AND is_deleted=0` 的分类，按 `sort_order` 排序 |

**ServiceItemMapper**

| 方法 | SQL 说明 |
|------|---------|
| selectPage(categoryIds, cursorSortOrder, cursorId, limit) | 游标查询项目，支持按分类及可见后代筛选；按 `(sort_order, id)` 向后读取 |
| selectById(Long id) | 按 ID 查项目详情 |
| searchPage(keyword, categoryIds, cursorSortOrder, cursorId, limit) | 游标搜索，`keyword` 模糊匹配 `name + description`，按 `(sort_order, id)` 向后读取 |

**ServiceSpecMapper**

| 方法 | SQL 说明 |
|------|---------|
| selectByItemId(Long serviceItemId) | 查询某个服务的所有规格（`status=1, is_deleted=0`） |

> **分页方式**：使用原生 MyBatis 游标分页，以 `(sort_order, id)` 作为键集条件，查询 `size + 1` 条记录生成 `nextCursor`，不执行 `OFFSET` 和总数计数。

### 2.5 Service 层

**CategoryService**

`buildCategoryTree(): List<CategoryTreeResponse>`

- 查询所有 `status=1` 且 `is_deleted=0` 的分类
- 按 `parent_id` 构建内存树
- 返回顶层根节点列表

**ItemService**

`getItemPage(categoryId, cursor, size): CursorPageResponse<ItemListResponse>`

- 校验分页参数
- 调用 mapper 查询 + 计数
- 组装 `PageResult`
- `categoryId` 为 `null` 时返回所有项目

`getItemDetail(id): ItemDetailResponse`

- 查 `service_item` + 查 `service_spec`
- 组装成 `ItemDetailResponse`
- id 无效时抛出 `BusinessException(NOT_FOUND, "服务项目不存在")`

`searchItems(keyword, categoryId, cursor, size): CursorPageResponse<ItemListResponse>`

- 校验 `keyword` 非空
- mapper 模糊搜索（匹配 `name` 和 `description`）
- 分页返回

### 2.6 Controller 层

**CategoryController**，路径前缀 `/api/v1/categories`

| 方法 | 路径 | 参数 | 响应 |
|------|------|------|------|
| GET | /api/v1/categories | 无 | Result<List<CategoryTreeResponse>> |

**ItemController**，类级 `@RequestMapping("/api/v1/items")`

| 方法 | 路径 | 参数 | 响应 |
|------|------|------|------|
| GET | /api/v1/items | categoryId(opt), keyword(opt), cursor(opt), size(def=20) | Result<CursorPageResponse<ItemListResponse>> |
| GET | /api/v1/items/{id} | id(path) | Result<ItemDetailResponse> |

**路径说明**：`/items` 和 `/items/search` 在 Spring 中不会冲突，`/search` 是精确路径，优先级更高。

### 2.7 Feign 客户端契约（供 order-service 调用）

order-service 在下单时通过 Feign 调用 `GET /api/v1/items/{id}` 获取最新价格。

关键要求：

- 该接口的 JSON 结构一经确定即保持向后兼容
- `price` 字段名保持为 `price`，类型为 `BigDecimal`
- 不随意删除现有字段

Feign 调用方使用 `nursing-common` 中的 `ServiceItemDTO` 与 `ServiceSpecDTO` 反序列化该详情响应。

### 2.8 Bean 配置

在 `config/CatalogConfig.java` 中配置 `SnowflakeIdWorker` Bean，使用 `application.yml` 中的 `nursing.snowflake` 配置。

---

## 三、最终文件结构

```text
nursing-catalog-service/src/main/java/com/nursing/catalog/
+-- CatalogApplication.java
+-- config/
|   +-- CatalogConfig.java                    # SnowflakeIdWorker Bean
+-- controller/
|   +-- CategoryController.java               # GET /api/v1/categories
|   +-- ItemController.java                   # GET /api/v1/items/**
+-- service/
|   +-- CategoryService.java
|   +-- ItemService.java
+-- repository/
|   +-- ServiceCategoryMapper.java
|   +-- ServiceItemMapper.java
|   +-- ServiceSpecMapper.java
+-- entity/
|   +-- ServiceCategory.java
+-- dto/
    +-- response/
        +-- CategoryTreeResponse.java
        +-- ItemListResponse.java
        +-- ItemDetailResponse.java
        +-- ServiceSpecResponse.java
        +-- CursorPageResponse.java

nursing-catalog-service/src/main/resources/
+-- mapper/
    +-- ServiceCategoryMapper.xml
    +-- ServiceItemMapper.xml
    +-- ServiceSpecMapper.xml

nursing-catalog-service/deploy/mysql/init/
+-- 02_catalog_schema.sql                     # + parent_id 列
```

---

## 四、API 请求/响应示例

### 4.1 分类树

```json
GET /api/v1/categories

Response:
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 101,
      "name": "康复护理",
      "icon": "/assets/default-category-icon.png",
      "children": [
        {
          "id": 111,
          "name": "术后康复",
          "icon": null,
          "children": []
        }
      ]
    }
  ]
}
```

### 4.2 项目列表（分页）

```json
GET /api/v1/items?categoryId=101&page=1&size=20

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 201,
        "name": "上门康复推拿",
        "coverImage": "/assets/default-service-cover.png",
        "categoryId": 101,
        "minPrice": 198.00,
        "status": 1
      }
    ],
    "total": 3,
    "page": 1,
    "size": 20
  }
}
```

### 4.3 项目详情（含规格价格）

```json
GET /api/v1/items/201

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 201,
    "name": "上门康复推拿",
    "description": "专业康复师上门推拿服务",
    "coverImage": "/assets/default-service-cover.png",
    "categoryId": 101,
    "specs": [
      { "id": 301, "name": "单次体验", "price": 198.00, "originalPrice": 298.00, "duration": 60 },
      { "id": 302, "name": "5次套餐", "price": 880.00, "originalPrice": 1490.00, "duration": 60 }
    ]
  }
}
```

### 4.4 搜索

```json
GET /api/v1/items/search?keyword=推拿&page=1&size=20

Response: 同项目列表分页结果
```

---

## 五、实施顺序

| 步骤 | 内容 | 文件数 |
|------|------|--------|
| 1. SQL Schema 补丁 | 更新 02_catalog_schema.sql，增加 `parent_id` + 种子数据 | 1 |
| 2. Entity + Config | 3 个实体类 + CatalogConfig | 4 |
| 3. Mapper XML + 接口 | 3 个 Mapper 接口 + 3 个 XML | 6 |
| 4. DTO | 4 个 VO + 2 个 Query DTO + 1 个 PriceDTO | 7 |
| 5. Service | CategoryService + ItemService | 2 |
| 6. Controller | CategoryController + ItemController | 2 |
| 7. 单测 | Service 层测试（可选，MVP 阶段可延后） | 2 |

**总新增**：约 22 个 Java 文件 + 3 个 XML + 1 个 SQL 补丁。

---

## 六、关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| ORM | 原生 MyBatis（XML Mapper） | 与项目骨架一致，保持轻量 |
| 分页方案 | 键集游标分页 | 以 `(sort_order, id)` 续页，避免深分页扫描和总数统计 |
| ID 生成 | SnowflakeIdWorker | 与 DDL（BIGINT NOT NULL）一致 |
| 分类树构建 | 一次性查询 + 内存递归 | 分类数据极少（< 50 条），无需多次查库 |
| `parent_id` 默认值 | 0 表示顶级 | 避免 `NULL` 带来的 SQL 三值逻辑问题 |
| 搜索实现 | LIKE `%keyword%` | MVP 阶段数据量小，后续可升级 ES |
| 排序规则 | 分类：`sort_order`；项目：`sort_order`；规格：`id` | 与 UI 展示需求一致 |
| 缓存 | 不做 Redis 缓存 | 接口免鉴权 + 数据量小 + 低频更新 |
| 列表/搜索共用响应对象 | ItemListResponse 同时作为列表和搜索返回 | 两个 API 结构完全一致 |
| 下单接口稳定性 | `items/{id}` 保持字段稳定 | order-service 通过 Feign 获取最新价格 |

---

## 七、已确认的决策清单（Plan Mode 逐项确认）

以下所有决策在 Plan 模式下逐项讨论确认：

| # | 决策项 | 确认结果 | 影响范围 |
|---|--------|---------|---------|
| 1 | `parent_id` 表示顶级的方式 | 0 表示顶级，不用 `NULL` | DDL + Mapper XML |
| 2 | 分类树层级 | 支持 2 级（一级 + 二级） | `Service.buildCategoryTree()` |
| 3 | 列表 `minPrice` 计算 | SQL 子查询（LEFT JOIN + MIN） | `ServiceItemMapper.xml` |
| 4 | 搜索匹配范围 | `keyword` 匹配 `name + description` | `ServiceItemMapper.xml` |
| 5 | 搜索支持 `categoryId` | 支持，可选参数 | Controller + Mapper |
| 6 | 种子数据二级分类 | 追加二级分类 INSERT 示例 | `02_catalog_schema.sql` |
| 7 | 同级分类排序 | `sort_order` 升序 | Mapper XML |
| 8 | 项目列表排序 | `sort_order` 升序 | Mapper XML |
| 9 | 空 `keyword` 搜索 | 返回 400 `BusinessException` | ItemService |
| 10 | `items/{id}` 查不到 | 返回 404 + `code=1005` | ItemService |

---

## 附录：更新计划

### 计划书更新历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-08 | 初稿 |
| v1.1 | 2026-07-08 | Plan 模式确认 10 项设计决策后定稿 |
