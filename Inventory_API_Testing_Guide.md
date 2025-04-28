# Inventory Management System API Testing Guide

This document provides a structured approach for testing the key API endpoints in the Inventory Management System, organized by functional areas and common workflows.

---

## 📋 Test Environment Setup

Before testing, ensure:
1. The application is running
2. The database is properly initialized
3. You have appropriate authentication credentials

### Database Reset Procedure

If you need to reset the database for testing:

#### Option 1: Complete Database Reset
```sql
-- Connect to the default database (usually postgres)
\c postgres;

-- Drop the existing database (this will disconnect all users)
DROP DATABASE IF EXISTS inventory_management;

-- Recreate the database
CREATE DATABASE inventory_management;

-- Connect to the new database
\c inventory_management;

-- Run your original database creation script
-- You would need to run the create_database_postgres.sql file after this
```

#### Option 2: Data Reset (Keep Structure)
```sql
-- Connect to your database
\c inventory_management;

-- Disable foreign key constraints temporarily
SET session_replication_role = 'replica';

-- Truncate all tables (in reverse order of dependencies)
TRUNCATE TABLE box_number CASCADE;
TRUNCATE TABLE bulk_logs CASCADE;
TRUNCATE TABLE broken CASCADE;
TRUNCATE TABLE lend CASCADE;
TRUNCATE TABLE sales CASCADE;
TRUNCATE TABLE current_stock CASCADE;
TRUNCATE TABLE logs CASCADE;
TRUNCATE TABLE invoice CASCADE;
TRUNCATE TABLE product_catalog CASCADE;
TRUNCATE TABLE barcode_status CASCADE;
TRUNCATE TABLE lent_id CASCADE;
TRUNCATE TABLE broken_id CASCADE;

-- Re-enable foreign key constraints
SET session_replication_role = 'origin';
```

---

## 🔄 Core Workflows for Testing

### 1️⃣ Product Catalog Management

#### Test Product Creation

**Endpoint:** `POST /api/products`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body:**
```json
{
  "boxBarcode": "TEST-BOX-001",
  "productName": "Test Product",
  "numberSn": 1,
  "description": "Product for testing"
}
```

**Expected Response:** 200 OK with product details

#### Test Product Retrieval

**Endpoint:** `GET /api/products/TEST-BOX-001`

**Expected Response:** 200 OK with product details

---

### 2️⃣ Inventory Addition Workflow

#### Add Stock for Non-Serialized Product (SN=0)

**Endpoint:** `POST /api/stock/add`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-NONSN | Box barcode for non-serialized product |
| quantity | 5 | Number of items to add |
| note | Testing non-serialized | Optional note |

**Expected Response:** 200 OK with updated stock

#### Add Stock for Single-SN Product (SN=1)

**Endpoint:** `POST /api/stock/add`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode for serialized product |
| productBarcode | TEST-PROD-001 | Individual product barcode |
| quantity | 1 | Number of items to add |
| note | Testing single SN | Optional note |

**Expected Response:** 200 OK with updated stock and box number assigned

#### Add Stock in Bulk for Paired-SN Product (SN=2)

**Endpoint:** `POST /api/stock/add-bulk`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body:**
```json
{
  "boxBarcode": "TEST-BOX-2SN",
  "productBarcodes": ["TEST-PROD-A1", "TEST-PROD-A2", "TEST-PROD-B1", "TEST-PROD-B2"],
  "quantity": 4,
  "note": "Testing paired SN"
}
```

**Expected Response:** 200 OK with updated stock and paired box numbers

---

### 3️⃣ Inventory Query Workflow

#### Check All Stock

**Endpoint:** `GET /api/stock`

**Expected Response:** 200 OK with list of all stock items

#### Check Stock for Specific Product

**Endpoint:** `GET /api/stock/box/TEST-BOX-1SN`

**Expected Response:** 200 OK with stock details for that product

---

### 4️⃣ Inventory Removal Workflow

#### Remove Stock for Non-Serialized Product

**Endpoint:** `POST /api/stock/remove`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-NONSN | Box barcode for non-serialized product |
| quantity | 2 | Number of items to remove |
| note | Testing removal | Optional note |

**Expected Response:** 200 OK with updated stock

#### Remove Stock for Serialized Product

**Endpoint:** `POST /api/stock/remove`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode for serialized product |
| productBarcode | TEST-PROD-001 | Individual product barcode |
| quantity | 1 | Number of items to remove |
| note | Testing serialized removal | Optional note |

**Expected Response:** 200 OK with updated stock

#### Remove Stock in Bulk for Serialized Products

