CREATE DATABASE IF NOT EXISTS operations_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE operations_db;
CREATE TABLE IF NOT EXISTS caregiver_application (
 id BIGINT NOT NULL, user_id BIGINT NOT NULL, real_name VARCHAR(32) NOT NULL, phone VARCHAR(20) NOT NULL,
 service_district VARCHAR(64) NOT NULL, skills VARCHAR(256) NOT NULL, status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2拒绝',
 review_remark VARCHAR(256), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_caregiver_application_user(user_id), INDEX idx_caregiver_application_status(status,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理员申请表';

CREATE TABLE IF NOT EXISTS merchant_member (
 id BIGINT NOT NULL, merchant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, position VARCHAR(32) NOT NULL,
 status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_merchant_member_user(user_id), KEY idx_merchant_member_merchant(merchant_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户成员与租户归属';

CREATE TABLE IF NOT EXISTS merchant_caregiver (
 id BIGINT NOT NULL, merchant_id BIGINT NOT NULL, caregiver_user_id BIGINT NOT NULL,
 status TINYINT NOT NULL DEFAULT 1 COMMENT '1有效 0暂停 2解除', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_merchant_caregiver(merchant_id,caregiver_user_id), KEY idx_merchant_caregiver_user(caregiver_user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户护理人员合作关系';

CREATE TABLE IF NOT EXISTS caregiver_profile (
 caregiver_id BIGINT NOT NULL, user_id BIGINT NOT NULL, real_name VARCHAR(32) NOT NULL,
 service_areas VARCHAR(256) NOT NULL, skills VARCHAR(512) NOT NULL, audit_status TINYINT NOT NULL DEFAULT 0,
 max_daily_orders INT NOT NULL DEFAULT 4, rating DECIMAL(3,2) NOT NULL DEFAULT 0,
 completed_orders INT NOT NULL DEFAULT 0, status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
 PRIMARY KEY(caregiver_id), UNIQUE KEY uk_caregiver_profile_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理人员业务档案';

CREATE TABLE IF NOT EXISTS service_assignment (
 id BIGINT NOT NULL, order_id BIGINT NOT NULL, active_order_id BIGINT DEFAULT NULL,
 merchant_id BIGINT NOT NULL, caregiver_user_id BIGINT NOT NULL,
 status TINYINT NOT NULL COMMENT '0待接单 1已接单 2已拒绝 3已取消', remark VARCHAR(256),
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, accepted_time DATETIME DEFAULT NULL,
 rejected_time DATETIME DEFAULT NULL, update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_service_assignment_active_order(active_order_id),
 KEY idx_service_assignment_caregiver(caregiver_user_id,status), KEY idx_service_assignment_merchant(merchant_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单派单记录';

CREATE TABLE IF NOT EXISTS service_action (
 id BIGINT NOT NULL, assignment_id BIGINT NOT NULL, action VARCHAR(16) NOT NULL,
 operator_user_id BIGINT NOT NULL, remark VARCHAR(256), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_service_action_assignment_action(assignment_id,action),
 KEY idx_service_action_assignment(assignment_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变履约动作记录';

CREATE TABLE IF NOT EXISTS operation_idempotency (
 id BIGINT NOT NULL, operation VARCHAR(64) NOT NULL, actor_user_id BIGINT NOT NULL,
 idempotent_key VARCHAR(128) NOT NULL, request_hash VARCHAR(64) NOT NULL,
 assignment_id BIGINT DEFAULT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 completed_time DATETIME DEFAULT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_operation_actor_key(operation,actor_user_id,idempotent_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='履约写操作幂等记录';
