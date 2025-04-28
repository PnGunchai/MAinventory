package com.inventory.controller;

import com.inventory.model.CurrentStock;
import com.inventory.model.ProductCatalog;
import com.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for inventory management operations
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    @Autowired
    private InventoryService inventoryService;
    
    /**
     * Add a new product to the catalog
     */
    @PostMapping("/products")
    public ResponseEntity<ProductCatalog> addProduct(@RequestBody ProductCatalog product) {
        return ResponseEntity.ok(inventoryService.addProductToCatalog(product));
    }
    
    /**
     * Get all products from catalog
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductCatalog>> getAllProducts() {
        return ResponseEntity.ok(inventoryService.getAllProducts());
    }
    
    /**
     * Get product by box barcode
     */
    @GetMapping("/products/{boxBarcode}")
    public ResponseEntity<ProductCatalog> getProductByBoxBarcode(@PathVariable String boxBarcode) {
        ProductCatalog product = inventoryService.getProductByBoxBarcode(boxBarcode);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }
    
    /**
     * Add stock for a product
     */
    @PostMapping("/stock")
    public ResponseEntity<CurrentStock> addStock(
            @RequestParam String boxBarcode,
            @RequestParam String productName,
            @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.addStock(boxBarcode, productName, quantity));
    }
    
    /**
     * Get all stock
     */
    @GetMapping("/stock")
    public ResponseEntity<List<CurrentStock>> getAllStock() {
        return ResponseEntity.ok(inventoryService.getAllStock());
    }
    
    /**
     * Get stock by box barcode
     */
    @GetMapping("/stock/{boxBarcode}")
    public ResponseEntity<List<CurrentStock>> getStockByBoxBarcode(@PathVariable String boxBarcode) {
        return ResponseEntity.ok(inventoryService.getStockByBoxBarcode(boxBarcode));
    }
} 