package com.inventory.controller;

import com.inventory.dto.OrderSummaryDTO;
import com.inventory.model.Sales;
import com.inventory.model.Invoice;
import com.inventory.repository.SalesRepository;
import com.inventory.repository.InvoiceRepository;
import com.inventory.service.SalesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for sales operations
 */
@RestController
@RequestMapping("/api/sales")
public class SalesController {
    
    private static final Logger logger = LoggerFactory.getLogger(SalesController.class);
    
    @Autowired
    private SalesRepository salesRepository;
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    @Autowired
    private SalesService salesService;
    
    /**
     * Get all sales orders with pagination
     */
    @GetMapping
    public ResponseEntity<Page<Invoice>> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<Invoice> orders = invoiceRepository.findAll(pageRequest);
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Get sales by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Sales> getSalesById(@PathVariable Long id) {
        return salesRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Search sales by order ID
     */
    @GetMapping("/search/order/{orderId}")
    public ResponseEntity<List<Sales>> searchByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(salesRepository.findByOrderId(orderId));
    }
    
    /**
     * Search sales by employee ID
     */
    @GetMapping("/search/employee/{employeeId}")
    public ResponseEntity<List<Sales>> searchByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity.ok(salesRepository.findByEmployeeId(employeeId));
    }
    
    /**
     * Search sales by shop name
     */
    @GetMapping("/search/shop/{shopName}")
    public ResponseEntity<List<Sales>> searchByShopName(@PathVariable String shopName) {
        return ResponseEntity.ok(salesRepository.findByShopName(shopName));
    }
    
    /**
     * Search sales by box barcode
     */
    @GetMapping("/search/box/{boxBarcode}")
    public ResponseEntity<List<Sales>> searchByBoxBarcode(@PathVariable String boxBarcode) {
        return ResponseEntity.ok(salesRepository.findByBoxBarcode(boxBarcode));
    }
    
    /**
     * Search sales by product barcode
     */
    @GetMapping("/search/product-barcode/{productBarcode}")
    public ResponseEntity<List<Sales>> searchByProductBarcode(@PathVariable String productBarcode) {
        return ResponseEntity.ok(salesRepository.findByProductBarcode(productBarcode));
    }
    
    /**
     * Search sales by product name
     */
    @GetMapping("/search/product-name/{productName}")
    public ResponseEntity<List<Sales>> searchByProductName(@PathVariable String productName) {
        return ResponseEntity.ok(salesRepository.findByProductName(productName));
    }
    
    /**
     * Search sales by date range
     */
    @GetMapping("/search/date-range")
    public ResponseEntity<List<Sales>> searchByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        return ResponseEntity.ok(salesRepository.findByTimestampBetween(startDateTime, endDateTime));
    }
    
    /**
     * Report: Get sales count by day
     */
    @GetMapping("/report/daily-count")
    public ResponseEntity<Long> getDailyCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDateTime dateTime = date.atStartOfDay();
        return ResponseEntity.ok(salesRepository.countByDay(dateTime));
    }
    
    /**
     * Report: Get sales quantity by day
     */
    @GetMapping("/report/daily-quantity")
    public ResponseEntity<Integer> getDailyQuantity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDateTime dateTime = date.atStartOfDay();
        Integer quantity = salesRepository.sumQuantityByDay(dateTime);
        return ResponseEntity.ok(quantity != null ? quantity : 0);
    }
    
    /**
     * Report: Get sales for current month
     */
    @GetMapping("/report/current-month")
    public ResponseEntity<List<Sales>> getCurrentMonthSales() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        LocalDateTime startDateTime = firstDayOfMonth.atStartOfDay();
        LocalDateTime endDateTime = lastDayOfMonth.atTime(LocalTime.MAX);
        
        return ResponseEntity.ok(salesRepository.findByTimestampBetween(startDateTime, endDateTime));
    }
    
    /**
     * Report: Get sales by employee for date range
     */
    @GetMapping("/report/employee/{employeeId}/date-range")
    public ResponseEntity<List<Sales>> getEmployeeSalesByDateRange(
            @PathVariable String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        return ResponseEntity.ok(salesRepository.findByEmployeeIdAndTimestampBetween(
                employeeId, startDateTime, endDateTime));
    }
    
    /**
     * Report: Get sales by shop for date range
     */
    @GetMapping("/report/shop/{shopName}/date-range")
    public ResponseEntity<List<Sales>> getShopSalesByDateRange(
            @PathVariable String shopName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        return ResponseEntity.ok(salesRepository.findByShopNameAndTimestampBetween(
                shopName, startDateTime, endDateTime));
    }
} 