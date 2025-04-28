package com.inventory.service;

import com.inventory.model.BarcodeStatus;
import com.inventory.repository.BarcodeStatusRepository;
import com.inventory.repository.BoxNumberRepository;
import com.inventory.repository.CurrentStockRepository;
import com.inventory.repository.ProductCatalogRepository;
import com.inventory.repository.InStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing barcode status
 */
@Service
public class BarcodeStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(BarcodeStatusService.class);
    
    @Autowired
    private BarcodeStatusRepository barcodeStatusRepository;
    
    /**
     * Mark a barcode as in use
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void markBarcodeInUse(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return;
        }
        
        logger.info("Marking barcode as in use: {}", barcode);
        
        BarcodeStatus status = barcodeStatusRepository.findByProductBarcode(barcode)
                .orElse(new BarcodeStatus());
        
        status.setProductBarcode(barcode);
        status.setStatus("IN_USE");
        status.setLastUpdated(LocalDateTime.now());
        
        barcodeStatusRepository.save(status);
    }
    
    /**
     * Mark a barcode as available
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void markBarcodeAvailable(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return;
        }
        
        logger.info("Marking barcode as available: {}", barcode);
        
        BarcodeStatus status = barcodeStatusRepository.findByProductBarcode(barcode)
                .orElse(new BarcodeStatus());
        
        status.setProductBarcode(barcode);
        status.setStatus("AVAILABLE");
        status.setLastUpdated(LocalDateTime.now());
        
        barcodeStatusRepository.save(status);
    }
} 