BEGIN;

-- Delete in reverse order of dependencies
DELETE FROM bulk_logs;
DELETE FROM box_number;
DELETE FROM sales;
DELETE FROM lent;
DELETE FROM broken;
DELETE FROM logs;
DELETE FROM current_stock;
DELETE FROM in_stock;
DELETE FROM invoice;
DELETE FROM lent_id;
DELETE FROM broken_id;
DELETE FROM barcode_status;
DELETE FROM product_catalog;
-- Not deleting from users table to preserve login credentials

COMMIT; 