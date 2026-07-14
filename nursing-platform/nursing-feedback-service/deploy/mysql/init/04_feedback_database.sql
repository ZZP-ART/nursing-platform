CREATE DATABASE IF NOT EXISTS feedback_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE feedback_db;

CREATE TABLE IF NOT EXISTS review (
    id              BIGINT NOT NULL,
    order_id        BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    service_item_id BIGINT,
    rating          TINYINT NOT NULL,
    content         TEXT,
    status          TINYINT DEFAULT 1,
    is_deleted      TINYINT DEFAULT 0,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS review_image (
    id              BIGINT NOT NULL,
    review_id       BIGINT NOT NULL,
    image_url       VARCHAR(256) NOT NULL,
    sort_order      INT DEFAULT 0,
    is_deleted      TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint (
    id              BIGINT NOT NULL,
    order_id        BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    type            TINYINT NOT NULL,
    content         TEXT,
    images          VARCHAR(1024),
    status          TINYINT DEFAULT 0,
    idempotent_key  VARCHAR(128),
    request_hash    CHAR(64) NULL,
    is_deleted      TINYINT DEFAULT 0,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_complaint_idempotent (user_id, idempotent_key),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint_track (
    id              BIGINT NOT NULL,
    complaint_id    BIGINT NOT NULL,
    operator        VARCHAR(32),
    content         VARCHAR(256),
    is_deleted      TINYINT DEFAULT 0,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_complaint_id (complaint_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS idempotent_record (
    id              BIGINT NOT NULL,
    idempotent_key  VARCHAR(128) NOT NULL,
    biz_type        VARCHAR(32) NOT NULL,
    subject_id      BIGINT NULL,
    request_hash    CHAR(64) NULL,
    biz_id          BIGINT,
    status          TINYINT NOT NULL DEFAULT 0,
    expire_time     DATETIME NOT NULL,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotent_scope (biz_type, subject_id, idempotent_key),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS event_message (
    id              BIGINT NOT NULL,
    topic           VARCHAR(64) NOT NULL,
    event_key       VARCHAR(128) NOT NULL,
    payload         JSON NOT NULL,
    status          TINYINT NOT NULL DEFAULT 0,
    retry_count     TINYINT NOT NULL DEFAULT 0,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event (topic, event_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS review_eligibility (
    order_id        BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    service_item_id BIGINT NOT NULL,
    paid_time       DATETIME NOT NULL,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
