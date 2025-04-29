package com.inventory.repository;

import com.inventory.model.BarcodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BarcodeStatusRepository extends JpaRepository<BarcodeStatus, Long> {
    
    Optional<BarcodeStatus> findByProductBarcode(String productBarcode);
} 