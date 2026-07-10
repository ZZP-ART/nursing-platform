USE user_db;

ALTER TABLE user
    ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 0 COMMENT '资料乐观锁版本号' AFTER is_deleted;

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
