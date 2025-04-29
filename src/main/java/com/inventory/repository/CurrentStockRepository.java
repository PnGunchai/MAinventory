package com.inventory.repository;

import com.inventory.model.CurrentStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CurrentStock entity
 */
@Repository
public interface CurrentStockRepository extends JpaRepository<CurrentStock, Long>, JpaSpecificationExecutor<CurrentStock> {
    
    /**
     * Find stock by box barcode
     */
    List<CurrentStock> findByBoxBarcode(String boxBarcode);
    
    /**
     * Find stock by box barcode and product name
     * Changed to return Optional to handle multiple results properly
     */
    Optional<CurrentStock> findByBoxBarcodeAndProductName(String boxBarcode, String productName);
    
    /**
     * Find stock by box barcode with pagination
     */
    Page<CurrentStock> findByBoxBarcode(String boxBarcode, Pageable pageable);
    
    /**
     * Find stock by product name with pagination
     */
    Page<CurrentStock> findByProductName(String productName, Pageable pageable);
    
    /**
     * Find stock by box number with pagination
     */
    Page<CurrentStock> findByBoxNumber(Integer boxNumber, Pageable pageable);
    
    /**
     * Find stock by quantity greater than or equal to
     */
    Page<CurrentStock> findByQuantityGreaterThanEqual(Integer minQuantity, Pageable pageable);
    
    /**
     * Find stock by quantity less than or equal to
     */
    Page<CurrentStock> findByQuantityLessThanEqual(Integer maxQuantity, Pageable pageable);
    
    /**
     * Find stock by quantity between
     */
    Page<CurrentStock> findByQuantityBetween(Integer minQuantity, Integer maxQuantity, Pageable pageable);
} 