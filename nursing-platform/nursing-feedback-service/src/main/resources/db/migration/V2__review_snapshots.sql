ALTER TABLE review
    ADD COLUMN service_item_name VARCHAR(128) NULL AFTER service_item_id,
    ADD COLUMN spec_name VARCHAR(64) NULL AFTER service_item_name,
    ADD INDEX idx_review_snapshot_missing (service_item_name, spec_name, id);
