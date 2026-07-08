# 寮€鍙戣鍒掍功锛歯ursing-catalog-service

> **鏂囨。鐗堟湰**锛歷1.0
> **鏃ユ湡**锛?026-07-08
> **缂栧啓渚濇嵁**锛氱郴缁熸灦鏋勮璁?md v2.0銆丄PI 鎺ュ彛鏂囨。.md v1.0銆佹暟鎹簱 DDL锛?2_catalog_schema.sql锛?> **瀵瑰簲鍒嗘敮**锛歝odex/catalog-service锛堝熀浜?develop锛夆啋 寮€鍙戝畬鎴愬悗 squash merge 鍥?develop

---

## 涓€銆佹杩?
### 1.1 鏈嶅姟瀹氫綅

nursing-catalog-service锛堟湇鍔＄洰褰曟湇鍔★級鏄櫤鎱ф姢鐞嗗钩鍙扮殑鏈嶅姟娴忚鏍稿績锛岃礋璐ｇ鐞嗘湇鍔″垎绫汇€佹湇鍔￠」鐩強瑙勬牸浠锋牸鐨?CRUD 鍜屾煡璇€傛墍鏈夊澶?API **鍏嶉壌鏉?*锛圙ateway 鐩存帴鏀捐锛夛紝渚涘墠绔敤鎴峰湪鏈櫥褰曠姸鎬佷笅娴忚鏈嶅姟鍒嗙被鍜岄」鐩€?
### 1.2 鏈嶅姟淇℃伅

| 椤圭洰 | 鍊?|
|------|-----|
| Maven artifactId | nursing-catalog-service |
| 鏈嶅姟娉ㄥ唽鍚?| nursing-catalog-service |
| 绔彛 | 8082 |
| 鏁版嵁搴?| catalog_db |
| Snowflake worker-id | 2 |
| Snowflake datacenter-id | 1 |

### 1.3 褰撳墠杩涘睍

楠ㄦ灦宸插畬鎴愶細

```
nursing-catalog-service/
+-- pom.xml                          # 渚濊禆灏辩华锛歮ybatis, mysql, common, web, nacos, feign, redis
+-- src/main/
    +-- java/com/nursing/catalog/
    |   +-- CatalogApplication.java  # @SpringBootApplication + @EnableDiscoveryClient
    +-- resources/
        +-- application.yml          # 绔彛銆佹暟鎹簮銆丷edis銆丯acos銆丮yBatis 宸查厤缃?        +-- application-dev.yml      # 鏃ュ織 DEBUG 绾у埆
        +-- bootstrap.yml            # Nacos 閰嶇疆涓績
```

鍏叡妯″潡锛坣ursing-common锛夊凡鎻愪緵锛歊esult銆丳ageResult銆丅usinessException銆丟lobalExceptionHandler銆丼nowflakeIdWorker銆丄piCode銆?
---

## 浜屻€侀渶瑕佸畬鎴愮殑宸ヤ綔

### 2.1 SQL Schema 鏇存柊锛圖DL 琛ヤ竵锛?
褰撳墠鐨?`02_catalog_schema.sql` 涓?`service_category` 琛?*缂哄皯 `parent_id` 瀛楁**锛屾棤娉曟敮鎸佸绾ф爲褰㈢粨鏋勩€傞渶瑕佸湪 DDL 涓ˉ涓婅瀛楁锛屽苟璋冩暣绉嶅瓙鏁版嵁銆?
**鍙樻洿鍐呭锛?*

1. `service_category` 琛ㄦ柊澧炲瓧娈?`parent_id BIGINT DEFAULT 0 COMMENT '鐖跺垎绫籌D(0琛ㄧず椤剁骇)'`
2. `service_category` 鏂板绱㈠紩 `INDEX idx_parent (parent_id)`
3. 绉嶅瓙鏁版嵁琛ュ厖 `parent_id` 鍊硷紙褰撳墠 4 涓竴绾у垎绫荤殑 parent_id = 0锛夛紝骞跺彲杩藉姞浜岀骇鍒嗙被绀轰緥

