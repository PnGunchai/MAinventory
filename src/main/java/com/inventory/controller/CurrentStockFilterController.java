package com.inventory.controller;

import com.inventory.dto.CurrentStockFilterDTO;
import com.inventory.dto.PageResponseDTO;
import com.inventory.model.CurrentStock;
import com.inventory.service.CurrentStockFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for filtering and paginating current stock
 */
@RestController
@RequestMapping("/api/stock/filter")
public class CurrentStockFilterController {
    
    @Autowired
    private CurrentStockFilterService currentStockFilterService;
    
    /**
     * Find current stock with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<PageResponseDTO<CurrentStock>> findCurrentStock(
            @RequestParam(required = false) String boxBarcode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer boxNumber,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastUpdated") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        CurrentStockFilterDTO filter = new CurrentStockFilterDTO();
        filter.setBoxBarcode(boxBarcode);
        filter.setProductName(productName);
        filter.setBoxNumber(boxNumber);
        filter.setMinQuantity(minQuantity);
        filter.setMaxQuantity(maxQuantity);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDirection(sortDirection);
        
        Page<CurrentStock> currentStock = currentStockFilterService.findCurrentStock(filter);
        
        return ResponseEntity.ok(PageResponseDTO.from(currentStock));
    }
    
    /**
     * Find current stock with filtering and pagination using POST
     */
    @PostMapping
    public ResponseEntity<PageResponseDTO<CurrentStock>> findCurrentStockPost(@RequestBody CurrentStockFilterDTO filter) {
        Page<CurrentStock> currentStock = currentStockFilterService.findCurrentStock(filter);
        
        return ResponseEntity.ok(PageResponseDTO.from(currentStock));
    }
} 