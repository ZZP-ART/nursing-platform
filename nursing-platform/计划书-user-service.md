# nursing-user-service 寮€鍙戣鍒掍功

> **鐗堟湰**锛歷1.0  
> **鏃ユ湡**锛?026-07-08  
> **瀵瑰簲鍒嗘敮**锛歚codex/user-service`  
> **闆嗘垚鐩爣**锛歴quash merge 鈫?`develop`

---

## 涓€銆佹帴鍙ｆ竻鍗曚笌瀹炵幇椤哄簭

| 搴忓彿 | 鏂规硶 | 璺緞 | 璇存槑 | 閴存潈 | 鍓嶇疆渚濊禆 |
|------|------|------|------|------|---------|
| 1 | POST | /api/v1/users/sms-code | 鍙戦€侀獙璇佺爜 | 鍏?| 鏃?|
| 2 | POST | /api/v1/users/register | 娉ㄥ唽锛堟墜鏈哄彿+楠岃瘉鐮?瀵嗙爜锛?| 鍏?| 鎺ュ彛 1 |
| 3 | POST | /api/v1/users/login | 鐧诲綍锛堝瘑鐮佹垨楠岃瘉鐮佷袱绉嶆ā寮忥級 | 鍏?| 鏃?|
| 4 | GET | /api/v1/users/profile | 鑾峰彇褰撳墠鐢ㄦ埛淇℃伅 | 闇€ | 鎺ュ彛 2/3 绛惧彂 Token |
| 5 | PATCH | /api/v1/users/profile | 鏇存柊涓汉淇℃伅 | 闇€ | 鎺ュ彛 2/3 绛惧彂 Token |
| 6 | POST | /api/v1/users/logout | 鐧诲嚭锛圱oken 鍏?Redis 榛戝悕鍗曪級 | 闇€ | 鎺ュ彛 2/3 绛惧彂 Token |

**瀹炵幇椤哄簭璇存槑**锛氭寜 1鈫?鈫?鈫?鈫?鈫? 鎵ц銆傞獙璇佺爜鏃犱緷璧栧厛琛岋紝娉ㄥ唽/鐧诲綍鏄涓€閬撻棬锛岀櫥鍑哄拰鐢ㄦ埛淇℃伅鏌ヨ/淇敼鍦?Token 绯荤粺灏辩华鍚庨『鐞嗘垚绔犮€?

---

## 浜屻€丮aven 渚濊禆澧炶ˉ

褰撳墠 `nursing-user-service/pom.xml` 缂哄皯 JWT 鍜?BCrypt 鐩稿叧渚濊禆锛岄渶杩藉姞锛?

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- Hutool 宸ュ叿闆嗭紙宸插湪鐖?POM 绠＄悊鐗堟湰锛?-->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
</dependency>
<!-- Spring Security Crypto锛堜粎寮曞叆 BCrypt锛?-->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
<!-- Spring Boot Starter Validation锛堝弬鏁版牎楠屾敞瑙ｏ級 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## 涓夈€佸垎灞傝璁?

```
src/main/java/com/nursing/user/
|-- UserApplication.java                     # 鍚姩绫?
|-- controller/
|   |-- SmsController.java                   # POST /api/v1/users/sms-code
|   +-- UserController.java                  # 娉ㄥ唽/鐧诲綍/鐧诲嚭/me
|-- service/
|   |-- SmsService.java                      # 楠岃瘉鐮佺敓鎴愩€佸彂閫併€佹牎楠?
|   |-- UserService.java                     # 娉ㄥ唽/鐧诲綍/鏌ヨ/鏇存柊
|   +-- TokenService.java                    # JWT 绛惧彂銆佹牎楠屻€侀粦鍚嶅崟绠＄悊
|-- mapper/
|   |-- UserMapper.java                      # 鐢ㄦ埛琛?CRUD
|   |-- UserTokenMapper.java                 # Token 鎸佷箙鍖栬褰?
|   +-- SmsRecordMapper.java                 # 鐭俊璁板綍瀹¤
|-- entity/
|   |-- User.java                            # 鏄犲皠 user 琛?
|   |-- UserToken.java                       # 鏄犲皠 user_token 琛?
|   +-- SmsRecord.java                       # 鏄犲皠 sms_record 琛?
|-- dto/
|   |-- request/
|   |   |-- SmsCodeRequest.java              # phone + smsType
|   |   |-- RegisterRequest.java             # phone + smsCode + password + nickname(opt)
|   |   |-- LoginRequest.java                # phone + loginMode + password/smsCode
|   |   +-- UpdateUserRequest.java           # nickname + gender + avatar
|   +-- response/
|       |-- SmsCodeResponse.java             # expireSeconds
|       |-- LoginResponse.java               # token + expireTime + user
|       +-- UserInfoResponse.java            # 鑴辨晱鐢ㄦ埛淇℃伅
|-- config/
|   |-- JwtConfig.java                       # 璇诲彇 jwt.secret/expire 閰嶇疆
|   |-- RedisConfig.java                     # RedisTemplate Bean
|   +-- SnowflakeConfig.java                 # SnowflakeIdWorker Bean
+-- interceptor/
    +-- UserTokenInterceptor.java            # 浠?Header 鎻愬彇 userId
