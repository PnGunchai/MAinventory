-- Add note field to logs table
ALTER TABLE logs ADD COLUMN IF NOT EXISTS note TEXT; 