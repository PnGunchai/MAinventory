-- Add note column to invoice table if it does not exist
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS note TEXT;

-- Add a comment to explain the purpose of the note column
COMMENT ON COLUMN invoice.note IS 'User-facing note for this invoice/order, editable by staff or user.'; 