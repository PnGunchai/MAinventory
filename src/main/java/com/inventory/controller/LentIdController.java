package com.inventory.controller;

import com.inventory.model.LentId;
import com.inventory.repository.LentIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for lent ID operations
 */
@RestController
@RequestMapping("/api/lent-ids")
public class LentIdController {
    
    @Autowired
    private LentIdRepository lentIdRepository;
    
    /**
     * Get all lent IDs with pagination and optional status filter
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @param status Filter by status ("active" or "completed")
     * @param sort Sort field
     * @param direction Sort direction ("asc" or "desc")
     */
    @GetMapping
    public ResponseEntity<?> getAllLentIds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "timestamp") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        if (status != null && !status.isEmpty()) {
            return ResponseEntity.ok(lentIdRepository.findByStatus(status, pageable));
        }
        
        return ResponseEntity.ok(lentIdRepository.findAll(pageable));
    }
    
    /**
     * Get lent ID by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LentId> getLentIdById(@PathVariable String id) {
        return lentIdRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Search lent IDs by employee ID
     */
    @GetMapping("/search/employee/{employeeId}")
    public ResponseEntity<List<LentId>> searchByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity.ok(lentIdRepository.findByEmployeeId(employeeId));
    }
    
    /**
     * Search lent IDs by shop name
     */
    @GetMapping("/search/shop/{shopName}")
    public ResponseEntity<List<LentId>> searchByShopName(@PathVariable String shopName) {
        return ResponseEntity.ok(lentIdRepository.findByShopName(shopName));
    }
    
    /**
     * Search lent IDs by date range
     */
    @GetMapping("/search/date")
    public ResponseEntity<List<LentId>> searchByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        return ResponseEntity.ok(lentIdRepository.findByTimestampBetween(startDateTime, endDateTime));
    }
    
    /**
     * Report: Get all active lent items
     */
    @GetMapping("/report/active")
    public ResponseEntity<List<LentId>> getActiveLentItems() {
        return ResponseEntity.ok(lentIdRepository.findByStatus("active"));
    }
    
    /**
     * Report: Get all returned lent items
     */
    @GetMapping("/report/returned")
    public ResponseEntity<List<LentId>> getReturnedLentItems() {
        return ResponseEntity.ok(lentIdRepository.findByStatus("returned"));
    }
    
    /**
     * Report: Get active lent items by employee
     */
    @GetMapping("/report/active/employee/{employeeId}")
    public ResponseEntity<List<LentId>> getActiveLentItemsByEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(lentIdRepository.findByEmployeeIdAndStatus(employeeId, "active"));
    }
    
    /**
     * Report: Get active lent items by shop
     */
    @GetMapping("/report/active/shop/{shopName}")
    public ResponseEntity<List<LentId>> getActiveLentItemsByShop(@PathVariable String shopName) {
        return ResponseEntity.ok(lentIdRepository.findByShopNameAndStatus(shopName, "active"));
    }
} 