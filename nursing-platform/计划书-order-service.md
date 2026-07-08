# nursing-order-service 寮€鍙戣鍒掍功

> 鐗堟湰锛歷1.0  
> 鏃ユ湡锛?026-07-08  
> 鐘舵€侊細宸茶瘎瀹? 
> 鍒嗘敮锛歝odex/order-service

---

## 涓€銆佹杩?
nursing-order-service 鏄櫤鎱ф姢鐞嗙Щ鍔ㄦ姢鐞嗗钩鍙扮殑璁㈠崟鏍稿績鏈嶅姟锛岃礋璐ｄ笅鍗曢绾︺€佽鍗曠敓鍛藉懆鏈熺鐞嗐€佸湴鍧€绠＄悊銆佹敮浠樺洖璋冨鐞嗙瓑涓氬姟銆傛湇鍔℃敞鍐屽悕 
ursing-order-service锛岀鍙?**8083**銆?
### 1.1 鏈嶅姟鐨勬牳蹇冭亴璐?
| 妯″潡 | 鑱岃矗 |
|------|------|
| **璁㈠崟** | 鍒涘缓璁㈠崟锛堝箓绛夛級銆佽鍗曞垪琛?璇︽儏銆佸彇娑堣鍗曪紙鍚€€娆炬祦绋嬶級銆佺姸鎬佹満娴佽浆 |
| **鏀粯** | 鍙戣捣鏀粯锛堟ā鎷熸敮浠樺疂锛夈€佹敮浠樺疂鏀粯鍥炶皟锛堥獙绛?骞傜瓑+Outbox 鍙戜簨浠讹級 |
| **鍦板潃** | 鐢ㄦ埛鏀惰揣鍦板潃 CRUD銆佽缃粯璁ゅ湴鍧€ |
| **骞傜瓑** | prepay-token 鍙戞斁 + Idempotent-Key 楠岃瘉 |
| **娑堟伅** | 鏀粯鎴愬姛鍚庨€氳繃 Outbox 妯″紡鍙戦€?ORDER_PAID 浜嬩欢鍒?Kafka |

### 1.2 鏈嶅姟渚濊禆

| 鏂瑰悜 | 鏈嶅姟 | 鏂瑰紡 | 鍦烘櫙 |
|------|------|------|------|
| 璋冪敤 | catalog-service | Feign 鍚屾 | 涓嬪崟鏃惰皟鐢?GET /api/v1/items/{id} 鑾峰彇瀹炴椂浠锋牸 |
| 琚皟 | feedback-service锛堟湭鏉ワ級 | Kafka 寮傛 | 璁㈠崟瀹屾垚 鈫?鏍囪鍙瘎浠?|
| 鍙戦€?| Kafka topic order_events | Outbox 妯″紡 | 鏀粯鎴愬姛鍚庡彂閫?ORDER_PAID 浜嬩欢 |

---

