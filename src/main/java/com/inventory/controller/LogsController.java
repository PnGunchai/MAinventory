package com.inventory.controller;

import com.inventory.model.Logs;
import com.inventory.service.LogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for logs operations
 */
@RestController
@RequestMapping("/api/logs")
public class LogsController {
    
    @Autowired
    private LogsService logsService;
    
    /**
     * Create a new log entry
     */
    @PostMapping
    public ResponseEntity<Logs> createLog(
            @RequestParam String boxBarcode,
            @RequestParam String productName,
            @RequestParam(required = false) String productBarcode,
            @RequestParam String operation) {
        
        return ResponseEntity.ok(logsService.createLog(boxBarcode, productName, productBarcode, operation));
    }
    
    /**
     * Get all logs
     */
    @GetMapping
    public ResponseEntity<List<Logs>> getAllLogs() {
        return ResponseEntity.ok(logsService.getAllLogs());
    }
    
    /**
     * Get logs by box barcode
     */
    @GetMapping("/box/{boxBarcode}")
    public ResponseEntity<List<Logs>> getLogsByBoxBarcode(@PathVariable String boxBarcode) {
        return ResponseEntity.ok(logsService.getLogsByBoxBarcode(boxBarcode));
    }
    
    /**
     * Get logs by operation
     */
    @GetMapping("/operation/{operation}")
    public ResponseEntity<List<Logs>> getLogsByOperation(@PathVariable String operation) {
        return ResponseEntity.ok(logsService.getLogsByOperation(operation));
    }
} 