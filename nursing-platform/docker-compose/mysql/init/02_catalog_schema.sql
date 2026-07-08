 CREATE DATABASE IF NOT EXISTS catalog_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
 
 USE catalog_db;
 
 CREATE TABLE IF NOT EXISTS service_category (
     id              BIGINT NOT NULL COMMENT '分类ID(雪花算法)',
     name            VARCHAR(64) NOT NULL COMMENT '分类名称',
     icon            VARCHAR(256) COMMENT '分类图标URL',
     sort_order      INT DEFAULT 0 COMMENT '排序',
     status          TINYINT DEFAULT 1 COMMENT '0隐藏 1展示',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务分类表';
 
 CREATE TABLE IF NOT EXISTS service_item (
     id              BIGINT NOT NULL COMMENT '服务项目ID(雪花算法)',
     category_id     BIGINT NOT NULL COMMENT '所属分类ID',
     name            VARCHAR(128) NOT NULL COMMENT '服务名称',
     description     TEXT COMMENT '图文详情',
     cover_image     VARCHAR(256) COMMENT '封面图URL',
     status          TINYINT DEFAULT 1 COMMENT '0下架 1上架',
     sort_order      INT DEFAULT 0 COMMENT '排序',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     INDEX idx_category (category_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务项目表';
 
 CREATE TABLE IF NOT EXISTS service_spec (
     id              BIGINT NOT NULL COMMENT '规格ID(雪花算法)',
     service_item_id BIGINT NOT NULL COMMENT '关联服务ID',
     name            VARCHAR(64) NOT NULL COMMENT '规格名称',
     price           DECIMAL(10,2) NOT NULL COMMENT '售价',
     original_price  DECIMAL(10,2) COMMENT '原价',
     duration        INT COMMENT '服务时长(分钟)',
     status          TINYINT DEFAULT 1 COMMENT '0下架 1上架',
     is_deleted      TINYINT DEFAULT 0 COMMENT '0未删 1已删',
     create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     INDEX idx_service_item (service_item_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务规格表';
 
 -- ========== 种子数据 ==========
 -- 服务分类
 INSERT INTO service_category (id, name, icon, sort_order, status)
 VALUES
     (101, '康复护理', 'https://via.placeholder.com/48', 1, 1),
     (102, '健康体检', 'https://via.placeholder.com/48', 2, 1),
     (103, '家政护理', 'https://via.placeholder.com/48', 3, 1),
     (104, '中医理疗', 'https://via.placeholder.com/48', 4, 1);
 
 -- 服务项目
 INSERT INTO service_item (id, category_id, name, description, status, sort_order)
 VALUES
     (201, 101, '上门康复推拿', '专业康复师上门推拿服务，适用于术后恢复、慢性疼痛缓解等场景。', 1, 1),
     (202, 101, '术后康复护理', '针对术后患者提供专业的康复护理服务，含伤口护理、康复指导。', 1, 2),
     (203, 101, '老年康复训练', '针对老年人的定制化康复训练方案，改善行动能力与生活质量。', 1, 3),
     (204, 102, '常规体检套餐', '上门体检服务，含血压血糖检测、心电图等基础项目。', 1, 1),
     (205, 102, '全身体检筛查', '全面体检涵盖生化检验、影像检查等，结果由专业医生解读。', 1, 2),
     (206, 103, '老人陪护服务', '专业护工/护理员上门陪护，含生活照料、心理陪伴。', 1, 1),
     (207, 103, '居家护理服务', '基础护理：翻身拍背、口腔护理、排泄护理、压疮预防等。', 1, 2),
     (208, 104, '中医推拿理疗', '资深中医师上门推拿理疗，缓解颈肩腰腿痛。', 1, 1),
     (209, 104, '艾灸养生调理', '传统艾灸调理，适用于寒湿体质、关节疼痛等。', 1, 2);
 
 -- 服务规格与价格
 INSERT INTO service_spec (id, service_item_id, name, price, original_price, duration)
 VALUES
     (301, 201, '单次体验', 198.00, 298.00, 60),
     (302, 201, '5次套餐', 880.00, 1490.00, 60),
     (303, 202, '单次护理', 258.00, 358.00, 90),
     (304, 203, '单次训练', 168.00, 238.00, 45),
     (305, 203, '10次套餐', 1480.00, 2380.00, 45),
     (306, 204, '基础套餐', 99.00, 199.00, 30),
     (307, 204, '升级套餐', 199.00, 399.00, 60),
     (308, 205, '全面筛查', 599.00, 999.00, 120),
     (309, 206, '日常陪护(4小时)', 128.00, 168.00, 240),
     (310, 206, '全天陪护(8小时)', 228.00, 328.00, 480),
     (311, 207, '基础护理(2小时)', 88.00, 128.00, 120),
     (312, 207, '全面护理(4小时)', 168.00, 248.00, 240),
     (313, 208, '全身推拿(60分钟)', 238.00, 338.00, 60),
     (314, 208, '局部推拿(30分钟)', 128.00, 178.00, 30),
     (315, 209, '艾灸调理(45分钟)', 168.00, 238.00, 45),
     (316, 209, '艾灸套餐(5次)', 720.00, 1190.00, 45);

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
