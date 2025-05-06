package com.inventory.controller;

import com.inventory.dto.SalesFilterDTO;
import com.inventory.dto.PageResponseDTO;
import com.inventory.model.Sales;
import com.inventory.service.SalesFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 * Controller for filtering and paginating sales
 */
@RestController
@RequestMapping("/api/sales/filter")
public class SalesFilterController {
    
    @Autowired
    private SalesFilterService salesFilterService;
    
    /**
     * Find sales with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<PageResponseDTO<Sales>> findSales(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String boxBarcode,
            @RequestParam(required = false) String productBarcode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) ZonedDateTime startDate,
            @RequestParam(required = false) ZonedDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        SalesFilterDTO filter = new SalesFilterDTO();
        filter.setOrderId(orderId);
        filter.setEmployeeId(employeeId);
        filter.setShopName(shopName);
        filter.setBoxBarcode(boxBarcode);
        filter.setProductBarcode(productBarcode);
        filter.setProductName(productName);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDirection(sortDirection);
        
        Page<Sales> sales = salesFilterService.findSales(filter);
        
        return ResponseEntity.ok(PageResponseDTO.from(sales));
    }
    
    /**
     * Find sales with filtering and pagination using POST
     */
    @PostMapping
    public ResponseEntity<PageResponseDTO<Sales>> findSalesPost(@RequestBody SalesFilterDTO filter) {
        Page<Sales> sales = salesFilterService.findSales(filter);
        
        return ResponseEntity.ok(PageResponseDTO.from(sales));
    }
} 