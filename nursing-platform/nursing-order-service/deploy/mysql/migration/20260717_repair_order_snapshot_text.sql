-- Repair historical UTF-8 values that were decoded as a single-byte charset
-- before being saved. The C2 marker avoids rewriting normal Chinese text.
USE order_db;
SET NAMES utf8mb4;

UPDATE order_header
SET service_item_name = CONVERT(BINARY CONVERT(service_item_name USING latin1) USING utf8mb4)
WHERE service_item_name IS NOT NULL AND HEX(service_item_name) LIKE '%C2%';

UPDATE order_header
SET category_name = CONVERT(BINARY CONVERT(category_name USING latin1) USING utf8mb4)
WHERE category_name IS NOT NULL AND HEX(category_name) LIKE '%C2%';

UPDATE order_header
SET spec_name = CONVERT(BINARY CONVERT(spec_name USING latin1) USING utf8mb4)
WHERE spec_name IS NOT NULL AND HEX(spec_name) LIKE '%C2%';

UPDATE order_header
SET receiver_name = CONVERT(BINARY CONVERT(receiver_name USING latin1) USING utf8mb4)
WHERE receiver_name IS NOT NULL AND HEX(receiver_name) LIKE '%C2%';

UPDATE order_header
SET address_detail = CONVERT(BINARY CONVERT(address_detail USING latin1) USING utf8mb4)
WHERE address_detail IS NOT NULL AND HEX(address_detail) LIKE '%C2%';

UPDATE order_header
SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4)
WHERE remark IS NOT NULL AND HEX(remark) LIKE '%C2%';

UPDATE order_header
SET cancel_reason = CONVERT(BINARY CONVERT(cancel_reason USING latin1) USING utf8mb4)
WHERE cancel_reason IS NOT NULL AND HEX(cancel_reason) LIKE '%C2%';

UPDATE order_operation_log
SET remark = CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4)
WHERE remark IS NOT NULL AND HEX(remark) LIKE '%C2%';
