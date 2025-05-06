package com.inventory.repository;

import com.inventory.model.Sales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Sales entity
 */
@Repository
public interface SalesRepository extends JpaRepository<Sales, Long>, JpaSpecificationExecutor<Sales> {
    
    /**
     * Find sales by order ID
     */
    List<Sales> findByOrderId(String orderId);
    
    /**
     * Find sales by employee ID
     */
    List<Sales> findByEmployeeId(String employeeId);
    
    /**
     * Find sales by shop name
     */
    List<Sales> findByShopName(String shopName);
    
    /**
     * Find sales by box barcode
     */
    List<Sales> findByBoxBarcode(String boxBarcode);
    
    /**
     * Find sales by product barcode
     */
    List<Sales> findByProductBarcode(String productBarcode);
    
    /**
     * Find sales by product name
     */
    List<Sales> findByProductName(String productName);
    
    /**
     * Find sales by timestamp between start and end time
     */
    List<Sales> findByTimestampBetween(ZonedDateTime startTime, ZonedDateTime endTime);
    
    /**
     * Get total sales count by day
     */
    @Query("SELECT COUNT(s) FROM Sales s WHERE DATE(s.timestamp) = DATE(?1)")
    Long countByDay(ZonedDateTime date);
    
    /**
     * Get total sales quantity by day
     */
    @Query("SELECT SUM(s.quantity) FROM Sales s WHERE DATE(s.timestamp) = DATE(?1)")
    Integer sumQuantityByDay(ZonedDateTime date);
    
    /**
     * Get sales by employee ID and date range
     */
    List<Sales> findByEmployeeIdAndTimestampBetween(
            String employeeId, ZonedDateTime startTime, ZonedDateTime endTime);
    
    /**
     * Get sales by shop name and date range
     */
    List<Sales> findByShopNameAndTimestampBetween(
            String shopName, ZonedDateTime startTime, ZonedDateTime endTime);
    
    // New methods for pagination
    
    /**
     * Find sales by order ID with pagination
     */
    Page<Sales> findByOrderId(String orderId, Pageable pageable);
    
    /**
     * Find sales by employee ID with pagination
     */
    Page<Sales> findByEmployeeId(String employeeId, Pageable pageable);
    
    /**
     * Find sales by shop name with pagination
     */
    Page<Sales> findByShopName(String shopName, Pageable pageable);
    
    /**
     * Find sales by box barcode with pagination
     */
    Page<Sales> findByBoxBarcode(String boxBarcode, Pageable pageable);
    
    /**
     * Find sales by product barcode with pagination
     */
    Page<Sales> findByProductBarcode(String productBarcode, Pageable pageable);
    
    /**
     * Find sales by product name with pagination
     */
    Page<Sales> findByProductName(String productName, Pageable pageable);
    
    /**
     * Find sales by timestamp between start and end time with pagination
     */
    Page<Sales> findByTimestampBetween(ZonedDateTime startTime, ZonedDateTime endTime, Pageable pageable);
    
    /**
     * Get sales by employee ID and date range with pagination
     */
    Page<Sales> findByEmployeeIdAndTimestampBetween(
            String employeeId, ZonedDateTime startTime, ZonedDateTime endTime, Pageable pageable);
    
    /**
     * Get sales by shop name and date range with pagination
     */
    Page<Sales> findByShopNameAndTimestampBetween(
            String shopName, ZonedDateTime startTime, ZonedDateTime endTime, Pageable pageable);

    Optional<Sales> findByOrderIdAndProductBarcode(String orderId, String productBarcode);
    
    Optional<Sales> findByOrderIdAndBoxBarcode(String orderId, String boxBarcode);
} 