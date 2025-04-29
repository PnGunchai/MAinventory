package com.inventory.service;

import com.inventory.model.Logs;
import com.inventory.model.BoxNumber;
import com.inventory.model.CurrentStock;
import com.inventory.repository.LogsRepository;
import com.inventory.repository.BoxNumberRepository;
import com.inventory.repository.CurrentStockRepository;
import com.inventory.repository.InStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;

/**
 * Centralized service for managing product barcode lifecycle
 */
@Service
public class BarcodeRegistryService {
    
    private static final Logger logger = LoggerFactory.getLogger(BarcodeRegistryService.class);
    
    // Use a lock for critical sections to prevent race conditions
    private final ReentrantLock barcodeLock = new ReentrantLock();
    
    // Set of barcodes currently being processed
    private final Set<String> processingBarcodes = new HashSet<>();
    
    @Autowired
    private LogsRepository logsRepository;
    
    @Autowired
    private BoxNumberRepository boxNumberRepository;
    
    @Autowired
    private CurrentStockRepository currentStockRepository;
    
    @Autowired
    private InStockRepository inStockRepository;
    
    /**
     * Check if a barcode is available for use and lock it for processing
     * This method is thread-safe and prevents race conditions
     * 
     * @param barcode The barcode to check
     * @return true if the barcode is available and has been locked for processing
     */
    public boolean checkAndLockBarcode(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            logger.debug("Barcode is null or empty");
            return false;
        }
        
