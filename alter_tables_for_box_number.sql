-- Add box_number field to current_stock table
ALTER TABLE current_stock ADD COLUMN box_number INT;

-- Add box_number field to logs table
ALTER TABLE logs ADD COLUMN box_number INT;

-- Add box_number field to sales table
ALTER TABLE sales ADD COLUMN box_number INT;

-- Add box_number field to lend table
ALTER TABLE lend ADD COLUMN box_number INT;

-- Add box_number field to broken table
ALTER TABLE broken ADD COLUMN box_number INT;

-- Make shop_name non-nullable in sales table
ALTER TABLE sales ALTER COLUMN shop_name SET NOT NULL;

-- Add box_barcode and product_barcode columns to lent_id table
ALTER TABLE lent_id ADD COLUMN box_barcode VARCHAR(255);
ALTER TABLE lent_id ADD COLUMN product_barcode VARCHAR(255);

-- Add isdirectsales column to sales table
ALTER TABLE sales ADD COLUMN isdirectsales BOOLEAN DEFAULT TRUE; 