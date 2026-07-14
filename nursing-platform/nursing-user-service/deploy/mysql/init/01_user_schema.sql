CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE user_db;

CREATE TABLE IF NOT EXISTS user (
    id              BIGINT NOT NULL COMMENT '用户ID(雪花算法)',
    phone           VARCHAR(20) NOT NULL COMMENT '手机号',
    password        VARCHAR(128) COMMENT 'BCrypt加密密码',
    nickname        VARCHAR(64) COMMENT '昵称',
    avatar          VARCHAR(256) COMMENT '头像URL',
    gender          TINYINT DEFAULT 0 COMMENT '0保密 1男 2女',
    id_card         VARCHAR(255) COMMENT '身份证号密文(医疗实名制)',
    status          TINYINT DEFAULT 0 COMMENT '0正常 1禁用',
    last_login_time DATETIME COMMENT '最后登录时间',
    register_ip     VARCHAR(45) COMMENT '注册IP',
    is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
    version         INT NOT NULL DEFAULT 0 COMMENT '资料乐观锁版本号',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS user_token (
    id              BIGINT NOT NULL COMMENT '主键(雪花算法)',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    token           VARCHAR(512) NOT NULL COMMENT 'JWT Token',
    expire_time     DATETIME NOT NULL COMMENT '过期时间',
    is_deleted      TINYINT DEFAULT 0 COMMENT '0正常 1已废弃',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户Token表';

CREATE TABLE IF NOT EXISTS sms_record (
    id              BIGINT NOT NULL COMMENT '主键(雪花算法)',
    phone           VARCHAR(20) NOT NULL COMMENT '手机号',
    sms_type        VARCHAR(32) NOT NULL COMMENT '短信类型:register/login/reset_password',
    code            VARCHAR(128) NOT NULL COMMENT '验证码BCrypt哈希',
    status          TINYINT DEFAULT 0 COMMENT '0待发送 1已发送 2发送失败 3已验证 4已过期 5结果未知',
    request_ip      VARCHAR(45) COMMENT '验证码请求IP',
    provider        VARCHAR(32) NOT NULL DEFAULT 'mock' COMMENT '短信发送器',
    provider_request_id VARCHAR(128) COMMENT '供应商请求ID或BizId',
    provider_receipt VARCHAR(1024) COMMENT '供应商回执摘要，不记录验证码明文',
    provider_receipt_time DATETIME COMMENT '供应商回执时间',
    failure_reason  VARCHAR(512) COMMENT '发送失败原因',
    send_time       DATETIME NOT NULL COMMENT '发送时间',
    expire_time     DATETIME NOT NULL COMMENT '过期时间',
    verify_time     DATETIME COMMENT '验证成功时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_phone_time (phone, send_time),
    INDEX idx_phone_type_status (phone, sms_type, status, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信发送记录表';

CREATE TABLE IF NOT EXISTS sms_send_request (
    id                      BIGINT NOT NULL COMMENT '主键(雪花算法)',
    idempotency_key         VARCHAR(64) NOT NULL COMMENT '客户端请求幂等键',
    request_fingerprint     CHAR(64) NOT NULL COMMENT '手机号和短信类型请求摘要',
    phone                   VARCHAR(20) NOT NULL COMMENT '手机号',
    sms_type                VARCHAR(32) NOT NULL COMMENT '短信类型',
    request_ip              VARCHAR(45) COMMENT '请求IP',
    status                  TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2供应商已受理 3明确失败 4结果未知',
    response_snapshot       VARCHAR(1024) NOT NULL COMMENT '首次受理响应快照',
    failure_reason          VARCHAR(512) COMMENT '失败或未知原因',
    idempotent_expire_time  DATETIME NOT NULL COMMENT '幂等记录过期时间',
    create_time             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sms_request_idempotency (idempotency_key),
    INDEX idx_sms_request_status_time (status, create_time),
    INDEX idx_sms_request_terminal_expiry (status, idempotent_expire_time),
    INDEX idx_sms_request_phone_type_time (phone, sms_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信发送请求表';

CREATE TABLE IF NOT EXISTS sms_outbox_event (
    id                      BIGINT NOT NULL COMMENT '主键(雪花算法)',
    request_id              BIGINT NOT NULL COMMENT '短信发送请求ID',
    event_key               VARCHAR(128) NOT NULL COMMENT '事件幂等键',
    event_type              VARCHAR(32) NOT NULL DEFAULT 'SMS_SEND' COMMENT '事件类型',
    status                  TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已完成 3结果未知 4死信',
    lease_owner             VARCHAR(128) COMMENT '领取Worker标识',
    lease_expire_time       DATETIME COMMENT '领取租约到期时间',
    next_execute_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次可执行时间',
    retry_count             INT NOT NULL DEFAULT 0 COMMENT '已尝试次数',
    processing_time         DATETIME COMMENT 'Worker领取时间',
    failure_reason          VARCHAR(512) COMMENT '处理失败或未知原因',
    create_time             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sms_outbox_event_key (event_key),
    UNIQUE KEY uk_sms_outbox_request (request_id),
    INDEX idx_sms_outbox_dispatch (status, next_execute_time),
    INDEX idx_sms_outbox_lease_expire (lease_expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信本地Outbox事件表';

CREATE TABLE IF NOT EXISTS sms_record_status_transition (
    id                      BIGINT NOT NULL COMMENT '主键(雪花算法)',
    sms_record_id           BIGINT NOT NULL COMMENT '短信审计记录ID',
    from_status             TINYINT COMMENT '变更前状态，创建时为空',
    to_status               TINYINT NOT NULL COMMENT '变更后状态',
    transition_reason       VARCHAR(512) COMMENT '状态变更原因',
    provider_receipt        VARCHAR(1024) COMMENT '供应商回执摘要',
    transition_time         DATETIME NOT NULL COMMENT '状态变更时间',
    create_time             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_sms_transition_record_time (sms_record_id, transition_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信审计状态转移表';

CREATE TABLE IF NOT EXISTS idempotent_record (
    id              BIGINT NOT NULL COMMENT '主键(雪花算法)',
    idempotent_key  VARCHAR(128) NOT NULL COMMENT '幂等键',
    biz_type        VARCHAR(32) NOT NULL COMMENT '业务类型',
    biz_id          BIGINT COMMENT '业务主键',
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '0处理中 1已完成',
    expire_time     DATETIME NOT NULL COMMENT '过期时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_key (idempotent_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='幂等记录表';

CREATE TABLE IF NOT EXISTS file_upload_record (
    id              BIGINT NOT NULL COMMENT '主键(雪花算法)',
    user_id         BIGINT NOT NULL COMMENT '上传用户ID',
    idempotent_key  VARCHAR(128) NOT NULL COMMENT '上传幂等键(含用户作用域)',
    biz_type        VARCHAR(32) NOT NULL COMMENT '业务类型',
    file_hash       CHAR(64) NOT NULL COMMENT '文件SHA-256',
    file_name       VARCHAR(128) NOT NULL COMMENT '文件名',
    file_url        VARCHAR(512) NOT NULL COMMENT '公开访问URL',
    relative_path   VARCHAR(512) NOT NULL COMMENT '相对存储路径',
    file_size       BIGINT NOT NULL COMMENT '文件大小',
    content_type    VARCHAR(128) COMMENT 'Content-Type',
    file_ext        VARCHAR(16) NOT NULL COMMENT '文件扩展名',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_upload_idempotent (user_id, idempotent_key),
    UNIQUE KEY uk_upload_hash (user_id, biz_type, file_hash, file_ext),
    INDEX idx_upload_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传记录表';
