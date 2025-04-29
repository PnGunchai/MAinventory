# Split Pair Functionality - Frontend Implementation Guide

This document outlines the approach for implementing the split pair functionality in the frontend interface of the Inventory Management System.

## Context

The inventory system handles different types of products, some of which come in pairs (like left and right earphones, pairs of shoes, etc.). These paired products have SN=2 (Serial Number Type 2) in the database. Historically, paired products had to be processed together - when one item was sold, its paired item would automatically be processed as well.

The new split pair functionality allows users to:
- Process only one item from a pair (e.g., sell just the left earphone without the right)
- Keep pairs together when desired (the default behavior)
- Make this choice on a per-product basis within the same order

This document explains how to implement this functionality in the frontend interface.

## Overview

The split pair functionality allows users to control whether paired products (SN=2) are processed together or separately when creating sales orders or moving stock. This guide provides recommendations for implementing this feature in the user interface.

## UI Design Recommendations

### Product Selection Interface

#### Table Layout Example
```
+-----------------+------------------+------------+---------------+
| Product Barcode | Product Name     | Quantity   | Split Pair    |
+-----------------+------------------+------------+---------------+
| TEST-PROD-A1    | Paired Headset   |     1      | [✓] Split     | <- SN=2 product (editable)
+-----------------+------------------+------------+---------------+
| TEST-PROD-B1    | Paired Speaker   |     1      | [  ] Split    | <- SN=2 product (editable)
+-----------------+------------------+------------+---------------+
| TEST-PROD-001   | Single Keyboard  |     1      |   N/A         | <- SN=1 product (disabled)
+-----------------+------------------+------------+---------------+
| TEST-BOX-NONSN  | Bulk Items       |     5      |   N/A         | <- SN=0 product (disabled)
+-----------------+------------------+------------+---------------+
```

#### Checkbox Behavior
- **Unchecked (default)**: Both items in the pair will be moved together
- **Checked**: Only the selected barcode will be moved, without its pair
- **Disabled/N/A**: For non-paired products (SN=0 or SN=1), the split pair option is not applicable

#### User Guidance
- Include a tooltip when hovering over the checkbox: "When checked, only this item will be processed without its paired item"
- Add a help icon or information panel explaining the split pair concept: "Paired products (SN=2) normally move together. The Split Pair option allows you to move only one of the items in a pair."

## Implementation Details

### Conditional Rendering

```javascript
function renderSplitPairControl(product) {
  if (product.numberSn === 2) {
    return (
      <Checkbox
        label="Split Pair"
        checked={product.splitPair || false}
        onChange={(e) => updateSplitPairSetting(product.id, e.target.checked)}
        tooltip="When checked, only this item will be processed without its paired item"
      />
    );
  } else {
    return (
      <span 
        className="text-muted" 
        title="Split pair option is only available for paired products"
      >
        N/A
      </span>
    );
  }
}
```

### Preparing API Request Data

```javascript
function prepareProductsForRequest(selectedProducts) {
  return selectedProducts.map(product => ({
    identifier: product.numberSn === 0 
      ? `${product.boxBarcode}:${product.quantity}` 
      : product.barcode,
    splitPair: product.numberSn === 2 ? product.splitPair : null
  }));
}
```

### Sample API Request

```javascript
// Example of submitting a sales order with product-specific split pair settings
async function submitSalesOrder() {
  const products = prepareProductsForRequest(selectedProducts);
  
  const orderData = {
    products: products,
    employeeId: selectedEmployee.id,
    shopName: selectedShop.name,
    note: orderNotes,
    orderId: generatedOrderId
  };
  
  try {
    const response = await fetch('/api/sales', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(orderData)
    });
    
    if (response.ok) {
      showSuccessMessage('Sales order created successfully');
    } else {
      showErrorMessage('Failed to create sales order');
    }
  } catch (error) {
    showErrorMessage(`Error: ${error.message}`);
  }
}
```

## Value Handling

For the backend API, use the following values for the `splitPair` parameter:

- **SN=2 products (paired)**: 
  - `true`: Split the pair (move only the specified barcode)
  - `false`: Keep the pair together (move both barcodes)
- **SN=0 and SN=1 products (non-paired)**:
  - `null`: Indicates the splitPair parameter is not applicable
  - Alternatively, these can be omitted when sending product-specific settings

