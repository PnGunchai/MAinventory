-- Migration to simplify lent_id table structure to match invoice table pattern

-- First, backup existing data if needed
CREATE TABLE IF NOT EXISTS lent_id_backup AS SELECT * FROM lent_id;

-- Drop columns that are redundant or product-specific
ALTER TABLE lent_id
    DROP COLUMN IF EXISTS box_barcode,
    DROP COLUMN IF EXISTS product_barcode;

-- Rename primary key column to match pattern (if needed)
-- No need to rename lent_id as it already matches the pattern of invoice table's invoice column

-- Make sure we have the essential columns with correct constraints
ALTER TABLE lent_id
    ALTER COLUMN lent_id SET NOT NULL,
    ALTER COLUMN employee_id SET NOT NULL,
    ALTER COLUMN shop_name SET NOT NULL,
    ALTER COLUMN timestamp SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

-- Add comment to explain the purpose of the table
COMMENT ON TABLE lent_id IS 'Header records for lending operations, similar to invoice table for sales'; 