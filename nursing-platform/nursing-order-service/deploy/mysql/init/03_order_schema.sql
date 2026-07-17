 CREATE DATABASE IF NOT EXISTS order_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
 
 USE order_db;
 
 CREATE TABLE IF NOT EXISTS user_address (
     id              BIGINT NOT NULL COMMENT '地址ID(雪花算法)',
     user_id         BIGINT NOT NULL COMMENT '用户ID',
     receiver_name   VARCHAR(32) NOT NULL COMMENT '收件人',
     receiver_phone  VARCHAR(20) NOT NULL COMMENT '联系电话',
     tag             VARCHAR(16) COMMENT '标签(家/公司/学校)',
     province        VARCHAR(32) COMMENT '省',
     city            VARCHAR(32) COMMENT '市',
     district        VARCHAR(32) COMMENT '区/县',
     detail_address  VARCHAR(256) NOT NULL COMMENT '详细地址',
     latitude        DECIMAL(10,7) COMMENT '纬度',
     longitude       DECIMAL(10,7) COMMENT '经度',
     is_default      TINYINT DEFAULT 0 COMMENT '0非默认 1默认',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     INDEX idx_user_id (user_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户地址表';
 
 CREATE TABLE IF NOT EXISTS order_header (
     id              BIGINT NOT NULL COMMENT '订单ID(雪花算法)',
     order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
     user_id         BIGINT NOT NULL COMMENT '用户ID',
     merchant_id     BIGINT NOT NULL DEFAULT 20001 COMMENT '服务归属商户ID',
     source          TINYINT DEFAULT 0 COMMENT '订单来源 0App 1小程序 2后台',
     version         INT DEFAULT 0 COMMENT '乐观锁',
     -- 服务快照
     service_item_id BIGINT NOT NULL COMMENT '服务项目ID',
     service_spec_id BIGINT NOT NULL COMMENT '规格ID',
     service_item_name VARCHAR(128) COMMENT '服务名称(快照)',
     category_name   VARCHAR(64) COMMENT '服务分类名称(快照)',
     spec_name       VARCHAR(64) COMMENT '规格名称(快照)',
     spec_price      DECIMAL(10,2) COMMENT '售价(快照)',
     spec_duration   INT COMMENT '服务时长(快照)',
     quantity        INT NOT NULL DEFAULT 1 COMMENT '下单数量(快照)',
     catalog_snapshot_version INT NOT NULL DEFAULT 1 COMMENT '目录快照版本',
     -- 地址快照
     address_id      BIGINT COMMENT '地址ID',
     receiver_name   VARCHAR(32) COMMENT '收件人(快照)',
     receiver_phone  VARCHAR(20) COMMENT '联系电话(快照)',
     address_detail  VARCHAR(256) COMMENT '详细地址(快照)',
     -- 服务时间
     service_date    DATE NOT NULL COMMENT '预约日期',
     service_time_slot VARCHAR(32) NOT NULL COMMENT '预约时段',
     -- 金额与状态
     total_amount    DECIMAL(10,2) NOT NULL COMMENT '订单金额',
     status          TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1待服务 2已完成 3已取消 4退款中 5已退款',
     slot_occupied   TINYINT DEFAULT 1 COMMENT '1占用预约时段 NULL已释放',
     remark          VARCHAR(256) COMMENT '用户备注',
     cancel_reason   VARCHAR(256) COMMENT '取消原因',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     UNIQUE KEY uk_order_no (order_no),
     UNIQUE KEY uk_user_service_slot (user_id, service_item_id, service_date, service_time_slot, slot_occupied),
     INDEX idx_user_id (user_id),
     INDEX idx_merchant_id (merchant_id, status),
     INDEX idx_status (status)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
 
 CREATE TABLE IF NOT EXISTS payment_record (
     id              BIGINT NOT NULL COMMENT '支付记录ID(雪花算法)',
     order_id        BIGINT NOT NULL COMMENT '订单ID',
     order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
     user_id         BIGINT NOT NULL COMMENT '用户ID',
     pay_amount      DECIMAL(10,2) NOT NULL COMMENT '支付金额',
     pay_type        TINYINT NOT NULL COMMENT '1支付宝 2微信',
     pay_status      TINYINT DEFAULT 0 COMMENT '0未支付 1已支付 2已退款',
     trade_no        VARCHAR(64) COMMENT '第三方交易流水号',
     notify_id       VARCHAR(128) COMMENT '第三方通知ID(幂等用)',
     pay_time        DATETIME COMMENT '支付时间',
     refund_time     DATETIME COMMENT '退款时间',
     version         INT DEFAULT 0 COMMENT '乐观锁',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     UNIQUE KEY uk_order_pay_type (order_no, pay_type),
     UNIQUE KEY uk_payment_notify_id (notify_id),
     UNIQUE KEY uk_payment_trade_no (trade_no),
     INDEX idx_order_id (order_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';
 
CREATE TABLE IF NOT EXISTS payment_intent (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    idempotent_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    pay_channel VARCHAR(32) NOT NULL,
    status TINYINT NOT NULL,
    response_snapshot JSON NULL,
    payment_record_id BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_intent_user_key (user_id, idempotent_key),
    UNIQUE KEY uk_payment_intent_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refund_record (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    payment_record_id BIGINT NOT NULL,
    refund_no VARCHAR(64) NOT NULL,
    refund_amount DECIMAL(10,2) NOT NULL,
    status TINYINT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    lease_owner VARCHAR(128) NULL,
    lease_expire_time DATETIME NULL,
    next_execute_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    provider_refund_no VARCHAR(128) NULL,
    failure_reason VARCHAR(512) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_order (order_id),
    UNIQUE KEY uk_refund_no (refund_no),
    INDEX idx_refund_dispatch (status, next_execute_time),
    INDEX idx_refund_lease (lease_expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_operation_log (
     id              BIGINT NOT NULL COMMENT '日志ID(雪花算法)',
     order_id        BIGINT NOT NULL COMMENT '订单ID',
     order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
     user_id         BIGINT COMMENT '操作人用户ID',
     operator        VARCHAR(32) COMMENT '操作人姓名',
     action          VARCHAR(32) NOT NULL COMMENT '动作:create/pay/cancel/complete/refund',
     from_status     TINYINT COMMENT '变更前状态',
     to_status       TINYINT NOT NULL COMMENT '变更后状态',
     remark          VARCHAR(256) COMMENT '备注',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     PRIMARY KEY (id),
     INDEX idx_order_id (order_id),
     INDEX idx_create_time (create_time)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单操作日志表';
 
 -- 订单号序列（用于生成 order_no）
 CREATE TABLE IF NOT EXISTS order_sequence (
     id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
     stub            CHAR(1) NOT NULL DEFAULT '0' COMMENT '占位',
     PRIMARY KEY (id),
     UNIQUE KEY uk_stub (stub)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单号序列表';

-- ============================================================
-- 通用表：幂等记录（每个服务独立拥有）
-- ============================================================
CREATE TABLE IF NOT EXISTS idempotent_record (
    id              BIGINT NOT NULL COMMENT '主键(雪花算法)',
    idempotent_key  VARCHAR(128) NOT NULL COMMENT '幂等键',
    biz_type        VARCHAR(32) NOT NULL COMMENT '业务类型',
    user_id         BIGINT NOT NULL COMMENT '令牌所属用户',
    request_fingerprint CHAR(64) NULL COMMENT '请求摘要',
    biz_id          BIGINT COMMENT '业务主键',
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '0处理中 1已完成',
    expire_time     DATETIME NOT NULL COMMENT '过期时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_key (idempotent_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='幂等记录表';

-- ============================================================
-- 通用表：本地消息表 Outbox（每个服务独立拥有）
-- ============================================================
CREATE TABLE IF NOT EXISTS event_message (
    id              BIGINT NOT NULL COMMENT '主键(雪花算法)',
    topic           VARCHAR(64) NOT NULL COMMENT 'MQ Topic',
    event_key       VARCHAR(128) NOT NULL COMMENT '事件幂等键',
    payload         JSON NOT NULL COMMENT '事件体',
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '0待投递 1已投递',
    retry_count     INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_execute_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次投递时间',
    last_error      VARCHAR(512) NULL COMMENT '最近投递失败原因',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event (topic, event_key),
    INDEX idx_pending_delivery (status, next_execute_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地消息表(Outbox)';