## UI Enhancement Suggestions

### Basic Implementation
- Simple checkbox for each SN=2 product
- Tooltip explaining the behavior
- Disabled state for non-applicable products

### Intermediate Enhancements
- Visual indicator showing which products are paired
- Warning when splitting a pair that explains the consequences
- Ability to view the paired product's details

### Advanced Features
- Visual connection between paired products in the UI
- Preview of what will happen when splitting a pair
- Option to see inventory status of both paired items

## Testing Recommendations

1. **Verify rendering**: Ensure the split pair checkbox only appears for SN=2 products
2. **Test functionality**: Confirm that checking/unchecking the checkbox updates the internal state
3. **Validate API request**: Check that the correct `splitPair` values are sent to the API
4. **Test edge cases**:
   - Multiple paired products in the same order
   - Mix of paired and non-paired products
   - All products with split pair enabled/disabled

## Backend Integration Notes

- The backend expects the `products` array with `ProductIdentifierDTO` objects when using product-specific split pair settings
- The `splitPair` field in each product DTO controls the behavior for that specific product
- For backward compatibility, the global `splitPair` field is still supported at the root level of the request
- When both global and product-specific settings are provided, the product-specific settings take precedence

### API Endpoint Reference

The split pair functionality is implemented in these API endpoints:

1. **Sales Order API**: `POST /api/sales`
   ```json
   {
     "products": [
       {"identifier": "BARCODE123", "splitPair": true},
       {"identifier": "BARCODE456", "splitPair": false}
     ],
     "employeeId": "EMP101",
     "shopName": "Main Store",
     "orderId": "ORD-123"
   }
   ```

2. **Stock Movement API**: `POST /api/stock/move`
   ```
   Parameters:
   - boxBarcode=BOX-001
   - productBarcode=BARCODE123
   - quantity=1
   - destination=sales
   - splitPair=true
   ```

3. **General Order API**: `POST /api/orders/create`
   ```json
   {
     "products": [
       {"identifier": "BARCODE123", "splitPair": true}
     ],
     "destination": "sales",
     "employeeId": "EMP101",
     "shopName": "Main Store"
   }
   ```

## Mockup Ideas

Consider implementing one of these UI approaches:

### Option 1: Table with Split Column
```
| Product      | Type      | Quantity | Split Pair |
|--------------|-----------|----------|------------|
| TEST-PROD-A1 | Headset   | 1        | [✓]        |
| TEST-PROD-B1 | Speaker   | 1        | [ ]        |
| TEST-PROD-001| Keyboard  | 1        | [N/A]      |
```

### Option 2: Expandable Product Cards
```
┌─ TEST-PROD-A1: Headset (1) ───────────────┐
│                                           │
│  Location: Warehouse A                    │
│  Status: In Stock                         │
│                                           │
│  [✓] Split from pair                      │
└───────────────────────────────────────────┘

┌─ TEST-PROD-001: Keyboard (1) ─────────────┐
│                                           │
│  Location: Warehouse B                    │
│  Status: In Stock                         │
│                                           │
│  [ ] Split from pair (disabled)           │
└───────────────────────────────────────────┘
```

### Option 3: Grouped Paired Items
```
┌─ Paired Item Set: Headphones ─────────────┐
│                                           │
│  ● TEST-PROD-A1: Left Earphone            │
│    [✓] Process individually               │
│                                           │
│  ● TEST-PROD-A2: Right Earphone           │
│    [ ] Process individually               │
│                                           │
└───────────────────────────────────────────┘
```

## Implementation Timeline Recommendation

1. **Phase 1**: Basic implementation with checkbox UI
2. **Phase 2**: Add visual indicators for paired products
3. **Phase 3**: Implement advanced preview and selection features

## Terminology

- **SN=2 (Serial Number Type 2)**: Products that come in pairs and have traditionally been processed together
- **SN=1 (Serial Number Type 1)**: Individual serialized products (not paired)
- **SN=0 (Serial Number Type 0)**: Non-serialized products that are tracked by quantity
- **Split Pair**: Processing only one item from a paired set, rather than both items together
- **ProductIdentifierDTO**: A data structure used to represent a product with specific settings
- **Identifier**: String representing either a serialized product barcode or a "boxBarcode:quantity" format for non-serialized products 