## 浜屻€侀」鐩粨鏋?
### 2.1 瀹屾暣鍖呯粨鏋?
`
nursing-order-service/
鈹溾攢鈹€ pom.xml
鈹斺攢鈹€ src/main/java/com/nursing/order/
    鈹溾攢鈹€ OrderApplication.java                        # 宸插瓨鍦?    鈹溾攢鈹€ controller/
    鈹?  鈹溾攢鈹€ OrderController.java                     # 璁㈠崟鐩稿叧 7 涓帴鍙?    鈹?  鈹斺攢鈹€ AddressController.java                   # 鍦板潃 5 涓帴鍙?    鈹溾攢鈹€ service/
    鈹?  鈹溾攢鈹€ IOrderService.java                       # 璁㈠崟鎺ュ彛
    鈹?  鈹溾攢鈹€ OrderServiceImpl.java                    # 璁㈠崟瀹炵幇
    鈹?  鈹溾攢鈹€ IAddressService.java                     # 鍦板潃鎺ュ彛
    鈹?  鈹溾攢鈹€ AddressServiceImpl.java                  # 鍦板潃瀹炵幇
    鈹?  鈹溾攢鈹€ PaymentService.java                      # 鏀粯涓庡洖璋冮€昏緫
    鈹?  鈹斺攢鈹€ IdempotentService.java                   # 骞傜瓑浠ょ墝鍙戞斁涓庢牎楠?    鈹溾攢鈹€ repository/                                  # MyBatis Mapper
    鈹?  鈹溾攢鈹€ OrderHeaderMapper.java
    鈹?  鈹溾攢鈹€ PaymentRecordMapper.java
    鈹?  鈹溾攢鈹€ UserAddressMapper.java
    鈹?  鈹溾攢鈹€ OrderOperationLogMapper.java
    鈹?  鈹溾攢鈹€ OrderSequenceMapper.java
    鈹?  鈹溾攢鈹€ IdempotentRecordMapper.java
    鈹?  鈹斺攢鈹€ EventMessageMapper.java
    鈹溾攢鈹€ entity/
    鈹?  鈹溾攢鈹€ OrderHeader.java
    鈹?  鈹溾攢鈹€ PaymentRecord.java
    鈹?  鈹溾攢鈹€ UserAddress.java
    鈹?  鈹溾攢鈹€ OrderOperationLog.java
    鈹?  鈹溾攢鈹€ OrderSequence.java
    鈹?  鈹溾攢鈹€ IdempotentRecord.java
    鈹?  鈹斺攢鈹€ EventMessage.java
    鈹溾攢鈹€ dto/
    鈹?  鈹溾攢鈹€ request/
    鈹?  鈹?  鈹溾攢鈹€ PrepayTokenResponse.java
    鈹?  鈹?  鈹溾攢鈹€ OrderCreateRequest.java
    鈹?  鈹?  鈹溾攢鈹€ OrderPageQuery.java
    鈹?  鈹?  鈹溾攢鈹€ CancelOrderRequest.java
    鈹?  鈹?  鈹溾攢鈹€ PayRequest.java
    鈹?  鈹?  鈹斺攢鈹€ AddressRequest.java
    鈹?  鈹斺攢鈹€ response/
    鈹?      鈹溾攢鈹€ OrderDetailResponse.java
    鈹?      鈹溾攢鈹€ OrderListResponse.java
    鈹?      鈹溾攢鈹€ PayResponse.java
    鈹?      鈹溾攢鈹€ AddressResponse.java
    鈹?      鈹斺攢鈹€ CancelResponse.java
    鈹溾攢鈹€ event/
    鈹?  鈹溾攢鈹€ OrderEventPublisher.java                 # Outbox 鍐欏叆 + 瀹氭椂鎶曢€?    鈹?  鈹斺攢鈹€ OrderPaidEvent.java                      # ORDER_PAID 浜嬩欢浣?POJO
    鈹溾攢鈹€ config/
    鈹?  鈹溾攢鈹€ SnowflakeConfig.java                     # SnowflakeIdWorker Bean
    鈹?  鈹斺攢鈹€ WebMvcConfig.java                        # 鎷︽埅鍣ㄩ厤缃紙鑾峰彇 user_id锛?    鈹斺攢鈹€ resources/
        鈹溾攢鈹€ application.yml                          # 宸插瓨鍦?        鈹溾攢鈹€ application-dev.yml                      # 宸插瓨鍦?        鈹斺攢鈹€ mapper/
            鈹溾攢鈹€ OrderHeaderMapper.xml
            鈹溾攢鈹€ PaymentRecordMapper.xml
            鈹斺攢鈹€ UserAddressMapper.xml
`

### 2.2 闇€瑕佸湪 common 妯″潡琛ュ厖鐨勬枃浠?
`
nursing-common/src/main/java/com/nursing/common/feign/
    鈹斺攢鈹€ CatalogServiceFeignClient.java             # Feign 鎺ュ彛
nursing-common/src/main/java/com/nursing/common/dto/
    鈹溾攢鈹€ ServiceItemDTO.java
    鈹斺攢鈹€ ServiceSpecDTO.java
`