        barcodeLock.lock();
        try {
            logger.debug("Checking and locking barcode: {}", barcode);
            
            // If the barcode is already being processed, it's not available
            if (processingBarcodes.contains(barcode)) {
                logger.info("Barcode {} is already being processed", barcode);
                return false;
            }
            
            // Check if the barcode is available
            if (!isBarcodeAvailable(barcode)) {
                logger.info("Barcode {} is not available", barcode);
                return false;
            }
            
            // Lock the barcode for processing
            processingBarcodes.add(barcode);
            logger.info("Barcode {} is available and has been locked for processing", barcode);
            return true;
        } finally {
            barcodeLock.unlock();
        }
    }
    
    /**
     * Release a barcode that was locked for processing
     * 
     * @param barcode The barcode to release
     * @param wasSuccessful Whether the processing was successful
     */
    public void releaseBarcode(String barcode, boolean wasSuccessful) {
        if (barcode == null || barcode.isEmpty()) {
            return;
        }
        
        barcodeLock.lock();
        try {
            logger.debug("Releasing barcode: {}", barcode);
            processingBarcodes.remove(barcode);
            logger.info("Barcode {} has been released", barcode);
        } finally {
            barcodeLock.unlock();
        }
    }
    
    /**
     * Check if a barcode is available for use
     * A barcode is available if:
     * 1. It has never been used before, OR
     * 2. It was used but its last operation was one that makes it available for reuse
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    private boolean isBarcodeAvailable(String barcode) {
        // Check if barcode exists in box_number
        Optional<BoxNumber> boxNumber = boxNumberRepository.findByProductBarcode(barcode);
        if (boxNumber.isPresent()) {
            logger.debug("Barcode {} exists in box_number", barcode);
            
            // If it exists in box_number, check its status in logs
            List<Logs> logs = logsRepository.findByProductBarcode(barcode);
            if (logs.isEmpty()) {
                // If it exists in box_number but has no logs, it's not available
                logger.info("Barcode {} exists in box_number but has no logs, not available", barcode);
                return false;
            }
            
            // Get the most recent log entry
            Logs latestLog = logs.stream()
                    .max(Comparator.comparing(Logs::getTimestamp))
                    .orElse(null);
            
            if (latestLog == null) {
                // This shouldn't happen if logs is not empty
                logger.warn("No latest log found for barcode {} despite logs being present", barcode);
                return false;
            }
            
            String operation = latestLog.getOperation();
            logger.debug("Latest operation for barcode {}: {}", barcode, operation);
            
            // If the latest operation is "add", it's not available
            if ("add".equals(operation)) {
                logger.info("Barcode {} was added but never processed further, not available", barcode);
                return false;
            }
            
            // If the latest operation indicates the product is no longer active, it's available for reuse
            boolean isAvailable = operation != null && 
                    (operation.equals("move_to_broken") || 
                     operation.equals("remove") || 
                     operation.equals("move_to_sales") || 
                     operation.equals("returned"));
            
            if (isAvailable) {
                logger.info("Barcode {} is available for reuse (last operation: {})", barcode, operation);
            } else {
                logger.info("Barcode {} is still in use (last operation: {})", barcode, operation);
            }
            
            return isAvailable;
        }
        
        // Check if barcode exists in in_stock
        boolean existsInStock = inStockRepository.existsByProductBarcode(barcode);
        
        if (existsInStock) {
            logger.info("Barcode {} exists in in_stock, not available", barcode);
            return false;
        }
        
        // Check if barcode exists in logs
        List<Logs> logs = logsRepository.findByProductBarcode(barcode);
        if (!logs.isEmpty()) {
            // Get the most recent log entry
            Logs latestLog = logs.stream()
                    .max(Comparator.comparing(Logs::getTimestamp))
                    .orElse(null);
            
            if (latestLog != null) {
                String operation = latestLog.getOperation();
                
                // If the latest operation is "add", it's not available
                if ("add".equals(operation)) {
                    logger.info("Barcode {} was added (in logs only) but never processed further, not available", barcode);
                    return false;
                }
                
                // If the latest operation indicates the product is no longer active, it's available for reuse
                boolean isAvailable = operation != null && 
                        (operation.equals("move_to_broken") || 
                         operation.equals("remove") || 
                         operation.equals("move_to_sales") || 
                         operation.equals("returned"));
                
                if (isAvailable) {
                    logger.info("Barcode {} is available for reuse (last operation: {})", barcode, operation);
                } else {
                    logger.info("Barcode {} is still in use (last operation: {})", barcode, operation);
                }
                
                return isAvailable;
            }
        }
        
        // If not found anywhere, it's available
        logger.info("Barcode {} has never been used, available", barcode);
        return true;
    }
    
    /**
     * Check if multiple barcodes are available and lock them for processing
     * 
     * @param barcodes The barcodes to check
     * @return A list of barcodes that are not available
     */
    public List<String> checkAndLockBarcodes(List<String> barcodes) {
        List<String> unavailableBarcodes = new ArrayList<>();
        
        // First check if any barcodes are already in use in the database
        for (String barcode : barcodes) {
            if (barcode == null || barcode.isEmpty()) {
                continue;
            }
            
            // Check directly in the database without locking
            if (!isBarcodeAvailableInDatabase(barcode)) {
                logger.info("Barcode {} is not available in database", barcode);
                unavailableBarcodes.add(barcode);
            }
        }
        
        // If any barcodes are unavailable, return them without locking anything
        if (!unavailableBarcodes.isEmpty()) {
            return unavailableBarcodes;
        }
        
        // Now try to lock all barcodes
        barcodeLock.lock();
        try {
            // Check again if any barcodes are already being processed
            for (String barcode : barcodes) {
                if (barcode == null || barcode.isEmpty()) {
                    continue;
                }
                
                // If the barcode is already being processed, it's not available
                if (processingBarcodes.contains(barcode)) {
                    logger.info("Barcode {} is already being processed", barcode);
                    unavailableBarcodes.add(barcode);
                }
            }
            
            // If any barcodes are unavailable, return them without locking anything
            if (!unavailableBarcodes.isEmpty()) {
                return unavailableBarcodes;
            }
            
            // All barcodes are available, lock them all
            for (String barcode : barcodes) {
                if (barcode == null || barcode.isEmpty()) {
                    continue;
                }
                
                processingBarcodes.add(barcode);
                logger.info("Barcode {} is available and has been locked for processing", barcode);
            }
        } finally {
            barcodeLock.unlock();
        }
        
        return unavailableBarcodes;
    }
    
    /**
     * Release multiple barcodes that were locked for processing
     * 
     * @param barcodes The barcodes to release
     */
    public void releaseBarcodes(List<String> barcodes) {
        barcodeLock.lock();
        try {
            for (String barcode : barcodes) {
                if (barcode == null || barcode.isEmpty()) {
                    continue;
                }
                
                processingBarcodes.remove(barcode);
                logger.info("Barcode {} has been released", barcode);
            }
        } finally {
            barcodeLock.unlock();
        }
    }
    
    /**
     * Check if a barcode is available in the database without locking
     * This is a direct database check without any caching or locking
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    private boolean isBarcodeAvailableInDatabase(String barcode) {
        // Check if barcode exists in box_number
        Optional<BoxNumber> boxNumber = boxNumberRepository.findByProductBarcode(barcode);
        if (boxNumber.isPresent()) {
            // If it exists in box_number, check its status in logs
            List<Logs> logs = logsRepository.findByProductBarcode(barcode);
            if (logs.isEmpty()) {
                // If it exists in box_number but has no logs, it's not available
                return false;
            }
            
            // Get the most recent log entry
            Logs latestLog = logs.stream()
                    .max(Comparator.comparing(Logs::getTimestamp))
                    .orElse(null);
            
            if (latestLog == null) {
                return false;
            }
            
            String operation = latestLog.getOperation();
            
            // If the latest operation is "add", it's not available
            if ("add".equals(operation)) {
                return false;
            }
            
            // If the latest operation indicates the product is no longer active, it's available for reuse
            return operation != null && 
                    (operation.equals("move_to_broken") || 
                     operation.equals("remove") || 
                     operation.equals("move_to_sales") || 
                     operation.equals("returned"));
        }
        
        // Check if barcode exists in in_stock
        boolean existsInStock = inStockRepository.existsByProductBarcode(barcode);
        
        if (existsInStock) {
            return false;
        }
        
        // Check if barcode exists in logs
        List<Logs> logs = logsRepository.findByProductBarcode(barcode);
        if (!logs.isEmpty()) {
            // Get the most recent log entry
            Logs latestLog = logs.stream()
                    .max(Comparator.comparing(Logs::getTimestamp))
                    .orElse(null);
            
            if (latestLog != null) {
                String operation = latestLog.getOperation();
                
                // If the latest operation is "add", it's not available
                if ("add".equals(operation)) {
                    return false;
                }
                
                // If the latest operation indicates the product is no longer active, it's available for reuse
                return operation != null && 
                        (operation.equals("move_to_broken") || 
                         operation.equals("remove") || 
                         operation.equals("move_to_sales") || 
                         operation.equals("returned"));
            }
        }
        
        // If not found anywhere, it's available
        return true;
    }
} 