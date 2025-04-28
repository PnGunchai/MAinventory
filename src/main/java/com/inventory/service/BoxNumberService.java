package com.inventory.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.inventory.exception.ResourceNotFoundException;
import com.inventory.exception.InvalidInputException;
import com.inventory.model.BoxNumber;
import com.inventory.model.Logs;
import com.inventory.model.ProductCatalog;
import com.inventory.model.CurrentStock;
import com.inventory.model.Lend;
import com.inventory.repository.BoxNumberRepository;
import com.inventory.repository.LogsRepository;
import com.inventory.repository.ProductCatalogRepository;
import com.inventory.repository.CurrentStockRepository;
import com.inventory.repository.LendRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for box number operations
 */
@Service
public class BoxNumberService {
    
    private static final Logger logger = LoggerFactory.getLogger(BoxNumberService.class);
    
    @Autowired
    private BoxNumberRepository boxNumberRepository;
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private LogsRepository logsRepository;
    
    @Autowired
    private CurrentStockRepository currentStockRepository;
    
    @Autowired
    private LendRepository lendRepository;
    
    /**
     * Get the highest box number for a given box barcode and product name
     */
    public Integer getHighestBoxNumber(String boxBarcode, String productName) {
        return boxNumberRepository.findMaxBoxNumberByBoxBarcodeAndProductName(boxBarcode, productName)
                .orElse(0);
    }
    
    /**
     * Get the next available box number for a given box barcode and product name
     */
    public Integer getNextBoxNumber(String boxBarcode, String productName) {
        Integer highestBoxNumber = getHighestBoxNumber(boxBarcode, productName);
        return highestBoxNumber + 1;
    }
    