---

## 涓夈€佹暟鎹簱瀹炰綋涓庢槧灏?
### 3.1 琛ㄦ竻鍗?
| 琛ㄥ悕 | Entity | Mapper | 璇存槑 |
|------|--------|--------|------|
| order_header | OrderHeader | OrderHeaderMapper | 璁㈠崟涓昏〃锛屽惈鏈嶅姟蹇収 + 鍦板潃蹇収 + 涔愯閿?|
| payment_record | PaymentRecord | PaymentRecordMapper | 鏀粯璁板綍锛寀k_order_pay_type 闃查噸 |
| user_address | UserAddress | UserAddressMapper | 鐢ㄦ埛鍦板潃 |
| order_operation_log | OrderOperationLog | OrderOperationLogMapper | 鐘舵€佸彉鏇村璁℃棩蹇?|
| order_sequence | OrderSequence | OrderSequenceMapper | 鐢熸垚 order_no 鐨勫簭鍒楄〃 |
| idempotent_record | IdempotentRecord | IdempotentRecordMapper | 骞傜瓑璁板綍 |
| event_message | EventMessage | EventMessageMapper | Outbox 鏈湴娑堟伅琛?|

### 3.2 鍏抽敭绱㈠紩涓庣害鏉?
| 琛?| 绾︽潫鍚?| 绫诲瀷 | 浣滅敤 |
|----|--------|------|------|
| order_header | uk_order_no | UNIQUE | 璁㈠崟鍙峰敮涓€ |
| order_header | uk_user_service_slot | UNIQUE(user_id, service_item_id, service_date, service_time_slot) | L3 骞傜瓑鍏滃簳 |
| order_header | idx_user_id | INDEX | 鐢ㄦ埛璁㈠崟鏌ヨ |
| order_header | idx_status | INDEX | 鐘舵€佺瓫閫?|
| payment_record | uk_order_pay_type | UNIQUE(order_no, pay_type) | 闃叉鏀粯鍥炶皟閲嶅鍏ヨ处锛圠4锛?|
| idempotent_record | uk_key | UNIQUE | 骞傜瓑閿敮涓€锛圠1/L2锛?|
| event_message | uk_event | UNIQUE(topic, event_key) | Outbox 骞傜瓑 |

---

## 鍥涖€佸箓绛夋€ц璁★紙鍥涘眰闃插尽锛?
### L1 鈥?prepay_token 鏈嶅姟绔彂鏀撅紙涓嬪崟鍏ュ彛锛?
`
POST /api/v1/orders/prepay-token 鈫?鐢熸垚 UUID锛孖NSERT idempotent_record(status=0, expire=30min)
`

- 鍓嶇鏀跺埌鍚庡瓨鍌ㄥ埌鏈湴锛坲ni.setStorageSync锛?- 鎻愪氦璁㈠崟鏃舵斁鍏?Idempotent-Key Header

### L2 鈥?Idempotent-Key Header 鏍￠獙锛堜笅鍗曟帴鍙ｏ級

`
Header: Idempotent-Key = <prepay_token>
`

- 鍚庣鍙?Key 鈫?SELECT status FROM idempotent_record
  - 鏃犺褰?鈫?鎷掔粷锛?000: 鏃犳晥浠ょ墝锛?  - status=1 宸插畬鎴?鈫?鐩存帴杩斿洖宸叉湁 biz_id锛堝箓绛夎繑鍥烇紝涓嶆敼鐘舵€侊級
  - status=0 澶勭悊涓?鈫?鎵ц涓氬姟 鈫?UPDATE status=1, biz_id=orderId

### L3 鈥?uk_user_service_slot 鍞竴绾︽潫锛堝厹搴曪級