```

### 3.1 Controller 灞傝鍒?

| Controller | API | 璇锋眰浣?鍙傛暟 |
|-----------|-----|------------|
| **SmsController** | `POST /api/v1/users/sms-code` | `@RequestBody @Valid SmsCodeRequest` |
| **UserController** | `POST /api/v1/users/register` | `@RequestBody @Valid RegisterRequest` |
| **UserController** | `POST /api/v1/users/login` | `@RequestBody @Valid LoginRequest` |
| **UserController** | `GET /api/v1/users/profile` | `@RequestAttribute("userId")` |
| **UserController** | `PATCH /api/v1/users/profile` | `@RequestAttribute("userId")` + `@RequestBody @Valid UpdateUserRequest` |
| **UserController** | `POST /api/v1/users/logout` | `@RequestHeader("Authorization")` |
| **UserController** | `POST /api/v1/users/password/reset` | `@RequestBody @Valid Phone + smsCode + newPassword` |
| **UserController** | `POST /api/v1/files/upload` | `@RequestParam("file") MultipartFile` |

> **閴存潈绛栫暐**锛氬湪 Gateway 灞傛湭瀹屾暣灏辩华鍓嶏紝user-service 鍐呴€氳繃 `UserTokenInterceptor` 鎷︽埅闇€閴存潈鐨勮矾寰勶紝浠?`Authorization` 澶磋В鏋?Token 鍚庢敞鍏?`userId` 鍒拌姹傚睘鎬с€傚叕寮€璺緞鐩存帴鏀捐銆傚悗鏈?Gateway JWT Filter 灏辩华鍚庡彲绉婚櫎璇ユ嫤鎴櫒銆?

### 3.2 Service 灞傝亴璐?

| Service | 鏍稿績鏂规硶 | 璇存槑 |
|---------|---------|------|
| **SmsService** | `sendSmsCode(phone, smsType)` | 鐢熸垚6浣嶆暟瀛楅獙璇佺爜 鈫?棰戠巼鏍￠獙(60s/5娆?鏃? 鈫?瀛楻edis(5min) 鈫?璁板綍sms_record 鈫?Mock鍙戦€?|
| **SmsService** | `verifySmsCode(phone, smsCode, smsType)` | 浠嶳edis鍙栭獙璇佺爜鏍″ 鈫?鎴愬姛鍚庡垹闄edis閿?|
| **UserService** | `register(request)` | 鏍￠獙楠岃瘉鐮?鈫?鏌ラ噸 鈫?BCrypt鍔犲瘑瀵嗙爜 鈫?鎻掑叆user 鈫?绛惧彂Token 鈫?杩斿洖鐧诲綍鎬?|
| **UserService** | `login(request)` | 鏌ョ敤鎴?鈫?瀵嗙爜妯″紡楠岃瘉瀵嗙爜/sms妯″紡鏍￠獙楠岃瘉鐮?鈫?妫€鏌ョ姸鎬?鈫?绛惧彂Token 鈫?鏇存柊last_login_time |
| **UserService** | `getCurrentUser(userId)` | 鎸塈D鏌ョ敤鎴?鈫?瀛楁鑴辨晱(鎵嬫満鍙蜂腑闂?浣?) 鈫?杩斿洖 |
| **UserService** | `updateUser(userId, request)` | 鏍￠獙 鈫?閮ㄥ垎鏇存柊鍙敼瀛楁 鈫?杩斿洖 |
| **TokenService** | `generateToken(user)` | 鍒涘缓JWT(鍚玼serId/phone/exp) 鈫?瀛楻edis 鈫?璁板綍user_token琛?|
| **TokenService** | `validateToken(token)` | 瑙ｆ瀽JWT 鈫?鏌edis榛戝悕鍗?鈫?杩斿洖userId |
| **TokenService** | `invalidateToken(token)` | 灏員oken鍔犲叆Redis榛戝悕鍗?TTL鍚孴oken鍓╀綑鏈夋晥鏈? |

### 3.3 Mapper 灞傦紙MyBatis XML锛?

| Mapper | 涓昏鎿嶄綔 | SQL 璇存槑 |
|--------|---------|---------|
| **UserMapper** | insert/selectById/selectByPhone/updateById | 绠€鍗?CRUD锛岄€昏緫鍒犻櫎杩囨护 |
| **UserTokenMapper** | insert/selectByUserId | Token 鎸佷箙鍖栬褰?|
| **SmsRecordMapper** | insert/selectCountByPhoneToday | 鐭俊鏃ュ彂閫佹鏁扮粺璁?|

### 3.4 Entity 鏄犲皠

> **ID 绛栫暐**锛氭墍鏈?Entity 鍧囦娇鐢?`SnowflakeIdWorker` 鍦?Service 灞傜敓鎴愬垎甯冨紡 ID锛屼富閿被鍨嬩负 `Long`锛屼笉渚濊禆鏁版嵁搴撹嚜澧炪€?

**User** 瀵瑰簲 `user` 琛細
- id, phone, password, nickname, avatar, gender, idCard, status, lastLoginTime, registerIp, isDeleted, createTime, updateTime

**UserToken** 瀵瑰簲 `user_token` 琛細
- id, userId, token, expireTime, isDeleted, createTime

**SmsRecord** 瀵瑰簲 `sms_record` 琛細
- id, phone, smsType, code, status, sendTime, expireTime, createTime

---

## 鍥涖€佸叧閿疄鐜扮粏鑺?

### 4.1 JWT 绛惧彂涓庢牎楠?

- **绠楁硶**锛欻MAC-SHA256锛坖jwt 0.12.6 鍘熺敓鏀寔锛?
- **瀵嗛挜**锛氱粺涓€閫氳繃 Nacos 閰嶇疆涓績涓嬪彂锛坉ata-id: `nursing-jwt.yaml`锛夛紝user-service 涓?gateway 鍏变韩鍚屼竴瀵嗛挜锛屼笉纭紪鐮佸湪鏈湴閰嶇疆涓?
- **杞借嵎**锛歚{ "userId": 10001, "phone": "138****5678" }`
- **鏈夋晥鏈?*锛氶粯璁?7 澶╋紙`nursing.jwt.expire-seconds: 604800`锛?
- **Token 瀛樺偍**锛?
  - Redis锛歚user:token:{userId}` 鈫?`{token, expireTime}`锛岀敤浜庨粦鍚嶅崟鏍￠獙
  - MySQL锛歚user_token` 琛ㄦ寔涔呭寲璁板綍锛堝璁＄敤锛?
