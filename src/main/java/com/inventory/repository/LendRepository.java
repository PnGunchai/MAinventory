package com.inventory.repository;

import com.inventory.model.Lend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for lent items
 */
@Repository
public interface LendRepository extends JpaRepository<Lend, Long> {
    
    /**
     * Find all lent items by order ID
     */
    List<Lend> findByOrderId(String orderId);
    
    /**
     * Find a lent item by product barcode and order ID
     */
    Optional<Lend> findByProductBarcodeAndOrderId(String productBarcode, String orderId);
    
    /**
     * Find a lent item by box barcode and product barcode
     */
    Optional<Lend> findByBoxBarcodeAndProductBarcode(String boxBarcode, String productBarcode);
    
    /**
     * Find lent items by box barcode
     */
    List<Lend> findByBoxBarcode(String boxBarcode);
    
    /**
     * Find lent items by box barcode and product name
     */
    List<Lend> findByBoxBarcodeAndProductName(String boxBarcode, String productName);
    
    /**
     * Find lent items by employee ID
     */
    List<Lend> findByEmployeeId(String employeeId);
    
    /**
     * Find lent items by shop name
     */
    List<Lend> findByShopName(String shopName);
    
    /**
     * Find lent items by product barcode
     */
    List<Lend> findByProductBarcode(String productBarcode);
    
    /**
     * Find lent items by box barcode and order ID
     */
    List<Lend> findByBoxBarcodeAndOrderId(String boxBarcode, String orderId);
    
    /**
     * Update lent item status by product barcode and order ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE Lend l SET l.status = :status WHERE l.productBarcode = :productBarcode AND l.orderId = :orderId")
    int updateStatusByProductBarcodeAndOrderId(@Param("productBarcode") String productBarcode, 
                                            @Param("orderId") String orderId,
                                            @Param("status") String status);

    Page<Lend> findByTimestampBetween(ZonedDateTime start, ZonedDateTime end, Pageable pageable);
    Page<Lend> findByBoxBarcode(String boxBarcode, Pageable pageable);
    Page<Lend> findByProductBarcode(String productBarcode, Pageable pageable);
    Page<Lend> findByOrderId(String orderId, Pageable pageable);
    Page<Lend> findByStatus(String status, Pageable pageable);

    /**
     * Find the latest lent record by product barcode and order ID
     */
    @Query("SELECT l FROM Lend l WHERE l.productBarcode = :productBarcode AND l.orderId = :orderId ORDER BY l.timestamp DESC")
    Optional<Lend> findLatestByProductBarcodeAndOrderId(@Param("productBarcode") String productBarcode, @Param("orderId") String orderId);
} 