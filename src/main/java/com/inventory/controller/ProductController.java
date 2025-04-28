package com.inventory.controller;

import com.inventory.model.ProductCatalog;
import com.inventory.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for product catalog operations
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    /**
     * Add a new product
     */
    @PostMapping
    public ResponseEntity<ProductCatalog> addProduct(
            @RequestParam String boxBarcode,
            @RequestParam String productName,
            @RequestParam Integer numberSn) {
        
        return ResponseEntity.ok(productService.addProduct(boxBarcode, productName, numberSn));
    }
    
    /**
     * Add a new product with special logging behavior
     */
    @PostMapping("/with-logs")
    public ResponseEntity<ProductCatalog> addProductWithLogs(
            @RequestBody ProductCatalog product,
            @RequestParam(required = false) String productBarcode) {
        
        return ResponseEntity.ok(productService.addProductWithLogs(product, productBarcode));
    }
    
    /**
     * Get a product by box barcode
     */
    @GetMapping("/{boxBarcode}")
    public ResponseEntity<ProductCatalog> getProduct(@PathVariable String boxBarcode) {
        return ResponseEntity.ok(productService.getProduct(boxBarcode));
    }
    
    /**
     * Get all products with sorting and pagination
     */
    @GetMapping
    public ResponseEntity<List<ProductCatalog>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(productService.getAllProducts(page, size, sort, direction));
    }
    
    /**
     * Update a product
     */
    @PutMapping("/{boxBarcode}")
    public ResponseEntity<ProductCatalog> updateProduct(
            @PathVariable String boxBarcode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer numberSn) {
        
        return ResponseEntity.ok(productService.updateProduct(boxBarcode, productName, numberSn));
    }
    
    /**
     * Delete a product
     */
    @DeleteMapping("/{boxBarcode}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String boxBarcode) {
        productService.deleteProduct(boxBarcode);
        return ResponseEntity.noContent().build();
    }
} 