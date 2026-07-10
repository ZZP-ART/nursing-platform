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
    provider        VARCHAR(32) NOT NULL DEFAULT 'aliyun' COMMENT '短信供应商',
    provider_request_id VARCHAR(128) COMMENT '供应商请求ID或BizId',
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

CREATE TABLE IF NOT EXISTS event_message (
    id              BIGINT NOT NULL COMMENT '主键(雪花算法)',
    topic           VARCHAR(64) NOT NULL COMMENT 'MQ Topic',
    event_key       VARCHAR(128) NOT NULL COMMENT '事件幂等键',
    payload         JSON NOT NULL COMMENT '事件体',
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '0待投递 1已投递 2失败',
    retry_count     TINYINT NOT NULL DEFAULT 0 COMMENT '重试次数',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event (topic, event_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地消息表(Outbox)';
