# Inventory System API Testing Guide

## Table of Contents
1. [Testing Environment Setup](#1-testing-environment-setup)
2. [Core Operations Testing](#2-core-operations-testing)
3. [Error Handling Testing](#3-error-handling-testing)
4. [Test Data Reference](#4-test-data-reference)

## 1. Testing Environment Setup

### 1.1 Prerequisites
- Test database configured
- Test user credentials ready
- Postman or similar API testing tool
- Sample test data prepared

### 1.2 Base Configuration
```json
{
    "baseUrl": "http://localhost:8080",
    "headers": {
        "Content-Type": "application/json",
        "Authorization": "Bearer {your-test-token}"
    }
}
```

## 2. Core Operations Testing

### 2.1 Stock Movement Operations

#### 2.1.1 Move Lent to Sales
**Endpoint:** `POST /api/stock/move`

**Test Case 1: Single Item (Including Paired Product)**
```json
Request:
{
    "boxBarcode": "BOX001",
    "productBarcode": "PROD001",
    "operation": "sales",
    "orderId": "ORDER001",
    "note": "Moving lent item to sales"
}

Expected Results:
1. Database Changes:
   - Lent table: status updated to "sold" for the specific product
   - Sales table: new record created for the specific product
   - Stock table: quantity unchanged
   
2. Important Note:
   - For paired products, each item must be moved to sales separately
   - The paired item remains in lent status until explicitly moved
```

#### 2.1.2 Normal Sales
**Endpoint:** `POST /api/stock/move`

**Test Case 1: Single Item (Including Paired Product)**
```json
Request:
{
    "boxBarcode": "BOX003",
    "productBarcode": "PROD003",
    "operation": "sales",
    "orderId": "ORDER003"
}

Expected Results:
1. Database Changes:
   - Sales record created for the specific product
   - Stock quantity decreased
   - Item removed from in_stock
   
2. Important Note:
   - For paired products, each item must be sold separately
   - The paired item remains in stock until explicitly sold
```

### 2.2 Lent Operations

#### 2.2.1 Create Lent Record
**Endpoint:** `POST /api/stock/move`

**Test Case 1: Single Item**
```json
Request:
{
    "boxBarcode": "BOX004",
    "productBarcode": "PROD004",
    "operation": "lent",
    "note": "Test lent operation"
}

Expected Results:
1. Database Changes:
   - Lent record created
   - Stock quantity updated
   - Item removed from in_stock
   
2. Logs:
   - Entry created with "lent" operation
```

**Test Case 2: Paired Products**
```json
Request:
{
    "boxBarcode": "BOX005",
    "productBarcode": "PAIR002",
    "operation": "lent",
    "splitPair": false
}

Expected Results:
1. Database Changes:
   - Both items marked as lent
   - Both removed from in_stock
   
2. Logs:
   - Two entries: one for each paired item
```

## 3. Error Handling Testing

### 3.1 Common Error Scenarios

#### 3.1.1 Invalid Input
```json
Test Cases:
1. Invalid box barcode
2. Non-existent product barcode
3. Missing required fields
4. Invalid operation type
5. Attempting to sell/move both paired items in one request

Expected Results:
- Status: 400 Bad Request
- Clear error message
- No database changes
```

#### 3.1.2 Business Logic Errors
```json
Test Cases:
1. Moving non-lent item from lent to sales
2. Selling out-of-stock item
3. Lending already lent item
4. Invalid status transitions

Expected Results:
- Status: 422 Unprocessable Entity
- Specific error message
- No database changes
```

## 4. Test Data Reference

### 4.1 Sample Test Data
```json
{
    "boxes": [
        {
            "boxBarcode": "BOX001",
            "products": [
                {
                    "productBarcode": "PROD001",
                    "productName": "Test Product 1",
                    "quantity": 5
                }
            ]
        },
        {
            "boxBarcode": "BOX002",
            "products": [
                {
                    "productBarcode": "PAIR001",
                    "productName": "Paired Product 1",
                    "isPaired": true,
                    "pairedBarcode": "PAIR002"
                }
            ]
        }
    ]
}
```

### 4.2 Test Scenarios Matrix
| Operation | Normal Case | Error Case | Edge Case |
|-----------|-------------|------------|------------|
| Move to Sales | Single item | Invalid barcode | Zero quantity |
| Lent | Paired items | Already lent | Split pair |
| Stock Update | Add stock | Negative quantity | Max quantity |

### 4.3 Response Codes
- 200: Success
- 400: Bad Request
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 422: Unprocessable Entity
- 500: Server Error

## Notes
- Run tests in isolation
- Clean up test data after each test
- Verify both success and error scenarios
- Each paired product must be processed individually for sales operations
- Paired products can only be lent together when splitPair is false
- Database changes should be verified directly as logs may not show all operations 