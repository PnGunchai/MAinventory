package com.inventory.controller;

import com.inventory.model.InStock;
import com.inventory.service.InStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing in-stock items
 */
@RestController
@RequestMapping("/api/in-stock")
public class InStockController {
    
    @Autowired
    private InStockService inStockService;
    
    /**
     * Get all in-stock items
     */
    @GetMapping
    public ResponseEntity<List<InStock>> getAllInStock() {
        return ResponseEntity.ok(inStockService.getAllInStock());
    }
    
    /**
     * Get in-stock items by box barcode
     */
    @GetMapping("/box/{boxBarcode}")
    public ResponseEntity<List<InStock>> getInStockByBoxBarcode(@PathVariable String boxBarcode) {
        return ResponseEntity.ok(inStockService.getInStockByBoxBarcode(boxBarcode));
    }
    
    /**
     * Get in-stock item by product barcode
     */
    @GetMapping("/product/{productBarcode}")
    public ResponseEntity<InStock> getInStockByProductBarcode(@PathVariable String productBarcode) {
        return ResponseEntity.ok(inStockService.getInStockByProductBarcode(productBarcode));
    }
} 