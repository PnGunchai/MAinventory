package com.inventory.controller;

import com.inventory.dto.BrokenOrderDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.service.BrokenOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for broken order operations
 */
@RestController
@RequestMapping("/api/broken")
public class BrokenOrderController {
    
    @Autowired
    private BrokenOrderService brokenOrderService;
    
    /**
     * Create a new broken order
     * 
     * For SN=2 products (paired products), there are two ways to control pair splitting:
     * 
     * 1. Global setting: Set splitPair=true in the root of the DTO to split all pairs
     * Example:
     * {
     *   "orderId": "BROKEN123",
     *   "employeeId": "EMP101",
     *   "condition": "Damaged",
     *   "productIdentifiers": ["BARCODE123", "BARCODE456"],
     *   "splitPair": true
     * }
     * 
     * 2. Product-specific setting: Use the products array with ProductIdentifierDTO objects
     * Example:
     * {
     *   "orderId": "BROKEN123",
     *   "employeeId": "EMP101",
     *   "condition": "Damaged",
     *   "products": [
     *     {"identifier": "BARCODE123", "splitPair": true},
     *     {"identifier": "BARCODE456", "splitPair": false}
     *   ]
     * }
     * 
     * This allows splitting some pairs while keeping others together in the same broken order.
     */
    @PostMapping
    public ResponseEntity<?> createBrokenOrder(@RequestBody BrokenOrderDTO brokenOrderDTO) {
        // Validate required fields
        if (brokenOrderDTO.getCondition() == null || brokenOrderDTO.getCondition().trim().isEmpty()) {
            throw new InvalidInputException("Condition is required for broken orders");
        }
        
        // Process the broken order
        brokenOrderService.processBrokenOrder(brokenOrderDTO);
        
        // Return success response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Broken order created successfully");
        
        return ResponseEntity.ok(response);
    }
} 