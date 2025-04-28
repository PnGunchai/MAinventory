# Frontend Implementation Notes: Lent Order Management

## Overview
This document outlines the frontend implementation recommendations for the lent order management feature, which allows users to view lent items and process them in batches (return to stock, mark as sold, mark as broken).

## API Endpoints to Use

1. **Get Lent Items by Order ID**:
   ```
   GET /api/lent/orders/{orderId}
   ```
   Returns all items in a specific lent order with their current status

2. **Process Multiple Items with Different Destinations**:
   ```
   POST /api/lent/orders/{orderId}/process
   ```
   Payload example:
   ```json
   {
     "employeeId": "EMP001",
     "shopName": "Main Store",
     "note": "Batch processing",
     "returnToStock": ["2001", "2003"],
     "moveToSales": ["2002", "2004"],
     "markAsBroken": ["2005"],
     "condition": "Damaged during use"
   }
   ```

## UI Components

### 1. Lent Order Search
- Order ID input field
- Search button
- Clear button
- Order history dropdown (optional)

### 2. Lent Items List
- Table with columns:
  - Checkbox for selection
  - Product Barcode
  - Box Barcode
  - Product Name
  - Lent Date
  - Current Status
  - Notes
- Pagination controls
- "Select All" checkbox
- Count of selected items

### 3. Action Buttons (enabled when items are selected)
- "Return to Stock" button
- "Mark as Sold" button
- "Mark as Broken" button
- "Process Selected" button

### 4. Modal Forms

#### Return to Stock Modal
- List of selected items
- Note field
- Confirm/Cancel buttons

#### Mark as Sold Modal
- List of selected items
- Shop name dropdown (required)
- Note field
- Confirm/Cancel buttons

#### Mark as Broken Modal
- List of selected items
- Condition dropdown/input (required)
- Note field
- Confirm/Cancel buttons

## User Flow

1. User navigates to "Manage Lent Orders" screen
2. User enters an order ID and clicks "Search"
3. System displays all lent items for that order ID
4. User selects items using checkboxes
5. User clicks one of the action buttons
6. System displays the appropriate modal
7. User completes the required fields and confirms
8. System submits the batch request to the API
9. On success, system refreshes the list and shows a success message
10. On failure, system shows error messages but maintains selections

## Implementation Considerations

### State Management
- Maintain selected items in local state
- Track API loading states
- Store form data for modals

### Error Handling
- Validate required fields before submission
- Handle API errors gracefully
- Provide clear error messages

### Optimizations
- Implement debounced search for large orders
- Use virtualized lists for performance with many items
- Consider websocket updates for real-time status changes

### Accessibility
- Ensure all interactive elements have keyboard support
- Use proper ARIA labels
- Maintain sufficient color contrast
- Support screen readers

## Example React Component Structure

```jsx
// Main components
<LentOrderManager>
  <LentOrderSearch />
  <LentItemsList>
    <LentItemRow />
  </LentItemsList>
  <ActionButtons />
  <ReturnModal />
  <SalesModal />
  <BrokenModal />
</LentOrderManager>
```

## Development Phases

1. **Phase 1: Basic Functionality**
   - Implement search and display of lent items
   - Add selection functionality
   - Create basic action modals

2. **Phase 2: Enhanced Features**
   - Add pagination and sorting
   - Implement batch processing
   - Add validation and error handling

3. **Phase 3: Optimizations**
   - Improve performance for large datasets
   - Add real-time updates
   - Enhance UI/UX with animations and feedback

## Testing Considerations

- Unit tests for component logic
- Integration tests for form submission
- E2E tests for complete user flows
- Accessibility testing
- Performance testing with large datasets 