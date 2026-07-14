CREATE TABLE payment_intent (
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

CREATE TABLE refund_record (
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

ALTER TABLE payment_record
    ADD UNIQUE KEY uk_payment_notify_id (notify_id),
    ADD UNIQUE KEY uk_payment_trade_no (trade_no);
