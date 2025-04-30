-- Table 1: product_catalog
CREATE TABLE IF NOT EXISTS product_catalog (
    box_barcode VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    number_sn SMALLINT NOT NULL CHECK (number_sn IN (0, 1, 2)),
    UNIQUE (box_barcode, product_name)
);

-- Table: lent_id
CREATE TABLE IF NOT EXISTS lent_id (
    lent_id VARCHAR(50) PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    shop_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    note TEXT,
    status VARCHAR(20) NOT NULL,
    box_barcode VARCHAR(255),
    product_barcode VARCHAR(255)
);

-- Table: invoice
CREATE TABLE IF NOT EXISTS invoice (
    invoice_id SERIAL PRIMARY KEY,
    invoice VARCHAR(50) NOT NULL,
    employee_id VARCHAR(10) NOT NULL,
    shop_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMPTZ,
    edit_count INTEGER DEFAULT 0,
    edit_history TEXT,
    note TEXT
);

-- Table: logs
CREATE TABLE IF NOT EXISTS logs (
    logs_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_barcode VARCHAR(50),
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('add', 'remove', 'sales', 'lent', 'returned', 'broken')),
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    note TEXT,
    box_number INTEGER,
    order_id VARCHAR(50),
    quantity INTEGER DEFAULT 1,
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table: current_stock
CREATE TABLE IF NOT EXISTS current_stock (
    stock_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    box_number INTEGER,
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table: sales
CREATE TABLE IF NOT EXISTS sales (
    sales_id SERIAL PRIMARY KEY,
    invoice_id INTEGER NOT NULL,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_barcode VARCHAR(50),
    employee_id VARCHAR(10) NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    box_number INTEGER,
    note TEXT,
    shop_name VARCHAR(100) NOT NULL,
    quantity INTEGER DEFAULT 1,
    order_id VARCHAR(50),
    isdirectsales BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name),
    FOREIGN KEY (invoice_id) REFERENCES invoice(invoice_id)
);

-- Table: lend
CREATE TABLE IF NOT EXISTS lend (
    lend_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_barcode VARCHAR(50),
    employee_id VARCHAR(10) NOT NULL,
    shop_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    box_number INTEGER,
    note TEXT,
    status VARCHAR(20) DEFAULT 'lent',
    quantity INTEGER DEFAULT 1,
    order_id VARCHAR(50),
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table: broken
CREATE TABLE IF NOT EXISTS broken (
    broken_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_barcode VARCHAR(50),
    condition VARCHAR(255) NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    box_number INTEGER,
    note TEXT,
    quantity INTEGER DEFAULT 1,
    order_id VARCHAR(50),
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table: broken_id
CREATE TABLE IF NOT EXISTS broken_id (
    broken_id VARCHAR(50) PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL,
    note TEXT
);

-- Table: bulk_logs
CREATE TABLE IF NOT EXISTS bulk_logs (
    bulk_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('add', 'remove', 'sales', 'lent', 'returned', 'broken')),
    date DATE NOT NULL,
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Table: box_number
CREATE TABLE IF NOT EXISTS box_number (
    box_id SERIAL PRIMARY KEY,
    box_barcode VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    box_number INTEGER NOT NULL,
    product_barcode VARCHAR(50),
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (box_barcode, product_name) REFERENCES product_catalog(box_barcode, product_name)
);

-- Add trigger to update last_updated timestamp
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Bangkok';
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Add triggers
DROP TRIGGER IF EXISTS update_current_stock_last_updated ON current_stock;
CREATE TRIGGER update_current_stock_last_updated
    BEFORE UPDATE ON current_stock
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

DROP TRIGGER IF EXISTS update_box_number_last_updated ON box_number;
CREATE TRIGGER update_box_number_last_updated
    BEFORE UPDATE ON box_number
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column(); 