`
UNIQUE KEY uk_user_service_slot (user_id, service_item_id, service_date, service_time_slot)
`

- 鍦?order_header 琛ュ厖姝ゅ敮涓€绱㈠紩
- 鐞嗚涓?L1+L2 宸查槻姝㈤噸澶嶄笅鍗曪紝姝ょ害鏉熶綔涓烘暟鎹簱灞傞潰鐨勬渶鍚庝竴閬撻槻绾?
### L4 鈥?uk_order_pay_type 鍞竴绾︽潫锛堟敮浠樺洖璋冮槻閲嶏級

`
UNIQUE KEY uk_order_pay_type (order_no, pay_type)
`

- 鏀粯瀹濋噸澶嶉€氱煡 鈫?INSERT 鍛戒腑鍞竴绾︽潫 鈫?鎹曡幏寮傚父 鈫?杩斿洖 success

---

## 浜斻€佽鍗曠姸鎬佹満

`
[鍒涘缓璁㈠崟] 鈫?0:寰呮敮浠?鈹€鈹€鏀粯鎴愬姛鈹€鈹€鈫?1:寰呮湇鍔?鈹€鈹€鏈嶅姟瀹屾垚鈹€鈹€鈫?2:宸插畬鎴?                  鈹?                     鈹?                  鈹?鐢ㄦ埛鍙栨秷              鈹?鏈嶅姟鍓嶅彇娑?                  鈫?                     鈫?              3:宸插彇娑?              3:宸插彇娑堬紙闇€閫€娆撅級
                                             鈹?                                       2:宸插畬鎴?鈹€鈹€鐢宠閫€娆锯攢鈹€鈫?4:閫€娆句腑
                                                                鈹?                                                      鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹粹攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?                                                      鈫?                  鈫?                                                5:宸查€€娆?            2:宸插畬鎴愶紙瀹℃牳鎷掔粷锛?`

### 鐘舵€佸€艰〃

| 鍊?| 鍚箟 | 鍙搷浣?| 璇存槑 |
|----|------|--------|------|
| 0 | 寰呮敮浠?| 鍙栨秷銆佹敮浠?| 瓒呮椂 30 鍒嗛挓鑷姩鍙栨秷锛堝畾鏃朵换鍔★級 |
| 1 | 寰呮湇鍔?| 鍙栨秷锛堥€€娆撅級 | 鏈嶅姟鍓嶅彇娑堥渶鍏ㄩ閫€娆?|
| 2 | 宸插畬鎴?| 鐢宠閫€娆俱€佽瘎浠?| 鏈嶅姟瀹屾垚鍚庝笉鍙洿鎺ュ彇娑?|
| 3 | 宸插彇娑?| 鈥?| 缁堟€侊紝閫€娆剧姸鎬佽 payment_record |
| 4 | 閫€娆句腑 | 鈥?| 瀹㈡湇瀹℃牳涓?|
| 5 | 宸查€€娆?| 鈥?| 缁堟€?|

### 鍏抽敭瑙勫垯

- 姣忎釜鐘舵€佸彉鏇撮兘璁板綍 order_operation_log锛坅ction, from_status, to_status锛?- 鍙栨秷鎿嶄綔锛氬緟鏀粯鐩存帴鍙栨秷锛涘緟鏈嶅姟鍙栨秷鍒欒褰?refund 娴佺▼
- order_header.version 涔愯閿侀槻姝㈠苟鍙戠姸鎬佽鐩?
---

## 鍏€丄PI 瀹炵幇鏄庣粏

