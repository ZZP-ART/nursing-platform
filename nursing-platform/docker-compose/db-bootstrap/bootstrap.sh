#!/bin/sh
set -eu

# Compose credentials are external input. Quote SQL literals before interpolating
# them so valid passwords containing apostrophes neither break bootstrap nor alter
# the privilege statements.
sql_quote() {
    printf '%s' "$1" | sed "s/'/''/g"
}

user_db_username=$(sql_quote "${NURSING_USER_DB_USERNAME}")
user_db_password=$(sql_quote "${NURSING_USER_DB_PASSWORD}")
catalog_db_username=$(sql_quote "${NURSING_CATALOG_DB_USERNAME}")
catalog_db_password=$(sql_quote "${NURSING_CATALOG_DB_PASSWORD}")
order_db_username=$(sql_quote "${NURSING_ORDER_DB_USERNAME}")
order_db_password=$(sql_quote "${NURSING_ORDER_DB_PASSWORD}")
feedback_db_username=$(sql_quote "${NURSING_FEEDBACK_DB_USERNAME}")
feedback_db_password=$(sql_quote "${NURSING_FEEDBACK_DB_PASSWORD}")
operations_db_username=$(sql_quote "${NURSING_OPERATIONS_DB_USERNAME}")
operations_db_password=$(sql_quote "${NURSING_OPERATIONS_DB_PASSWORD}")
nacos_db_username=$(sql_quote "${NURSING_NACOS_DB_USERNAME}")
nacos_db_password=$(sql_quote "${NURSING_NACOS_DB_PASSWORD}")

mysql -h mysql -uroot --password="${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS catalog_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS feedback_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS operations_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Existing developer volumes can predate the authorization-version field used
-- by the current login and seed-data paths. This works on older MySQL 8 builds.
SET @has_authorization_version = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'user_db' AND table_name = 'user' AND column_name = 'authorization_version'
);
SET @authorization_version_sql = IF(@has_authorization_version = 0,
    'ALTER TABLE user_db.user ADD COLUMN authorization_version INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE authorization_version_upgrade FROM @authorization_version_sql;
EXECUTE authorization_version_upgrade;
DEALLOCATE PREPARE authorization_version_upgrade;

CREATE USER IF NOT EXISTS '${user_db_username}'@'%' IDENTIFIED BY '${user_db_password}';
CREATE USER IF NOT EXISTS '${catalog_db_username}'@'%' IDENTIFIED BY '${catalog_db_password}';
CREATE USER IF NOT EXISTS '${order_db_username}'@'%' IDENTIFIED BY '${order_db_password}';
CREATE USER IF NOT EXISTS '${feedback_db_username}'@'%' IDENTIFIED BY '${feedback_db_password}';
CREATE USER IF NOT EXISTS '${operations_db_username}'@'%' IDENTIFIED BY '${operations_db_password}';
CREATE USER IF NOT EXISTS '${nacos_db_username}'@'%' IDENTIFIED BY '${nacos_db_password}';
ALTER USER '${user_db_username}'@'%' IDENTIFIED BY '${user_db_password}';
ALTER USER '${catalog_db_username}'@'%' IDENTIFIED BY '${catalog_db_password}';
ALTER USER '${order_db_username}'@'%' IDENTIFIED BY '${order_db_password}';
ALTER USER '${feedback_db_username}'@'%' IDENTIFIED BY '${feedback_db_password}';
ALTER USER '${operations_db_username}'@'%' IDENTIFIED BY '${operations_db_password}';
ALTER USER '${nacos_db_username}'@'%' IDENTIFIED BY '${nacos_db_password}';

GRANT SELECT, INSERT, UPDATE, DELETE ON user_db.* TO '${user_db_username}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON catalog_db.* TO '${catalog_db_username}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON order_db.* TO '${order_db_username}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON feedback_db.* TO '${feedback_db_username}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON operations_db.* TO '${operations_db_username}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON nacos_config.* TO '${nacos_db_username}'@'%';
DROP USER IF EXISTS 'nursing'@'%';
FLUSH PRIVILEGES;
SQL

# MySQL only consumes docker-entrypoint-initdb.d on an empty data volume. Reapply
# idempotent schemas on every bootstrap so existing developer volumes receive
# tables introduced after their initial creation.
mysql -h mysql -uroot --password="${MYSQL_ROOT_PASSWORD}" < /user-schema.sql
mysql -h mysql -uroot --password="${MYSQL_ROOT_PASSWORD}" < /order-schema.sql
mysql -h mysql -uroot --password="${MYSQL_ROOT_PASSWORD}" <<SQL
SET @has_order_merchant_id = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'order_db' AND table_name = 'order_header' AND column_name = 'merchant_id'
);
SET @order_merchant_id_upgrade = IF(@has_order_merchant_id = 0,
    'ALTER TABLE order_db.order_header ADD COLUMN merchant_id BIGINT NOT NULL DEFAULT 20001 AFTER user_id, ADD INDEX idx_merchant_id (merchant_id, status)',
    'SELECT 1'
);
PREPARE order_merchant_id_upgrade FROM @order_merchant_id_upgrade;
EXECUTE order_merchant_id_upgrade;
DEALLOCATE PREPARE order_merchant_id_upgrade;
SQL
mysql -h mysql -uroot --password="${MYSQL_ROOT_PASSWORD}" < /operations-schema.sql
mysql -h mysql -uroot --password="${MYSQL_ROOT_PASSWORD}" < /catalog-text-repair.sql
mysql -h mysql -uroot --password="${MYSQL_ROOT_PASSWORD}" < /merchant-service-lifecycle.sql
mysql -h mysql -uroot --password="${MYSQL_ROOT_PASSWORD}" < /order-text-repair.sql
