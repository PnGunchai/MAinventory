# Inventory Management System API Documentation

## Overview

This document provides details about the API endpoints available in the Inventory Management System.

## Base URL

All API endpoints are relative to the base URL: `/api`

## Authentication

Authentication is required for all API endpoints. The API uses JWT (JSON Web Token) for authentication.

## Common Response Formats

### Success Response

```json
{
  "status": "success",
  "message": "Operation completed successfully",
  "data": { ... }
}
```

### Error Response

```json
{
  "status": "error",
  "message": "Error message describing what went wrong"
}
```

## Stock Management

### Get All Stock

**Endpoint:** `GET /stock`

**Description:** Retrieves all current stock items.

**Response:**
- `200 OK`: Returns a list of all stock items.

### Get Stock by Box Barcode

**Endpoint:** `GET /stock/box/{boxBarcode}`

**Description:** Retrieves all stock items for a specific box barcode.

**Parameters:**
- `boxBarcode`: The barcode of the box.

**Response:**
- `200 OK`: Returns a list of stock items for the specified box barcode.
- `404 Not Found`: If no stock is found for the specified box barcode.

### Get Stock by Box Barcode and Product Name

**Endpoint:** `GET /stock/box/{boxBarcode}/product/{productName}`

**Description:** Retrieves a specific stock item by box barcode and product name.

**Parameters:**
- `boxBarcode`: The barcode of the box.
- `productName`: The name of the product.

**Response:**
- `200 OK`: Returns the stock item for the specified box barcode and product name.
- `404 Not Found`: If no stock is found for the specified box barcode and product name.

### Add Stock

**Endpoint:** `POST /stock/add`

**Description:** Adds stock for a product.

**Parameters:**
- `boxBarcode`: The barcode of the box (required).
- `productBarcode`: The barcode of the product (required for serialized products).
- `quantity`: The quantity to add (required).
- `note`: Additional notes (optional).

**Response:**
- `200 OK`: Returns the updated stock item.
- `400 Bad Request`: If the request is invalid.
- `404 Not Found`: If the product is not found.

### Add Stock in Bulk

**Endpoint:** `POST /stock/add-bulk`

**Description:** Adds stock for multiple products or multiple units of a product.

**Request Body:**
```json
{
  "boxBarcode": "BOX123",
  "productBarcodes": ["PROD1", "PROD2"],
  "quantity": 2,
  "note": "Bulk addition"
}
```

**Notes:**
- For serialized products (SN=1 or SN=2), `productBarcodes` is required and `quantity` must match the number of barcodes.
- For non-serialized products (SN=0), only `quantity` is required.
- For products with SN=2, if an odd number of barcodes is provided, an auto-filled barcode will be generated.

**Response:**
- `200 OK`: Returns the updated stock items.
- `400 Bad Request`: If the request is invalid.
- `404 Not Found`: If the product is not found.

### Remove Stock

**Endpoint:** `POST /stock/remove`

**Description:** Removes stock for a product.

**Parameters:**
- `boxBarcode`: The barcode of the box (required).
- `productBarcode`: The barcode of the product (required for serialized products).
- `quantity`: The quantity to remove (required).
- `note`: Additional notes (optional).

**Response:**
- `200 OK`: Returns the updated stock item.
- `400 Bad Request`: If the request is invalid or if there's not enough stock.
- `404 Not Found`: If the product is not found.

### Remove Stock in Bulk

**Endpoint:** `POST /stock/remove-bulk`

**Description:** Removes stock for multiple products or multiple units of a product.

**Request Body:**
```json
{
  "boxBarcode": "BOX123",
  "productBarcodes": ["PROD1", "PROD2"],
  "quantity": 2,
  "note": "Bulk removal"
}
```

**Notes:**
- For serialized products (SN=1 or SN=2), `productBarcodes` is required.
- For non-serialized products (SN=0), only `quantity` is required.

**Response:**
- `200 OK`: Returns the updated stock items.
- `400 Bad Request`: If the request is invalid or if there's not enough stock.
- `404 Not Found`: If the product is not found.

### Move Stock

**Endpoint:** `POST /stock/move`

**Description:** Moves stock to another destination (sales, lent, broken).

**Parameters:**
- `boxBarcode`: The barcode of the box (required).
- `productBarcode`: The barcode of the product (required for serialized products).
- `quantity`: The quantity to move (required).
- `destination`: The destination to move to (required). Must be one of: "sales", "lent", "broken".
- `employeeId`: The ID of the employee (required for "lent" destination).
- `shopName`: The name of the shop (required for "sales" destination).
- `condition`: The condition of the product (required for "broken" destination).
- `note`: Additional notes (optional).
- `orderId`: The ID of the order (optional, for "sales" destination).

