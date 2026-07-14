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
nacos_db_username=$(sql_quote "${NURSING_NACOS_DB_USERNAME}")
nacos_db_password=$(sql_quote "${NURSING_NACOS_DB_PASSWORD}")
order_migration_username=$(sql_quote "${NURSING_ORDER_MIGRATION_DB_USERNAME}")
order_migration_password=$(sql_quote "${NURSING_ORDER_MIGRATION_DB_PASSWORD}")
feedback_migration_username=$(sql_quote "${NURSING_FEEDBACK_MIGRATION_DB_USERNAME}")
feedback_migration_password=$(sql_quote "${NURSING_FEEDBACK_MIGRATION_DB_PASSWORD}")

mysql -h mysql -uroot --password="${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS catalog_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS feedback_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '${user_db_username}'@'%' IDENTIFIED BY '${user_db_password}';
CREATE USER IF NOT EXISTS '${catalog_db_username}'@'%' IDENTIFIED BY '${catalog_db_password}';
CREATE USER IF NOT EXISTS '${order_db_username}'@'%' IDENTIFIED BY '${order_db_password}';
CREATE USER IF NOT EXISTS '${feedback_db_username}'@'%' IDENTIFIED BY '${feedback_db_password}';
CREATE USER IF NOT EXISTS '${nacos_db_username}'@'%' IDENTIFIED BY '${nacos_db_password}';
CREATE USER IF NOT EXISTS '${order_migration_username}'@'%' IDENTIFIED BY '${order_migration_password}';
CREATE USER IF NOT EXISTS '${feedback_migration_username}'@'%' IDENTIFIED BY '${feedback_migration_password}';

GRANT SELECT, INSERT, UPDATE, DELETE ON user_db.* TO '${user_db_username}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON catalog_db.* TO '${catalog_db_username}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON order_db.* TO '${order_db_username}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON feedback_db.* TO '${feedback_db_username}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON nacos_config.* TO '${nacos_db_username}'@'%';
GRANT ALTER, CREATE, DELETE, DROP, INDEX, INSERT, REFERENCES, SELECT, TRIGGER, UPDATE ON order_db.* TO '${order_migration_username}'@'%';
GRANT ALTER, CREATE, DELETE, DROP, INDEX, INSERT, REFERENCES, SELECT, TRIGGER, UPDATE ON feedback_db.* TO '${feedback_migration_username}'@'%';
DROP USER IF EXISTS 'nursing'@'%';
FLUSH PRIVILEGES;
SQL
