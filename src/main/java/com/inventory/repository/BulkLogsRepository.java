package com.inventory.repository;

import com.inventory.model.BulkLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for BulkLogs entity
 */
@Repository
public interface BulkLogsRepository extends JpaRepository<BulkLogs, Long> {
    
    /**
     * Find bulk logs by date
     */
    List<BulkLogs> findByDate(LocalDate date);
    
    /**
     * Find bulk logs by box barcode
     */
    List<BulkLogs> findByBoxBarcode(String boxBarcode);
    
    /**
     * Find bulk logs by product name
     */
    List<BulkLogs> findByProductName(String productName);
    
    /**
     * Find bulk logs by operation
     */
    List<BulkLogs> findByOperation(String operation);
    
    /**
     * Find bulk logs by box barcode and product name
     */
    List<BulkLogs> findByBoxBarcodeAndProductName(String boxBarcode, String productName);
    
    /**
     * Find bulk logs by date range
     */
    List<BulkLogs> findByDateBetween(LocalDate startDate, LocalDate endDate);
} 