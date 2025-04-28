-- Drop foreign key constraints first to avoid dependency issues
ALTER TABLE IF EXISTS sales_order_item
DROP CONSTRAINT IF EXISTS fk_sales_order_item_sales_order;

-- Drop the tables
DROP TABLE IF EXISTS sales_order_item;
DROP TABLE IF EXISTS sales_order;

-- Add a comment explaining the migration
COMMENT ON SCHEMA public IS 'Removed unused sales_order and sales_order_item tables as they were redundant with the existing sales table implementation.'; 