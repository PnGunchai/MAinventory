-- Create lent_id table
CREATE TABLE lent_id (
    lent_id VARCHAR(50) PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    shop_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    note TEXT,
    status VARCHAR(20) NOT NULL
);

-- Create broken_id table
CREATE TABLE broken_id (
    broken_id VARCHAR(50) PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    note TEXT
);

-- Add order_id column to logs table if not already exists
ALTER TABLE logs ADD COLUMN IF NOT EXISTS order_id VARCHAR(50); 