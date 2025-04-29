package com.inventory.repository;

import com.inventory.model.InStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for in_stock table
 */
@Repository
public interface InStockRepository extends JpaRepository<InStock, Long> {
    
    /**
     * Find by product barcode
     */
    Optional<InStock> findByProductBarcode(String productBarcode);
    
    /**
     * Find by box barcode
     */
    List<InStock> findByBoxBarcode(String boxBarcode);
    
    /**
     * Find by box barcode and product name
     */
    List<InStock> findByBoxBarcodeAndProductName(String boxBarcode, String productName);
    
    /**
     * Find by box number
     */
    List<InStock> findByBoxNumber(Integer boxNumber);
    
    /**
     * Check if product barcode exists
     */
    boolean existsByProductBarcode(String productBarcode);
    
    /**
     * Delete by product barcode
     */
    void deleteByProductBarcode(String productBarcode);
    
    /**
     * Count by box barcode and product name
     */
    long countByBoxBarcodeAndProductName(String boxBarcode, String productName);
} 