- **榛戝悕鍗曟満鍒?*锛氱櫥鍑烘椂浠?Token 鐨?jti 涓?key 鍐欏叆 Redis 榛戝悕鍗曪紝TTL = Token 鍓╀綑鏈夋晥鏈?

### 4.2 楠岃瘉鐮佹祦绋?

1. 鏍￠獙鎵嬫満鍙锋牸寮忥紙11浣嶏紝1寮€澶达級
2. 棰戠巼妫€鏌ワ細鍚屼竴鎵嬫満鍙?60 绉掑唴涓嶅彲閲嶅鍙戦€?鈫?鏌?Redis key `sms:rate:{phone}` 鏄惁瀛樺湪
3. 鏃ラ檺妫€鏌ワ細鍚屼竴鎵嬫満鍙峰綋澶╂渶澶?5 娆?鈫?`SELECT COUNT(*) FROM sms_record WHERE phone=? AND DATE(send_time)=CURDATE()`
4. smsType 涓氬姟鏍￠獙锛?
   - `register` 鈫?鎵嬫満鍙蜂笉鑳藉凡娉ㄥ唽
   - `login` / `reset_password` 鈫?鎵嬫満鍙峰繀椤诲凡娉ㄥ唽
5. 鐢熸垚 6 浣嶇函鏁板瓧楠岃瘉鐮侊紙`String.format("%06d", random.nextInt(1000000))`锛?
6. 瀛樺偍鍒?Redis锛歚sms:code:{smsType}:{phone}` 鈫?`{code}`锛孴TL 300 绉?
7. 璁板綍 `sms_record`锛坰tatus=1锛宑ode 鏄庢枃瀛樺偍浠呯敤浜庡璁★級
8. 褰撳墠涓?Mock 妯″紡锛坄nursing.sms.mock: true`锛夛紝浠呮棩蹇楁墦鍗帮紝涓嶇湡瀹炶皟鐢?SMS 閫氶亾
9. 杩斿洖 `{ "expireSeconds": 300 }`

