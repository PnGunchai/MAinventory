package com.inventory.service;

import com.inventory.model.CurrentStock;
import com.inventory.model.ProductCatalog;
import com.inventory.repository.CurrentStockRepository;
import com.inventory.repository.ProductCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service to synchronize CurrentStock with ProductCatalog
 */
@Service
public class SyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SyncService.class);
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private CurrentStockRepository currentStockRepository;
    
    @Autowired
    private BoxNumberService boxNumberService;
    
    /**
     * Synchronize CurrentStock with ProductCatalog
     * This ensures every product in the catalog has a corresponding stock entry
     */
    @Transactional
    public void syncCurrentStockWithCatalog() {
        logger.info("Starting synchronization of CurrentStock with ProductCatalog");
        
        try {
            // Get all products from catalog
            List<ProductCatalog> allProducts = productCatalogRepository.findAll();
            int created = 0;
            
            for (ProductCatalog product : allProducts) {
                try {
                    String boxBarcode = product.getBoxBarcode();
                    String productName = product.getProductName();
                    
                    // Check if stock entry exists
                    Optional<CurrentStock> stockOpt = currentStockRepository.findByBoxBarcodeAndProductName(
                            boxBarcode, productName);
                    
                    if (stockOpt.isEmpty()) {
                        // Create new stock entry with zero quantity
                        CurrentStock newStock = new CurrentStock();
                        newStock.setBoxBarcode(boxBarcode);
                        newStock.setProductName(productName);
                        newStock.setQuantity(0);
                        newStock.setLastUpdated(LocalDateTime.now());
                        
                        // Get highest box number if available
                        Integer highestBoxNumber = boxNumberService.getHighestBoxNumber(boxBarcode, productName);
                        if (highestBoxNumber != null) {
                            newStock.setBoxNumber(highestBoxNumber);
                        }
                        
                        currentStockRepository.save(newStock);
                        created++;
                    }
                } catch (Exception e) {
                    logger.error("Error processing product {}: {}", product.getBoxBarcode(), e.getMessage(), e);
                    // Continue with next product
                }
            }
            
            logger.info("Completed synchronization of CurrentStock with ProductCatalog. Created {} new stock entries", created);
        } catch (Exception e) {
            logger.error("Error during stock synchronization: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Run synchronization on application startup and daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledSync() {
        syncCurrentStockWithCatalog();
    }
    
    /**
     * Create or update stock entry for a specific product
     * Uses SERIALIZABLE isolation to prevent race conditions
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized CurrentStock createOrUpdateStock(String boxBarcode, String productName) {
        Optional<CurrentStock> stockOpt = currentStockRepository.findByBoxBarcodeAndProductName(
                boxBarcode, productName);
        
        CurrentStock stock;
        if (stockOpt.isEmpty()) {
            // Create new stock entry
            stock = new CurrentStock();
            stock.setBoxBarcode(boxBarcode);
            stock.setProductName(productName);
            stock.setQuantity(0);
        } else {
            stock = stockOpt.get();
        }
        
        // Update last updated timestamp
        stock.setLastUpdated(LocalDateTime.now());
        
        // Get highest box number if available
        Integer highestBoxNumber = boxNumberService.getHighestBoxNumber(boxBarcode, productName);
        if (highestBoxNumber != null) {
            stock.setBoxNumber(highestBoxNumber);
        }
        
        return currentStockRepository.save(stock);
    }
} 