**Endpoint:** `POST /api/stock/remove-bulk`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body:**
```json
{
  "boxBarcode": "TEST-BOX-1SN",
  "productBarcodes": ["TEST-PROD-002", "TEST-PROD-003"],
  "note": "Testing bulk removal"
}
```

**Expected Response:** 200 OK with updated stock

---

### 5️⃣ Sales Workflow

#### Move Stock to Sales (Testing Required shopName and orderId)

**Endpoint:** `POST /api/stock/move`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode for serialized product |
| productBarcode | TEST-PROD-001 | Individual product barcode (required for serialized products) |
| quantity | 1 | Number of items to move |
| destination | sales | Destination type |
| shopName | Test Shop | **Required** for sales destination |
| employeeId | EMP-001 | **Required** for sales destination |
| orderId | ORD-001 | **Recommended** for sales (used as invoice ID) |
| note | Testing sales | Optional note |

**Expected Response:** 200 OK

#### Move Non-Serialized Product to Sales

**Endpoint:** `POST /api/stock/move`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-NONSN | Box barcode for non-serialized product |
| quantity | 1 | Number of items to move |
| destination | sales | Destination type |
| shopName | Test Shop | **Required** for sales destination |
| employeeId | EMP-001 | **Required** for sales destination |
| orderId | ORD-001 | **Recommended** for sales (used as invoice ID) |
| note | Testing sales | Optional note |

**Expected Response:** 200 OK

#### ⚠️ Test Missing shopName (Should Fail)

**Endpoint:** `POST /api/stock/move`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-NONSN | Box barcode |
| quantity | 1 | Number of items to move |
| destination | sales | Destination type |
| note | Testing sales | Optional note |
| shopName | *(missing)* | **Required** but intentionally omitted |

**Expected Response:** 400 Bad Request with error about missing shopName

#### Create Sales Order

**Endpoint:** `POST /api/sales`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body (Basic):**
```json
{
  "productIdentifiers": ["TEST-PROD-001", "TEST-BOX-NONSN:1"],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Testing sales order",
  "orderId": "ORD-002"
}
```

**Request Body (With Global splitPair):**
```json
{
  "productIdentifiers": ["TEST-PROD-001", "TEST-PROD-002"],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Testing sales order with global splitPair",
  "orderId": "ORD-002",
  "splitPair": true
}
```

**Request Body (With Product-Specific splitPair):**
```json
{
  "products": [
    {"identifier": "TEST-PROD-001", "splitPair": true},
    {"identifier": "TEST-PROD-002", "splitPair": false},
    {"identifier": "TEST-BOX-NONSN:1", "splitPair": null}
  ],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Testing sales order with product-specific splitPair",
  "orderId": "ORD-002"
}
```

**Expected Response:** 200 OK with success message

**Note:** 
- For serialized products, provide the product barcode directly (e.g., "TEST-PROD-001")
- For non-serialized products, use the format "BOX-BARCODE:QUANTITY" (e.g., "TEST-BOX-NONSN:1")
- The `orderId` parameter is used as the invoice number and helps track related sales
- If `orderId` is not provided, the system will generate one automatically
- For SN=2 products (paired products), the `splitPair` parameter controls whether both products in the pair are processed together:
  - When `splitPair=false` (default): Both items in the pair will be moved together
  - When `splitPair=true`: Only the specified barcode will be moved, without its pair
  - You can set this at the global level or for individual products using the `products` array

#### Understanding Product Identifiers

The `productIdentifiers` parameter is used in both the Sales Order and General Order endpoints to specify which products to process. The format depends on whether the product is serialized or non-serialized:

**For Serialized Products (SN=1 or SN=2):**
- Use the product barcode directly (e.g., `"TEST-PROD-001"`)
- Each barcode represents exactly one item
- Example: `["TEST-PROD-001", "TEST-PROD-002"]` will process two serialized products

**For Non-Serialized Products (SN=0):**
- Use the format `"BOX-BARCODE:QUANTITY"` (e.g., `"TEST-BOX-NONSN:5"`)
- The number after the colon represents how many items to process
- Example: `["TEST-BOX-NONSN:5"]` will process 5 units of the non-serialized product

**Mixed Example:**
```json
{
  "productIdentifiers": [
    "TEST-PROD-001",           // One serialized product
    "TEST-PROD-002",           // Another serialized product
    "TEST-BOX-NONSN:5"         // 5 units of a non-serialized product
  ],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Mixed product types example",
  "orderId": "ORD-004"
}
```