> **璁捐绾﹀畾**锛氫娇鐢?`parent_id = 0` 琛ㄧず椤剁骇鍒嗙被銆傛渶澶氭敮鎸?3 绾э紝鐢变笟鍔″眰閫掑綊鏋勫缓鏍戯紝涓嶅仛娣卞害闄愬埗銆?
### 2.2 瀹炰綋灞傦紙entity锛?
姣忎釜瀹炰綋瀵瑰簲涓€寮犱笟鍔¤〃锛屼娇鐢?SnowflakeIdWorker 鐢熸垚 ID锛堟墍鏈?DDL 鏄惧紡鎸囧畾 `BIGINT NOT NULL`锛屾棤 AUTO_INCREMENT锛夈€?
| 绫诲悕 | 瀵瑰簲琛?| 鍏抽敭瀛楁 |
|------|--------|---------|
| ServiceCategory | service_category | id, parentId, name, icon, sortOrder, status, isDeleted, createTime, updateTime |
| ServiceItem | service_item | id, categoryId, name, description, coverImage, status, sortOrder, isDeleted, createTime, updateTime |
| ServiceSpec | service_spec | id, serviceItemId, name, price, originalPrice, duration, status, isDeleted, createTime, updateTime |

鎵€鏈夊疄浣撳姞 @Data锛圠ombok锛夈€?
### 2.3 DTO 灞傦紙dto锛?
| 绫诲悕 | 鐢ㄩ€?| 璇存槑 |
|------|------|------|
| CategoryTreeVO | 鍒嗙被鏍戣妭鐐?| id, name, icon, children锛堥€掑綊 List锛?|
| ItemPageVO | 椤圭洰鍒楄〃椤?| id, name, coverImage, categoryId, minPrice, status |
| ItemDetailVO | 椤圭洰璇︽儏 | id, name, description, coverImage, categoryId, specs |
| SpecVO | 瑙勬牸淇℃伅 | id, name, price, originalPrice, duration |
| ItemQueryDTO | 椤圭洰鏌ヨ鍙傛暟 | categoryId锛堝彲閫夛級, page, size |
| ItemSearchDTO | 鎼滅储鍙傛暟 | keyword锛堝繀濉級, categoryId锛堝彲閫夛級, page, size |

### 2.4 Mapper 灞傦紙MyBatis XML锛?
3 涓?Mapper 鎺ュ彛 + 3 涓?XML 鏂囦欢銆?
**ServiceCategoryMapper锛?*

| 鏂规硶 | SQL 璇存槑 |
|------|---------|
| selectListVisible() | 鏌ユ墍鏈?status=1 AND is_deleted=0 鐨勫垎绫伙紝鎸?sort_order 鎺掑簭 |
| selectByParentId(Long parentId) | 鏍规嵁 parent_id 鏌ュ瓙鍒嗙被 |

**ServiceItemMapper锛?*

| 鏂规硶 | SQL 璇存槑 |
|------|---------|
| selectPage(categoryId, offset, limit) | 鍒嗛〉鏌ラ」鐩紝鏀寔鎸?categoryId 绛涢€?|
| count(categoryId) | 璁℃暟 |
| selectById(Long id) | 鎸?ID 鏌ラ」鐩鎯?|
| searchPage(keyword, categoryId, offset, limit) | 鍒嗛〉鎼滅储锛宬eyword 妯＄硦鍖归厤 name + description |
| searchCount(keyword, categoryId) | 鎼滅储璁℃暟 |

**ServiceSpecMapper锛?*

| 鏂规硶 | SQL 璇存槑 |
|------|---------|
| selectByItemId(Long serviceItemId) | 鏌ユ煇涓湇鍔＄殑鎵€鏈夎鏍硷紙status=1, is_deleted=0锛?|