### 6.1 璁㈠崟鎺ュ彛锛圤rderController锛?
| 搴忓彿 | 鏂规硶 | 璺緞 | 閴存潈 | 璇存槑 |
|------|------|------|------|------|
| 1 | POST | /api/v1/orders/prepay-token | JWT | 鍙戞斁骞傜瓑浠ょ墝锛岃繑鍥?{prepayToken, expireTime} |
| 2 | POST | /api/v1/orders | JWT | 鍒涘缓璁㈠崟锛圚eader Idempotent-Key锛夛紝闃查噸 |
| 3 | GET | /api/v1/orders | JWT | 璁㈠崟鍒楄〃锛?status=&page=&size=锛夛紝鍒嗛〉 |
| 4 | GET | /api/v1/orders/{id} | JWT | 璁㈠崟璇︽儏锛堝惈鏈嶅姟蹇収銆佸湴鍧€蹇収銆佹敮浠樼姸鎬侊級 |
| 5 | POST | /api/v1/orders/{id}/cancel | JWT | 鍙栨秷璁㈠崟锛堝箓绛夛細鍓嶇浼?Idempotent-Key锛?|
| 6 | POST | /api/v1/orders/{id}/pay | JWT | 鍙戣捣鏀粯锛堣繑鍥炴敮浠樺弬鏁?妯℃嫙鐩存帴鎴愬姛锛?|
| 7 | POST | /api/v1/orders/pay/callback | 鍏岼WT路楠岀 | 鏀粯瀹濆紓姝ュ洖璋冿紝form-urlencoded 鏍煎紡 |

### 6.2 鍦板潃鎺ュ彛锛圓ddressController锛?
| 搴忓彿 | 鏂规硶 | 璺緞 | 閴存潈 | 璇存槑 |
|------|------|------|------|------|
| 1 | GET | /api/v1/addresses | JWT | 鍦板潃鍒楄〃锛坕s_default 浼樺厛锛宑reate_time 鍊掑簭锛?|
| 2 | POST | /api/v1/addresses | JWT | 鏂板鍦板潃锛坕sDefault=1 鏃惰嚜鍔ㄦ竻闄ゅ叾浠栭粯璁わ級 |
| 3 | PATCH | /api/v1/addresses/{id} | JWT | 缂栬緫鍦板潃锛堥儴鍒嗘洿鏂帮級 |
| 4 | DELETE | /api/v1/addresses/{id} | JWT | 閫昏緫鍒犻櫎锛坕s_deleted=1锛?|
| 5 | PUT | /api/v1/addresses/{id}/default | JWT | 璁剧疆榛樿鍦板潃锛堝叾浠栧湴鍧€ is_default 缃?0锛?|

### 6.3 閿欒鐮侊紙order-service 3000-3999锛?
| HTTP | code | message | 瑙﹀彂鏉′欢 |
|------|------|---------|----------|
| 400 | 3000 | 骞傜瓑浠ょ墝鏃犳晥鎴栧凡杩囨湡 | prepay_token 涓嶅瓨鍦ㄦ垨宸茶繃鏈?|
| 409 | 3001 | 閲嶅涓嬪崟锛堝箓绛夎繑鍥炲凡鏈夎鍗曪級 | 鍚?Key 宸插鐞嗭紝鐩存帴杩斿洖 orderId |
| 422 | 3002 | 鏈嶅姟椤圭洰涓嶅瓨鍦?| 璋冪敤 catalog-service 杩斿洖 null |
| 422 | 3003 | 鏈嶅姟瑙勬牸宸蹭笅鏋?| 瑙勬牸 status=0 |
| 422 | 3005 | 璇ユ椂娈靛凡琚绾?| 鍛戒腑 uk_user_service_slot |
| 404 | 3007 | 璁㈠崟涓嶅瓨鍦?| 璁㈠崟 ID 鏃犳晥 |
| 403 | 3008 | 鏃犳潈鎿嶄綔姝よ鍗?| 璁㈠崟涓嶅睘浜庡綋鍓嶇敤鎴?|
| 422 | 3009 | 褰撳墠鐘舵€佷笉鍙彇娑?| 璁㈠崟闈炲緟鏀粯/寰呮湇鍔?|
| 422 | 3010 | 褰撳墠鐘舵€佷笉鍙敮浠?| 璁㈠崟闈炲緟鏀粯 |
| 422 | 3011 | 鏀粯宸插鐞嗭紝璇峰嬁閲嶅鎻愪氦 | 鍛戒腑 uk_order_pay_type |
| 400 | 3012 | 鏀粯瀹濋獙绛惧け璐?| RSA2 绛惧悕鏍￠獙涓嶉€氳繃 |
| 422 | 3013 | 鍦板潃涓嶅瓨鍦?| addressId 鏃犳晥鎴栧凡鍒犻櫎 |
| 500 | 3999 | 璁㈠崟鏈嶅姟鍐呴儴閿欒 | 绯荤粺寮傚父 |

