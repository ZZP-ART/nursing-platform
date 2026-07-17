-- Repair legacy UTF-8 text that was decoded as a single-byte charset and then
-- stored again as UTF-8. The C2 byte marker prevents this from touching normal
-- Chinese text, which does not contain that byte sequence.
USE catalog_db;
SET NAMES utf8mb4;

UPDATE service_category
SET name = CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4)
WHERE HEX(name) LIKE '%C2%';

UPDATE service_item
SET name = CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4)
WHERE HEX(name) LIKE '%C2%';

UPDATE service_item
SET description = CONVERT(BINARY CONVERT(description USING latin1) USING utf8mb4)
WHERE description IS NOT NULL
  AND HEX(description) LIKE '%C2%';

UPDATE service_spec
SET name = CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4)
WHERE HEX(name) LIKE '%C2%';