> **鍒嗛〉鏂瑰紡**锛氫娇鐢ㄥ師鐢?MyBatis + LIMIT #{offset}, #{limit} 鎵嬪姩鍒嗛〉銆備笉寮曞叆 PageHelper銆?
### 2.5 Service 灞?
**CategoryService**

buildCategoryTree(): List<CategoryTreeVO>
- 鏌ヨ鎵€鏈?status=1 涓?is_deleted=0 鐨勫垎绫?- 鎸?parent_id 鏋勫缓鍐呭瓨鏍?- 杩斿洖椤跺眰鏍硅妭鐐瑰垪琛?
**ItemService**

getItemPage(categoryId, page, size): PageResult<ItemPageVO>
- 鏍￠獙鍒嗛〉鍙傛暟
- 璋冪敤 mapper 鏌ヨ + 璁℃暟
- 缁勮 PageResult
- categoryId 涓?null 鏃惰繑鍥炴墍鏈夐」鐩?
getItemDetail(id): ItemDetailVO
- 鏌?service_item + 鏌?service_spec
- 缁勮涓?ItemDetailVO
- id 鏃犳晥 -> throw BusinessException(NOT_FOUND, "鏈嶅姟椤圭洰涓嶅瓨鍦?)

searchItems(keyword, categoryId, page, size): PageResult<ItemPageVO>
- 鏍￠獙 keyword 闈炵┖
- mapper 妯＄硦鎼滅储锛堝尮閰?name 鍜?description锛?- 鍒嗛〉杩斿洖

### 2.6 Controller 灞?
**CategoryController** -- 璺緞鍓嶇紑 /api/v1/categories

| 鏂规硶 | 璺緞 | 鍙傛暟 | 鍝嶅簲 |
|------|------|------|------|
| GET | /api/v1/categories | 鏃?| Result<List<CategoryTreeVO>> |

**ItemController** -- 绫荤骇 @RequestMapping("/api/v1/items")

| 鏂规硶 | 璺緞 | 鍙傛暟 | 鍝嶅簲 |
|------|------|------|------|
| GET | (鍒楄〃) | categoryId(opt), page(def=1), size(def=20) | Result<PageResult<ItemPageVO>> |
| GET | /{id} | id(path) | Result<ItemDetailVO> |
| GET | / | 列表/搜索共存：keyword 为空时返回列表，非空时按关键词搜索 name+description | Result<PageResult<ItemPageVO>> |

**璺緞璇存槑**锛?items 鍜?/items/search 鍦?Spring 涓笉浼氬啿绐侊紝/search 鏄簿纭矾寰勪紭鍏堢骇鏇撮珮銆?
### 2.7 Feign 瀹㈡埛绔绾︼紙渚?order-service 璋冪敤锛?
order-service 鍦ㄤ笅鍗曟椂閫氳繃 Feign 璋冪敤 `GET /api/v1/items/{id}` 鑾峰彇鏈€鏂颁环鏍笺€?
鍏抽敭瑕佹眰锛?- 璇ユ帴鍙ｇ殑 JSON 缁撴瀯涓€缁忕‘瀹氬嵆淇濇寔鍚戝悗鍏煎
- price 瀛楁鍚嶄繚鎸?`price`锛岀被鍨嬩负 `BigDecimal`
- 涓嶉殢鎰忓垹闄ょ幇鏈夊瓧娈?
鎻愪緵 `dto/ItemPriceDTO` 鍙傝€冪粨鏋勶紙瀹為檯 Feign Client 瀹氫箟鍦?order-service 渚э級銆?
### 2.8 Bean 閰嶇疆

鍦?`config/CatalogConfig.java` 涓厤缃?SnowflakeIdWorker Bean锛屼娇鐢?`application.yml` 涓?`nursing.snowflake` 閰嶇疆銆?
---

## 涓夈€佹渶缁堟枃浠剁粨鏋?
```
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
|   +-- ServiceItem.java
|   +-- ServiceSpec.java
+-- dto/
    +-- vo/
    |   +-- CategoryTreeVO.java
    |   +-- ItemPageVO.java
    |   +-- ItemDetailVO.java
    |   +-- SpecVO.java
    |   +-- ItemPriceDTO.java
    +-- query/
        +-- ItemQueryDTO.java
        +-- ItemSearchDTO.java

nursing-catalog-service/src/main/resources/
+-- mapper/
    +-- ServiceCategoryMapper.xml
    +-- ServiceItemMapper.xml
    +-- ServiceSpecMapper.xml

docker-compose/mysql/init/
+-- 02_catalog_schema.sql                      # + parent_id 鍒?```

---

## 鍥涖€丄PI 璇锋眰/鍝嶅簲绀轰緥

### 4.1 鍒嗙被鏍?
```
GET /api/v1/categories

Response:
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 101,
      "name": "搴峰鎶ょ悊",
      "icon": "https://via.placeholder.com/48",
      "children": [
        {
          "id": 111,
          "name": "鏈悗搴峰",
          "icon": null,
          "children": []
        }
      ]
    }
  ]
}
```

### 4.2 椤圭洰鍒楄〃锛堝垎椤碉級

```
GET /api/v1/items?categoryId=101&page=1&size=20

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 201,
        "name": "涓婇棬搴峰鎺ㄦ嬁",
        "coverImage": "https://via.placeholder.com/200",
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

### 4.3 椤圭洰璇︽儏锛堝惈瑙勬牸浠锋牸锛?
```
GET /api/v1/items/201

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 201,
    "name": "涓婇棬搴峰鎺ㄦ嬁",
    "description": "涓撲笟搴峰甯堜笂闂ㄦ帹鎷挎湇鍔?,
    "coverImage": "https://via.placeholder.com/600",
    "categoryId": 101,
    "specs": [
      { "id": 301, "name": "鍗曟浣撻獙", "price": 198.00, "originalPrice": 298.00, "duration": 60 },
      { "id": 302, "name": "5娆″椁?, "price": 880.00, "originalPrice": 1490.00, "duration": 60 }
    ]
  }
}
```

### 4.4 鎼滅储

```
GET /api/v1/items?keyword=鎺ㄦ嬁&page=1&size=20

