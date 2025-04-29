-- Create the database
CREATE DATABASE inventory_management;
\c inventory_management;

-- Table 1: product_catalog
CREATE TABLE IF NOT EXISTS product_catalog (
    box_barcode VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    number_sn SMALLINT NOT NULL CHECK (number_sn IN (0, 1, 2)),
    UNIQUE (box_barcode, product_name)
);

-- Table 7: invoice (moved up to resolve dependency)
CREATE TABLE IF NOT EXISTS invoice (
    invoice_id SERIAL PRIMARY KEY,
    invoice VARCHAR(50) NOT NULL,
    employee_id VARCHAR(10) NOT NULL CHECK (employee_id BETWEEN '01' AND '10'),
    shop_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table 2: logs
CREATE TABLE IF NOT EXISTS logs (
    logs_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_barcode VARCHAR(50),
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('add', 'remove', 'sales', 'lent', 'returned', 'broken')),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table 3: current_stock
CREATE TABLE IF NOT EXISTS current_stock (
    stock_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table 4: sales
CREATE TABLE IF NOT EXISTS sales (
    sales_id SERIAL PRIMARY KEY,
    invoice_id INTEGER NOT NULL,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_barcode VARCHAR(50),
    employee_id VARCHAR(10) NOT NULL CHECK (employee_id BETWEEN '01' AND '10'),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    box_number INTEGER,
    note TEXT,
    shop_name VARCHAR(100) NOT NULL,
    quantity INTEGER DEFAULT 1,
    order_id VARCHAR(50),
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name),
    FOREIGN KEY (invoice_id) REFERENCES invoice(invoice_id)
);

-- Table 5: lend
CREATE TABLE IF NOT EXISTS lend (
    lend_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_barcode VARCHAR(50),
    employee_id VARCHAR(10) NOT NULL CHECK (employee_id BETWEEN '01' AND '10'),
    shop_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    box_number INTEGER,
    note TEXT,
    status VARCHAR(20) DEFAULT 'lent',
    quantity INTEGER DEFAULT 1,
    order_id VARCHAR(50),
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table 6: broken
CREATE TABLE IF NOT EXISTS broken (
    broken_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_barcode VARCHAR(50),
    condition VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    box_number INTEGER,
    note TEXT,
    quantity INTEGER DEFAULT 1,
    order_id VARCHAR(50),
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table 8: bulk_logs
CREATE TABLE IF NOT EXISTS bulk_logs (
    bulk_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('add', 'remove', 'sales', 'lent', 'returned', 'broken')),
    date DATE NOT NULL,
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table 9: box_number
CREATE TABLE IF NOT EXISTS box_number (
    box_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    box_number INTEGER NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Add trigger to update last_updated timestamp in current_stock
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_current_stock_last_updated
BEFORE UPDATE ON current_stock
FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();

-- Add trigger to update last_updated timestamp in box_number
CREATE TRIGGER update_box_number_last_updated
BEFORE UPDATE ON box_number
FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();

-- Add comments to explain table purposes
COMMENT ON TABLE product_catalog IS 'Catalog of products with their box barcodes and number of serial numbers';
COMMENT ON TABLE logs IS 'Log of all operations (add, remove, sales, lent, returned, broken)';
COMMENT ON TABLE current_stock IS 'Current inventory of products in stock';
COMMENT ON TABLE sales IS 'Record of products sold';
COMMENT ON TABLE lend IS 'Record of products lent out';
COMMENT ON TABLE broken IS 'Record of broken products';
COMMENT ON TABLE invoice IS 'Invoice information for sales';
COMMENT ON TABLE bulk_logs IS 'Daily summary of operations';
COMMENT ON TABLE box_number IS 'Auto-running box numbers for products'; 