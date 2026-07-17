# catalog-service 接口实现逻辑

> 本文描述当前源码中的实际调用链、校验、数据过滤与异常行为，并与 `API.md` 的调用契约一一对应；其“逐接口调用链、执行步骤、边界”结构作为本仓库实现文档的统一排版标准。

## 1. 架构与通用规则

```text
HTTP Controller
  -> CategoryService / ItemService
    -> MyBatis Mapper
      -> catalog_db (service_category, service_item, service_spec)
  -> Result<T> / CursorPageResponse<T>
```

| 项目 | 实现规则 |
| --- | --- |
| 分类可见性 | `service_category.status = 1 AND is_deleted = 0` |
| 项目可见性 | `service_item.status = 1 AND is_deleted = 0` |
| 规格可见性 | `service_spec.status = 1 AND is_deleted = 0` |
| 统一成功包装 | `Result.success(data)`，业务码为 `0` |
| 业务异常 | `BusinessException` 由 `GlobalExceptionHandler` 转为对应 HTTP 状态与 `Result.error` |
| 查询型接口 | 3 个业务端点均不写数据库、缓存或消息表 |

所有可见性规则都由 Mapper SQL 执行，因此下架或逻辑删除的数据不会被任何业务接口返回。

## 2. `GET /api/v1/categories`

### 调用链

```text
CategoryController.listCategories
  -> CategoryService.buildCategoryTree
    -> ServiceCategoryMapper.selectListVisible
    -> 在内存中构建树
  -> Result.success(roots)
```

### 执行步骤

1. `ServiceCategoryMapper.selectListVisible()` 一次性读取全部可见分类，并按 `parent_id ASC, sort_order ASC, id ASC` 排序。
2. 遍历结果，将每个 `ServiceCategory` 转为独立的 `CategoryTreeResponse`，放入按查询顺序保持顺序的 `LinkedHashMap<Long, CategoryTreeResponse>`。
3. 再次遍历同一列表：`parentId` 为 `null` 或 `0` 的节点加入根列表；父节点存在时追加至父节点的 `children`；父节点不在可见集合中时，该节点被降级为根节点。
4. 返回根列表。由于子节点在原始排序后的遍历中加入，兄弟节点保持 SQL 查询顺序。

### 边界与复杂度

- 没有可见分类时返回空数组。
- 不做树深度限制；数据存在更多层级时仍可组装。
- 父节点隐藏或删除而子节点仍可见时，不丢弃子节点，而是将其作为根节点返回。
- 仅进行一次数据库查询与两次线性遍历，时间复杂度为 `O(n)`，额外内存为 `O(n)`。

## 3. `GET /api/v1/items`

### 控制器分流

```text
keyword == null
  -> ItemService.getItemPage(categoryId, cursor, size)
keyword != null
  -> ItemService.searchItems(keyword, categoryId, cursor, size)
```

这意味着 `?keyword=` 和 `?keyword=%20%20%20` 不会退化为普通列表，而会进入搜索分支后因空白关键字返回参数错误。

### 3.1 普通游标查询 `getItemPage`

1. `resolveVisibleCategoryIds(categoryId)`：未传 `categoryId` 时不加分类过滤；传入非正数时抛 HTTP 400 / `code=1000`；分类或其任一祖先不可见时抛 HTTP 404 / `code=1005`。有效分类按物化路径解析自身及全部可见后代分类 ID。
2. `normalizeSize(size)`：未传时使用 `20`；小于 `1` 或大于 `50` 时抛参数错误。
3. `decodeCursor(cursor)`：首次请求为空；后续请求将 URL 安全 Base64 游标解码为 `(sortOrder, itemId)`，格式或主键非法时抛 HTTP 400 / `code=1000`。
4. `ServiceItemMapper.selectPage(categoryIds, cursorSortOrder, cursorId, size + 1)` 查询。传入分类时追加 `si.category_id IN (...)`；游标存在时追加 `si.sort_order > cursorSortOrder OR (si.sort_order = cursorSortOrder AND si.id > cursorId)`；项目始终按 `si.sort_order ASC, si.id ASC` 排序。SQL 连接分类表取 `category_name`，并用子查询计算启用规格的 `MIN(price)`。
5. 第 `size + 1` 条仅用于判断下一页，不返回给调用方。存在该条时移除它，以返回列表最后一条的 `(sortOrder, itemId)` 生成 `nextCursor`，同时设置 `hasNext=true`。
6. `attachSpecs(items)` 仅提取返回列表的项目 ID，调用一次 `ServiceSpecMapper.selectByItemIds(ids)` 批量查询规格，按 `service_item_id` 分组并回填每个项目的 `specs`。空列表时直接返回，不发送空 `IN` 查询。

### 3.2 搜索游标查询 `searchItems`

