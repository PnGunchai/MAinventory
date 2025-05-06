package com.inventory.repository;

import com.inventory.model.Broken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Repository interface for Broken entity
 */
@Repository
public interface BrokenRepository extends JpaRepository<Broken, Long> {
    
    /**
     * Find broken items by box barcode
     */
    List<Broken> findByBoxBarcode(String boxBarcode);
    
    /**
     * Find broken items by product barcode
     */
    List<Broken> findByProductBarcode(String productBarcode);

    Page<Broken> findByTimestampBetween(ZonedDateTime start, ZonedDateTime end, Pageable pageable);
    Page<Broken> findByBoxBarcode(String boxBarcode, Pageable pageable);
    Page<Broken> findByProductBarcode(String productBarcode, Pageable pageable);
    Page<Broken> findByCondition(String condition, Pageable pageable);

    List<Broken> findByTimestampBetween(ZonedDateTime startTime, ZonedDateTime endTime);
} 