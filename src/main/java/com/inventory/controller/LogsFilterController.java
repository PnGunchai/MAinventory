package com.inventory.controller;

import com.inventory.dto.LogsFilterDTO;
import com.inventory.dto.PageResponseDTO;
import com.inventory.model.Logs;
import com.inventory.service.LogsFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 * Controller for filtering and paginating logs
 */
@RestController
@RequestMapping("/api/logs/filter")
public class LogsFilterController {
    
    @Autowired
    private LogsFilterService logsFilterService;
    
    /**
     * Find logs with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<PageResponseDTO<Logs>> findLogs(
            @RequestParam(required = false) String boxBarcode,
            @RequestParam(required = false) String productBarcode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) ZonedDateTime startDate,
            @RequestParam(required = false) ZonedDateTime endDate,
            @RequestParam(required = false) Integer boxNumber,
            @RequestParam(required = false) String orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        LogsFilterDTO filter = new LogsFilterDTO();
        filter.setBoxBarcode(boxBarcode);
        filter.setProductBarcode(productBarcode);
        filter.setProductName(productName);
        filter.setOperation(operation);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setBoxNumber(boxNumber);
        filter.setOrderId(orderId);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDirection(sortDirection);
        
        Page<Logs> logs = logsFilterService.findLogs(filter);
        
        return ResponseEntity.ok(PageResponseDTO.from(logs));
    }
    
    /**
     * Find logs with filtering and pagination using POST
     */
    @PostMapping
    public ResponseEntity<PageResponseDTO<Logs>> findLogsPost(@RequestBody LogsFilterDTO filter) {
        Page<Logs> logs = logsFilterService.findLogs(filter);
        
        return ResponseEntity.ok(PageResponseDTO.from(logs));
    }
} 