### 4.3 瀵嗙爜鍔犲瘑

- 浣跨敤 `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`
- 姣忔娉ㄥ唽鏃剁敓鎴愰殢鏈虹洂锛屽瘑鐮佸瓨鍌ㄤ负 BCrypt 鍝堝笇
- 鐧诲綍鏃?`encoder.matches(rawPassword, encodedPassword)` 鏍￠獙

### 4.4 鐧诲綍妯″紡

鏀寔涓ょ鐧诲綍鏂瑰紡锛堢敱 `loginMode` 瀛楁鍖哄垎锛夛細

| 妯″紡 | loginMode 鍊?| 鏍￠獙鏂瑰紡 |
|------|-------------|---------|
| 瀵嗙爜鐧诲綍 | `password` | 鏌?DB 鈫?BCrypt 鍖归厤瀵嗙爜 |
| 鐭俊楠岃瘉鐮佺櫥褰?| `sms` | Redis 鍙栭獙璇佺爜瀵规瘮锛堝悓娉ㄥ唽鏍￠獙閫昏緫锛墊

### 4.5 鐢ㄦ埛淇℃伅鑴辨晱

杩斿洖鍓嶇鐨勭敤鎴峰璞′腑锛屾墜鏈哄彿涓棿鍥涗綅鏇挎崲涓?`*`锛?

```
13812345678 鈫?138****5678
```

### 4.6 璇锋眰楠岃瘉

