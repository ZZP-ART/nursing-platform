 CREATE DATABASE IF NOT EXISTS feedback_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
 
 USE feedback_db;
 
 CREATE TABLE IF NOT EXISTS review (
     id              BIGINT NOT NULL COMMENT '评价ID(雪花算法)',
     order_id        BIGINT NOT NULL COMMENT '订单ID(一单一评)',
     user_id         BIGINT NOT NULL COMMENT '用户ID',
     service_item_id BIGINT COMMENT '服务项目ID',
     rating          TINYINT NOT NULL COMMENT '评分1-5星',
     content         TEXT COMMENT '评价内容',
     status          TINYINT DEFAULT 1 COMMENT '1待审核 2已展示 3隐藏',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     UNIQUE KEY uk_order_id (order_id),
     INDEX idx_user_id (user_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';
 
 CREATE TABLE IF NOT EXISTS review_image (
     id              BIGINT NOT NULL COMMENT '主键(雪花算法)',
     review_id       BIGINT NOT NULL COMMENT '评价ID',
     image_url       VARCHAR(256) NOT NULL COMMENT '图片URL',
     sort_order      INT DEFAULT 0 COMMENT '排序',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     PRIMARY KEY (id),
     INDEX idx_review_id (review_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价图片表';
 
 CREATE TABLE IF NOT EXISTS complaint (
     id              BIGINT NOT NULL COMMENT '投诉ID(雪花算法)',
     order_id        BIGINT NOT NULL COMMENT '订单ID',
     user_id         BIGINT NOT NULL COMMENT '用户ID',
     type            TINYINT NOT NULL COMMENT '投诉类型 1服务质量 2服务态度 3乱收费 4其他',
     content         TEXT COMMENT '投诉内容',
     images          VARCHAR(1024) COMMENT '投诉截图URL列表(JSON数组)',
     status          TINYINT DEFAULT 0 COMMENT '0待处理 1处理中 2已处理 3已关闭',
     idempotent_key  VARCHAR(64) COMMENT '幂等键',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     UNIQUE KEY uk_idempotent (idempotent_key),
     INDEX idx_user_id (user_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投诉表';
 
 CREATE TABLE IF NOT EXISTS complaint_track (
     id              BIGINT NOT NULL COMMENT '主键(雪花算法)',
     complaint_id    BIGINT NOT NULL COMMENT '投诉ID',
     operator        VARCHAR(32) COMMENT '处理人',
     content         VARCHAR(256) COMMENT '处理意见',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     INDEX idx_complaint_id (complaint_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投诉处理记录表';

-- ============================================================
-- 通用表：幂等记录（每个服务独立拥有）
-- ============================================================
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

-- ============================================================
-- 通用表：本地消息表 Outbox（每个服务独立拥有）
-- ============================================================
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