除第 1 步先校验关键字外，其余游标、规格回填与返回行为同普通游标查询。

1. `StringUtils.hasText(keyword)` 为 `false` 时抛 HTTP 400 / `code=1000`，不访问数据库。
2. 对关键字执行 `trim()`，再校验可选分类、游标和分页大小。
3. `searchPage` 在可见项目条件和游标条件上附加：

```sql
(si.name LIKE CONCAT('%', :keyword, '%')
 OR si.description LIKE CONCAT('%', :keyword, '%'))
```

4. 当前实现直接使用 SQL `LIKE '%keyword%'`，没有全文索引、分词、相关性排序或缓存；结果仍按 `sort_order, id` 排序。

### 性能特性

- 传入分类的非空游标查询通常执行 4 次查询：分类存在性校验、后代分类 ID、项目页、批量规格；没有 `COUNT(*)` 查询。
- 未传分类时，非空游标查询通常为项目页和批量规格两次查询。
- SQL 通过 `(sort_order, id)` 键集条件直接从当前位置向后读取，避免深页 `OFFSET` 丢弃大量前置行；`size + 1` 仅额外读取一条记录判断下一页。
- 批量规格查询避免了列表项目数量导致的 N+1 查询；但 `minPrice` 仍由项目页 SQL 中的相关子查询计算。

## 4. `GET /api/v1/items/{id}`

### 调用链

```text
ItemController.getItemDetail
  -> ItemService.getItemDetail
    -> ServiceItemMapper.selectById
    -> ServiceSpecMapper.selectByItemId
    -> 封面图回填 images
  -> Result.success(detail)
```

### 执行步骤

1. 检查路径变量 `id`：`null` 或小于等于 `0` 时，抛 HTTP 400 / `code=1000`。
2. `selectById(id)` 查询一个已上架、未逻辑删除且所属分类及祖先可见的项目，并内连接分类表取分类名称。项目不存在、下架、逻辑删除或分类不可见时均查询不到结果。
3. 未查到项目时抛 HTTP 404 / `code=1005`；控制器不会返回成功空对象。
4. `selectByItemId(id)` 查询该项目全部启用、未逻辑删除的规格，按规格 ID 升序设置到 `detail.specs`。
5. `images` 由 VO 初始化为空列表。若 `coverImage` 有文本且 `images` 仍为空，将封面图加入 `images`，确保详情响应至少包含封面图；当前没有独立图片表查询。
6. 包装为成功 `Result<ItemDetailResponse>` 返回。

### 查询次数与边界

- 有效且存在的项目执行 2 次查询：项目详情和规格列表。
- 项目不存在时仅执行项目查询。
- 项目可以没有启用规格，接口仍成功返回，`specs` 为空数组。
- 分类表使用内连接；项目缺少有效分类或任一祖先分类不可见时，不会返回该项目。

## 5. 错误处理与外部依赖

### 错误转换

| 触发点 | 异常 | HTTP | 业务码 |
| --- | --- | ---: | ---: |
| 空白关键字、非法 ID、非法游标或分页大小 | `BusinessException(PARAM_ERROR)` | 400 | 1000 |
| 指定分类不可见、项目不可见或不存在 | `BusinessException(NOT_FOUND)` | 404 | 1005 |
| 其他未处理异常 | `Exception` | 500 | 1999 |

全局异常处理器会记录业务异常的 WARN 日志，未处理异常会记录 ERROR 日志。控制器方法本身没有 `@Valid` 请求对象；所有业务参数校验都在 `ItemService` 中完成。

### 网关与服务间调用

- Gateway 将分类和项目路径路由至 `lb://nursing-catalog-service`。
- Gateway 默认将这些路径视为公开资源，并会清除客户端伪造的用户身份头；catalog 服务自身不读取用户身份，也未配置方法级权限控制。
- 订单服务的 `CatalogServiceFeignClient` 复用详情 URL。该客户端声明的返回类型与实际控制器 VO 不同，接口字段调整应做调用方反序列化回归验证。

## 6. 源码对应表

| 职责 | 文件 |
| --- | --- |
| 分类 HTTP 入口 | `src/main/java/com/nursing/catalog/controller/CategoryController.java` |
| 项目 HTTP 入口 | `src/main/java/com/nursing/catalog/controller/ItemController.java` |
| 分类树组装 | `src/main/java/com/nursing/catalog/service/CategoryService.java` |
| 项目查询、搜索、参数校验 | `src/main/java/com/nursing/catalog/service/ItemService.java` |
| 分类、项目、规格 SQL | `src/main/resources/mapper/ServiceCategoryMapper.xml`、`ServiceItemMapper.xml`、`ServiceSpecMapper.xml` |
| 通用响应与异常 | `nursing-common/src/main/java/com/nursing/common/result/`、`nursing-common/src/main/java/com/nursing/common/exception/` |
