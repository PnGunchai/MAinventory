package com.inventory.controller;

import com.inventory.dto.LentOrderDTO;
import com.inventory.dto.LentItemBatchProcessDTO;
import com.inventory.dto.OrderSummaryDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.model.Lend;
import com.inventory.model.LentId;
import com.inventory.service.LentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for lent order operations
 */
@RestController
@RequestMapping("/api/lent-orders")
public class LentOrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(LentOrderController.class);
    
    @Autowired
    private LentOrderService lentOrderService;
    
    /**
     * Create a new lent order
     * 
     * For SN=2 products (paired products), there are two ways to control pair splitting:
     * 
     * 1. Global setting: Set splitPair=true in the root of the DTO to split all pairs
     * Example:
     * {
     *   "orderId": "LENT123",
     *   "employeeId": "EMP101",
     *   "shopName": "Main Store",
     *   "productIdentifiers": ["BARCODE123", "BARCODE456"],
     *   "splitPair": true
     * }
     * 
     * 2. Product-specific setting: Use the products array with ProductIdentifierDTO objects
     * Example:
     * {
     *   "orderId": "LENT123",
     *   "employeeId": "EMP101",
     *   "shopName": "Main Store",
     *   "products": [
     *     {"identifier": "BARCODE123", "splitPair": true},
     *     {"identifier": "BARCODE456", "splitPair": false}
     *   ]
     * }
     * 
     * This allows splitting some pairs while keeping others together in the same lent order.
     * 
     * You can also specify different destinations for each product using the "destination" field:
     * - "lent" (default): The product will be moved to lent status
     * - "return": The product will be marked as lent and then immediately returned to stock
     * - "sales": The product will be moved directly to sales instead of lent
     * 
     * Example with mixed destinations:
     * {
     *   "orderId": "LENT123",
     *   "employeeId": "EMP101",
     *   "shopName": "Main Store",
     *   "products": [
     *     {"identifier": "BARCODE123", "destination": "lent"},      // Move to lent
     *     {"identifier": "BARCODE456", "destination": "return"},    // Move to lent and immediately return
     *     {"identifier": "BARCODE789", "destination": "sales"}      // Move directly to sales
     *   ]
     * }
     */
    @PostMapping
    public ResponseEntity<?> createLentOrder(@RequestBody LentOrderDTO lentOrderDTO) {
        // Validate required fields
        if (lentOrderDTO.getShopName() == null || lentOrderDTO.getShopName().trim().isEmpty()) {
            throw new InvalidInputException("Shop name is required for lent orders");
        }
        
        if (lentOrderDTO.getEmployeeId() == null || lentOrderDTO.getEmployeeId().trim().isEmpty()) {
            throw new InvalidInputException("Employee ID is required for lent orders");
        }
        
        if (lentOrderDTO.getOrderId() == null || lentOrderDTO.getOrderId().trim().isEmpty()) {
            throw new InvalidInputException("Order ID is required for lent orders");
        }
        
        // Process the lent order
        lentOrderService.processLentOrder(lentOrderDTO);
        
        // Return success response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Lent order created successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all lent items for a specific order ID
     * 
     * @param orderId The order ID to fetch lent items for
     * @return List of lent items with their details
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<Lend>> getLentOrderItems(@PathVariable String orderId) {
        List<Lend> lentItems = lentOrderService.getLentItemsByOrderId(orderId);
        
        return ResponseEntity.ok(lentItems);
    }
    
    /**
     * Process multiple lent items from an order with different destinations
     * 
     * @param orderId The order ID to process items for
     * @param request The request body containing items and their destinations
     * @return Success response
     */
    @PostMapping("/orders/{orderId}/process")
    public ResponseEntity<?> processLentOrderItems(
            @PathVariable String orderId, 
            @RequestBody LentItemBatchProcessDTO request) {
        
        // Validate the employee ID is provided
        if (request.getEmployeeId() == null || request.getEmployeeId().trim().isEmpty()) {
            throw new InvalidInputException("Employee ID is required for processing lent items");
        }
        
        // Process the items based on their destinations
        lentOrderService.processBatchLentItems(orderId, request);
        
        // Return success response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Lent items processed successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all lent orders with optional status filtering and pagination
     * 
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @param status Optional status filter ("active", "returned", or "all")
     * @param sort Sort field
     * @param direction Sort direction
     * @return Paginated list of lent orders
     */
    @GetMapping
    public ResponseEntity<?> getLentOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "timestamp") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        // Validate status parameter
        if (!status.equals("all") && !status.equals("active") && !status.equals("returned")) {
            throw new InvalidInputException("Invalid status. Must be 'active', 'returned', or 'all'");
        }
        
        // Map frontend field names to entity field names
        String sortField = sort;
        if (sort.equals("orderId")) {
            sortField = "lentId";
        }
        
        // Create pageable object for pagination
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        
        // Get paginated orders with status filtering
        Page<LentId> orders = lentOrderService.getLentOrders(status, pageable);
        
        // Create response with pagination info
        Map<String, Object> response = new HashMap<>();
        response.put("content", orders.getContent());
        response.put("totalElements", orders.getTotalElements());
        response.put("totalPages", orders.getTotalPages());
        response.put("number", orders.getNumber());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all lent orders with total items and employee ID
     * 
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @param status Optional status filter ("active", "returned", or "all")
     * @param sort Sort field
     * @param direction Sort direction
     * @return Paginated list of lent orders with total items and employee ID
     */
    @GetMapping("/summary")
    public ResponseEntity<Page<OrderSummaryDTO>> getLentOrdersSummary(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "timestamp") String sort,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        logger.info("Getting lent orders summary with status: {}, page: {}, size: {}, sort: {}, direction: {}", 
                 status, page, size, sort, direction);
                 
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<OrderSummaryDTO> summaries = lentOrderService.getLentOrdersSummary(status, pageable);
        return ResponseEntity.ok(summaries);
    }
} 