    /**
     * Create a new box number
     */
    @Transactional
    public BoxNumber createBoxNumber(String boxBarcode, String productName) {
        // If productName is null, retrieve it from the database
        if (productName == null) {
            ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));
            productName = product.getProductName();
        }
        
        // Get the next box number
        Integer nextBoxNumber = getNextBoxNumber(boxBarcode, productName);
        
        // Create a new box number entry
        BoxNumber boxNumber = new BoxNumber();
        boxNumber.setBoxBarcode(boxBarcode);
        boxNumber.setProductName(productName);
        boxNumber.setBoxNumber(nextBoxNumber);
        boxNumber.setLastUpdated(LocalDateTime.now());
        
        return boxNumberRepository.save(boxNumber);
    }
    
    /**
     * Create a box number entry if it doesn't already exist or if the existing one is inactive
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized BoxNumber createBoxNumberIfNeeded(String boxBarcode, String productName, String productBarcode) {
        logger.info("Creating box number if needed: boxBarcode={}, productName={}, productBarcode={}", 
                   boxBarcode, productName, productBarcode);
        
        // Get product to check if it's a serialized product
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + boxBarcode));
        
        // For serialized products, validate product barcode
        if (product.getNumberSn() > 0) {
            // Require product barcode for serialized products
            if (productBarcode == null || productBarcode.isEmpty()) {
                throw new InvalidInputException("Product barcode is required for serialized products");
            }
            
            // Check if this product barcode already exists but is NOT available for reuse
            Optional<BoxNumber> existingBoxNumber = boxNumberRepository.findByProductBarcode(productBarcode);
            if (existingBoxNumber.isPresent() && !isProductBarcodeAvailable(productBarcode)) {
                throw new InvalidInputException("Product barcode already exists and is not available for reuse: " + productBarcode);
            }
            
            // For paired products (SN=2), perform additional validation
            if (product.getNumberSn() == 2) {
                validatePairedBarcode(boxBarcode, productBarcode);
            }
        }
        
        // Create new box number
        return createNewBoxNumber(boxBarcode, productName, productBarcode);
    }
    
    /**
     * Validate a barcode for a paired product (SN=2)
     */
    private void validatePairedBarcode(String boxBarcode, String productBarcode) {
        // Get all box numbers for this box barcode
        List<BoxNumber> existingBoxNumbers = boxNumberRepository.findByBoxBarcode(boxBarcode);
        
        for (BoxNumber existing : existingBoxNumbers) {
            if (existing.getProductBarcode() != null && !existing.getProductBarcode().isEmpty()) {
                // Check if they appear to be a pair
                if (arePotentialPairs(existing.getProductBarcode(), productBarcode)) {
                    throw new InvalidInputException("Potential duplicate barcode detected: " + 
                                                  productBarcode + " appears to be paired with existing barcode " + 
                                                  existing.getProductBarcode());
                }
            }
        }
    }
    
    /**
     * Check if two barcodes are potential pairs
     */
    private boolean arePotentialPairs(String barcode1, String barcode2) {
        try {
            // Try to extract numbers from the barcodes
            int num1 = extractNumberFromBarcode(barcode1);
            int num2 = extractNumberFromBarcode(barcode2);
            
            // Check if they follow the pairing rule:
            // odd pairs with odd+1, even pairs with even-1
            if ((num1 % 2 != 0 && num2 == num1 + 1) || // odd pairs with odd+1
                (num1 % 2 == 0 && num2 == num1 - 1)) { // even pairs with even-1
                return true;
            }
            
            return false;
        } catch (Exception e) {
            // If we can't extract numbers or there's an error, fall back to character-by-character comparison
            // If they're the same length and differ by only 1-2 characters
            if (barcode1.length() == barcode2.length()) {
                int differences = 0;
                int diffPosition = -1;
                
                for (int i = 0; i < barcode1.length(); i++) {
                    if (barcode1.charAt(i) != barcode2.charAt(i)) {
                        differences++;
                        diffPosition = i;
                    }
                }
                
                // If they differ by only 1 character
                if (differences == 1) {
                    // Check if the difference is at the end and is a common pair pattern
                    if (diffPosition == barcode1.length() - 1) {
                        char c1 = barcode1.charAt(diffPosition);
                        char c2 = barcode2.charAt(diffPosition);
                        
                        // Check for common pair patterns: A/B, 1/2
                        return (c1 == 'A' && c2 == 'B') || (c1 == 'B' && c2 == 'A') ||
                               (c1 == '1' && c2 == '2') || (c1 == '2' && c2 == '1');
                    }
                }
            }
            
            return false;
        }
    }
    
    /**
     * Extract a number from a barcode
     */
    private int extractNumberFromBarcode(String barcode) {
        // If the barcode ends with a number, extract it
        if (barcode.matches(".*\\d+$")) {
            String numberPart = barcode.replaceAll("^.*?(?=(\\d+)$)", "");
            return Integer.parseInt(numberPart);
        } else if (barcode.matches(".*\\d+.*")) {
            // If there are numbers in the middle, extract the last one
            String[] parts = barcode.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (parts[i].matches("\\d+")) {
                    return Integer.parseInt(parts[i]);
                }
            }
        }
        
        // No number found
        throw new NumberFormatException("No number found in barcode: " + barcode);
    }
    
    /**
     * Create a new box number
     */
    private BoxNumber createNewBoxNumber(String boxBarcode, String productName, String productBarcode) {
        // If productName is null, retrieve it from the database
        if (productName == null) {
            ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + boxBarcode));
            productName = product.getProductName();
        }
        
        // Get the highest box number for this product
        Integer highestBoxNumber = getHighestBoxNumber(boxBarcode, productName);
        
        // Create new box number
        BoxNumber boxNumber = new BoxNumber();
        boxNumber.setBoxBarcode(boxBarcode);
        boxNumber.setProductName(productName);
        boxNumber.setProductBarcode(productBarcode);
        boxNumber.setBoxNumber(highestBoxNumber + 1);
        boxNumber.setLastUpdated(LocalDateTime.now());
        
        return boxNumberRepository.save(boxNumber);
    }
    
    /**
     * Get the box number for a specific product barcode
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Integer getBoxNumberForBarcode(String boxBarcode, String productName, String productBarcode) {
        // Find the box number in the logs table
        Optional<Logs> logEntry = logsRepository.findTopByBoxBarcodeAndProductNameAndProductBarcodeOrderByTimestampDesc(
                boxBarcode, productName, productBarcode);
        
        if (logEntry.isPresent()) {
            return logEntry.get().getBoxNumber();
        }
        
        return null;
    }

    /**
     * Get all product barcodes for a specific box number
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<String> getProductBarcodesForBoxNumber(String boxBarcode, String productName, Integer boxNumber) {
        // Find all product barcodes with this box number
        List<Logs> logEntries = logsRepository.findByBoxBarcodeAndProductNameAndBoxNumber(
                boxBarcode, productName, boxNumber);
        
        // Extract unique product barcodes
        return logEntries.stream()
                .map(Logs::getProductBarcode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Find box number by product barcode
     */
    public Optional<BoxNumber> findByProductBarcode(String productBarcode) {
        return boxNumberRepository.findByProductBarcode(productBarcode);
    }

    /**
     * Check if a product barcode is available for use
     * This method is now transactional with SERIALIZABLE isolation to prevent race conditions
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean isProductBarcodeAvailable(String productBarcode) {
        if (productBarcode == null || productBarcode.isEmpty()) {
            logger.debug("Product barcode is null or empty");
            return false;
        }
        
        logger.debug("Checking if product barcode {} is available for use", productBarcode);
        
        // Check if the product is currently lent out (status is not 'returned')
        List<Lend> lentItems = lendRepository.findByProductBarcode(productBarcode);
        for (Lend lent : lentItems) {
            if (lent.getStatus() == null || !lent.getStatus().equals("returned")) {
                logger.info("Product barcode {} is currently lent out", productBarcode);
                return false;
            }
        }
        
        // Check logs for this product barcode
        List<Logs> logs = logsRepository.findByProductBarcode(productBarcode);
        
        // If no logs found, the barcode has never been used and is available
        if (logs.isEmpty()) {
            logger.info("Product barcode {} has never been used, available", productBarcode);
            return true;
        }
        
        // Get the most recent log entry
        Logs latestLog = logs.stream()
                .max(Comparator.comparing(Logs::getTimestamp))
                .orElse(null);
        
        if (latestLog == null) {
            // This shouldn't happen if logs is not empty
            return true;
        }
        
        String operation = latestLog.getOperation();
        logger.debug("Latest operation for product barcode {}: {}", productBarcode, operation);
        
        // If the latest operation indicates the product is no longer active, it's available for reuse
        boolean isAvailable = operation != null && 
                (operation.equals("move_to_broken") || 
                 operation.equals("remove") || 
                 operation.equals("move_to_sales") || 
                 operation.equals("returned"));
        
        // If the latest operation is "add", it's not available
        if ("add".equals(operation)) {
            isAvailable = false;
        }
        
        if (isAvailable) {
            logger.info("Product barcode {} is available for reuse (last operation: {})", productBarcode, operation);
        } else {
            logger.info("Product barcode {} is still in use (last operation: {})", productBarcode, operation);
        }
        
        return isAvailable;
    }
} 