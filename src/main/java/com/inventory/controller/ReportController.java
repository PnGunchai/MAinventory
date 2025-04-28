package com.inventory.controller;

import com.inventory.dto.CurrentInventoryDTO;
import com.inventory.dto.InventoryMovementDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;
    
    /**
     * Get current inventory levels
     */
    @GetMapping("/inventory/current")
    public ResponseEntity<List<CurrentInventoryDTO>> getCurrentInventoryLevels(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") boolean includeZeroQuantity) {
        return ResponseEntity.ok(reportService.getCurrentInventoryLevels(category, includeZeroQuantity));
    }
    
    /**
     * Get inventory movement analysis
     */
    @GetMapping("/inventory/movement")
    public ResponseEntity<List<InventoryMovementDTO>> getInventoryMovementAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String boxBarcode) {
        
        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new InvalidInputException("Start date must be before end date");
        }
        
        // Validate date range is not too large (e.g., more than 1 year)
        if (startDate.plusYears(1).isBefore(endDate)) {
            throw new InvalidInputException("Date range cannot exceed 1 year");
        }
        
        return ResponseEntity.ok(reportService.getInventoryMovementAnalysis(startDate, endDate, boxBarcode));
    }
} 