Response: 鍚岄」鐩垪琛ㄥ垎椤电粨鏋?```

---

## 浜斻€佸疄鏂介『搴?
| 姝ラ | 鍐呭 | 鏂囦欢鏁?|
|------|------|--------|
| 1. SQL Schema 琛ヤ竵 | 鏇存柊 02_catalog_schema.sql 鍔?parent_id + 绉嶅瓙鏁版嵁 | 1 |
| 2. Entity + Config | 3 涓疄浣撶被 + CatalogConfig | 4 |
| 3. Mapper XML + 鎺ュ彛 | 3 涓?Mapper 鎺ュ彛 + 3 涓?XML | 6 |
| 4. DTO | 4 涓?VO + 2 涓?Query DTO + 1 涓?PriceDTO | 7 |
| 5. Service | CategoryService + ItemService | 2 |
| 6. Controller | CategoryController + ItemController | 2 |
| 7. 鍗曟祴 | Service 灞傛祴璇曪紙鍙€夛紝MVP 闃舵鍙欢鍚庯級 | 2 |

**鎬绘柊澧?*锛氱害 22 涓?Java 鏂囦欢 + 3 涓?XML + 1 涓?SQL 琛ヤ竵銆?
---

## 鍏€佸叧閿璁″喅绛?
| 鍐崇瓥 | 閫夋嫨 | 鐞嗙敱 |
|------|------|------|
| ORM | 鍘熺敓 MyBatis锛圶ML Mapper锛?| 涓庨」鐩鏋朵竴鑷达紝淇濇寔杞婚噺 |
| 鍒嗛〉鏂规 | 鎵嬪姩 LIMIT offset/limit | 涓嶅紩鍏?PageHelper锛宑atalog 鏁版嵁閲忔瀬灏?|
| ID 鐢熸垚 | SnowflakeIdWorker | 涓?DDL锛圔IGINT NOT NULL锛変竴鑷?|
| 鍒嗙被鏍戞瀯寤?| 涓€娆℃€ф煡璇?+ 鍐呭瓨閫掑綊 | 鍒嗙被鏁版嵁鏋佸皯锛? 50 鏉★級锛屾棤闇€澶氭鏌ュ簱 |
| parent_id 榛樿鍊?| 0 琛ㄧず椤剁骇 | 閬垮厤 NULL 甯︽潵鐨?SQL 涓夊€奸€昏緫闂 |
| 鎼滅储瀹炵幇 | LIKE %keyword% | MVP 闃舵鏁版嵁閲忓皬锛屽悗缁彲鍗囩骇 ES |
| 鎺掑簭瑙勫垯 | 鍒嗙被锛歴ort_order锛涢」鐩細sort_order锛涜鏍硷細id | 涓?UI 灞曠ず闇€姹備竴鑷?|
| 缂撳瓨 | 涓嶅仛 Redis 缂撳瓨 | 鎺ュ彛鍏嶉壌鏉?+ 鏁版嵁閲忓皬 + 浣庨鏇存柊 |
| 鍒楄〃/鎼滅储鍏辩敤 VO | ItemPageVO 鍚屾椂浣滀负鍒楄〃鍜屾悳绱㈣繑鍥?| 涓や釜 API 缁撴瀯瀹屽叏涓€鑷?|
| 涓嬪崟鎺ュ彛绋冲畾鎬?| items/{id} 淇濇寔瀛楁绋冲畾 | order-service 閫氳繃 Feign 鑾峰彇鏈€鏂颁环鏍?|
---

## 涓冦€佸凡纭鐨勫喅绛栨竻鍗曪紙Plan Mode 閫愰」纭锛?
浠ヤ笅鎵€鏈夊喅绛栧湪 Plan 妯″紡涓嬮€愰」璁ㄨ纭锛?
| # | 鍐崇瓥椤?| 纭缁撴灉 | 褰卞搷鑼冨洿 |
|---|--------|---------|---------|
| 1 | parent_id 琛ㄧず椤剁骇鐨勬柟寮?| 0 琛ㄧず椤剁骇锛屼笉鐢?NULL | DDL + Mapper XML |
| 2 | 鍒嗙被鏍戝眰绾?| 鏀寔 2 绾э紙涓€绾?+ 浜岀骇锛?| Service.buildCategoryTree() |
| 3 | 鍒楄〃 minPrice 璁＄畻 | SQL 瀛愭煡璇紙LEFT JOIN + MIN锛?| ServiceItemMapper.xml |
| 4 | 鎼滅储鍖归厤鑼冨洿 | keyword 鍖归厤 name + description | ServiceItemMapper.xml |
| 5 | 鎼滅储鏀寔 categoryId | 鏀寔锛屽彲閫夊弬鏁?| Controller + Mapper |
| 6 | 绉嶅瓙鏁版嵁浜岀骇鍒嗙被 | 杩藉姞浜岀骇鍒嗙被 INSERT 绀轰緥 | 02_catalog_schema.sql |
| 7 | 鍚岀骇鍒嗙被鎺掑簭 | sort_order 鍗囧簭 | Mapper XML |
| 8 | 椤圭洰鍒楄〃鎺掑簭 | sort_order 鍗囧簭 | Mapper XML |
| 9 | 绌?keyword 鎼滅储 | 杩斿洖 400 BusinessException | ItemService |
| 10 | items/{id} 鏌ヤ笉鍒?| 杩斿洖 404 + code=1005 | ItemService |

---

## 闄勫綍锛氭洿鏂拌鍒?
### 璁″垝涔︽洿鏂板巻鍙?
| 鐗堟湰 | 鏃ユ湡 | 鍙樻洿 |
|------|------|------|
| v1.0 | 2026-07-08 | 鍒濈 |
| v1.1 | 2026-07-08 | Plan 妯″紡纭 10 椤硅璁″喅绛栧悗瀹氱 |
