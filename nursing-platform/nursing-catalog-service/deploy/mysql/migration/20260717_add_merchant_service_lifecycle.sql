USE catalog_db;
SET NAMES utf8mb4;

-- db-bootstrap runs this file on every start for existing development volumes.
-- Guard each DDL operation so repeated starts remain safe.
SET @has_owner_user_id = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'catalog_db' AND table_name = 'service_item' AND column_name = 'owner_user_id');
SET @owner_user_id_sql = IF(@has_owner_user_id = 0,
    'ALTER TABLE catalog_db.service_item ADD COLUMN owner_user_id BIGINT DEFAULT NULL COMMENT ''商户成员用户ID，NULL表示平台目录'' AFTER category_id', 'SELECT 1');
PREPARE owner_user_id_upgrade FROM @owner_user_id_sql;
EXECUTE owner_user_id_upgrade;
DEALLOCATE PREPARE owner_user_id_upgrade;

SET @has_audit_status = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'catalog_db' AND table_name = 'service_item' AND column_name = 'audit_status');
SET @audit_status_sql = IF(@has_audit_status = 0,
    'ALTER TABLE catalog_db.service_item ADD COLUMN audit_status VARCHAR(32) NOT NULL DEFAULT ''APPROVED'' COMMENT ''DRAFT/APPROVED'' AFTER status', 'SELECT 1');
PREPARE audit_status_upgrade FROM @audit_status_sql;
EXECUTE audit_status_upgrade;
DEALLOCATE PREPARE audit_status_upgrade;

SET @has_publish_status = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'catalog_db' AND table_name = 'service_item' AND column_name = 'publish_status');
SET @publish_status_sql = IF(@has_publish_status = 0,
    'ALTER TABLE catalog_db.service_item ADD COLUMN publish_status VARCHAR(32) NOT NULL DEFAULT ''PUBLISHED'' COMMENT ''OFFLINE/PUBLISHED'' AFTER audit_status', 'SELECT 1');
PREPARE publish_status_upgrade FROM @publish_status_sql;
EXECUTE publish_status_upgrade;
DEALLOCATE PREPARE publish_status_upgrade;

SET @has_version = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'catalog_db' AND table_name = 'service_item' AND column_name = 'version');
SET @version_sql = IF(@has_version = 0,
    'ALTER TABLE catalog_db.service_item ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT ''服务版本'' AFTER publish_status', 'SELECT 1');
PREPARE version_upgrade FROM @version_sql;
EXECUTE version_upgrade;
DEALLOCATE PREPARE version_upgrade;

SET @has_owner_index = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'catalog_db' AND table_name = 'service_item' AND index_name = 'idx_item_owner_update');
SET @owner_index_sql = IF(@has_owner_index = 0,
    'ALTER TABLE catalog_db.service_item ADD INDEX idx_item_owner_update (owner_user_id, is_deleted, update_time, id)', 'SELECT 1');
PREPARE owner_index_upgrade FROM @owner_index_sql;
EXECUTE owner_index_upgrade;
DEALLOCATE PREPARE owner_index_upgrade;
