package com.inventory.controller;

import com.inventory.model.*;
import com.inventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Controller for accessing all table data
 * Provides paginated access to all database tables with search functionality
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    @Autowired private LendRepository lendRepository;
    @Autowired private LogsRepository logsRepository;
    @Autowired private LentIdRepository lentIdRepository;
    @Autowired private InStockRepository inStockRepository;
    @Autowired private BulkLogsRepository bulkLogsRepository;
    @Autowired private BrokenIdRepository brokenIdRepository;
    @Autowired private BrokenRepository brokenRepository;
    @Autowired private BarcodeStatusRepository barcodeStatusRepository;
    @Autowired private InvoiceRepository invoiceRepository;

    /**
     * Get all lent items with search and pagination
     */
    @GetMapping("/lent")
    public ResponseEntity<Page<Lend>> getAllLent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(required = false) String boxBarcode,
            @RequestParam(required = false) String productBarcode,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            return ResponseEntity.ok(lendRepository.findByTimestampBetween(start, end, pageable));
        }
        
        if (boxBarcode != null) {
            return ResponseEntity.ok(lendRepository.findByBoxBarcode(boxBarcode, pageable));
        }
        
        if (productBarcode != null) {
            return ResponseEntity.ok(lendRepository.findByProductBarcode(productBarcode, pageable));
        }
        
        if (orderId != null) {
            return ResponseEntity.ok(lendRepository.findByOrderId(orderId, pageable));
        }
        
        if (status != null) {
            return ResponseEntity.ok(lendRepository.findByStatus(status, pageable));
        }
        
        return ResponseEntity.ok(lendRepository.findAll(pageable));
    }

    /**
     * Get all logs with search and pagination
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<Logs>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(required = false) String boxBarcode,
            @RequestParam(required = false) String productBarcode,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            return ResponseEntity.ok(logsRepository.findByTimestampBetween(start, end, pageable));
        }
        
        if (boxBarcode != null) {
            return ResponseEntity.ok(logsRepository.findByBoxBarcode(boxBarcode, pageable));
        }
        
        if (productBarcode != null) {
            return ResponseEntity.ok(logsRepository.findByProductBarcode(productBarcode, pageable));
        }
        
        if (operation != null) {
            return ResponseEntity.ok(logsRepository.findByOperation(operation, pageable));
        }
        
        if (orderId != null) {
            return ResponseEntity.ok(logsRepository.findByOrderId(orderId, pageable));
        }
        
        return ResponseEntity.ok(logsRepository.findAll(pageable));
    }

    /**
     * Get all lent order IDs with pagination
     */
    @GetMapping("/lent-ids")
    public ResponseEntity<Page<LentId>> getAllLentIds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(lentIdRepository.findAll(pageable));
    }

    /**
     * Get all in-stock items with pagination
     */
    @GetMapping("/in-stock")
    public ResponseEntity<Page<InStock>> getAllInStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(inStockRepository.findAll(pageable));
    }

    /**
     * Get all bulk operation logs with pagination
     */
    @GetMapping("/bulk-logs")
    public ResponseEntity<Page<BulkLogs>> getAllBulkLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(bulkLogsRepository.findAll(pageable));
    }

    /**
     * Get all broken item IDs with pagination
     */
    @GetMapping("/broken-ids")
    public ResponseEntity<Page<BrokenId>> getAllBrokenIds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(brokenIdRepository.findAll(pageable));
    }

    /**
     * Get all broken items with search and pagination
     */
    @GetMapping("/broken")
    public ResponseEntity<Page<Broken>> getAllBroken(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(required = false) String boxBarcode,
            @RequestParam(required = false) String productBarcode,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            return ResponseEntity.ok(brokenRepository.findByTimestampBetween(start, end, pageable));
        }
        
        if (boxBarcode != null) {
            return ResponseEntity.ok(brokenRepository.findByBoxBarcode(boxBarcode, pageable));
        }
        
        if (productBarcode != null) {
            return ResponseEntity.ok(brokenRepository.findByProductBarcode(productBarcode, pageable));
        }
        
        if (condition != null) {
            return ResponseEntity.ok(brokenRepository.findByCondition(condition, pageable));
        }
        
        return ResponseEntity.ok(brokenRepository.findAll(pageable));
    }

    /**
     * Get all barcode statuses with pagination
     */
    @GetMapping("/barcode-status")
    public ResponseEntity<Page<BarcodeStatus>> getAllBarcodeStatus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(barcodeStatusRepository.findAll(pageable));
    }

    /**
     * Get all invoices with pagination
     */
    @GetMapping("/invoices")
    public ResponseEntity<Page<Invoice>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(invoiceRepository.findAll(pageable));
    }
} 