浣跨敤 Jakarta Validation锛坄@Valid`锛夊仛鍙傛暟鏍￠獙锛?
- 鎵嬫満鍙凤細`@Pattern(regexp = "^1\\\d{10}$")`
- 瀵嗙爜锛歚@Size(min=8, max=32)` + 鑷畾涔夋牎楠屽瓧姣?鏁板瓧缁勫悎
- 鏄电О锛歚@Size(min=2, max=16)`
- 楠岃瘉鐮侊細`@Pattern(regexp = "^\\\d{6}$")`

### 4.7 閿欒鐮侊紙User 鍩?2000-2999锛?

| code | message | 瑙﹀彂鏉′欢 |
|------|---------|---------|
| 2001 | 鍙戦€佽繃浜庨绻侊紝璇?60 绉掑悗閲嶈瘯 | 鍚屼竴鎵嬫満鍙?60s 鍐呴噸澶嶈姹?|
| 2002 | 浠婃棩鍙戦€佹鏁板凡杈句笂闄?| 鍚屼竴鎵嬫満鍙锋瘡鏃ヨ秴杩?5 娆?|
| 2003 | 鎵嬫満鍙峰凡琚敞鍐?| register 鏃舵墜鏈哄彿宸插瓨鍦?|
| 2004 | 鎵嬫満鍙锋湭娉ㄥ唽 | login/reset 鏃舵墜鏈哄彿涓嶅瓨鍦?|
| 2005 | 鐭俊鍙戦€佸け璐ワ紝璇风◢鍚庨噸璇?| SMS 閫氶亾寮傚父 |
| 2006 | 楠岃瘉鐮侀敊璇?| 楠岃瘉鐮佷笉鍖归厤 |
| 2007 | 楠岃瘉鐮佸凡杩囨湡 | 楠岃瘉鐮佽秴杩?5 鍒嗛挓鏈夋晥鏈?|
| 2008 | 璇ユ墜鏈哄彿宸叉敞鍐?| 娉ㄥ唽鍐茬獊 |
| 2009 | 瀵嗙爜涓嶇鍚堝畨鍏ㄨ姹?| 瀵嗙爜鏈寘鍚瓧姣嶅拰鏁板瓧鎴栭暱搴︿笉瓒?|
| 2010 | 瀵嗙爜閿欒 | 瀵嗙爜涓嶅尮閰?|
| 2011 | 璐﹀彿宸茶绂佺敤 | 鐢ㄦ埛 status=1 |

---

## 浜斻€侀厤缃枃浠跺彉鏇?

闇€鍦?`application.yml` 涓柊澧炰互涓嬮厤缃細

```yaml
nursing:
  jwt:
    expire-seconds: 604800   # 7 澶╋紙瀵嗛挜 secret 鐢?Nacos 涓嬪彂锛?
  sms:
    mock: true               # 寮€鍙戦樁娈?Mock锛屼笉鐪熷疄鍙戠煭淇?
    rate-limit-seconds: 60   # 鍙戦€侀鐜囬檺鍒?
    daily-limit: 5           # 姣忔棩涓婇檺
