package com.inventory.controller;

import com.inventory.dto.StockAdditionDTO;
import com.inventory.dto.BulkRemoveDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.model.CurrentStock;
import com.inventory.model.BoxNumber;
import com.inventory.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for stock operations
 */
@RestController
@RequestMapping("/api/stock")
public class StockController {
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private com.inventory.repository.BoxNumberRepository boxNumberRepository;
    
    /**
     * Add stock (single item)
     */
    @PostMapping("/add")
    public ResponseEntity<CurrentStock> addStock(
            @RequestParam String boxBarcode,
            @RequestParam(required = false) String productBarcode,
            @RequestParam int quantity,
            @RequestParam(required = false) String note) {
        
        return ResponseEntity.ok(stockService.addStock(boxBarcode, productBarcode, quantity, note));
    }
    
    /**
     * Add stock in bulk
     */
    @PostMapping("/add-bulk")
    public ResponseEntity<?> addStockBulk(@RequestBody StockAdditionDTO request) {
        try {
            List<CurrentStock> results = stockService.addStockBulk(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Stock added successfully");
            response.put("data", results);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * Remove stock
     */
    @PostMapping("/remove")
    public ResponseEntity<CurrentStock> removeStock(
            @RequestParam String boxBarcode,
            @RequestParam(required = false) String productBarcode,
            @RequestParam int quantity,
            @RequestParam(required = false) String note) {
        
        return ResponseEntity.ok(stockService.removeStock(boxBarcode, productBarcode, quantity, note));
    }
    
    /**
     * Remove stock in bulk
     */
    @PostMapping("/remove-bulk")
    public ResponseEntity<?> removeStockBulk(@RequestBody BulkRemoveDTO request) {
        try {
            List<CurrentStock> results = stockService.removeStockBulk(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Stock removed successfully");
            response.put("data", results);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * Get all stock
     */
    @GetMapping
    public ResponseEntity<List<CurrentStock>> getAllStock() {
        return ResponseEntity.ok(stockService.getAllStock());
    }
    
    /**
     * Get stock by box barcode
     */
    @GetMapping("/box/{boxBarcode}")
    public ResponseEntity<List<CurrentStock>> getStockByBoxBarcode(@PathVariable String boxBarcode) {
        return ResponseEntity.ok(stockService.getStockByBoxBarcode(boxBarcode));
    }
    
    /**
     * Get stock by box barcode and product name
     */
    @GetMapping("/box/{boxBarcode}/product/{productName}")
    public ResponseEntity<CurrentStock> getStockByBoxBarcodeAndProductName(
            @PathVariable String boxBarcode,
            @PathVariable String productName) {
        
        return ResponseEntity.ok(stockService.getStockByBoxBarcodeAndProductName(boxBarcode, productName));
    }
    
    /**
     * Move stock to another destination (sales, lent, broken)
     * 
     * For SN=2 products (paired products):
     * - By default, both products in a pair will be moved together
     * - Set splitPair=true to move only the specified barcode without its pair
     */
    @PostMapping("/move")
    public ResponseEntity<Void> moveStock(
            @RequestParam String boxBarcode,
            @RequestParam(required = false) String productBarcode,
            @RequestParam int quantity,
            @RequestParam String destination,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false, defaultValue = "false") boolean splitPair) {
        
        // Validate shopName is provided when destination is "sales"
        if ("sales".equals(destination) && (shopName == null || shopName.isEmpty())) {
            throw new InvalidInputException("Shop name is required when moving items to sales");
        }
        
        stockService.moveStock(boxBarcode, productBarcode, quantity, 
                              destination, employeeId, shopName, condition, note, orderId, splitPair);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Return a lent item
     */
    @PostMapping("/return")
    public ResponseEntity<CurrentStock> returnLentItem(
            @RequestParam String boxBarcode,
            @RequestParam String productBarcode,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String lentId,
            @RequestParam(required = false) Integer quantity) {
        
        CurrentStock stock = stockService.returnLentItem(boxBarcode, productBarcode, note, lentId, quantity);
        return ResponseEntity.ok(stock);
    }
    
    /**
     * Recombine two product barcodes from different pairs into a new pair in a new box
     */
    @PostMapping("/recombine")
    public ResponseEntity<CurrentStock> recombineItems(
            @RequestParam String targetBoxBarcode,
            @RequestParam String productBarcode1,
            @RequestParam String productBarcode2,
            @RequestParam(required = false, defaultValue = "Items recombined") String note) {
        
        CurrentStock stock = stockService.recombineItems(
                targetBoxBarcode, productBarcode1, productBarcode2, note);
        
        return ResponseEntity.ok(stock);
    }

    /**
     * Get all box numbers with their associated barcodes
     */
    @GetMapping("/box-numbers")
    public ResponseEntity<List<BoxNumber>> getAllBoxNumbers() {
        List<BoxNumber> boxNumbers = boxNumberRepository.findAll();
        return ResponseEntity.ok(boxNumbers);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<Map<String, String>> handleInvalidInputException(InvalidInputException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }
} 