**Important Notes:**
- The system automatically detects whether a product is serialized or non-serialized based on the `numberSn` value in the product catalog
- For serialized products (SN=1 or SN=2), the system will verify that each barcode exists and is in stock
- For non-serialized products (SN=0), the system will verify that sufficient quantity is available
- For paired products (SN=2), you can control whether pairs are split using the `splitPair` parameter

#### Control Pair Splitting in Sales Orders

For SN=2 products (paired products), there are two ways to control pair splitting:

**Option 1: Global Setting**
- Set `splitPair=true` in the root of the DTO to split all pairs
- This applies the same setting to all paired products in the order
- Example:
```json
{
  "orderId": "ORDER123",
  "employeeId": "EMP101",
  "shopName": "Main Store",
  "productIdentifiers": ["BARCODE123", "BARCODE456"],
  "splitPair": true
}
```

**Option 2: Product-Specific Setting**
- Use the `products` array with `ProductIdentifierDTO` objects
- This allows different splitting behavior for different products in the same order
- Example:
```json
{
  "orderId": "ORDER123",
  "employeeId": "EMP101",
  "shopName": "Main Store",
  "products": [
    {"identifier": "BARCODE123", "splitPair": true},
    {"identifier": "BARCODE456", "splitPair": false}
  ]
}
```

In this example:
- BARCODE123 will be split from its pair (only this specific barcode will be moved to sales)
- BARCODE456 will be moved together with its pair (both this barcode and its paired barcode will be moved)

**Note:** When using both `productIdentifiers` and `products` arrays, items in the `products` array take precedence.

#### Move Stock to Sales with Split Pair

**Endpoint:** `POST /api/stock/move`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-2SN | Box barcode for paired product |
| productBarcode | TEST-PROD-A1 | Individual product barcode |
| quantity | 1 | Number of items to move |
| destination | sales | Destination type |
| shopName | Test Shop | **Required** for sales destination |
| employeeId | EMP-001 | **Required** for sales destination |
| orderId | ORD-001 | **Recommended** for sales (used as invoice ID) |
| splitPair | true | **Optional** Set to true to move only this barcode without its pair |
| note | Testing sales with splitPair | Optional note |

**Expected Response:** 200 OK

**Note:**
- For paired products (SN=2), by default both products in a pair will be moved together
- Setting `splitPair=true` allows moving only the specified barcode without its pair
- This parameter is only applicable to paired products (SN=2)

#### Create General Order (For Sales, Lent, or Broken)

**Endpoint:** `POST /api/orders/create`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body (Basic):**
```json
{
  "productIdentifiers": ["TEST-PROD-001", "TEST-BOX-NONSN:1"],
  "destination": "sales",
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "condition": null,
  "note": "Testing general order",
  "orderId": "ORD-003"
}
```

**Request Body (With Global splitPair):**
```json
{
  "productIdentifiers": ["TEST-PROD-001", "TEST-PROD-002"],
  "destination": "sales",
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "condition": null,
  "note": "Testing general order with global splitPair",
  "orderId": "ORD-003",
  "splitPair": true
}
```

**Request Body (With Product-Specific splitPair):**
```json
{
  "products": [
    {"identifier": "TEST-PROD-001", "splitPair": true},
    {"identifier": "TEST-PROD-002", "splitPair": false}
  ],
  "destination": "sales",
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "condition": null,
  "note": "Testing general order with product-specific splitPair",
  "orderId": "ORD-003"
}
```

**Expected Response:** 200 OK with success message

**Note:** 
- For serialized products, provide the product barcode directly (e.g., "TEST-PROD-001")
- For non-serialized products, use the format "BOX-BARCODE:QUANTITY" (e.g., "TEST-BOX-NONSN:1")
- Set `destination` to "sales", "lent", or "broken"
- `shopName` is required for "sales" and "lent" destinations
- `condition` is required for "broken" destination
- `employeeId` is required for "sales" and "lent" destinations
- `orderId` is required for "sales" and "lent" destinations, and optional for "broken" destinations
- The `splitPair` parameter works the same way as in the Sales Order API, allowing you to control whether paired products (SN=2) are processed together or individually

#### Create Sales Order (Mixed Products)

**Endpoint:** `POST /api/sales`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body:**
```json
{
  "productIdentifiers": [
    "TEST-PROD-001",
    "TEST-PROD-002",
    "TEST-BOX-NONSN:5"
  ],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Mixed product types example",
  "orderId": "ORD-004"
}
```

