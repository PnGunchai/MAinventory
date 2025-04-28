-- Add unique constraint to product_barcode in box_number table
ALTER TABLE box_number ADD CONSTRAINT uk_box_number_product_barcode UNIQUE (product_barcode); 