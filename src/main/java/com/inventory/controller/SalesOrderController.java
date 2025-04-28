package com.inventory.controller;

import com.inventory.dto.SalesOrderDTO;
import com.inventory.dto.OrderItemDTO;
import com.inventory.dto.OrderNoteDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.service.SalesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for sales order operations
 */
@RestController
@RequestMapping("/api/sales")
public class SalesOrderController {
    
    @Autowired
    private SalesOrderService salesOrderService;
    
    /**
     * Create a new sales order
     * 
     * For SN=2 products (paired products), there are two ways to control pair splitting:
     * 
     * 1. Global setting: Set splitPair=true in the root of the DTO to split all pairs
     * Example:
     * {
     *   "orderId": "ORDER123",
     *   "employeeId": "EMP101",
     *   "shopName": "Main Store",
     *   "productIdentifiers": ["BARCODE123", "BARCODE456"],
     *   "splitPair": true
     * }
     * 
     * 2. Product-specific setting: Use the products array with ProductIdentifierDTO objects
     * Example:
     * {
     *   "orderId": "ORDER123",
     *   "employeeId": "EMP101",
     *   "shopName": "Main Store",
     *   "products": [
     *     {"identifier": "BARCODE123", "splitPair": true},
     *     {"identifier": "BARCODE456", "splitPair": false}
     *   ]
     * }
     * 
     * This allows splitting some pairs while keeping others together in the same order.
     */
    @PostMapping
    public ResponseEntity<?> createSalesOrder(@RequestBody SalesOrderDTO salesOrderDTO) {
        // Validate required fields
        if (salesOrderDTO.getShopName() == null || salesOrderDTO.getShopName().trim().isEmpty()) {
            throw new InvalidInputException("Shop name is required for sales orders");
        }
        
        if (salesOrderDTO.getOrderId() == null || salesOrderDTO.getOrderId().trim().isEmpty()) {
            throw new InvalidInputException("Order ID is required for sales orders");
        }
        
        // Process the sales order
        salesOrderService.processSalesOrder(salesOrderDTO);
        
        // Return success response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sales order created successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing sales order by adding new items
     */
    @PostMapping("/orders/{orderId}/items")
    public ResponseEntity<?> addItemsToOrder(
            @PathVariable String orderId,
            @RequestBody OrderItemDTO itemDTO) {
        
        salesOrderService.addItemsToOrder(orderId, itemDTO);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Items added to order successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Remove items from an existing sales order
     */
    @DeleteMapping("/orders/{orderId}/items/{productBarcode}")
    public ResponseEntity<?> removeItemFromOrder(
            @PathVariable String orderId,
            @PathVariable String productBarcode) {
        
        salesOrderService.removeItemFromOrder(orderId, productBarcode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Item removed from order successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update order notes
     */
    @PutMapping("/orders/{orderId}/notes")
    public ResponseEntity<?> updateOrderNotes(
            @PathVariable String orderId,
            @RequestBody OrderNoteDTO noteDTO) {
        
        salesOrderService.updateOrderNotes(orderId, noteDTO.getNote());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Order notes updated successfully");
        
        return ResponseEntity.ok(response);
    }
} 