### 6.4 鏍稿績鎺ュ彛璇︾粏璁捐

#### 6.4.1 鍒涘缓璁㈠崟锛堟牳蹇冮€昏緫锛?
澶勭悊娴佺▼锛?
1. **骞傜瓑鏍￠獙**锛氭煡 idempotent_record(idempotent_key) 鈫?status=1 鐩存帴杩斿洖 {orderId}
2. **鍙傛暟鏍￠獙**锛氭湇鍔￠」/瑙勬牸/鍦板潃鍚堟硶鎬?3. **Feign 鏌ュ疄鏃朵环鏍?*锛氳皟鐢?catalog-service GET /api/v1/items/{serviceItemId}
4. **浠锋牸蹇収**锛氬彇鏈€鏂?specPrice 浣滀负 totalAmount
5. **鍦板潃蹇収**锛氫粠 user_address 璇诲彇褰撳墠鍦板潃锛屽啑浣欏埌 order_header
6. **鐢熷崟**锛歋nowflakeIdWorker 鐢熸垚 orderId锛沷rder_sequence 鍙栧簭鍙锋嫾 YYYYMMDD+搴忓垪 鎴?order_no
7. **鍐欏叆 order_header**锛坰tatus=0锛寁ersion=1锛?8. **鍐欏叆 order_operation_log**锛坅ction=create, from=null, to=0锛?9. **鏇存柊骞傜瓑璁板綍**锛歎PDATE idempotent_record SET status=1, biz_id=orderId
10. **杩斿洖**锛歿orderId, orderNo}

#### 6.4.2 鏀粯鍥炶皟锛堝箓绛?Outbox锛?
澶勭悊娴佺▼锛?
1. **楠岀**锛氱敤鏀粯瀹濆叕閽?RSA2 鏍￠獙 sign 鈫?澶辫触杩斿洖 3012
2. **骞傜瓑**锛氬湪 payment_record 鏌?notify_id 纭鏄惁宸插鐞?3. **INSERT payment_record**锛氬埄鐢?uk_order_pay_type (order_no, pay_type) 闃查噸 鈫?鍛戒腑璇存槑宸插鐞嗭紝杩斿洖 success
4. **涔愯閿佹洿鏂?order_header**锛歎PDATE order_header SET status=1, version=version+1 WHERE id=? AND status=0 AND version=?
5. **璁板綍鏃ュ織**锛氬啓鍏?order_operation_log (action=pay, from=0, to=1)
6. **Outbox 鍐欏叆**锛氬悓涓€浜嬪姟鍐?INSERT event_message(topic=order_events, payload, status=0)
7. **杩斿洖绾枃鏈?*锛歴uccess锛堟敮浠樺疂瑕佹眰鐨勭函鏂囨湰鍝嶅簲锛?
#### 6.4.3 鍦板潃 CRUD 瑕佺偣

- **鏂板**锛歋nowflakeIdWorker 鐢熸垚 id锛涜嫢 isDefault=1 鍒欏厛 UPDATE user_address SET is_default=0 WHERE user_id=?
- **缂栬緫**锛歅ATCH 璇箟锛岄儴鍒嗗瓧娈垫洿鏂?- **鍒犻櫎**锛氶€昏緫鍒犻櫎 SET is_deleted=1
- **璁剧疆榛樿**锛氫簨鍔″唴涓ゆ锛氭竻闄ゅ叾浠栭粯璁?鈫?璁剧疆鏂伴粯璁?- 鎵€鏈夊湴鍧€鎿嶄綔鍙搷浣滃綋鍓嶇敤鎴风殑鏁版嵁