**Request Body (With Mixed Product Types and Product-Specific splitPair):**
```json
{
  "products": [
    {"identifier": "TEST-PROD-001", "splitPair": true},
    {"identifier": "TEST-PROD-002", "splitPair": false},
    {"identifier": "TEST-BOX-NONSN:5", "splitPair": null}
  ],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Mixed product types with specific splitPair settings",
  "orderId": "ORD-004"
}
```

**Expected Response:** 200 OK with success message

#### Create General Order (Mixed Products)

**Endpoint:** `POST /api/orders/create`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body:**
```json
{
  "productIdentifiers": [
    "TEST-PROD-001",
    "TEST-PROD-002",
    "TEST-BOX-NONSN:5"
  ],
  "destination": "sales",
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "condition": null,
  "note": "Mixed product types example",
  "orderId": "ORD-004"
}
```

**Expected Response:** 200 OK with success message

#### Create Sales Order (Mixed Products with Product-Specific splitPair)

**Request Body:**
```json
{
  "products": [
    {"identifier": "TEST-PROD-001", "splitPair": true},
    {"identifier": "TEST-PROD-002", "splitPair": false},
    {"identifier": "TEST-BOX-NONSN:5", "splitPair": null}
  ],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Mixed product types with specific splitPair settings",
  "orderId": "ORD-004"
}
```

**Expected Response:** 200 OK with success message

#### Create Lent Order

**Endpoint:** `POST /api/lent`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body (Basic):**
```json
{
  "productIdentifiers": ["TEST-PROD-001", "TEST-BOX-NONSN:1"],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Testing lent order",
  "orderId": "LENT-001"
}
```

**Request Body (With Global splitPair):**
```json
{
  "productIdentifiers": ["TEST-PROD-001", "TEST-PROD-002"],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Testing lent order with global splitPair",
  "orderId": "LENT-001",
  "splitPair": true
}
```

**Request Body (With Product-Specific splitPair):**
```json
{
  "products": [
    {"identifier": "TEST-PROD-001", "splitPair": true},
    {"identifier": "TEST-PROD-002", "splitPair": false},
    {"identifier": "TEST-BOX-NONSN:1", "splitPair": null}
  ],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Testing lent order with product-specific splitPair",
  "orderId": "LENT-001"
}
```

**Request Body (With Mixed Destinations):**
```json
{
  "products": [
    {"identifier": "TEST-PROD-001", "destination": "lent"},      // Move to lent
    {"identifier": "TEST-PROD-002", "destination": "return"},    // Move to lent and immediately return
    {"identifier": "TEST-PROD-003", "destination": "sales"},     // Move directly to sales
    {"identifier": "TEST-BOX-NONSN:1", "destination": "lent"}    // Move non-serialized item to lent
  ],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Testing lent order with mixed destinations",
  "orderId": "LENT-001"
}
```

**Expected Response:** 200 OK with success message

**Note:** 
- For serialized products, provide the product barcode directly (e.g., "TEST-PROD-001")
- For non-serialized products, use the format "BOX-BARCODE:QUANTITY" (e.g., "TEST-BOX-NONSN:1")
- Both `shopName` and `employeeId` are required for lent orders
- The `orderId` parameter is used for tracking and is required
- The `splitPair` parameter controls whether paired products (SN=2) are processed together or individually
- The `destination` parameter allows you to specify different actions for each product:
  - `lent`: Move to lent status (default)
  - `return`: For serialized products, this moves to lent and immediately returns to stock
  - `sales`: Move directly to sales instead of lent

### Get Lent Items by Order ID

Retrieves all items in a specific lent order with their current status.

**Endpoint:** `GET /api/lent/orders/{orderId}`

**Example:** `GET /api/lent/orders/LENT-20230615`

**Expected Response:** 200 OK with an array of lent items
```json
[
  {
    "lentId": 1,
    "boxBarcode": "BOX001",
    "productName": "Black Shirt",
    "productBarcode": "2001",
    "employeeId": "EMP101",
    "timestamp": "2023-06-15T10:30:00",
    "boxNumber": 1,
    "note": "Regular lending",
    "shopName": "Main Store",
    "quantity": 1,
    "status": "lent",
    "orderId": "LENT-20230615"
  },
  {
    "lentId": 2,
    "boxBarcode": "BOX001",
    "productName": "Black Shirt",
    "productBarcode": "2002",
    "employeeId": "EMP101",
    "timestamp": "2023-06-15T10:30:00",
    "boxNumber": 1,
    "note": "Paired with 2001",
    "shopName": "Main Store",
    "quantity": 1,
    "status": "lent",
    "orderId": "LENT-20230615"
  }
]
```

### Batch Process Lent Items

Process multiple lent items from an order with different destinations (return to stock, move to sales, mark as broken).

