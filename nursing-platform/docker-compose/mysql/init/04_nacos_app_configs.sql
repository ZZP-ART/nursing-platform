USE nacos_config;

SET @content = 'logging:
  level:
    org.springframework.cloud.gateway: INFO
    com.nursing: INFO
';
INSERT INTO config_info (data_id, group_id, content, md5, type, tenant_id)
VALUES ('nursing-gateway-dev.yaml', 'NURSING', @content, MD5(@content), 'yaml', '')
ON DUPLICATE KEY UPDATE content = @content, md5 = MD5(@content), type = 'yaml', gmt_modified = CURRENT_TIMESTAMP;

SET @content = 'nursing:
  jwt:
    expire-seconds: 604800
  sms:
    provider: aliyun
    rate-limit-seconds: 60
    phone-daily-limit: 10
    ip-hourly-limit: 60
    ip-daily-limit: 300
    verify-max-attempts: 5
    expire-seconds: 300
    aliyun:
      endpoint: dysmsapi.aliyuncs.com
      connect-timeout-millis: 3000
      read-timeout-millis: 5000
  file:
    upload-dir: uploads
    public-base-url: http://localhost:8080/uploads
  snowflake:
    worker-id: 1
    datacenter-id: 1
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB
logging:
  level:
    com.nursing: DEBUG
';
INSERT INTO config_info (data_id, group_id, content, md5, type, tenant_id)
VALUES ('nursing-user-service-dev.yaml', 'NURSING', @content, MD5(@content), 'yaml', '')
ON DUPLICATE KEY UPDATE content = @content, md5 = MD5(@content), type = 'yaml', gmt_modified = CURRENT_TIMESTAMP;

SET @content = 'logging:
  level:
    com.nursing: INFO
nursing:
  snowflake:
    worker-id: 2
    datacenter-id: 1
';
INSERT INTO config_info (data_id, group_id, content, md5, type, tenant_id)
VALUES ('nursing-catalog-service-dev.yaml', 'NURSING', @content, MD5(@content), 'yaml', '')
ON DUPLICATE KEY UPDATE content = @content, md5 = MD5(@content), type = 'yaml', gmt_modified = CURRENT_TIMESTAMP;

SET @content = 'nursing:
  order:
    payment-timeout-minutes: 30
    timeout-scan-fixed-delay: 60000
  kafka:
    order-events-topic: order_events
    outbox-fixed-delay: 5000
    outbox-batch-size: 50
';
INSERT INTO config_info (data_id, group_id, content, md5, type, tenant_id)
VALUES ('nursing-order-service-dev.yaml', 'NURSING', @content, MD5(@content), 'yaml', '')
ON DUPLICATE KEY UPDATE content = @content, md5 = MD5(@content), type = 'yaml', gmt_modified = CURRENT_TIMESTAMP;

SET @content = 'logging:
  level:
    com.nursing: INFO
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 5000
            read-timeout: 5000
';
INSERT INTO config_info (data_id, group_id, content, md5, type, tenant_id)
VALUES ('nursing-feedback-service-dev.yaml', 'NURSING', @content, MD5(@content), 'yaml', '')
ON DUPLICATE KEY UPDATE content = @content, md5 = MD5(@content), type = 'yaml', gmt_modified = CURRENT_TIMESTAMP;