---

## 涓冦€丗eign 闆嗘垚 鈥?catalog-service

### 7.1 Feign 鎺ュ彛

`java
// nursing-common/src/main/java/com/nursing/common/feign/CatalogServiceFeignClient.java

@FeignClient(name = "nursing-catalog-service", path = "/api/v1/items")
public interface CatalogServiceFeignClient {
    @GetMapping("/{id}")
    Result<ServiceItemDTO> getItemDetail(@PathVariable("id") Long id);
}
`

### 7.2 DTO锛堟斁鍦?common 妯″潡锛?
- ServiceItemDTO: id, name, categoryId, status, specs (List<ServiceSpecDTO>)
- ServiceSpecDTO: id, serviceItemId, name, price, originalPrice, duration, status

### 7.3 璋冪敤鍦烘櫙

涓嬪崟鏃?OrderServiceImpl.createOrder() 璋冪敤 Feign锛?1. 鏌?GET /api/v1/items/{itemId}
2. 閬嶅巻 specs 鎵惧埌鍖归厤鐨?spec锛岄獙璇?status=1锛堜笂鏋讹級
3. 鍙栨渶鏂?specPrice 浣滀负 totalAmount 鍐欏叆蹇収
4. 鑻ユ湇鍔￠」涓嶅瓨鍦ㄦ垨宸蹭笅鏋?鈫?鎶?BusinessException(3002/3003)

---

## 鍏€並afka Outbox 妯″紡

### 8.1 浜嬩欢鍙戦€佹祦绋?
`
@Transactional
  1. 鏇存柊 order_header status=1
  2. INSERT event_message(status=0)

--- 浜嬪姟鎻愪氦 ---

@Scheduled(fixedDelay = 5000)  // 姣?绉掕疆璇?  1. SELECT * FROM event_message WHERE status=0 ORDER BY create_time LIMIT 50
  2. for each: kafkaTemplate.send(topic, event_key, payload)
  3. 鎴愬姛 鈫?UPDATE status=1
  4. 澶辫触 鈫?UPDATE retry_count=retry_count+1
  5. retry_count >= 3 鈫?UPDATE status=2 (澶辫触锛屼汉宸ヤ粙鍏?
`

### 8.2 浜嬩欢浣?
`json
{
  "eventType": "ORDER_PAID",
  "orderId": 20001,
  "orderNo": "20260708123456",
  "userId": 10001,
  "paidAt": "2026-07-08T14:30:00+08:00"
}
`

event_key = order_paid:{orderId}

---

## 涔濄€侀厤缃ˉ鍏?
| 閰嶇疆椤?| 璇存槑 |
|--------|------|
| nursing.snowflake.worker-id=3 | 璁㈠崟鏈嶅姟鐨勯洩鑺辩畻娉?worker-id |
| nursing.snowflake.datacenter-id=1 | 鏁版嵁涓績 ID |
| nursing.payment.mock=true | DEV 鐜妯℃嫙鏀粯 |
| nursing.payment.alipay.* | 鏀粯瀹濋厤缃紙鍗犱綅锛?|

---

## 鍗併€佸疄鐜伴『搴忥紙5 涓?Phase锛?
### Phase 1锛氬熀纭€楠ㄦ灦
- 鍒涘缓鎵€鏈?entity + Mapper 鎺ュ彛 + XML
- 鍦?common 妯″潡鍒涘缓 CatalogServiceFeignClient + 鐩稿叧 DTO
- SnowflakeConfig Bean

### Phase 2锛氬湴鍧€ CRUD
- AddressRequest / AddressResponse DTO
- IAddressService + AddressServiceImpl
- AddressController锛? 涓帴鍙ｏ級

