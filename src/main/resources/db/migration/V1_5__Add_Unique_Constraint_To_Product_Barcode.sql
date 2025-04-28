-- Add unique constraint to product_barcode in box_number table
-- First, clean up any duplicate entries
DELETE FROM box_number 
WHERE id IN (
    SELECT id 
    FROM (
        SELECT id, 
               ROW_NUMBER() OVER (PARTITION BY product_barcode ORDER BY last_updated DESC) as row_num 
        FROM box_number 
        WHERE product_barcode IS NOT NULL AND product_barcode != ''
    ) t 
    WHERE row_num > 1
);

-- Now add the constraint
ALTER TABLE box_number ADD CONSTRAINT unique_product_barcode_box UNIQUE (product_barcode);

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