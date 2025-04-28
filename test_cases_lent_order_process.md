# Lent Order Processing Test Cases

## Test Case 1: Non-serialized (0 SN) Product Edge Cases
### Initial Setup
```sql
-- First create lent order
INSERT INTO lent_id (lent_id, employee_id, shop_name, timestamp, status, note)
VALUES ('LTEST-001', '1', 'Test', CURRENT_TIMESTAMP, 'active', 'Testing non-serialized edge cases');

-- Record initial lent
INSERT INTO lent (box_barcode, product_name, product_barcode, employee_id, shop_name, timestamp, quantity, status, order_id)
VALUES ('0001', 'Aerocell', NULL, '1', 'Test', CURRENT_TIMESTAMP, 8, 'lent', 'LTEST-001');

-- Record in logs
INSERT INTO logs (box_barcode, product_name, product_barcode, operation, quantity, timestamp, order_id)
VALUES ('0001', 'Aerocell', NULL, 'lent', 8, CURRENT_TIMESTAMP, 'LTEST-001');
```

### Test Request
```json
{
  "employeeId": "1",
  "shopName": "Test",
  "orderId": "LTEST-001",
  "note": "Testing non-serialized edge cases",
  "moveToSales": ["0001:5"],
  "returnToStock": ["0001:3"],
  "markAsBroken": [],
  "salesOrderId": "STEST-001",
  "isDirectSales": false
}
```

### Expected Database State After Processing
```sql
-- In lent table:
-- Original record updated
SELECT * FROM lent WHERE order_id = 'LTEST-001' ORDER BY timestamp DESC;
/*
Expected:
- Original record: status='processed', quantity=0
- Sales record: status='lent to sales', quantity=5
- Return record: status='returned', quantity=3
*/

-- In logs table:
SELECT * FROM logs WHERE order_id IN ('LTEST-001', 'STEST-001') ORDER BY timestamp DESC;
/*
Expected:
- moved_from_lent_to_sales: quantity=5, order_id='STEST-001'
- returned: quantity=3, order_id='LTEST-001'
- lent: quantity=8, order_id='LTEST-001' (original)
*/

-- In sales table:
SELECT * FROM sales WHERE order_id = 'STEST-001';
/*
Expected:
- One record with quantity=5
*/

-- In current_stock:
SELECT * FROM current_stock WHERE box_barcode = '0001';
/*
Expected:
- Quantity should be increased by 3 (returned amount)
*/
```

## Test Case 2: Mixed Product Types in Single Order
### Initial Setup
```sql
-- Create lent order
INSERT INTO lent_id (lent_id, employee_id, shop_name, timestamp, status, note)
VALUES ('LTEST-002', '1', 'Test', CURRENT_TIMESTAMP, 'active', 'Testing mixed product types');

-- Record initial lents
INSERT INTO lent (box_barcode, product_name, product_barcode, employee_id, shop_name, timestamp, quantity, status, order_id)
VALUES 
('0001', 'Aerocell', NULL, '1', 'Test', CURRENT_TIMESTAMP, 5, 'lent', 'LTEST-002'),
('0002', 'Amplifier', '2001', '1', 'Test', CURRENT_TIMESTAMP, 1, 'lent', 'LTEST-002'),
('0003', 'Speaker', '3003', '1', 'Test', CURRENT_TIMESTAMP, 1, 'lent', 'LTEST-002'),
('0003', 'Speaker', '3004', '1', 'Test', CURRENT_TIMESTAMP, 1, 'lent', 'LTEST-002');

-- Record in logs
INSERT INTO logs (box_barcode, product_name, product_barcode, operation, quantity, timestamp, order_id)
VALUES 
('0001', 'Aerocell', NULL, 'lent', 5, CURRENT_TIMESTAMP, 'LTEST-002'),
('0002', 'Amplifier', '2001', 'lent', 1, CURRENT_TIMESTAMP, 'LTEST-002'),
('0003', 'Speaker', '3003', 'lent', 1, CURRENT_TIMESTAMP, 'LTEST-002'),
('0003', 'Speaker', '3004', 'lent', 1, CURRENT_TIMESTAMP, 'LTEST-002');
```

### Test Request
```json
{
  "employeeId": "1",
  "shopName": "Test",
  "orderId": "LTEST-002",
  "note": "Testing mixed product types",
  "moveToSales": [
    "0001:3",
    "2001",
    "3003"
  ],
  "returnToStock": [
    "0001:2",
    "3004"
  ],
  "markAsBroken": [],
  "salesOrderId": "STEST-002",
  "isDirectSales": false
}
```

### Expected Database State After Processing
```sql
-- In lent table:
SELECT * FROM lent WHERE order_id = 'LTEST-002' ORDER BY timestamp DESC;
/*
Expected:
- Non-serialized (0001): 
  * Original record: status='processed', quantity=0
  * Sales record: status='lent to sales', quantity=3
  * Return record: status='returned', quantity=2
- Serialized (2001): status='lent to sales'
- Serialized (3003): status='lent to sales'
- Serialized (3004): status='returned'
*/

-- In logs table:
SELECT * FROM logs WHERE order_id IN ('LTEST-002', 'STEST-002') ORDER BY timestamp DESC;
/*
Expected:
- moved_from_lent_to_sales: 0001, quantity=3
- moved_from_lent_to_sales: 2001, quantity=1
- moved_from_lent_to_sales: 3003, quantity=1
- returned: 0001, quantity=2
- returned: 3004, quantity=1
*/

-- In current_stock:
SELECT * FROM current_stock WHERE box_barcode IN ('0001', '0002', '0003');
/*
Expected:
- 0001: Increased by 2
- 0002: No change (moved to sales)
- 0003: Increased by 1 (3004 returned)
*/
```

