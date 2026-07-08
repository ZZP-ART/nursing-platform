 CREATE DATABASE IF NOT EXISTS common_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
 
 USE common_db;
 
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
