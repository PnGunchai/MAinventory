package com.inventory.controller;

import com.inventory.model.BoxNumber;
import com.inventory.service.BoxNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for box number operations
 */
@RestController
@RequestMapping("/api/box-numbers")
public class BoxNumberController {
    
    @Autowired
    private BoxNumberService boxNumberService;
    
    /**
     * Create a new box number
     */
    @PostMapping
    public ResponseEntity<BoxNumber> createBoxNumber(
            @RequestParam String boxBarcode,
            @RequestParam(required = false) String productBarcode) {
        
        return ResponseEntity.ok(boxNumberService.createBoxNumberIfNeeded(boxBarcode, null, productBarcode));
    }
    
    /**
     * Get the highest box number for a product
     */
    @GetMapping("/highest")
    public ResponseEntity<Integer> getHighestBoxNumber(
            @RequestParam String boxBarcode,
            @RequestParam String productName) {
        
        Integer highestBoxNumber = boxNumberService.getHighestBoxNumber(boxBarcode, productName);
        
        if (highestBoxNumber == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(highestBoxNumber);
    }
    
    /**
     * Get the next available box number for a product
     */
    @GetMapping("/next")
    public ResponseEntity<Integer> getNextBoxNumber(
            @RequestParam String boxBarcode,
            @RequestParam String productName) {
        
        return ResponseEntity.ok(boxNumberService.getNextBoxNumber(boxBarcode, productName));
    }
} 