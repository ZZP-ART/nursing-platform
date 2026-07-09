CREATE USER IF NOT EXISTS 'nursing'@'%' IDENTIFIED BY 'nursing_dev_password_change_me';
GRANT ALL PRIVILEGES ON user_db.* TO 'nursing'@'%';
GRANT ALL PRIVILEGES ON catalog_db.* TO 'nursing'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'nursing'@'%';
GRANT ALL PRIVILEGES ON feedback_db.* TO 'nursing'@'%';
GRANT ALL PRIVILEGES ON nacos_config.* TO 'nursing'@'%';
FLUSH PRIVILEGES;