## Test Case 3: Multiple Operations on Same Product
### Initial Setup
```sql
INSERT INTO lent_id (lent_id, employee_id, shop_name, timestamp, status, note)
VALUES ('LTEST-003', '1', 'Test', CURRENT_TIMESTAMP, 'active', 'Testing multiple operations');

INSERT INTO lent (box_barcode, product_name, product_barcode, employee_id, shop_name, timestamp, quantity, status, order_id)
VALUES ('0002', 'Amplifier', '2001', '1', 'Test', CURRENT_TIMESTAMP, 1, 'lent', 'LTEST-003');
```

### Test Request
```json
{
  "employeeId": "1",
  "shopName": "Test",
  "orderId": "LTEST-003",
  "note": "Testing multiple operations",
  "moveToSales": ["2001"],
  "returnToStock": ["2001"],
  "salesOrderId": "STEST-003",
  "isDirectSales": false
}
```

### Expected Database State After Processing
```sql
-- In lent table:
SELECT * FROM lent WHERE order_id = 'LTEST-003' ORDER BY timestamp DESC;
/*
Expected:
- Only one status change should be processed (first operation)
- Status should be either 'lent to sales' or 'returned' depending on which operation processed first
*/

-- In logs table:
SELECT * FROM logs WHERE order_id IN ('LTEST-003', 'STEST-003') ORDER BY timestamp DESC;
/*
Expected:
- Only one operation log (either moved_from_lent_to_sales or returned)
*/
```

## Test Case 4: Quantity Validation Test
### Initial Setup
```sql
INSERT INTO lent_id (lent_id, employee_id, shop_name, timestamp, status, note)
VALUES ('LTEST-004', '1', 'Test', CURRENT_TIMESTAMP, 'active', 'Testing quantity validation');

INSERT INTO lent (box_barcode, product_name, product_barcode, employee_id, shop_name, timestamp, quantity, status, order_id)
VALUES 
('0001', 'Aerocell', NULL, '1', 'Test', CURRENT_TIMESTAMP, 5, 'lent', 'LTEST-004');
```

### Test Request
```json
{
  "employeeId": "1",
  "shopName": "Test",
  "orderId": "LTEST-004",
  "note": "Testing quantity validation",
  "moveToSales": [
    "0001:10",
    "0002:0",
    "0003:-1"
  ],
  "returnToStock": ["0001:6"],
  "salesOrderId": "STEST-004",
  "isDirectSales": false
}
```

### Expected Database State After Processing
```sql
-- In lent table:
SELECT * FROM lent WHERE order_id = 'LTEST-004';
/*
Expected:
- Should remain unchanged due to invalid quantities
- Status should still be 'lent'
- Original quantity should be preserved
*/

-- In logs table:
SELECT * FROM logs WHERE order_id IN ('LTEST-004', 'STEST-004');
/*
Expected:
- Should only contain original lent operation
- No additional operations should be logged
*/
```

## Test Case 5: Split Pair Testing
### Initial Setup
```sql
INSERT INTO lent_id (lent_id, employee_id, shop_name, timestamp, status, note)
VALUES ('LTEST-005', '1', 'Test', CURRENT_TIMESTAMP, 'active', 'Testing split pairs');

INSERT INTO lent (box_barcode, product_name, product_barcode, employee_id, shop_name, timestamp, quantity, status, order_id)
VALUES 
('0003', 'Speaker', '3003', '1', 'Test', CURRENT_TIMESTAMP, 1, 'lent', 'LTEST-005'),
('0003', 'Speaker', '3004', '1', 'Test', CURRENT_TIMESTAMP, 1, 'lent', 'LTEST-005');
```

### Test Request
```json
{
  "employeeId": "1",
  "shopName": "Test",
  "orderId": "LTEST-005",
  "note": "Testing split pairs",
  "moveToSales": ["3003"],
  "returnToStock": ["3004"],
  "salesOrderId": "STEST-005",
  "isDirectSales": false
}
```

### Expected Database State After Processing
```sql
-- In lent table:
SELECT * FROM lent WHERE order_id = 'LTEST-005' ORDER BY timestamp DESC;
/*
Expected:
- 3003: status='lent to sales'
- 3004: status='returned'
*/

-- In logs table:
SELECT * FROM logs WHERE order_id IN ('LTEST-005', 'STEST-005') ORDER BY timestamp DESC;
/*
Expected:
- moved_from_lent_to_sales: 3003
- returned: 3004
*/

-- In current_stock:
SELECT * FROM current_stock WHERE box_barcode = '0003';
/*
Expected:
- Quantity increased by 1 (3004 returned)
*/
```

## Important Notes
1. **Status Transitions:**
   - lent -> returned
   - lent -> lent to sales
   - lent -> broken
   - No direct transitions between returned/sales/broken

2. **Quantity Validation:**
   - Total processed quantity ≤ original lent quantity
   - No negative quantities
   - No zero quantities

3. **Order Status Updates:**
   - Order status should be 'completed' when all items are processed
   - Order status should remain 'active' if any items are still in 'lent' status

4. **Database Consistency:**
   - Check current_stock quantities match after operations
   - Verify all operations are logged in logs table
   - Ensure sales records have corresponding invoice records
   - Verify lent record status changes are atomic 