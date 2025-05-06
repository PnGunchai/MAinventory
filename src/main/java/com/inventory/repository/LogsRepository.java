package com.inventory.repository;

import com.inventory.model.Logs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Repository interface for Logs entity
 */
@Repository
public interface LogsRepository extends JpaRepository<Logs, Long>, JpaSpecificationExecutor<Logs> {
    
    /**
     * Find logs by box barcode
     */
    List<Logs> findByBoxBarcode(String boxBarcode);
    
    /**
     * Find logs by product barcode
     */
    List<Logs> findByProductBarcode(String productBarcode);
    
    /**
     * Find the box barcode for a product barcode, considering all operations
     * to ensure we don't miss any valid product locations
     */
    @Query(value = "SELECT l.box_barcode FROM logs l " +
           "WHERE l.product_barcode = ?1 " +
           "ORDER BY l.timestamp DESC LIMIT 1", 
           nativeQuery = true)
    Optional<String> findBoxBarcodeByProductBarcode(String productBarcode);

    /**
     * Find logs by box barcode and product name
     */
    List<Logs> findByBoxBarcodeAndProductName(String boxBarcode, String productName);

    /**
     * Find logs by timestamp between start and end time
     */
    List<Logs> findByTimestampBetween(ZonedDateTime startTime, ZonedDateTime endTime);

    /**
     * Find logs by timestamp between start and end time and box barcode
     */
    List<Logs> findByTimestampBetweenAndBoxBarcode(ZonedDateTime startTime, ZonedDateTime endTime, String boxBarcode);

    /**
     * Count logs by box barcode, product name, and box number
     */
    long countByBoxBarcodeAndProductNameAndBoxNumber(String boxBarcode, String productName, Integer boxNumber);

    /**
     * Count logs by box barcode, product name, box number, and operation
     */
    long countByBoxBarcodeAndProductNameAndBoxNumberAndOperation(
        String boxBarcode, String productName, Integer boxNumber, String operation);

    /**
     * Count logs by box barcode, product name, and operation
     */
    long countByBoxBarcodeAndProductNameAndOperation(
        String boxBarcode, String productName, String operation);

    /**
     * Count logs by box barcode, product name, box number, and operation starting with
     */
    @Query("SELECT COUNT(l) FROM Logs l WHERE l.boxBarcode = ?1 AND l.productName = ?2 AND l.boxNumber = ?3 AND l.operation LIKE CONCAT(?4, '%')")
    long countByBoxBarcodeAndProductNameAndBoxNumberAndOperationStartingWith(
        String boxBarcode, String productName, Integer boxNumber, String operationPrefix);

    /**
     * Find logs by box barcode, product name, and product barcode
     */
    List<Logs> findByBoxBarcodeAndProductNameAndProductBarcode(
        String boxBarcode, String productName, String productBarcode);

    /**
     * Find logs by operation
     */
    List<Logs> findByOperation(String operation);

    Optional<Logs> findTopByBoxBarcodeAndProductNameAndProductBarcodeOrderByTimestampDesc(
            String boxBarcode, String productName, String productBarcode);

    List<Logs> findByBoxBarcodeAndProductNameAndBoxNumber(
            String boxBarcode, String productName, Integer boxNumber);

    /**
     * Check if a product barcode exists
     */
    @Query("SELECT COUNT(l) > 0 FROM Logs l WHERE l.productBarcode = ?1")
    boolean existsByProductBarcode(String productBarcode);
    
    /**
     * Find logs by box barcode with pagination
     */
    Page<Logs> findByBoxBarcode(String boxBarcode, Pageable pageable);
    
    /**
     * Find logs by product barcode with pagination
     */
    Page<Logs> findByProductBarcode(String productBarcode, Pageable pageable);
    
    /**
     * Find logs by operation with pagination
     */
    Page<Logs> findByOperation(String operation, Pageable pageable);
    
    /**
     * Find logs by timestamp between start and end time with pagination
     */
    Page<Logs> findByTimestampBetween(ZonedDateTime startTime, ZonedDateTime endTime, Pageable pageable);
    
    /**
     * Find logs by box barcode and timestamp between with pagination
     */
    Page<Logs> findByBoxBarcodeAndTimestampBetween(
            String boxBarcode, ZonedDateTime startTime, ZonedDateTime endTime, Pageable pageable);
    
    /**
     * Find logs by product barcode and timestamp between with pagination
     */
    Page<Logs> findByProductBarcodeAndTimestampBetween(
            String productBarcode, ZonedDateTime startTime, ZonedDateTime endTime, Pageable pageable);
    
    /**
     * Find logs by operation and timestamp between with pagination
     */
    Page<Logs> findByOperationAndTimestampBetween(
            String operation, ZonedDateTime startTime, ZonedDateTime endTime, Pageable pageable);

    Page<Logs> findByOrderId(String orderId, Pageable pageable);

    /**
     * Find logs by box barcode and order ID
     */
    List<Logs> findByBoxBarcodeAndOrderId(String boxBarcode, String orderId);
} 