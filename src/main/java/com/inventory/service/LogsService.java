package com.inventory.service;

import com.inventory.model.Logs;
import com.inventory.repository.LogsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Service for logs operations
 */
@Service
public class LogsService {
    
    @Autowired
    private LogsRepository logsRepository;
    
    @Autowired
    private BarcodeRegistryService barcodeRegistryService;
    
    /**
     * Create a new log entry with all possible parameters
     */
    @Transactional
    public Logs createLog(String boxBarcode, String productName, String productBarcode, String operation, 
                         String note, Integer boxNumber, Integer quantity, String orderId) {
        Logs log = new Logs();
        log.setBoxBarcode(boxBarcode);
        log.setProductName(productName);
        log.setProductBarcode(productBarcode);
        log.setOperation(operation);
        log.setTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Bangkok")));
        log.setNote(note);
        log.setBoxNumber(boxNumber);
        log.setQuantity(quantity != null ? quantity : 1);
        log.setOrderId(orderId);
        
        // Notify the BarcodeRegistryService
        if (productBarcode != null && !productBarcode.isEmpty()) {
            // We don't need to do anything here anymore, as the BarcodeRegistryService
            // now uses a different approach with checkAndLockBarcode and releaseBarcode
            // The StockService will handle locking and releasing barcodes directly
        }
        
        return logsRepository.save(log);
    }
    
    /**
     * Create a new log entry with quantity parameter
     */
    @Transactional
    public Logs createLog(String boxBarcode, String productName, String productBarcode, String operation, 
                         String note, Integer boxNumber, int quantity) {
        return createLog(boxBarcode, productName, productBarcode, operation, note, boxNumber, quantity, null);
    }
    
    /**
     * Create a new log entry with required parameters and box number
     */
    @Transactional
    public Logs createLog(String boxBarcode, String productName, String productBarcode, String operation, 
                         String note, Integer boxNumber) {
        return createLog(boxBarcode, productName, productBarcode, operation, note, boxNumber, 1, null);
    }
    
    /**
     * Create a new log entry with basic parameters
     */
    @Transactional
    public Logs createLog(String boxBarcode, String productName, String productBarcode, String operation, String note) {
        return createLog(boxBarcode, productName, productBarcode, operation, note, null, 1, null);
    }

    /**
     * Create a new log entry with minimal parameters
     */
    @Transactional
    public Logs createLog(String boxBarcode, String productName, String productBarcode, String operation) {
        return createLog(boxBarcode, productName, productBarcode, operation, null, null, 1, null);
    }
    
    /**
     * Save a log entry
     */
    @Transactional
    public Logs save(Logs log) {
        return logsRepository.save(log);
    }
    
    /**
     * Get all logs
     */
    public List<Logs> getAllLogs() {
        return logsRepository.findAll();
    }
    
    /**
     * Get logs by box barcode
     */
    public List<Logs> getLogsByBoxBarcode(String boxBarcode) {
        return logsRepository.findByBoxBarcode(boxBarcode);
    }
    
    /**
     * Get logs by product barcode
     */
    public List<Logs> getLogsByProductBarcode(String productBarcode) {
        return logsRepository.findByProductBarcode(productBarcode);
    }
    
    /**
     * Get logs by box barcode and product name
     */
    public List<Logs> getLogsByBoxBarcodeAndProductName(String boxBarcode, String productName) {
        return logsRepository.findByBoxBarcodeAndProductName(boxBarcode, productName);
    }
    
    /**
     * Get logs by operation
     */
    public List<Logs> getLogsByOperation(String operation) {
        return logsRepository.findAll().stream()
                .filter(log -> log.getOperation().equals(operation))
                .toList();
    }
    
    /**
     * Get logs by timestamp between start and end time
     */
    public List<Logs> getLogsByTimestampBetween(ZonedDateTime startTime, ZonedDateTime endTime) {
        return logsRepository.findByTimestampBetween(startTime, endTime);
    }
    
    /**
     * Get logs by timestamp between start and end time and box barcode
     */
    public List<Logs> getLogsByTimestampBetweenAndBoxBarcode(ZonedDateTime startTime, ZonedDateTime endTime, String boxBarcode) {
        return logsRepository.findByTimestampBetweenAndBoxBarcode(startTime, endTime, boxBarcode);
    }

    /**
     * Get logs by box barcode, product name, and product barcode
     */
    public List<Logs> getLogsByBoxBarcodeAndProductNameAndProductBarcode(
            String boxBarcode, String productName, String productBarcode) {
        return logsRepository.findByBoxBarcodeAndProductNameAndProductBarcode(
                boxBarcode, productName, productBarcode);
    }

    /**
     * Find logs by box barcode
     */
    public List<Logs> findByBoxBarcode(String boxBarcode) {
        return logsRepository.findByBoxBarcode(boxBarcode);
    }

    /**
     * Find logs by product barcode
     */
    public List<Logs> findByProductBarcode(String productBarcode) {
        return logsRepository.findByProductBarcode(productBarcode);
    }
} 