### Phase 3锛氬箓绛?+ 涓嬪崟闂幆
- IdempotentService
- PrepayTokenResponse / OrderCreateRequest DTO
- IOrderService + OrderServiceImpl.createOrder()
- OrderController锛坧repay-token + 鍒涘缓璁㈠崟锛?
### Phase 4锛氳鍗曟煡璇?+ 鍙栨秷
- 鍒嗛〉鏌ヨ DTO + 璁㈠崟璇︽儏 DTO锛圥ageHelper锛?- 璁㈠崟鍒楄〃 + 璇︽儏锛堝惈鏀粯鐘舵€侊級
- 鍙栨秷璁㈠崟锛堢姸鎬佹満 + 涔愯閿侊級

### Phase 5锛氭敮浠?+ Outbox
- PayRequest / PayResponse DTO
- PaymentService锛堟ā鎷熸敮浠?鐪熷疄鏀粯锛?- OrderPaidEvent + OrderEventPublisher锛圤utbox + 瀹氭椂鎶曢€掞級
- 鏀粯鍥炶皟澶勭悊 + 鍙戣捣鏀粯鎺ュ彛

---

## 鍗佷竴銆佹祴璇曡鐐?
| 绫诲瀷 | 瑕嗙洊鍐呭 |
|------|---------|
| 骞傜瓑娴嬭瘯 | 鍚屼竴 Idempotent-Key 閲嶅璋冪敤鍒涘缓璁㈠崟杩斿洖鐩稿悓缁撴灉 |
| 鐘舵€佹満娴嬭瘯 | 鍚勭鐘舵€佺粍鍚堜笅鏄惁鍏佽鎿嶄綔 |
| 涔愯閿佹祴璇?| 骞跺彂鏇存柊 order_header version 鍐茬獊澶勭悊 |
| Outbox 娴嬭瘯 | 浜嬪姟鍐?event_message 鍐欏叆 + 瀹氭椂浠诲姟鎶曢€掑埌 Kafka |
| 鍦板潃鏉冮檺娴嬭瘯 | 鐢ㄦ埛 A 鏃犳硶鎿嶄綔鐢ㄦ埛 B 鐨勫湴鍧€ |

---

## 鍗佷簩銆佸凡纭鐨勫喅绛?
浠ヤ笅涓?Plan 闃舵閫愪竴纭鍚庣殑鏈€缁堝喅绛栵細

| # | 闂 | 鍐崇瓥 |
|---|------|------|
| 1 | 鍒涘缓璁㈠崟璺敱 | **POST /api/v1/orders**锛圧ESTful 椋庢牸锛?|
| 2 | 缂栬緫鍦板潃鏂规硶 | **PATCH /api/v1/addresses/{id}**锛堥儴鍒嗘洿鏂拌涔夛級 |
| 3 | uk_user_service_slot 绱㈠紩 | **琛ュ厖**鍞竴绱㈠紩 (user_id, service_item_id, service_date, service_time_slot) |
| 4 | 鍒嗛〉鏂规 | **寮曞叆 PageHelper** starter 渚濊禆 |
| 5 | order_item 鎷嗗垎 | **涓嶆媶鍒?*锛屽綋鍓?header 鍐椾綑鏂规 MVP 澶熺敤 |
| 6 | 鏀粯鍥炶皟杩斿洖鍊?| **绾枃鏈?\"success\"**锛岀鍚堟敮浠樺疂濂戠害 |

---

## 闄勫綍锛氬弬鑰冩枃妗?
- 绯荤粺鏋舵瀯璁捐.md锛埪у洓 鏁版嵁搴? 搂浜?骞傜瓑鎬? 搂鍏?娑堟伅闃熷垪锛?- API鎺ュ彛鏂囨。.md锛埪т簲 璁㈠崟涓庡湴鍧€锛?- 03_order_schema.sql锛坥rder_db 瀹屾暣 DDL锛
