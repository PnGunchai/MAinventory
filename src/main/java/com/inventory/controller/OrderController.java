package com.inventory.controller;

import com.inventory.dto.OrderDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for order operations
 */
@RestController
@RequestMapping("/api")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * Create a new order
     */
    @PostMapping("/orders/create")
    public ResponseEntity<Void> createOrder(@RequestBody OrderDTO orderDTO) {
        // Validate destination
        if (orderDTO.getDestination() == null || orderDTO.getDestination().isEmpty()) {
            throw new InvalidInputException("Destination is required");
        }
        
        String destination = orderDTO.getDestination().toLowerCase();
        
        // Validate orderId for sales and lent orders
        if ((destination.equals("sales") || destination.equals("lent")) && 
            (orderDTO.getOrderId() == null || orderDTO.getOrderId().trim().isEmpty())) {
            throw new InvalidInputException("Order ID is required for " + destination + " orders");
        }
        
        orderService.processOrder(orderDTO);
        return ResponseEntity.ok().build();
    }
} 