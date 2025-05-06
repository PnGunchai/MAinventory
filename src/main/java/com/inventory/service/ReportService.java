package com.inventory.service;

import com.inventory.dto.CurrentInventoryDTO;
import com.inventory.dto.InventoryMovementDTO;
import com.inventory.model.CurrentStock;
import com.inventory.model.Logs;
import com.inventory.repository.CurrentStockRepository;
import com.inventory.repository.LogsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private CurrentStockRepository currentStockRepository;
    
    @Autowired
    private LogsRepository logsRepository;
    
    /**
     * Get current inventory levels report
     * @param categoryFilter Optional category filter
     * @return List of current inventory items with quantities
     */
    public List<CurrentInventoryDTO> getCurrentInventoryLevels(String categoryFilter) {
        logger.info("Fetching current inventory levels with category filter: {}", categoryFilter);
        
        try {
            List<CurrentStock> allStock = currentStockRepository.findAll();
            
            // Apply category filter if provided
            if (categoryFilter != null && !categoryFilter.isEmpty()) {
                logger.debug("Applying category filter: {}", categoryFilter);
                allStock = allStock.stream()
                    .filter(stock -> stock.getProductName().contains(categoryFilter))
                    .collect(Collectors.toList());
            }
            
            // Convert to DTOs with additional information
            List<CurrentInventoryDTO> result = allStock.stream().map(stock -> {
                CurrentInventoryDTO dto = new CurrentInventoryDTO();
                dto.setBoxBarcode(stock.getBoxBarcode());
                dto.setProductName(stock.getProductName());
                dto.setQuantity(stock.getQuantity());
                dto.setBoxNumber(stock.getBoxNumber());
                dto.setLastUpdated(stock.getLastUpdated());
                
                // Calculate days in inventory
                if (stock.getLastUpdated() != null) {
                    long daysInInventory = ChronoUnit.DAYS.between(stock.getLastUpdated().toLocalDate(), ZonedDateTime.now(ZoneId.of("Asia/Bangkok")).toLocalDate());
                    dto.setDaysInInventory(daysInInventory);
                }
                
                return dto;
            }).collect(Collectors.toList());
            
            logger.info("Found {} inventory items matching criteria", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Error fetching inventory levels", e);
            throw e;
        }
    }
    
    /**
     * Get inventory movement analysis
     * @param startDate Start date for analysis
     * @param endDate End date for analysis
     * @param boxBarcode Optional box barcode filter
     * @return Movement analysis data
     */
    public List<InventoryMovementDTO> getInventoryMovementAnalysis(
            ZonedDateTime startDate, ZonedDateTime endDate, String boxBarcode) {
        
        // Get all relevant logs within the date range
        List<Logs> logs;
        if (boxBarcode != null && !boxBarcode.isEmpty()) {
            logs = logsRepository.findByTimestampBetweenAndBoxBarcode(startDate, endDate, boxBarcode);
        } else {
            logs = logsRepository.findByTimestampBetween(startDate, endDate);
        }
        
        // Group logs by product and operation
        Map<String, Map<String, Integer>> productOperationCounts = new HashMap<>();
        
        for (Logs log : logs) {
            String productKey = log.getBoxBarcode() + ":" + log.getProductName();
            String operation = log.getOperation();
            
            productOperationCounts.putIfAbsent(productKey, new HashMap<>());
            Map<String, Integer> operationCounts = productOperationCounts.get(productKey);
            
            operationCounts.put(operation, operationCounts.getOrDefault(operation, 0) + 1);
        }
        
        // Convert to DTOs
        List<InventoryMovementDTO> result = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Integer>> entry : productOperationCounts.entrySet()) {
            String key = entry.getKey();
            String[] productParts = key.split(":");
            
            // Add null and bounds checking
            if (productParts.length < 2) {
                // Log the error and skip this entry
                logger.warn("Invalid product key format: {}", key);
                continue;
            }
            
            String productBoxBarcode = productParts[0];
            String productName = productParts[1];
            
            InventoryMovementDTO dto = new InventoryMovementDTO();
            dto.setBoxBarcode(productBoxBarcode);
            dto.setProductName(productName);
            
            Map<String, Integer> operations = entry.getValue();
            dto.setAddCount(operations.getOrDefault("add", 0));
            dto.setRemoveCount(operations.getOrDefault("remove", 0));
            dto.setSalesCount(operations.getOrDefault("move_to_sales", 0));
            dto.setLentCount(operations.getOrDefault("move_to_lent", 0));
            dto.setReturnedCount(operations.getOrDefault("returned", 0));
            dto.setBrokenCount(operations.getOrDefault("move_to_broken", 0));
            
            // Calculate net movement
            int inflow = dto.getAddCount() + dto.getReturnedCount();
            int outflow = dto.getRemoveCount() + dto.getSalesCount() + dto.getLentCount() + dto.getBrokenCount();
            dto.setNetMovement(inflow - outflow);
            
            result.add(dto);
        }
        
        return result;
    }

    /**
     * Get current inventory levels, optionally filtering out zero-quantity items
     */
    public List<CurrentInventoryDTO> getCurrentInventoryLevels(String category, boolean includeZeroQuantity) {
        List<CurrentStock> allStock = currentStockRepository.findAll();
        
        return allStock.stream()
                .filter(stock -> includeZeroQuantity || stock.getQuantity() > 0)
                .filter(stock -> category == null || stock.getProductName().contains(category))
                .map(stock -> {
                    CurrentInventoryDTO dto = new CurrentInventoryDTO();
                    dto.setBoxBarcode(stock.getBoxBarcode());
                    dto.setProductName(stock.getProductName());
                    dto.setQuantity(stock.getQuantity());
                    dto.setBoxNumber(stock.getBoxNumber());
                    dto.setLastUpdated(stock.getLastUpdated());
                    
                    // Calculate days in inventory
                    if (stock.getLastUpdated() != null) {
                        long daysInInventory = ChronoUnit.DAYS.between(stock.getLastUpdated().toLocalDate(), ZonedDateTime.now(ZoneId.of("Asia/Bangkok")).toLocalDate());
                        dto.setDaysInInventory(daysInInventory);
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }
} 