```
> **Nacos 閰嶇疆**锛氬湪閰嶇疆涓績鍒涘缓 `nursing-jwt.yaml`锛屽寘鍚?`nursing.jwt.secret` 瀛楁锛寀ser-service 涓?gateway 鍏卞悓寮曠敤銆?

---

## 鍏€侀璁″伐浣滈噺

| 妯″潡 | 鏂囦欢鏁?| 棰勪及宸ユ椂 | 澶囨敞 |
|------|-------|---------|------|
| POM 渚濊禆澧炶ˉ | 1 | 0.5h | 杩藉姞 jjwt/security-crypto 渚濊禆 |
| Entity 绫?| 3 | 1h | User / UserToken / SmsRecord |
| Mapper 鎺ュ彛 + XML | 3 脳 2 | 2h | 姣忎釜 Mapper 鎺ュ彛 + 瀵瑰簲鐨?XML |
| Service 灞?| 3 | 2.5h | SmsService / UserService / TokenService |
| Controller + DTO | 4 + 5 | 2h | 2 涓?Controller锛? 涓?Request DTO锛? 涓?Response DTO |
| Config + Interceptor | 3 | 1.5h | JwtConfig / RedisConfig / SnowflakeConfig / UserTokenInterceptor |
| 鍚姩绫?+ application.yml 琛ュ厖 | 2 | 0.5h | 琛ュ厖 JWT 鍜?SMS 閰嶇疆 |
| 鍗曞厓娴嬭瘯 | ~10 | 2h | 閲嶇偣瑕嗙洊 Service 灞?|
| **鍚堣** | **~30 鏂囦欢** | **~12h** | 鍙湪涓€涓伐浣滄棩鍐呭畬鎴?|

瀹為檯缂栫爜椤哄簭寤鸿锛?
1. POM 渚濊禆 + 閰嶇疆锛堝熀纭€璁炬柦锛?
2. Entity + Mapper锛堟暟鎹眰锛?
3. Config 绫?+ TokenService锛圝WT 鍩虹璁炬柦锛?
4. SmsService + SmsController锛堥獙璇佺爜锛?
5. UserService + UserController锛堟敞鍐?鐧诲綍/鐢ㄦ埛淇℃伅锛?
6. UserTokenInterceptor锛堥壌鏉冩嫤鎴級
7. 鍗曞厓娴嬭瘯 + 闆嗘垚楠岃瘉

---

## 涓冦€佹湭绾冲叆鏈鑼冨洿

- **閲嶇疆瀵嗙爜** API锛圥OST /api/v1/users/password/reset锛夆€?涓嬩竴杩唬瀹炵幇
- **鏂囦欢涓婁紶** API锛圥OST /api/v1/files/upload锛夆€?涓嬩竴杩唬瀹炵幇
- **骞傜瓑璁板綍琛?*锛坕dempotent_record锛夊拰 **鏈湴娑堟伅琛?*锛坋vent_message锛夆€?鍚庣画璁㈠崟鏈嶅姟浣跨敤
- **Kafka 闆嗘垚** 鈥?鐩墠 user-service 涓嶉渶瑕佸彂閫佸紓姝ヤ簨浠讹紝浣嗕繚鐣欎緷璧栫敤浜庡悗缁?

---

## 鍏€佸凡纭鐨勮璁″喅绛?

浠ヤ笅涓轰笌璐熻矗浜烘矡閫氱‘璁ょ殑鍐崇瓥娓呭崟锛岀紪鐮侀樁娈典笉鍐嶅彉鏇达細

| # | 鍐崇瓥椤?| 缁撹 | 渚濇嵁 |
|---|-------|------|------|
| 1 | API 璺緞椋庢牸 | 鎸?API 鎺ュ彛鏂囨。 | 涓庣郴缁熸灦鏋勬枃妗ｅ榻?|
| 2 | HTTP 鏂规硶锛堟洿鏂拌祫鏂欙級 | PATCH | API 鎺ュ彛鏂囨。瑙勮寖 |
| 3 | JWT 瀵嗛挜鏉ユ簮 | Nacos 閰嶇疆涓績锛坣ursing-jwt.yaml锛墊 渚夸簬 gateway 涓?service 鍏变韩锛岄伩鍏嶇‖缂栫爜 |
| 4 | 閴存潈瀹炵幇 | 鏈嶅姟鍐?UserTokenInterceptor | Gateway 缁勪欢灏辩华鍓嶄繚闅滄帴鍙ｅ畨鍏?|
| 5 | ID 鐢熸垚绛栫暐 | SnowflakeIdWorker 闆姳绠楁硶 | 棰勭暀鍒嗗簱鍒嗚〃鎵╁睍鑳藉姏 |
| 6 | 鐧诲綍璇锋眰浣?| 缁熶竴 LoginRequest锛園Conditional 鏍￠獙锛?| 鍑忓皯 DTO 鏁伴噺锛岄€昏緫鍐呰仛 |
| 7 | 鏇存柊璧勬枡瀛楁鑼冨洿 | nickname + gender + avatar 涓変釜瀛楁 | MVP 鏈€灏忛泦锛屾寜闇€鎵╁睍 |
| 8 | 瀵嗙爜鏍￠獙鏂瑰紡 | @Pattern + @Size 姝ｅ垯鏍￠獙 | 绠€鍗曠洿鎺ワ紝鏃犻渶鑷畾涔夋敞瑙?|
