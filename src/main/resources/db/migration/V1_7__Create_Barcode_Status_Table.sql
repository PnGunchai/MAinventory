-- Create a table to track barcode status
CREATE TABLE barcode_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_barcode VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_barcode_status_product_barcode UNIQUE (product_barcode)
); 