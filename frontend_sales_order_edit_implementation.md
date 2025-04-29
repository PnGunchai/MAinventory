# Sales Order Editing - Frontend Implementation Guide

## Overview
This document outlines the frontend implementation requirements for the sales order editing feature, which allows users to modify existing sales orders by adding/removing items and updating notes.

## API Endpoints

### 1. Add Items to Order
```http
POST /api/sales/orders/{orderId}/items
```
Request body:
```json
{
    "products": [
        {
            "identifier": "SHOE-RED-42"  // For serial products
        },
        {
            "identifier": "SOCKS-001:5"  // For non-serial products (with quantity)
        }
    ],
    "note": "Optional note for these additions"
}
```

### 2. Remove Item from Order
```http
DELETE /api/sales/orders/{orderId}/items/{productBarcode}
```

### 3. Update Order Notes
```http
PUT /api/sales/orders/{orderId}/notes
```
Request body:
```json
{
    "note": "Updated note content"
}
```

## UI Components

### 1. Order List View Enhancements
- Add "Edit" button/icon for each order
- Display edit information:
  ```
  Last Modified: [Date Time]
  Modifications: [Count]
  ```
- Visual indicator for modified orders (e.g., badge or icon)

### 2. Order Details View
```
Order #SO-2024-001
-------------------
Created: [Original Date]
Last Modified: [Modification Date]
Total Modifications: [Count]

Items:
□ Product A                    [Remove]
  Serial: SHOE-RED-42
  
□ Product B                    [Remove]
  Quantity: 5
  Box: SOCKS-001

[+ Add Items]

Edit History:
[2024-03-20 11:30] Added items: SHOE-BLUE-42, SOCKS-001:3
[2024-03-20 11:00] Removed item: Red Shoes
[2024-03-20 10:30] Updated notes

Notes:
┌────────────────────────┐
│ [Editable Notes Field] │
└────────────────────────┘
[Save Notes]
```

### 3. Add Items Modal
```
Add Items to Order #SO-2024-001
------------------------------
Scan/Enter Product:
┌────────────────────────┐
│ [Product Search Field] │
└────────────────────────┘

Selected Items:
□ SHOE-RED-42
  Name: Red Shoes
  Type: Serial Product

□ SOCKS-001
  Name: Black Socks
  Type: Non-serial Product
  Quantity: [   5   ]

Notes:
┌────────────────────────┐
│ [Optional Notes Field] │
└────────────────────────┘

[Cancel] [Add Items]
```

### 4. Remove Item Confirmation
```
Confirm Item Removal
-------------------
Are you sure you want to remove this item?

Product: Red Shoes
Barcode: SHOE-RED-42
From Order: SO-2024-001

[Cancel] [Remove Item]
```

## Implementation Guidelines

### 1. State Management
```typescript
interface OrderState {
    orderId: string;
    items: OrderItem[];
    editHistory: EditRecord[];
    lastModified: Date;
    editCount: number;
    notes: string;
}

interface OrderItem {
    productBarcode?: string;  // For serial products
    boxBarcode: string;
    productName: string;
    quantity: number;
}

interface EditRecord {
    timestamp: Date;
    operation: 'ADD_ITEMS' | 'REMOVE_ITEM' | 'UPDATE_NOTES';
    details: string;
}
```

### 2. Error Handling
- Display error messages for:
  - Product not found
  - Invalid quantities
  - Server errors
  - Insufficient stock
  - Invalid operations

### 3. Loading States
- Show loading indicators during:
  - Adding items
  - Removing items
  - Updating notes
- Disable relevant buttons during operations

### 4. Validation Rules
- Product identifiers must not be empty
- Quantities must be positive numbers
- Serial products must have valid barcodes
- Non-serial products must have valid box codes and quantities

## Mobile Responsiveness

### 1. Order List
- Stack order information vertically
- Full-width action buttons
- Swipeable rows for quick actions

### 2. Order Details
- Collapsible sections
- Touch-friendly remove buttons
- Bottom sheet for add items on mobile
- Floating action button for primary actions

### 3. Add Items
- Full-screen modal on mobile
- Native barcode scanner integration
- Touch-optimized quantity controls
- Sticky action buttons

## Testing Scenarios

1. **Adding Items**
   - Add single serial product
   - Add multiple serial products
   - Add non-serial product with quantity
   - Add mixed products (serial and non-serial)
   - Add items with notes

2. **Removing Items**
   - Remove serial product
   - Remove non-serial product
   - Cancel removal
   - Handle removal errors

3. **Notes Management**
   - Add new note
   - Update existing note
   - Clear notes
   - Handle long notes

4. **Error Cases**
   - Invalid product codes
   - Network errors
   - Concurrent modifications
   - Invalid quantities

## Security Considerations

1. **Authorization**
   - Check user permissions before showing edit options
   - Validate operations against user roles
   - Handle unauthorized access gracefully

2. **Input Validation**
   - Sanitize all user inputs
   - Validate product codes
   - Check quantity limits
   - Prevent XSS in notes

## Performance Optimization

1. **Data Loading**
   - Implement pagination for large orders
   - Cache frequently accessed data
   - Lazy load edit history
   - Optimize images and assets

2. **User Experience**
   - Debounce search inputs
   - Implement optimistic updates
   - Pre-fetch likely needed data
   - Minimize server roundtrips 