package com.inventory.service;

import com.inventory.model.BulkLogs;
import com.inventory.model.Logs;
import com.inventory.repository.BulkLogsRepository;
import com.inventory.repository.LogsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for bulk logs operations
 */
@Service
public class BulkLogsService {

    @Autowired
    private LogsRepository logsRepository;
    
    @Autowired
    private BulkLogsRepository bulkLogsRepository;
    
    /**
     * Generate bulk logs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void generateDailyBulkLogs() {
        // Get yesterday's date
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // Generate bulk logs for yesterday
        generateBulkLogsForDate(yesterday);
    }
    
    /**
     * Manually generate bulk logs for a specific date
     */
    @Transactional
    public List<BulkLogs> generateBulkLogsForDate(LocalDate date) {
        // Delete existing bulk logs for the date to avoid duplicates
        List<BulkLogs> existingLogs = bulkLogsRepository.findByDate(date);
        if (!existingLogs.isEmpty()) {
            bulkLogsRepository.deleteAll(existingLogs);
        }
        
        // Get start and end time for the date
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);
        
        // Get all logs for the date
        List<Logs> logs = logsRepository.findByTimestampBetween(startTime, endTime);
        
        // Group logs by box_barcode, product_name, and operation
        Map<String, Map<String, Map<String, List<Logs>>>> groupedLogs = logs.stream()
                .collect(Collectors.groupingBy(Logs::getBoxBarcode,
                        Collectors.groupingBy(Logs::getProductName,
                                Collectors.groupingBy(Logs::getOperation))));
        
        // Create bulk logs for each group
        for (Map.Entry<String, Map<String, Map<String, List<Logs>>>> boxEntry : groupedLogs.entrySet()) {
            String boxBarcode = boxEntry.getKey();
            
            for (Map.Entry<String, Map<String, List<Logs>>> productEntry : boxEntry.getValue().entrySet()) {
                String productName = productEntry.getKey();
                
                for (Map.Entry<String, List<Logs>> operationEntry : productEntry.getValue().entrySet()) {
                    String operation = operationEntry.getKey();
                    List<Logs> operationLogs = operationEntry.getValue();
                    
                    // Create bulk log
                    BulkLogs bulkLog = new BulkLogs();
                    bulkLog.setBoxBarcode(boxBarcode);
                    bulkLog.setProductName(productName);
                    bulkLog.setOperation(operation);
                    bulkLog.setQuantity(operationLogs.size());
                    bulkLog.setDate(date);
                    
                    bulkLogsRepository.save(bulkLog);
                }
            }
        }
        
        return bulkLogsRepository.findByDate(date);
    }
    
    /**
     * Get bulk logs by date
     */
    public List<BulkLogs> getBulkLogsByDate(LocalDate date) {
        return bulkLogsRepository.findByDate(date);
    }
    
    /**
     * Get bulk logs by date range
     */
    public List<BulkLogs> getBulkLogsByDateRange(LocalDate startDate, LocalDate endDate) {
        return bulkLogsRepository.findByDateBetween(startDate, endDate);
    }
    
    /**
     * Get bulk logs by box barcode
     */
    public List<BulkLogs> getBulkLogsByBoxBarcode(String boxBarcode) {
        return bulkLogsRepository.findByBoxBarcode(boxBarcode);
    }
    
    /**
     * Get bulk logs by product name
     */
    public List<BulkLogs> getBulkLogsByProductName(String productName) {
        return bulkLogsRepository.findByProductName(productName);
    }
    
    /**
     * Get bulk logs by operation
     */
    public List<BulkLogs> getBulkLogsByOperation(String operation) {
        return bulkLogsRepository.findByOperation(operation);
    }
} 