**Endpoint:** `POST /api/lent/orders/{orderId}/process`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body:**
```json
{
  "employeeId": "EMP101",
  "shopName": "Main Store",
  "note": "Batch processing",
  "returnToStock": ["2001"],
  "moveToSales": ["2002"],
  "markAsBroken": ["2003"],
  "condition": "Damaged during use"
}
```

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| employeeId | EMP101 | ID of employee processing the items (Required) |
| shopName | Main Store | Shop name (Required for sales) |
| note | Batch processing | Optional note |
| returnToStock | ["2001"] | Array of product barcodes to return to stock |
| moveToSales | ["2002"] | Array of product barcodes to move to sales |
| markAsBroken | ["2003"] | Array of product barcodes to mark as broken |
| condition | Damaged during use | Required when marking items as broken |

**Expected Response:** 200 OK
```json
{
  "status": "success",
  "message": "Lent items processed successfully"
}
```

**Error Cases:**
- Missing employeeId: 400 Bad Request
- Missing shopName when moveToSales is provided: 400 Bad Request
- Missing condition when markAsBroken is provided: 400 Bad Request
- Item not found: 404 Not Found
- Item already processed: 400 Bad Request

**Notes:**
- Any items not included in the lists will remain in "lent" status
- Each item can only be in one list (returnToStock, moveToSales, or markAsBroken)
- For items moved to sales, a new sales order ID is generated as "SALES-FROM-LENT-{orderId}"
- For items marked as broken, a new broken order ID is generated as "BROKEN-FROM-LENT-{orderId}"

#### Create Broken Order

**Endpoint:** `POST /api/broken`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/json |

**Request Body (Basic):**
```json
{
  "productIdentifiers": ["TEST-PROD-001", "TEST-BOX-NONSN:1"],
  "employeeId": "EMP-001",
  "condition": "Damaged",
  "note": "Testing broken order",
  "orderId": "BROKEN-001"
}
```

**Request Body (With Global splitPair):**
```json
{
  "productIdentifiers": ["TEST-PROD-001", "TEST-PROD-002"],
  "employeeId": "EMP-001",
  "condition": "Damaged",
  "note": "Testing broken order with global splitPair",
  "orderId": "BROKEN-001",
  "splitPair": true
}
```

**Request Body (With Product-Specific splitPair):**
```json
{
  "products": [
    {"identifier": "TEST-PROD-001", "splitPair": true},
    {"identifier": "TEST-PROD-002", "splitPair": false},
    {"identifier": "TEST-BOX-NONSN:1", "splitPair": null}
  ],
  "employeeId": "EMP-001",
  "condition": "Damaged",
  "note": "Testing broken order with product-specific splitPair",
  "orderId": "BROKEN-001"
}
```

**Expected Response:** 200 OK with success message

**Note:** 
- For serialized products, provide the product barcode directly (e.g., "TEST-PROD-001")
- For non-serialized products, use the format "BOX-BARCODE:QUANTITY" (e.g., "TEST-BOX-NONSN:1")
- The `condition` field is required for broken orders
- The `orderId` parameter is optional for broken orders
- The `splitPair` parameter works the same way as in the Sales Order API

---

### 6️⃣ Lending Workflow

#### Move Stock to Lent

**Endpoint:** `POST /api/stock/move`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode |
| productBarcode | TEST-PROD-002 | Individual product barcode |
| quantity | 1 | Number of items to move |
| destination | lent | Destination type |
| employeeId | EMP-001 | **Required** for lent destination |
| shopName | Test Shop | Shop where item is being lent |
| note | Testing lending | Optional note |

**Expected Response:** 200 OK

#### Return Lent Item

**Endpoint:** `POST /api/stock/return`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode |
| productBarcode | TEST-PROD-002 | Individual product barcode |
| note | Testing return | Optional note |
| lentId | LENT-12345 | Optional specific lent record ID to return |

**Expected Response:** 200 OK with updated stock

**Note:**
- This endpoint is for returning serialized products (with product barcodes)
- The `lentId` parameter is optional and can be used to specify which lent record to update

#### Verify Lent Record Status

**Endpoint:** `GET /api/lend/barcode/TEST-PROD-002`

**Expected Response:** 200 OK with lent record showing status="returned"

---

### 7️⃣ Barcode Reuse Testing

#### Test Barcode Reuse After Return

**Step 1:** Lend an item (use the "Move Stock to Lent" endpoint above)

**Step 2:** Return the item (use the "Return Lent Item" endpoint above)

**Step 3:** Try to add the same barcode back to stock

