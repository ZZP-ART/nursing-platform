 CREATE TABLE IF NOT EXISTS user_address (
     id              BIGINT NOT NULL COMMENT '鍦板潃ID(闆姳绠楁硶)',
     user_id         BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
     receiver_name   VARCHAR(32) NOT NULL COMMENT '鏀朵欢浜?,
     receiver_phone  VARCHAR(20) NOT NULL COMMENT '鑱旂郴鐢佃瘽',
     tag             VARCHAR(16) COMMENT '鏍囩(瀹?鍏徃/瀛︽牎)',
     province        VARCHAR(32) COMMENT '鐪?,
     city            VARCHAR(32) COMMENT '甯?,
     district        VARCHAR(32) COMMENT '鍖?鍘?,
     detail_address  VARCHAR(256) NOT NULL COMMENT '璇︾粏鍦板潃',
     latitude        DECIMAL(10,7) COMMENT '绾害',
     longitude       DECIMAL(10,7) COMMENT '缁忓害',
     is_default      TINYINT DEFAULT 0 COMMENT '0闈為粯璁?1榛樿',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0鏈垹 1宸插垹',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
     PRIMARY KEY (id),
     INDEX idx_user_id (user_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢ㄦ埛鍦板潃琛?;
 
 CREATE TABLE IF NOT EXISTS order_header (
     id              BIGINT NOT NULL COMMENT '璁㈠崟ID(闆姳绠楁硶)',
     order_no        VARCHAR(32) NOT NULL COMMENT '璁㈠崟鍙?,
     user_id         BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
     source          TINYINT DEFAULT 0 COMMENT '璁㈠崟鏉ユ簮 0App 1灏忕▼搴?2鍚庡彴',
     version         INT DEFAULT 0 COMMENT '涔愯閿?,
     -- 鏈嶅姟蹇収
     service_item_id BIGINT NOT NULL COMMENT '鏈嶅姟椤圭洰ID',
     service_spec_id BIGINT NOT NULL COMMENT '瑙勬牸ID',
     service_item_name VARCHAR(128) COMMENT '鏈嶅姟鍚嶇О(蹇収)',
     category_name   VARCHAR(64) COMMENT '鏈嶅姟鍒嗙被鍚嶇О(蹇収)',
     spec_name       VARCHAR(64) COMMENT '瑙勬牸鍚嶇О(蹇収)',
     spec_price      DECIMAL(10,2) COMMENT '鍞环(蹇収)',
     spec_duration   INT COMMENT '鏈嶅姟鏃堕暱(蹇収)',
     quantity        INT NOT NULL DEFAULT 1 COMMENT '涓嬪崟鏁伴噺(蹇収)',
     catalog_snapshot_version INT NOT NULL DEFAULT 1 COMMENT '鐩綍蹇収鐗堟湰',
     -- 鍦板潃蹇収
     address_id      BIGINT COMMENT '鍦板潃ID',
     receiver_name   VARCHAR(32) COMMENT '鏀朵欢浜?蹇収)',
     receiver_phone  VARCHAR(20) COMMENT '鑱旂郴鐢佃瘽(蹇収)',
     address_detail  VARCHAR(256) COMMENT '璇︾粏鍦板潃(蹇収)',
     -- 鏈嶅姟鏃堕棿
     service_date    DATE NOT NULL COMMENT '棰勭害鏃ユ湡',
     service_time_slot VARCHAR(32) NOT NULL COMMENT '棰勭害鏃舵',
     -- 閲戦涓庣姸鎬?
     total_amount    DECIMAL(10,2) NOT NULL COMMENT '璁㈠崟閲戦',
     status          TINYINT NOT NULL DEFAULT 0 COMMENT '0寰呮敮浠?1寰呮湇鍔?2宸插畬鎴?3宸插彇娑?4閫€娆句腑 5宸查€€娆?,
     slot_occupied   TINYINT DEFAULT 1 COMMENT '1鍗犵敤棰勭害鏃舵 NULL宸查噴鏀?,
     remark          VARCHAR(256) COMMENT '鐢ㄦ埛澶囨敞',
     cancel_reason   VARCHAR(256) COMMENT '鍙栨秷鍘熷洜',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0鏈垹 1宸插垹',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
     PRIMARY KEY (id),
     UNIQUE KEY uk_order_no (order_no),
     UNIQUE KEY uk_user_service_slot (user_id, service_item_id, service_date, service_time_slot, slot_occupied),
     INDEX idx_user_id (user_id),
     INDEX idx_status (status)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='璁㈠崟琛?;
 
 CREATE TABLE IF NOT EXISTS payment_record (
     id              BIGINT NOT NULL COMMENT '鏀粯璁板綍ID(闆姳绠楁硶)',
     order_id        BIGINT NOT NULL COMMENT '璁㈠崟ID',
     order_no        VARCHAR(32) NOT NULL COMMENT '璁㈠崟鍙?,
     user_id         BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
     pay_amount      DECIMAL(10,2) NOT NULL COMMENT '鏀粯閲戦',
     pay_type        TINYINT NOT NULL COMMENT '1鏀粯瀹?2寰俊',
     pay_status      TINYINT DEFAULT 0 COMMENT '0鏈敮浠?1宸叉敮浠?2宸查€€娆?,
     trade_no        VARCHAR(64) COMMENT '绗笁鏂逛氦鏄撴祦姘村彿',
     notify_id       VARCHAR(128) COMMENT '绗笁鏂归€氱煡ID(骞傜瓑鐢?',
     pay_time        DATETIME COMMENT '鏀粯鏃堕棿',
     refund_time     DATETIME COMMENT '閫€娆炬椂闂?,
     version         INT DEFAULT 0 COMMENT '涔愯閿?,
     is_deleted      TINYINT DEFAULT 0 COMMENT '0鏈垹 1宸插垹',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
     PRIMARY KEY (id),
     UNIQUE KEY uk_order_pay_type (order_no, pay_type),
     INDEX idx_order_id (order_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鏀粯璁板綍琛?;
 
 CREATE TABLE IF NOT EXISTS order_operation_log (
     id              BIGINT NOT NULL COMMENT '鏃ュ織ID(闆姳绠楁硶)',
     order_id        BIGINT NOT NULL COMMENT '璁㈠崟ID',
     order_no        VARCHAR(32) NOT NULL COMMENT '璁㈠崟鍙?,
     user_id         BIGINT COMMENT '鎿嶄綔浜虹敤鎴稩D',
     operator        VARCHAR(32) COMMENT '鎿嶄綔浜哄鍚?,
     action          VARCHAR(32) NOT NULL COMMENT '鍔ㄤ綔:create/pay/cancel/complete/refund',
     from_status     TINYINT COMMENT '鍙樻洿鍓嶇姸鎬?,
     to_status       TINYINT NOT NULL COMMENT '鍙樻洿鍚庣姸鎬?,
     remark          VARCHAR(256) COMMENT '澶囨敞',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
     PRIMARY KEY (id),
     INDEX idx_order_id (order_id),
     INDEX idx_create_time (create_time)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='璁㈠崟鎿嶄綔鏃ュ織琛?;
 
 -- 璁㈠崟鍙峰簭鍒楋紙鐢ㄤ簬鐢熸垚 order_no锛?
 CREATE TABLE IF NOT EXISTS order_sequence (
     id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '鑷涓婚敭',
     stub            CHAR(1) NOT NULL DEFAULT '0' COMMENT '鍗犱綅',
     PRIMARY KEY (id),
     UNIQUE KEY uk_stub (stub)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='璁㈠崟鍙峰簭鍒楄〃';

-- ============================================================
-- 閫氱敤琛細骞傜瓑璁板綍锛堟瘡涓湇鍔＄嫭绔嬫嫢鏈夛級
-- ============================================================
CREATE TABLE IF NOT EXISTS idempotent_record (
    id              BIGINT NOT NULL COMMENT '涓婚敭(闆姳绠楁硶)',
    idempotent_key  VARCHAR(128) NOT NULL COMMENT '骞傜瓑閿?,
    biz_type        VARCHAR(32) NOT NULL COMMENT '涓氬姟绫诲瀷',
    user_id         BIGINT NOT NULL COMMENT '浠ょ墝鎵€灞炵敤鎴?,
    request_fingerprint CHAR(64) NULL COMMENT '璇锋眰鎽樿',
    biz_id          BIGINT COMMENT '涓氬姟涓婚敭',
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '0澶勭悊涓?1宸插畬鎴?,
    expire_time     DATETIME NOT NULL COMMENT '杩囨湡鏃堕棿',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    PRIMARY KEY (id),
    UNIQUE KEY uk_key (idempotent_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='骞傜瓑璁板綍琛?;

-- ============================================================
-- 閫氱敤琛細鏈湴娑堟伅琛?Outbox锛堟瘡涓湇鍔＄嫭绔嬫嫢鏈夛級
-- ============================================================
CREATE TABLE IF NOT EXISTS event_message (
    id              BIGINT NOT NULL COMMENT '涓婚敭(闆姳绠楁硶)',
    topic           VARCHAR(64) NOT NULL COMMENT 'MQ Topic',
    event_key       VARCHAR(128) NOT NULL COMMENT '浜嬩欢骞傜瓑閿?,
    payload         JSON NOT NULL COMMENT '浜嬩欢浣?,
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '0寰呮姇閫?1宸叉姇閫?,
    retry_count     INT NOT NULL DEFAULT 0 COMMENT '閲嶈瘯娆℃暟',
    next_execute_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '涓嬫鎶曢€掓椂闂?,
    last_error      VARCHAR(512) NULL COMMENT '鏈€杩戞姇閫掑け璐ュ師鍥?,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event (topic, event_key),
    INDEX idx_pending_delivery (status, next_execute_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鏈湴娑堟伅琛?Outbox)';