**Important Notes:**
- When moving stock to "sales", the `shopName` parameter is required.
- When moving stock to "sales", the `employeeId` parameter is required.
- The `orderId` parameter is used as the invoice identifier and should be provided for sales operations.
- If `orderId` is not provided, a temporary invoice ID will be generated.
- When moving stock to "lent", the `employeeId` parameter is required.
- When moving stock to "broken", the `condition` parameter is required.

**Response:**
- `200 OK`: If the stock was moved successfully.
- `400 Bad Request`: If the request is invalid or if there's not enough stock.
- `404 Not Found`: If the product is not found.

### Return Lent Item

**Endpoint:** `POST /stock/return`

**Description:** Returns a lent item to stock.

**Parameters:**
- `boxBarcode`: The barcode of the box (required).
- `productBarcode`: The barcode of the product (required).
- `note`: Additional notes (optional).
- `lentId`: The ID of the lent record (optional).

**Response:**
- `200 OK`: Returns the updated stock item.
- `400 Bad Request`: If the request is invalid.
- `404 Not Found`: If the product is not found.

## Sales Management

### Create Sales Order

**Endpoint:** `POST /sales`

**Description:** Creates a new sales order.

**Request Body:**
```json
{
  "orderId": "ORD123",
  "shopName": "Main Store",
  "items": [
    {
      "boxBarcode": "BOX123",
      "productBarcodes": ["PROD1", "PROD2"],
      "quantity": 2
    }
  ],
  "note": "Sales order note"
}
```

**Important Notes:**
- `shopName` is required for all sales orders.
- For serialized products (SN=1 or SN=2), `productBarcodes` is required.
- For non-serialized products (SN=0), only `quantity` is required.

**Response:**
- `200 OK`: If the sales order was created successfully.
- `400 Bad Request`: If the request is invalid or if there's not enough stock.
- `404 Not Found`: If any product is not found.

## Product Catalog Management

### Get All Products

**Endpoint:** `GET /products`

**Description:** Retrieves all products in the catalog.

**Response:**
- `200 OK`: Returns a list of all products.

### Get Product by Box Barcode

**Endpoint:** `GET /products/{boxBarcode}`

**Description:** Retrieves a specific product by box barcode.

**Parameters:**
- `boxBarcode`: The barcode of the box.

**Response:**
- `200 OK`: Returns the product for the specified box barcode.
- `404 Not Found`: If no product is found for the specified box barcode.

### Create Product

**Endpoint:** `POST /products`

**Description:** Creates a new product in the catalog.

**Request Body:**
```json
{
  "boxBarcode": "BOX123",
  "productName": "Product Name",
  "numberSn": 1,
  "description": "Product description"
}
```

**Notes:**
- `numberSn` indicates the number of serial numbers per box:
  - 0: Non-serialized product
  - 1: One serial number per box
  - 2: Two serial numbers per box

**Response:**
- `200 OK`: Returns the created product.
- `400 Bad Request`: If the request is invalid.
- `409 Conflict`: If a product with the same box barcode already exists.

### Update Product

**Endpoint:** `PUT /products/{boxBarcode}`

**Description:** Updates an existing product in the catalog.

**Parameters:**
- `boxBarcode`: The barcode of the box.

**Request Body:**
```json
{
  "productName": "Updated Product Name",
  "numberSn": 1,
  "description": "Updated product description"
}
```

**Response:**
- `200 OK`: Returns the updated product.
- `400 Bad Request`: If the request is invalid.
- `404 Not Found`: If no product is found for the specified box barcode.

### Delete Product

**Endpoint:** `DELETE /products/{boxBarcode}`

**Description:** Deletes a product from the catalog.

**Parameters:**
- `boxBarcode`: The barcode of the box.

**Response:**
- `200 OK`: If the product was deleted successfully.
- `404 Not Found`: If no product is found for the specified box barcode.

## Box Number Management

Box numbers are automatically assigned to products based on their `numberSn` value:

- For products with `numberSn=0` (non-serialized), no box number is assigned.
- For products with `numberSn=1`, each product gets a unique box number.
- For products with `numberSn=2`, products are paired and share the same box number.

Box numbers are visible in the stock, logs, sales, lent, and broken records.

## Error Handling

The API uses standard HTTP status codes to indicate the success or failure of a request:

- `200 OK`: The request was successful.
- `400 Bad Request`: The request was invalid.
- `401 Unauthorized`: Authentication is required.
- `403 Forbidden`: The authenticated user does not have permission to access the requested resource.
- `404 Not Found`: The requested resource was not found.
- `409 Conflict`: The request could not be completed due to a conflict with the current state of the resource.
- `500 Internal Server Error`: An error occurred on the server.

## Pagination

For endpoints that return lists of items, pagination is supported using the following query parameters:

- `page`: The page number (default: 0).
- `size`: The number of items per page (default: 20).
- `sort`: The field to sort by (default: id).
- `direction`: The sort direction (default: asc).

Example: `GET /stock?page=0&size=10&sort=lastUpdated&direction=desc`