**Endpoint:** `POST /api/stock/add`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode for serialized product |
| productBarcode | TEST-PROD-002 | Same barcode that was previously lent and returned |
| quantity | 1 | Number of items to add |
| note | Testing barcode reuse | Optional note |

**Expected Response:** 200 OK with updated stock and new box number assigned

#### Test Barcode Reuse After Sales

**Step 1:** Move an item to sales (use the "Move Stock to Sales" endpoint)

**Step 2:** Try to add the same barcode back to stock

**Endpoint:** `POST /api/stock/add`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode for serialized product |
| productBarcode | TEST-PROD-003 | Barcode that was previously sold |
| quantity | 1 | Number of items to add |
| note | Testing barcode reuse after sales | Optional note |

**Expected Response:** 200 OK with updated stock and new box number assigned

#### Test Barcode Reuse After Broken

**Step 1:** Move an item to broken (use the "Move to Broken" endpoint)

**Step 2:** Try to add the same barcode back to stock

**Endpoint:** `POST /api/stock/add`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode for serialized product |
| productBarcode | TEST-PROD-004 | Barcode that was previously marked as broken |
| quantity | 1 | Number of items to add |
| note | Testing barcode reuse after broken | Optional note |

**Expected Response:** 200 OK with updated stock and new box number assigned

---

### 8️⃣ Broken Items Workflow

#### Move Stock to Broken

**Endpoint:** `POST /api/stock/move`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode for serialized product |
| productBarcode | TEST-PROD-004 | Individual product barcode |
| quantity | 1 | Number of items to move |
| destination | broken | Destination type |
| condition | Damaged | **Required** for broken destination |
| note | Testing broken | Optional note |

**Expected Response:** 200 OK

#### Move Non-Serialized Product to Broken

**Endpoint:** `POST /api/stock/move`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-NONSN | Box barcode for non-serialized product |
| quantity | 1 | Number of items to move |
| destination | broken | Destination type |
| condition | Damaged | **Required** for broken destination |
| note | Testing broken | Optional note |

**Expected Response:** 200 OK

---

### 9️⃣ Box Number Verification

After performing the above operations, verify box numbers are correctly assigned:

1. **For SN=1 products:** Each item should have a unique box number
2. **For SN=2 products:** Items should be paired with the same box number
3. **For SN=0 products:** No box number should be assigned

**Endpoint:** `GET /api/box-number/box/TEST-BOX-1SN`

**Expected Response:** 200 OK with list of box numbers for that product

---

## ⚠️ Error Testing

### Test Invalid Inputs
- Try removing more stock than available
- Try adding stock with invalid product barcodes
- Try moving stock to an invalid destination

### Test Duplicate Barcode Prevention

**Endpoint:** `POST /api/stock/add`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode for serialized product |
| productBarcode | TEST-PROD-001 | Barcode that is already in use (not returned/sold/broken) |
| quantity | 1 | Number of items to add |
| note | Testing duplicate prevention | Optional note |

**Expected Response:** 400 Bad Request with error about duplicate barcode

### Test Missing Required Fields
- Test sales operations without shopName
- Test lent operations without employeeId
- Test broken operations without condition

### Test Null Product Barcode for Serialized Products

**Endpoint:** `POST /api/stock/add`

**Headers:**
| Header | Value |
|--------|-------|
| Content-Type | application/x-www-form-urlencoded |

**Parameters:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| boxBarcode | TEST-BOX-1SN | Box barcode for serialized product |
| productBarcode | *(missing)* | Intentionally omitted for serialized product |
| quantity | 1 | Number of items to add |
| note | Testing null barcode validation | Optional note |

**Expected Response:** 400 Bad Request with error about required product barcode

## 🚀 Performance Testing

For larger deployments, test with bulk operations:
- Add 100+ items in a single bulk operation
- Query stock with various filters
- Process multiple sales orders simultaneously

### Concurrent Operation Testing

Test adding the same barcode from multiple clients simultaneously to verify that the synchronization mechanisms prevent duplicates.

## 🔍 Database Verification

After API testing, verify the database state:
- Check `current_stock` table for correct quantities
- Verify `box_number` table has correct assignments
- Confirm `logs` table has entries for all operations
- Verify `lend` table has correct status values for returned items

### SQL Queries for Verification

#### Check Box Numbers
```sql
SELECT * FROM box_number WHERE box_barcode = 'TEST-BOX-1SN';
```

#### Check Lent Items Status
```sql
SELECT * FROM lend WHERE product_barcode = 'TEST-PROD-002';
```

