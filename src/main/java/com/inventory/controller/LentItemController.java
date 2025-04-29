package com.inventory.controller;

import com.inventory.dto.LentItemUpdateDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.model.Lend;
import com.inventory.service.LentItemUpdateService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing lent items
 */
@RestController
@RequestMapping("/api/lent-items")
public class LentItemController {
    
    @Autowired
    private LentItemUpdateService lentItemUpdateService;
    
    /**
     * Get all lent items for a specific order
     * 
     * @param orderId The order ID to retrieve lent items for
     * @return List of lent items in the order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Lend>> getLentItemsByOrderId(@PathVariable String orderId) {
        List<Lend> lentItems = lentItemUpdateService.getLentItemsByOrderId(orderId);
        return ResponseEntity.ok(lentItems);
    }
    
    /**
     * Update the status of lent items (mark as returned or sold)
     * 
     * Updates the status of multiple lent items in a single request. Each item can be marked either as:
     * - "returned": The item is returned to stock
     * - "sold": The item is moved from lent to sales
     * 
     * When marking an item as "sold", you can optionally provide a salesOrderId to link the sale
     * to a specific sales order. If not provided, a sales order ID will be generated automatically
     * by appending "-SOLD" to the lent order ID.
     * 
     * Example Request:
     * {
     *   "orderId": "LENT123",
     *   "employeeId": "EMP101",
     *   "note": "Customer returned some items and purchased others",
     *   "items": [
     *     {"productBarcode": "PROD001", "boxBarcode": "BOX001", "status": "returned"},
     *     {"productBarcode": "PROD002", "boxBarcode": "BOX002", "status": "sold", "salesOrderId": "SALE456"}
     *   ]
     * }
     */
    @PostMapping("/update-status")
    public ResponseEntity<?> updateLentItemsStatus(@RequestBody LentItemUpdateDTO updateDTO) {
        // Validate required fields
        if (updateDTO.getOrderId() == null || updateDTO.getOrderId().trim().isEmpty()) {
            throw new InvalidInputException("Order ID is required");
        }
        
        if (updateDTO.getEmployeeId() == null || updateDTO.getEmployeeId().trim().isEmpty()) {
            throw new InvalidInputException("Employee ID is required");
        }
        
        if (updateDTO.getItems() == null || updateDTO.getItems().isEmpty()) {
            throw new InvalidInputException("No items specified for update");
        }
        
        // Process the update
        List<Lend> updatedItems = lentItemUpdateService.updateLentItemsStatus(updateDTO);
        
        // Return success response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Lent items updated successfully");
        response.put("updatedItemsCount", updatedItems.size());
        
        return ResponseEntity.ok(response);
    }
} 