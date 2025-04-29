package com.inventory.repository;

import com.inventory.model.BoxNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BoxNumber entity
 */
@Repository
public interface BoxNumberRepository extends JpaRepository<BoxNumber, Long> {
    
    /**
     * Find box number by box barcode
     */
    List<BoxNumber> findByBoxBarcode(String boxBarcode);
    
    /**
     * Find box number by box barcode and product name
     */
    Optional<BoxNumber> findByBoxBarcodeAndProductName(String boxBarcode, String productName);
    
    /**
     * Find box number by box number
     */
    List<BoxNumber> findByBoxNumber(Integer boxNumber);
    
    /**
     * Find the highest box number for a given box barcode and product name
     */
    @Query("SELECT MAX(b.boxNumber) FROM BoxNumber b WHERE b.boxBarcode = ?1 AND b.productName = ?2")
    Optional<Integer> findHighestBoxNumber(String boxBarcode, String productName);
    
    /**
     * Find box number by product barcode
     */
    Optional<BoxNumber> findByProductBarcode(String productBarcode);
    
    /**
     * Find the maximum box number for a given box barcode and product name
     */
    @Query("SELECT MAX(b.boxNumber) FROM BoxNumber b WHERE b.boxBarcode = :boxBarcode AND b.productName = :productName")
    Optional<Integer> findMaxBoxNumberByBoxBarcodeAndProductName(String boxBarcode, String productName);

    /**
     * Check if a product barcode exists
     */
    @Query("SELECT COUNT(b) > 0 FROM BoxNumber b WHERE b.productBarcode = ?1")
    boolean existsByProductBarcode(String productBarcode);
    
    /**
     * Find box numbers by box barcode and box number
     */
    List<BoxNumber> findByBoxBarcodeAndBoxNumber(String boxBarcode, Integer boxNumber);
} 