#### Check Logs for a Product
```sql
SELECT * FROM logs WHERE product_barcode = 'TEST-PROD-002' ORDER BY timestamp DESC;
```

## 📦 Product Identifiers Reference

### Understanding Product Identifiers

The `productIdentifiers` parameter is a key concept in the inventory system, used in both the Sales Order and General Order endpoints to specify which products to process.

### Format Based on Product Type

The format depends on whether the product is serialized or non-serialized:

#### For Serialized Products (SN=1 or SN=2):
- Use the product barcode directly (e.g., `"TEST-PROD-001"`)
- Each barcode represents exactly one item
- Example: `["TEST-PROD-001", "TEST-PROD-002"]` will process two serialized products

#### For Non-Serialized Products (SN=0):
- Use the format `"BOX-BARCODE:QUANTITY"` (e.g., `"TEST-BOX-NONSN:5"`)
- The number after the colon represents how many items to process
- Example: `["TEST-BOX-NONSN:5"]` will process 5 units of the non-serialized product

### Product Identifier DTO

The system now supports a more structured approach to specifying products using the `ProductIdentifierDTO` class. This provides additional flexibility, particularly for controlling pair splitting for SN=2 products:

```json
{
  "products": [
    {
      "identifier": "TEST-PROD-001",  // Serialized product barcode
      "splitPair": true               // Split this barcode from its pair
    },
    {
      "identifier": "TEST-PROD-002",  // Another serialized product
      "splitPair": false              // Keep this barcode with its pair
    },
    {
      "identifier": "TEST-BOX-NONSN:5", // Non-serialized product with quantity
      "splitPair": null                // Not applicable for non-serialized products
    }
  ]
}
```

This structured approach offers several advantages:
- Product-specific control over pair splitting
- Clearer semantics for API consumers
- Forward compatibility for future product-level attributes

When using both the legacy `productIdentifiers` array and the new `products` array, the system gives precedence to the `products` array. For backward compatibility, the `productIdentifiers` array continues to work as before.

### Mixed Example

You can combine both types in a single request:

```json
{
  "productIdentifiers": [
    "TEST-PROD-001",           // One serialized product
    "TEST-PROD-002",           // Another serialized product
    "TEST-BOX-NONSN:5"         // 5 units of a non-serialized product
  ],
  "employeeId": "EMP-001",
  "shopName": "Test Shop",
  "note": "Mixed product types example",
  "orderId": "ORD-004"
}
```

### Processing Logic

- The system automatically detects whether a product is serialized or non-serialized based on the `numberSn` value in the product catalog
- For serialized products (SN=1 or SN=2), the system will verify that each barcode exists and is in stock
- For non-serialized products (SN=0), the system will verify that sufficient quantity is available
- When processing paired products (SN=2), both items in the pair will be processed together

### Common Errors

- **Invalid format**: Non-serialized products must use the format `"BOX-BARCODE:QUANTITY"`
- **Product not found**: The box barcode doesn't exist in the product catalog
- **Insufficient stock**: Not enough items available for the requested quantity
- **Barcode not found**: The serialized product barcode doesn't exist in the system
- **Barcode not in stock**: The serialized product has already been removed, sold, lent, or marked as broken

## 📦 Postman Collection

For easier testing, you can import the following Postman collection:

