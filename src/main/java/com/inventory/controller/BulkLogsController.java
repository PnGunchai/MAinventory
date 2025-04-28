package com.inventory.controller;

import com.inventory.model.BulkLogs;
import com.inventory.service.BulkLogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for bulk logs operations
 */
@RestController
@RequestMapping("/api/bulk-logs")
public class BulkLogsController {
    
    @Autowired
    private BulkLogsService bulkLogsService;
    
    /**
     * Generate bulk logs for a specific date
     */
    @PostMapping("/generate")
    public ResponseEntity<List<BulkLogs>> generateBulkLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        return ResponseEntity.ok(bulkLogsService.generateBulkLogsForDate(date));
    }
    
    /**
     * Get bulk logs by date
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<BulkLogs>> getBulkLogsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        return ResponseEntity.ok(bulkLogsService.getBulkLogsByDate(date));
    }
    
    /**
     * Get bulk logs by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<BulkLogs>> getBulkLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return ResponseEntity.ok(bulkLogsService.getBulkLogsByDateRange(startDate, endDate));
    }
    
    /**
     * Get bulk logs by box barcode
     */
    @GetMapping("/box/{boxBarcode}")
    public ResponseEntity<List<BulkLogs>> getBulkLogsByBoxBarcode(@PathVariable String boxBarcode) {
        return ResponseEntity.ok(bulkLogsService.getBulkLogsByBoxBarcode(boxBarcode));
    }
    
    /**
     * Get bulk logs by product name
     */
    @GetMapping("/product/{productName}")
    public ResponseEntity<List<BulkLogs>> getBulkLogsByProductName(@PathVariable String productName) {
        return ResponseEntity.ok(bulkLogsService.getBulkLogsByProductName(productName));
    }
    
    /**
     * Get bulk logs by operation
     */
    @GetMapping("/operation/{operation}")
    public ResponseEntity<List<BulkLogs>> getBulkLogsByOperation(@PathVariable String operation) {
        return ResponseEntity.ok(bulkLogsService.getBulkLogsByOperation(operation));
    }
} 