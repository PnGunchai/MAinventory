-- Add unique constraint to product_barcode in box_number table
ALTER TABLE box_number ADD CONSTRAINT unique_product_barcode UNIQUE (product_barcode);

-- Add unique constraint to product_barcode in logs table
-- First, we need to clean up any duplicate entries
DELETE FROM logs 
WHERE logs_id IN (
    SELECT logs_id 
    FROM (
        SELECT logs_id, 
               ROW_NUMBER() OVER (PARTITION BY product_barcode ORDER BY timestamp DESC) as row_num 
        FROM logs 
        WHERE product_barcode IS NOT NULL AND product_barcode != ''
    ) t 
    WHERE row_num > 1
);

-- Now add the constraint
ALTER TABLE logs ADD CONSTRAINT unique_product_barcode_logs UNIQUE (product_barcode);

-- Add unique constraint to product_barcode in current_stock table
-- First, clean up any duplicate entries
DELETE FROM current_stock 
WHERE stock_id IN (
    SELECT stock_id 
    FROM (
        SELECT stock_id, 
               ROW_NUMBER() OVER (PARTITION BY product_barcode ORDER BY last_updated DESC) as row_num 
        FROM current_stock 
        WHERE product_barcode IS NOT NULL AND product_barcode != ''
    ) t 
    WHERE row_num > 1
);

-- Now add the constraint
ALTER TABLE current_stock ADD CONSTRAINT unique_product_barcode_stock UNIQUE (product_barcode); 