```json
{
  "info": {
    "name": "Inventory Management System",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Product Management",
      "item": [
        {
          "name": "Create Product",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/products",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"boxBarcode\": \"TEST-BOX-001\",\n  \"productName\": \"Test Product\",\n  \"numberSn\": 1,\n  \"description\": \"Product for testing\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        },
        {
          "name": "Get Product",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/products/TEST-BOX-001"
          }
        }
      ]
    },
    {
      "name": "Stock Management",
      "item": [
        {
          "name": "Add Stock (Non-Serialized)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/add",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-NONSN"},
                {"key": "quantity", "value": "5"},
                {"key": "note", "value": "Testing non-serialized"}
              ]
            }
          }
        },
        {
          "name": "Add Stock (Single-SN)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/add",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-1SN"},
                {"key": "productBarcode", "value": "TEST-PROD-001"},
                {"key": "quantity", "value": "1"},
                {"key": "note", "value": "Testing single SN"}
              ]
            }
          }
        },
        {
          "name": "Add Stock Bulk (Paired-SN)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/add-bulk",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"boxBarcode\": \"TEST-BOX-2SN\",\n  \"productBarcodes\": [\"TEST-PROD-A1\", \"TEST-PROD-A2\", \"TEST-PROD-B1\", \"TEST-PROD-B2\"],\n  \"quantity\": 4,\n  \"note\": \"Testing paired SN\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        },
        {
          "name": "Get All Stock",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/stock"
          }
        },
        {
          "name": "Get Stock by Box Barcode",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/stock/box/TEST-BOX-1SN"
          }
        },
        {
          "name": "Remove Stock (Non-Serialized)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/remove",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-NONSN"},
                {"key": "quantity", "value": "2"},
                {"key": "note", "value": "Testing removal"}
              ]
            }
          }
        },
        {
          "name": "Remove Stock (Serialized)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/remove",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-1SN"},
                {"key": "productBarcode", "value": "TEST-PROD-001"},
                {"key": "quantity", "value": "1"},
                {"key": "note", "value": "Testing serialized removal"}
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Sales Workflow",
      "item": [
        {
          "name": "Move to Sales (Valid)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/move",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-1SN"},
                {"key": "productBarcode", "value": "TEST-PROD-001"},
                {"key": "quantity", "value": "1"},
                {"key": "destination", "value": "sales"},
                {"key": "shopName", "value": "Test Shop"},
                {"key": "employeeId", "value": "EMP-001"},
                {"key": "orderId", "value": "ORD-001"}
              ]
            }
          }
        },
        {
          "name": "Move to Sales (Missing shopName)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/move",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-NONSN"},
                {"key": "quantity", "value": "1"},
                {"key": "destination", "value": "sales"},
                {"key": "note", "value": "Testing sales"}
              ]
            }
          }
        },
        {
          "name": "Create Sales Order",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/sales",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"productIdentifiers\": [\"TEST-PROD-001\", \"TEST-BOX-NONSN:1\"],\n  \"employeeId\": \"EMP-001\",\n  \"shopName\": \"Test Shop\",\n  \"note\": \"Testing sales order\",\n  \"orderId\": \"ORD-002\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        },
        {
          "name": "Create General Order",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/orders/create",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"productIdentifiers\": [\"TEST-PROD-001\", \"TEST-BOX-NONSN:1\"],\n  \"destination\": \"sales\",\n  \"employeeId\": \"EMP-001\",\n  \"shopName\": \"Test Shop\",\n  \"condition\": null,\n  \"note\": \"Testing general order\",\n  \"orderId\": \"ORD-003\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        },
        {
          "name": "Create Sales Order (Mixed Products)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/sales",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"productIdentifiers\": [\n    \"TEST-PROD-001\",\n    \"TEST-PROD-002\",\n    \"TEST-BOX-NONSN:5\"\n  ],\n  \"employeeId\": \"EMP-001\",\n  \"shopName\": \"Test Shop\",\n  \"note\": \"Mixed product types example\",\n  \"orderId\": \"ORD-004\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        }
      ]
    },
    {
      "name": "Lending Workflow",
      "item": [
        {
          "name": "Move to Lent",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/move",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-1SN"},
                {"key": "productBarcode", "value": "TEST-PROD-002"},
                {"key": "quantity", "value": "1"},
                {"key": "destination", "value": "lent"},
                {"key": "employeeId", "value": "EMP-001"},
                {"key": "shopName", "value": "Test Shop"},
                {"key": "note", "value": "Testing lending"}
              ]
            }
          }
        },
        {
          "name": "Return Lent Item",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/return",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-1SN"},
                {"key": "productBarcode", "value": "TEST-PROD-002"},
                {"key": "note", "value": "Testing return"},
                {"key": "lentId", "value": "LENT-12345"}
              ]
            }
          }
        },
        {
          "name": "Reuse Returned Barcode",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/add",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-1SN"},
                {"key": "productBarcode", "value": "TEST-PROD-002"},
                {"key": "quantity", "value": "1"},
                {"key": "note", "value": "Testing barcode reuse"}
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Broken Items Workflow",
      "item": [
        {
          "name": "Move to Broken",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/stock/move",
            "body": {
              "mode": "urlencoded",
              "urlencoded": [
                {"key": "boxBarcode", "value": "TEST-BOX-1SN"},
                {"key": "productBarcode", "value": "TEST-PROD-004"},
                {"key": "quantity", "value": "1"},
                {"key": "destination", "value": "broken"},
                {"key": "condition", "value": "Damaged"},
                {"key": "note", "value": "Testing broken"}
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Database Reset",
      "item": [
        {
          "name": "Reset Database (Keep Structure)",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/admin/reset-data",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"confirmReset\": true\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        }
      ]
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    }
  ]
}
```

This testing guide covers the core functionality of the Inventory Management System, with special focus on barcode reuse functionality, database reset procedures, and comprehensive error testing scenarios.