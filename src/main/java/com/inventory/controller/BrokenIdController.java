package com.inventory.controller;

import com.inventory.model.BrokenId;
import com.inventory.repository.BrokenIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for broken ID operations
 */
@RestController
@RequestMapping("/api/broken-ids")
public class BrokenIdController {
    
    @Autowired
    private BrokenIdRepository brokenIdRepository;
    
    /**
     * Get all broken IDs
     */
    @GetMapping
    public ResponseEntity<List<BrokenId>> getAllBrokenIds() {
        return ResponseEntity.ok(brokenIdRepository.findAll());
    }
    
    /**
     * Get broken ID by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BrokenId> getBrokenIdById(@PathVariable String id) {
        return brokenIdRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Search broken IDs by date range
     */
    @GetMapping("/search/date-range")
    public ResponseEntity<List<BrokenId>> searchByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        return ResponseEntity.ok(brokenIdRepository.findByTimestampBetween(startDateTime, endDateTime));
    }
    
    /**
     * Search broken IDs by note content
     */
    @GetMapping("/search/note")
    public ResponseEntity<List<BrokenId>> searchByNote(@RequestParam String query) {
        return ResponseEntity.ok(brokenIdRepository.findByNoteContaining(query));
    }
    
    /**
     * Report: Get broken items count by day
     */
    @GetMapping("/report/daily-count")
    public ResponseEntity<Long> getDailyCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);
        
        List<BrokenId> brokenItems = brokenIdRepository.findByTimestampBetween(startDateTime, endDateTime);
        return ResponseEntity.ok((long) brokenItems.size());
    }
    
    /**
     * Report: Get broken items for current month
     */
    @GetMapping("/report/current-month")
    public ResponseEntity<List<BrokenId>> getCurrentMonthBrokenItems() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        LocalDateTime startDateTime = firstDayOfMonth.atStartOfDay();
        LocalDateTime endDateTime = lastDayOfMonth.atTime(LocalTime.MAX);
        
        return ResponseEntity.ok(brokenIdRepository.findByTimestampBetween(startDateTime, endDateTime));
    }
} 