package com.inventory.service;

import com.inventory.dto.StockAdditionDTO;
import com.inventory.dto.BulkRemoveDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.CurrentStock;
import com.inventory.model.ProductCatalog;
import com.inventory.model.BoxNumber;
import com.inventory.model.Logs;
import com.inventory.model.Sales;
import com.inventory.model.Lend;
import com.inventory.model.Broken;
import com.inventory.model.LentId;
import com.inventory.model.InStock;
import com.inventory.model.Invoice;
import com.inventory.repository.CurrentStockRepository;
import com.inventory.repository.ProductCatalogRepository;
import com.inventory.repository.LentIdRepository;
import com.inventory.repository.LendRepository;
import com.inventory.repository.BoxNumberRepository;
import com.inventory.repository.LogsRepository;
import com.inventory.repository.InStockRepository;
import com.inventory.repository.BarcodeStatusRepository;
import com.inventory.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import com.inventory.util.InventoryUtils;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Service for stock operations
 */
@Service
public class StockService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockService.class);
    
    // Add a static lock object for synchronization
    private static final Object STOCK_LOCK = new Object();
    
    @Autowired
    private CurrentStockRepository currentStockRepository;
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private InStockService inStockService;
    
    @Autowired
    private InStockRepository inStockRepository;
    
    @Autowired
    private LogsService logsService;
    
    @Autowired
    private SalesService salesService;
    
    @Autowired
    private LendService lendService;
    
    @Autowired
    private BrokenService brokenService;
    
    @Autowired
    private BoxNumberService boxNumberService;
    
    @Autowired
    private LendRepository lendRepository;
    
    @Autowired
    private LentIdRepository lentIdRepository;
    
    @Autowired
    private SyncService syncService;
    
    @Autowired
    private BoxNumberRepository boxNumberRepository;
    
    @Autowired
    private LogsRepository logsRepository;
    
    @Autowired
    private BarcodeStatusService barcodeStatusService;
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private BarcodeStatusRepository barcodeStatusRepository;
    
    @Autowired
    private BarcodeRegistryService barcodeRegistryService;
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    private Map<String, Invoice> invoiceCache = new HashMap<>();
    
    private Invoice getOrCreateInvoice(String orderId, String employeeId, String shopName) {
        return invoiceCache.computeIfAbsent(orderId, k -> {
            // Check if invoice already exists
            List<Invoice> existingInvoices = invoiceRepository.findByInvoice(orderId);
            if (!existingInvoices.isEmpty()) {
                return existingInvoices.get(0);
            }
            
            // Create new invoice if not exists
            Invoice invoice = new Invoice();
            invoice.setInvoice(orderId);
            invoice.setEmployeeId(employeeId);
            invoice.setShopName(shopName);
            invoice.setTimestamp(LocalDateTime.now());
            return invoiceRepository.save(invoice);
        });
    }
    
    /**
     * Add stock with SERIALIZABLE isolation to prevent race conditions
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized CurrentStock addStock(String boxBarcode, String productBarcode, int quantity, String note) {
        synchronized(STOCK_LOCK) {
            try {
                // Add delay between operations to prevent race conditions
                Thread.sleep(100);
                
                // Validate quantity
                if (quantity <= 0) {
                    throw new InvalidInputException("Quantity must be greater than zero");
                }
                
                logger.info("Adding stock: boxBarcode={}, productBarcode={}, quantity={}", 
                           boxBarcode, productBarcode, quantity);
                
                // Find product in catalog with lock
                ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));
                
                // Explicitly lock the product catalog entry
                entityManager.lock(product, LockModeType.PESSIMISTIC_WRITE);
                
                // For serialized products, validate product barcode
                if (product.getNumberSn() > 0) {
                    if (productBarcode == null || productBarcode.isEmpty()) {
                        throw new InvalidInputException("Product barcode is required for serialized products");
                    }
                    
                    // Check if product barcode already exists in stock
                    if (inStockService.isInStock(productBarcode)) {
                        throw new InvalidInputException("Product barcode already exists in stock: " + productBarcode);
                    }
                }
                
                BoxNumber boxNumber = boxNumberService.createBoxNumberIfNeeded(boxBarcode, product.getProductName(), productBarcode);
                logger.info("Created box number: {} for barcode: {}", boxNumber.getBoxNumber(), productBarcode);
                
                // Explicitly flush to ensure the box number is written to the database
                entityManager.flush();
                
                // Find or create stock
                Optional<CurrentStock> stockOpt = currentStockRepository.findByBoxBarcodeAndProductName(
                        boxBarcode, product.getProductName());
                
                CurrentStock stock;
                if (stockOpt.isPresent()) {
                    stock = stockOpt.get();
                    stock.setQuantity(stock.getQuantity() + quantity);
                } else {
                    stock = new CurrentStock();
                    stock.setBoxBarcode(boxBarcode);
                    stock.setProductName(product.getProductName());
                    stock.setQuantity(quantity);
                }
                
                // Set box number
                stock.setBoxNumber(boxNumber.getBoxNumber());
                
                stock.setLastUpdated(LocalDateTime.now());
                
                // Save and then explicitly refresh from the database
                CurrentStock savedStock = currentStockRepository.save(stock);
                entityManager.refresh(savedStock);
                
                // Create log entry
                if (product.getNumberSn() == 0) {
                    // For non-serialized products, create one log entry with quantity
                    logsService.createLog(boxBarcode, product.getProductName(), null, "add", note, boxNumber.getBoxNumber(), quantity);
                } else {
                    // For serialized products, create log entry with quantity=1
                    logsService.createLog(boxBarcode, product.getProductName(), productBarcode, "add", note, boxNumber.getBoxNumber(), 1);
                }
                
                // Add to in_stock table for serialized products
                if (product.getNumberSn() > 0 && productBarcode != null && !productBarcode.isEmpty()) {
                    try {
                        inStockService.addToStock(boxBarcode, productBarcode, product.getProductName(), boxNumber.getBoxNumber());
                        logger.info("Added to in_stock table: {}", productBarcode);
                        
                        // Sync CurrentStock with InStock after adding to in_stock table
                        syncCurrentStockWithInStock(boxBarcode, product.getProductName());
                    } catch (Exception e) {
                        logger.error("Failed to add to in_stock table: {}", e.getMessage(), e);
                        // Don't throw the exception, as we want to continue with the stock addition
                        // The in_stock table is a secondary tracking mechanism
                    }
                }
                
                return savedStock;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Operation interrupted", e);
            }
        }
    }
    
    /**
     * Check if two barcodes are potential duplicates
     */
    private boolean isPotentialDuplicate(String barcode1, String barcode2) {
        // If either barcode is null or empty, they're not duplicates
        if (barcode1 == null || barcode1.isEmpty() || barcode2 == null || barcode2.isEmpty()) {
            return false;
        }
        
        // If they're exactly the same
        if (barcode1.equals(barcode2)) {
            return true;
        }
        
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
            
            // If they differ by 2 or fewer characters but are very similar
            return differences <= 2;
        }
        
        // Check for common prefixes (for barcodes of different lengths)
        if (Math.abs(barcode1.length() - barcode2.length()) <= 2) {
            String longer = barcode1.length() > barcode2.length() ? barcode1 : barcode2;
            String shorter = barcode1.length() <= barcode2.length() ? barcode1 : barcode2;
            
            // Check if shorter is a prefix of longer
            if (longer.startsWith(shorter)) {
                return true;
            }
            
            // Check for common prefix (at least 80% of the shorter string)
            int commonPrefixLength = 0;
            for (int i = 0; i < shorter.length(); i++) {
                if (shorter.charAt(i) == longer.charAt(i)) {
                    commonPrefixLength++;
                } else {
                    break;
                }
            }
            
            return commonPrefixLength >= (shorter.length() * 0.8);
        }
        
        return false;
    }
    
    /**
     * Check for potential paired barcodes to prevent duplicates
     */
    private void checkForPairedBarcodes(String boxBarcode, String productBarcode) {
        // Get all product barcodes from in_stock for this box barcode
        List<InStock> inStockItems = inStockService.getInStockByBoxBarcode(boxBarcode);
        
        for (InStock inStockItem : inStockItems) {
            String existingBarcode = inStockItem.getProductBarcode();
            if (existingBarcode != null && !existingBarcode.isEmpty()) {
                // Check for common patterns (A/B, 1/2 suffixes)
                if (arePotentialPairs(existingBarcode, productBarcode)) {
                    throw new InvalidInputException("Potential duplicate barcode detected: " + 
                                                  productBarcode + " appears to be paired with existing barcode " + 
                                                  existingBarcode);
                }
            }
        }
    }
    
    /**
     * Check if two barcodes are potential pairs
     */
    public boolean arePotentialPairs(String barcode1, String barcode2) {
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
            
            // If they differ by 2 characters but are very similar
            return differences <= 2;
        }
        
        return false;
    }
    
    /**
     * Simplified method to remove stock using only boxBarcode
     * This method retrieves the productName from the database
     * Uses SERIALIZABLE isolation to prevent race conditions
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized CurrentStock removeStock(String boxBarcode, String productBarcode, int quantity, String note) {
        // Validate quantity
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero");
        }
        
        // Retrieve product from catalog
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));
        
        String productName = product.getProductName();
        
        // For serialized products (SN=1 or SN=2), validate product barcode
        if (product.getNumberSn() > 0) {
            // Require product barcode for serialized products
            if (productBarcode == null || productBarcode.isEmpty()) {
                throw new InvalidInputException("Product barcode is required for serialized products");
            }
            
            // Check if the product is in the in_stock table
            if (!inStockService.isInStock(productBarcode)) {
                throw new InvalidInputException("Product barcode " + productBarcode + " is not in stock. It may have been already removed, sold, lent, or marked as broken.");
            }
            
            // Get the box number for this product barcode
            Integer boxNumber = null;
            Optional<BoxNumber> boxNumberRecord = boxNumberRepository.findByProductBarcode(productBarcode);
            if (boxNumberRecord.isPresent()) {
                boxNumber = boxNumberRecord.get().getBoxNumber();
            }
            
            // Find or create stock entry
            CurrentStock stock = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found for box barcode: " + boxBarcode));
            
            // Update stock quantity
            stock.setQuantity(stock.getQuantity() - quantity);
            stock.setLastUpdated(LocalDateTime.now());
            
            // Create log entry
            Logs log = logsService.createLog(boxBarcode, productName, productBarcode, "remove", note);
            if (boxNumber != null) {
                log.setBoxNumber(boxNumber);
            }
            logsService.save(log);
            
            try {
                // Remove from in_stock table
                inStockService.removeFromStock(productBarcode);
                logger.info("Removed from in_stock table: {}", productBarcode);
                
                // Sync CurrentStock with InStock after removing from in_stock table
                syncCurrentStockWithInStock(boxBarcode, productName);
            } catch (Exception e) {
                logger.error("Failed to remove from in_stock table: {}", e.getMessage(), e);
                throw e;
            }
            
            // Mark the barcode as available for reuse
            barcodeStatusService.markBarcodeAvailable(productBarcode);
            
            // Save and return updated stock
            return currentStockRepository.save(stock);
        } else {
            // For non-serialized products, find existing stock
            CurrentStock stock = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found for box barcode: " + boxBarcode));
            
            // Check if there's enough stock
            if (stock.getQuantity() < quantity) {
                throw new InvalidInputException("Not enough stock available. Requested: " + quantity + ", Available: " + stock.getQuantity());
            }
            
            // Update stock quantity
            stock.setQuantity(stock.getQuantity() - quantity);
            stock.setLastUpdated(LocalDateTime.now());
            
            // Create log entry
            Logs log = logsService.createLog(boxBarcode, productName, productBarcode, "remove", note);
            
            // Set box number in log if available
            if (stock.getBoxNumber() != null) {
                log.setBoxNumber(stock.getBoxNumber());
            }
            logsService.save(log);
            
            // Save and return updated stock
            return currentStockRepository.save(stock);
        }
    }
    
    /**
     * Get all stock
     */
    public List<CurrentStock> getAllStock() {
        return currentStockRepository.findAll();
    }
    
    /**
     * Get stock by box barcode
     */
    public List<CurrentStock> getStockByBoxBarcode(String boxBarcode) {
        return currentStockRepository.findByBoxBarcode(boxBarcode);
    }
    
    /**
     * Get stock by box barcode and product name
     */
    public CurrentStock getStockByBoxBarcodeAndProductName(String boxBarcode, String productName) {
        return currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found for box barcode: " + boxBarcode + " and product name: " + productName));
    }
    
    /**
     * Move stock to another destination (sales, lent, broken)
     * Uses SERIALIZABLE isolation to prevent race conditions
     */
    @Transactional
    public void moveStock(String boxBarcode, String productBarcode, int quantity, 
                         String destination, String employeeId, String shopName, 
                         String condition, String note, String orderId, boolean splitPair,
                         Boolean isDirectSales) {
        // Call the main implementation with the isDirectSales parameter
        moveStockImpl(boxBarcode, productBarcode, quantity, destination, employeeId, 
                   shopName, condition, note, orderId, splitPair, isDirectSales);
    }

    /**
     * Move stock to another destination (sales, lent, broken)
     * This overloaded method maintains backward compatibility
     */
    @Transactional
    public void moveStock(String boxBarcode, String productBarcode, int quantity, 
                         String destination, String employeeId, String shopName, 
                         String condition, String note, String orderId, boolean splitPair) {
        // Call the main implementation with default isDirectSales=true
        moveStockImpl(boxBarcode, productBarcode, quantity, destination, employeeId, 
                   shopName, condition, note, orderId, splitPair, true);
    }

    /**
     * Move stock to another destination (sales, lent, broken)
     * This overloaded method maintains backward compatibility
     */
    @Transactional
    public void moveStock(String boxBarcode, String productBarcode, int quantity, 
                         String destination, String employeeId, String shopName, 
                         String condition, String note, String orderId) {
        // Call the main implementation with default splitPair=true and isDirectSales=true
        moveStockImpl(boxBarcode, productBarcode, quantity, destination, employeeId, 
                   shopName, condition, note, orderId, true, true);
    }

    /**
     * Move stock to another destination (sales, lent, broken)
     * This overloaded method maintains backward compatibility
     */
    @Transactional
    public void moveStock(String boxBarcode, String productBarcode, int quantity, 
                         String destination, String employeeId, String shopName, 
                         String condition, String note) {
        // Generate a default order ID for backward compatibility
        String defaultOrderId = destination.toUpperCase() + "-" + 
                               LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        // Call the main implementation with default splitPair=true and isDirectSales=true
        moveStockImpl(boxBarcode, productBarcode, quantity, destination, employeeId, 
                   shopName, condition, note, defaultOrderId, true, true);
    }

    /**
     * Specifically move an item from lent to sales, using the original lent order ID
     * to find the correct lent record. This is especially useful for non-serialized products.
     * 
     * @param boxBarcode Box barcode of the product
     * @param productBarcode Product barcode (can be null for non-serialized products)
     * @param quantity Quantity to move
     * @param employeeId Employee ID making the move
     * @param shopName Shop name 
     * @param note Optional note
     * @param newSalesOrderId The new sales order ID to use
     * @param originalLentOrderId The original lent order ID to identify the lent record
     * @param isDirectSales Whether this is a direct sale
     */
    @Transactional
    public void moveFromLentToSales(String boxBarcode, String productBarcode, int quantity,
                                 String employeeId, String shopName, String note,
                                 String newSalesOrderId, String originalLentOrderId, Boolean isDirectSales) {
        logger.info("Moving from lent to sales with lent order ID: {}", originalLentOrderId);
        
        // Get product from catalog to check if it's serialized or not
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + boxBarcode));
                
        // For non-serialized products, directly find the lent record using lentOrderId for more accurate matching
        if (product.getNumberSn() == 0) {
            logger.info("Processing non-serialized product move from lent to sales");
            List<Lend> lentItems = lendRepository.findByBoxBarcode(boxBarcode);
            Optional<Lend> lentItem = lentItems.stream()
                .filter(item -> item.getProductBarcode() == null && "lent".equals(item.getStatus()) &&
                                originalLentOrderId.equals(item.getOrderId()))
                .findFirst();
                
            if (lentItem.isPresent()) {
                logger.info("Found lent item with order ID: {}", originalLentOrderId);
                
                // Create log entry for moving from lent to sales
                logger.info("CREATING LOG: Operation='moved_from_lent_to_sales', boxBarcode={}, productName={}, isLentItem=true", 
                           boxBarcode, product.getProductName());
                           
                // For non-serialized products, get the box number but set it to null
                Logs lentLog = logsService.createLog(
                    boxBarcode,
                    product.getProductName(),
                    productBarcode, // Include product barcode for serialized products
                    "moved_from_lent_to_sales", // Use underscores for uniqueness
                    note + " (original lent order: " + originalLentOrderId + ")",
                    null, // Always null for non-serialized products
                    quantity
                );
                
                // Extra check to ensure correct operation value
                if (!"moved_from_lent_to_sales".equals(lentLog.getOperation())) {
                    logger.warn("Operation was not set correctly! Overriding to 'moved_from_lent_to_sales'");
                    lentLog.setOperation("moved_from_lent_to_sales");
                }
                
                lentLog.setOrderId(newSalesOrderId); // Use the new sales order ID
                logsService.save(lentLog);
                logger.info("SAVED LOG: id={}, operation={}", lentLog.getLogsId(), lentLog.getOperation());
                
                // Get or create invoice
                Invoice invoice = getOrCreateInvoice(newSalesOrderId, employeeId, shopName);

                // Create sales record for the non-serialized lent item
                Sales sale = new Sales();
                sale.setBoxBarcode(boxBarcode);
                sale.setProductName(product.getProductName());
                sale.setProductBarcode(null);
                sale.setEmployeeId(employeeId);
                sale.setShopName(shopName);
                sale.setOrderId(newSalesOrderId);
                sale.setNote("Batch processing (from lent order: " + originalLentOrderId + ")");
                sale.setBoxNumber(null); // Set box number to null for non-serialized products
                
                // Log the isDirectSales value being set
                logger.info("StockService - Setting isDirectSales={} for non-serialized lent-to-sales conversion", isDirectSales);
                
                // Make sure we're using the passed value, not hardcoding
                sale.setIsDirectSales(isDirectSales);
                sale.setQuantity(quantity);
                sale.setInvoiceId(invoice.getInvoiceId());
                sale.setTimestamp(LocalDateTime.now());
                salesService.save(sale);

                // Update the lent record status
                lentItem.get().setStatus("lent to sales"); // Use 'lent to sales' as status in lent table
                lendRepository.save(lentItem.get());
                
                // Update current stock if needed (stock was already adjusted during the lent operation)
                
                logger.info("Successfully moved non-serialized product from lent to sales");
                return;
            } else {
                logger.error("Could not find lent record for boxBarcode={} and lentOrderId={}", boxBarcode, originalLentOrderId);
                throw new ResourceNotFoundException("Lent record not found for box barcode " + boxBarcode + " with order ID " + originalLentOrderId);
            }
        }
        
        // For serialized products or fallback, call the general implementation
        moveStockImpl(boxBarcode, productBarcode, quantity, "sales", employeeId, 
                   shopName, null, note, newSalesOrderId, true, isDirectSales, originalLentOrderId);
    }
    
    /**
     * Simplified version of moveFromLentToSales that uses default isDirectSales=false
     */
    @Transactional
    public void moveFromLentToSales(String boxBarcode, String productBarcode, int quantity,
                                 String employeeId, String shopName, String note,
                                 String newSalesOrderId, String originalLentOrderId) {
        // Using false as default for isDirectSales since it's coming from lent, not a direct sale
        moveFromLentToSales(boxBarcode, productBarcode, quantity, employeeId, shopName, 
                           note, newSalesOrderId, originalLentOrderId, false);
    }

    /**
     * Main implementation of stock movement
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    private CurrentStock moveStockImpl(String boxBarcode, String productBarcode, int quantity,
                                     String destination, String employeeId, String shopName,
                                     String condition, String note, String orderId, boolean splitPair,
                                     Boolean isDirectSales) {
        return moveStockImpl(boxBarcode, productBarcode, quantity, destination, employeeId, shopName, 
                          condition, note, orderId, splitPair, isDirectSales, null);
    }

    /**
     * Main implementation of stock movement
     * @param lentOrderId - Optional parameter used to identify a specific lent record when moving from lent to sales
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    private CurrentStock moveStockImpl(String boxBarcode, String productBarcode, int quantity,
                                     String destination, String employeeId, String shopName,
                                     String condition, String note, String orderId, boolean splitPair,
                                     Boolean isDirectSales, String lentOrderId) {
        // Lock the product catalog row to prevent concurrent modifications
        entityManager.createNativeQuery("select box_barcode from product_catalog where box_barcode = ? for update")
                .setParameter(1, boxBarcode)
                .getSingleResult();
        
        logger.info("Moving stock: boxBarcode={}, productBarcode={}, quantity={}, destination={}, splitPair={}, lentOrderId={}", 
                   boxBarcode, productBarcode, quantity, destination, splitPair, lentOrderId);
        
        // Get product from catalog
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + boxBarcode));
        
        String productName = product.getProductName();
        
        // For serialized products, check if product barcode exists in logs
        if (product.getNumberSn() > 0 && productBarcode != null && !productBarcode.isEmpty()) {
            List<Logs> productLogs = logsService.getLogsByProductBarcode(productBarcode);
            if (productLogs.isEmpty()) {
                throw new ResourceNotFoundException("Product barcode not found: " + productBarcode);
            }
        }
        
        // Find or create stock
        Optional<CurrentStock> stockOpt = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName);
        CurrentStock stock = null;
        if (stockOpt.isPresent()) {
            stock = stockOpt.get();
            logger.info("Found stock for {} product: quantity={}", 
                       productBarcode != null ? "serialized" : "non-serialized", stock.getQuantity());
        }
        
        // If stock not found, try to create it
        if (stock == null) {
            stock = syncService.createOrUpdateStock(boxBarcode, productName);
            logger.info("Created new stock entry: quantity={}", stock.getQuantity());
        }
        
        // Check if this is a lent item being moved to sales
        boolean isLentItem = false;
        boolean isMoveFromLentToSales = false; // Add a specific flag for this operation
        boolean isSerializedProduct = product.getNumberSn() > 0;
        Integer boxNumber = stock.getBoxNumber();
        boolean isFirstOfPair = true; // Default to true
        Integer originalBoxNumber = null; // Add this declaration
        
        if (productBarcode != null) {
            // For serialized products, check by product barcode
            Optional<Lend> lentItem = lendRepository.findByProductBarcode(productBarcode)
                    .stream()
                    .filter(item -> "lent".equals(item.getStatus()))
                    .findFirst();
            if (lentItem.isPresent()) {
                isLentItem = true;
                isMoveFromLentToSales = true; // Set the specific flag
                String foundLentOrderId = lentItem.get().getOrderId();

                // Get the box number from the original lent record
                Integer lentBoxNumber = lentItem.get().getBoxNumber();
                
                // Create log entry for moving from lent to sales with original box number
                Logs moveFromLentLog = logsService.createLog(
                    boxBarcode,
                    productName,
                    productBarcode, // Include product barcode for serialized products
                    "moved_from_lent_to_sales",
                    "Moved from lent order: " + foundLentOrderId,
                    lentBoxNumber, // Use box number from lent record
                    quantity
                );
                moveFromLentLog.setOrderId(orderId); // Use the new sales order ID
                logsService.save(moveFromLentLog);

                // Get or create invoice
                Invoice invoice = getOrCreateInvoice(orderId, employeeId, shopName);

                // Create sales record with original box number
                Sales sale = new Sales();
                sale.setBoxBarcode(boxBarcode);
                sale.setProductName(productName);
                sale.setProductBarcode(productBarcode);
                sale.setEmployeeId(employeeId);
                sale.setShopName(shopName);
                sale.setOrderId(orderId);
                sale.setNote(note + " (from lent order: " + foundLentOrderId + ")"); // Only user note
                sale.setBoxNumber(lentBoxNumber); // Use box number from lent record
                
                // Log the isDirectSales value being set for serialized products
                logger.info("StockService - Setting isDirectSales={} for serialized lent-to-sales conversion", isDirectSales);
                
                // Make sure we're using the passed value, not hardcoding
                sale.setIsDirectSales(isDirectSales);
                sale.setQuantity(quantity);
                sale.setInvoiceId(invoice.getInvoiceId()); // Use the actual invoice ID
                sale.setTimestamp(LocalDateTime.now()); // Set the timestamp
                salesService.save(sale);

                // Update the lent record status
                lentItem.get().setStatus("lent to sales"); // Use 'lent to sales' as status in lent table
                lendRepository.save(lentItem.get());
            }
        } else {
            // For non-serialized products, check by box barcode
            List<Lend> lentItems = lendRepository.findByBoxBarcode(boxBarcode);
            Optional<Lend> lentItem;
            
            // If a specific lent order ID was provided, use it to find the lent record
            if (lentOrderId != null && !lentOrderId.isEmpty()) {
                lentItem = lentItems.stream()
                    .filter(item -> item.getProductBarcode() == null && "lent".equals(item.getStatus()) &&
                                    lentOrderId.equals(item.getOrderId())) // Use the specific lent order ID
                    .findFirst();
            } else {
                // Otherwise, try to match with the new order ID (legacy behavior)
                lentItem = lentItems.stream()
                    .filter(item -> item.getProductBarcode() == null && "lent".equals(item.getStatus()) &&
                                    orderId.equals(item.getOrderId())) 
                    .findFirst();
            }
            
            if (lentItem.isPresent()) {
                isLentItem = true;
                isMoveFromLentToSales = true; // Set the specific flag
                String foundLentOrderId = lentItem.get().getOrderId();
                
                // Create log entry for moving from lent to sales (system note)
                Logs moveFromLentLog = logsService.createLog(
                    boxBarcode,
                    productName,
                    null, // For non-serialized products, this should be null
                    "moved_from_lent_to_sales", // Use underscores for uniqueness
                    "Batch processing (from lent order: " + foundLentOrderId + ")", 
                    null, // For non-serialized products, boxNumber should be null
                    quantity
                );
                moveFromLentLog.setOrderId(orderId); // Use the new sales order ID
                logsService.save(moveFromLentLog);
                
                // Get or create invoice
                Invoice invoice = getOrCreateInvoice(orderId, employeeId, shopName);
                
                // Create sales record for the non-serialized lent item (user note)
                Sales sale = new Sales();
                sale.setBoxBarcode(boxBarcode);
                sale.setProductName(productName);
                sale.setProductBarcode(null);
                sale.setEmployeeId(employeeId);
                sale.setShopName(shopName);
                sale.setOrderId(orderId);
                sale.setNote(note + " (from lent order: " + foundLentOrderId + ")"); // Updated note format
                sale.setBoxNumber(null); // Always null for non-serialized products
                
                // Log the isDirectSales value being set
                logger.info("StockService - Setting isDirectSales={} for non-serialized lent-to-sales conversion", isDirectSales);
                
                // Make sure we're using the passed value, not hardcoding
                sale.setIsDirectSales(isDirectSales);
                sale.setQuantity(quantity);
                sale.setInvoiceId(invoice.getInvoiceId()); // Use the actual invoice ID
                sale.setTimestamp(LocalDateTime.now()); // Set the timestamp
                salesService.save(sale);

                // Update the lent record status
                lentItem.get().setStatus("lent to sales"); // Use 'lent to sales' as status in lent table
                lendRepository.save(lentItem.get());
            }
        }

        // Debug log to see if isLentItem flag is properly set
        logger.info("DEBUG - After processing lent items, isLentItem={}, isMoveFromLentToSales={}", isLentItem, isMoveFromLentToSales);

        // Process the stock movement based on destination
        switch (destination) {
            case "sales":
                // Only create additional logs if this is NOT a move from lent to sales
                // This prevents creating "sold" logs for items already logged as "moved from lent to sales"
                if (!isMoveFromLentToSales) {
                    // If not a lent item being moved to sales, handle regular sales
                    if (!isLentItem) {
                        if (isSerializedProduct) {
                            // Get the in-stock details before removing from stock
                            InStock inStockDetails = inStockService.getInStockDetails(productBarcode);
                            if (inStockDetails == null) {
                                throw new InvalidInputException("Product barcode " + productBarcode + " is not in stock");
                            }
                            
                            // Use the box number from in_stock
                            originalBoxNumber = inStockDetails.getBoxNumber();
                            
                            // Remove from in_stock table
                            inStockService.removeFromStock(productBarcode);
                            
                            // Create sales record with original box number
                            Sales sale = new Sales();
                            sale.setBoxBarcode(boxBarcode);
                            sale.setProductName(productName);
                            sale.setProductBarcode(productBarcode);
                            sale.setEmployeeId(employeeId);
                            sale.setShopName(shopName);
                            sale.setOrderId(orderId);
                            sale.setNote(note); // Only user note
                            sale.setBoxNumber(originalBoxNumber); // Use original box number from in_stock
                            sale.setIsDirectSales(true); // Set to true for direct sales
                            sale.setQuantity(quantity);
                            
                            // Get or create invoice
                            Invoice invoice = getOrCreateInvoice(orderId, employeeId, shopName);
                            
                            sale.setInvoiceId(invoice.getInvoiceId()); // Use the actual invoice ID
                            sale.setTimestamp(LocalDateTime.now()); // Set the timestamp
                            salesService.save(sale);
                            
                            // Create log entry for sales with original box number
                            Logs salesLog = logsService.createLog(
                                boxBarcode,
                                productName,
                                productBarcode,
                                "sold",
                                "Sold item",
                                originalBoxNumber,
                                quantity
                            );
                            salesLog.setOrderId(orderId);
                            logsService.save(salesLog);
                        } else {
                            // Create sales record for non-serialized item
                            Sales sale = new Sales();
                            sale.setBoxBarcode(boxBarcode);
                            sale.setProductName(productName);
                            sale.setProductBarcode(null);
                            sale.setEmployeeId(employeeId);
                            sale.setShopName(shopName);
                            sale.setOrderId(orderId);
                            sale.setNote(note); // Only user note
                            sale.setBoxNumber(null); // Always set boxNumber to null for non-serialized products
                            sale.setIsDirectSales(true); // Set to true for direct sales
                            sale.setQuantity(quantity);
                            
                            // Get or create invoice
                            Invoice invoice = getOrCreateInvoice(orderId, employeeId, shopName);
                            
                            sale.setInvoiceId(invoice.getInvoiceId()); // Use the actual invoice ID
                            sale.setTimestamp(LocalDateTime.now()); // Set the timestamp
                            salesService.save(sale);
                            
                            // Create log entry for non-serialized sales
                            Logs salesLog = logsService.createLog(
                                boxBarcode,
                                productName,
                                null,
                                "sold", // Always use "sold" here since this is the direct sales case (!isLentItem)
                                "Sold item",
                                null, // Always set boxNumber to null for non-serialized products
                                quantity
                            );
                            salesLog.setOrderId(orderId);
                            logsService.save(salesLog);
                        }
                        
                        // Update current stock quantity
                        // For paired items (SN=2), only update on the first item and adjust quantity
                        if (!isSerializedProduct || product.getNumberSn() != 2 || isFirstOfPair) {
                            CurrentStock currentStock = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
                            int quantityToDeduct = product.getNumberSn() == 2 ? quantity / 2 : quantity;
                            currentStock.setQuantity(currentStock.getQuantity() - quantityToDeduct);
                            currentStock.setLastUpdated(LocalDateTime.now());
                            currentStockRepository.save(currentStock);
                        }

                        // Sync current stock with in_stock to ensure accurate quantity
                        syncCurrentStockWithInStock(boxBarcode, productName);
                    }
                }
                break;
                
            case "lent":
                // For serialized products, check in-stock status
                if (isSerializedProduct) {
                    // Get the in-stock details before removing from stock
                    InStock inStockDetails = inStockService.getInStockDetails(productBarcode);
                    if (inStockDetails == null) {
                        throw new InvalidInputException("Product barcode " + productBarcode + " is not in stock");
                    }
                    
                    // Use the box number from in_stock
                    originalBoxNumber = inStockDetails.getBoxNumber();
                    
                    // Remove from in_stock table
                    inStockService.removeFromStock(productBarcode);
                    
                    // If this is a paired product (SN=2) and not splitting pairs, handle the paired item
                    if (product.getNumberSn() == 2 && !splitPair) {
                        // Find paired barcode
                        List<BoxNumber> pairedBoxNumbers = boxNumberRepository.findByBoxBarcodeAndBoxNumber(boxBarcode, originalBoxNumber);
                        List<String> pairedBarcodes = pairedBoxNumbers.stream()
                            .filter(bn -> bn.getProductBarcode() != null && !bn.getProductBarcode().isEmpty())
                            .map(BoxNumber::getProductBarcode)
                            .filter(barcode -> !barcode.equals(productBarcode)) // Exclude the original barcode
                            .toList();
                    
                        for (String pairedBarcode : pairedBarcodes) {
                            if (inStockService.isInStock(pairedBarcode)) {
                                // Get the paired item's in_stock details
                                InStock pairedInStockDetails = inStockService.getInStockDetails(pairedBarcode);
                                Integer pairedBoxNumber = pairedInStockDetails != null ? pairedInStockDetails.getBoxNumber() : originalBoxNumber;
                                
                                // Remove paired barcode from in_stock
                                inStockService.removeFromStock(pairedBarcode);
                                
                                // Create lent record for paired barcode
                                lendService.createLendRecord(
                                    boxBarcode,
                                    productName,
                                    pairedBarcode,
                                    employeeId,
                                    shopName,
                                    note + " (paired with " + productBarcode + ")", 
                                    pairedBoxNumber,
                                    orderId
                                );
                                
                                // Create log entry for paired barcode
                                Logs pairedLog = logsService.createLog(
                                    boxBarcode,
                                    productName,
                                    pairedBarcode,
                                    "lent",
                                    note + " (paired with " + productBarcode + ")"
                                );
                                pairedLog.setBoxNumber(pairedBoxNumber);
                                pairedLog.setOrderId(orderId);
                                logsService.save(pairedLog);
                            }
                        }
                    }
                }
                
                // Create lent record for original item
                if (isSerializedProduct) {
                    lendService.createLendRecord(
                        boxBarcode,
                        productName,
                        productBarcode,
                        employeeId,
                        shopName,
                        note,
                        originalBoxNumber, // Use original box number from in_stock
                        orderId
                    );
                } else {
                    lendService.createNonSerializedLendRecord(
                        boxBarcode,
                        productName,
                        employeeId,
                        shopName,
                        note,
                        quantity,
                        orderId
                    );
                }
                
                // Create log entry
                Logs lentLog = logsService.createLog(
                    boxBarcode,
                    productName,
                    productBarcode,
                    "lent",
                    note,
                    isSerializedProduct ? originalBoxNumber : null, // Use original box number for serialized products
                    quantity
                );
                lentLog.setBoxNumber(isSerializedProduct ? originalBoxNumber : null);
                lentLog.setOrderId(orderId);
                logsService.save(lentLog);
                
                // Update current stock quantity
                CurrentStock lentStock = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                        .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
                int lentQuantityToDeduct = product.getNumberSn() == 2 ? quantity / 2 : quantity;
                lentStock.setQuantity(lentStock.getQuantity() - lentQuantityToDeduct);
                lentStock.setLastUpdated(LocalDateTime.now());
                currentStockRepository.save(lentStock);

                // Sync current stock with in_stock to ensure accurate quantity
                syncCurrentStockWithInStock(boxBarcode, productName);
                break;
                
            case "broken":
                // For serialized products, check in-stock status
                if (isSerializedProduct) {
                    // Check if the product is in stock
                    if (!inStockService.isInStock(productBarcode)) {
                        throw new InvalidInputException("Product barcode " + productBarcode + " is not in stock");
                    }
                    
                    // Remove from in_stock table
                    inStockService.removeFromStock(productBarcode);
                }
                
                // Create broken record
                if (isSerializedProduct) {
                    brokenService.createBrokenRecord(
                        boxBarcode,
                        productName,
                        productBarcode,
                        condition, 
                        note,
                        boxNumber,
                        1,
                        orderId
                    );
                } else {
                    brokenService.createNonSerializedBrokenRecord(
                        boxBarcode,
                        productName,
                        condition,
                        note,
                        quantity,
                        orderId
                    );
                }
                
                // Create log entry
                Logs brokenLog = logsService.createLog(
                    boxBarcode,
                    productName,
                    productBarcode,
                    "broken",
                    note
                );
                brokenLog.setBoxNumber(isSerializedProduct ? boxNumber : null); // Set boxNumber to null for non-serialized products
                brokenLog.setOrderId(orderId);
                logsService.save(brokenLog);
                
                // Update current stock quantity
                CurrentStock brokenStock = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                        .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
                int brokenQuantityToDeduct = product.getNumberSn() == 2 ? quantity / 2 : quantity;
                brokenStock.setQuantity(brokenStock.getQuantity() - brokenQuantityToDeduct);
                brokenStock.setLastUpdated(LocalDateTime.now());
                currentStockRepository.save(brokenStock);
                break;
                
            default:
                throw new InvalidInputException("Invalid destination: " + destination);
        }
        
        return stock;
    }
    
    /**
     * Return a lent item
     */
    @Transactional
    public CurrentStock returnLentItem(String boxBarcode, String productBarcode, String note, String lentId, Integer quantity) {
        // Validate box barcode is provided
        if (boxBarcode == null || boxBarcode.isEmpty()) {
            throw new InvalidInputException("Box barcode is required for returning lent items");
        }
        
        // Retrieve product from catalog
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));
        
        String productName = product.getProductName();
        
        // Check if the item is lent - handle differently for serialized and non-serialized products
        Lend lent;
        if (product.getNumberSn() == 0) {
            // For non-serialized products, find by box barcode only
            lent = lendRepository.findByBoxBarcodeAndOrderId(boxBarcode, lentId)
                    .stream()
                    .filter(item -> item.getProductBarcode() == null && "lent".equals(item.getStatus()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Lent item not found with box barcode: " + boxBarcode));
        } else {
            // For serialized products, find by both box barcode and product barcode
            lent = lendRepository.findByBoxBarcodeAndProductBarcode(boxBarcode, productBarcode)
                    .orElseThrow(() -> new ResourceNotFoundException("Lent item not found with box barcode: " + boxBarcode + " and product barcode: " + productBarcode));
        }
        
        // Get the box number from the lent record
        Integer boxNumber = lent.getBoxNumber();
        
        // Store the order ID for looking up paired items
        String orderId = lent.getOrderId();
        
        // Create or update stock
        CurrentStock stock = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                .orElse(new CurrentStock());
        
        stock.setBoxBarcode(boxBarcode);
        stock.setProductName(productName);
        stock.setBoxNumber(boxNumber);
        
        try {
            // Add the item to in_stock
            if (product.getNumberSn() > 0) {
                inStockService.addToStock(boxBarcode, productBarcode, productName, boxNumber, true);
                logger.info("Added item to in_stock table: {}", productBarcode);
            }
            
            // Create log entry for the item
            Logs log = logsService.createLog(
                boxBarcode, 
                productName, 
                product.getNumberSn() > 0 ? productBarcode : null,
                "returned", 
                note,
                boxNumber,
                product.getNumberSn() == 0 ? (quantity != null ? quantity : lent.getQuantity()) : 1
            );
            log.setOrderId(orderId);
            logsService.save(log);
            
            // Update the item's lent status
            lent.setStatus("returned");
            lendRepository.save(lent);
            
            // For 2SN products, update stock quantity appropriately
            if (product.getNumberSn() == 2) {
                // Always increment by 1 for individual items
                int existingQuantity = stock.getQuantity() != null ? stock.getQuantity() : 0;
                stock.setQuantity(existingQuantity + 1);
            } else if (product.getNumberSn() == 0) {
                // For non-serialized products
                int existingQuantity = stock.getQuantity() != null ? stock.getQuantity() : 0;
                int returnedQuantity = quantity != null ? quantity : lent.getQuantity();
                stock.setQuantity(existingQuantity + returnedQuantity);
            } else {
                // For SN=1 products
                stock.setQuantity(1);
            }
            
            // Update LentId status if provided
            if (lentId != null && !lentId.isEmpty()) {
                LentId lentIdRecord = lentIdRepository.findById(lentId).orElse(null);
                if (lentIdRecord != null) {
                    lentIdRecord.setStatus("returned");
                    lentIdRepository.save(lentIdRecord);
                }
            }
            
            // Save and return updated stock
            CurrentStock savedStock = currentStockRepository.save(stock);
            
            // Sync CurrentStock with InStock after returning item
            syncCurrentStockWithInStock(boxBarcode, productName);
            
            return savedStock;
            
        } catch (Exception e) {
            logger.error("Failed to process return for item {}: {}", productBarcode, e.getMessage());
            throw new RuntimeException("Failed to process return: " + e.getMessage(), e);
        }
    }
    
    /**
     * Add stock in bulk
     */
    @Transactional
    public List<CurrentStock> addStockBulk(StockAdditionDTO request) {
        // Retrieve product from catalog
        ProductCatalog product = productCatalogRepository.findById(request.getBoxBarcode())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + request.getBoxBarcode()));
        
        String productName = product.getProductName();
        
        // Validate the request based on product type
        validateBulkAdditionRequest(product, request);
        
        List<CurrentStock> results = new ArrayList<>();
        
        if (product.getNumberSn() == 0) {
            // For products without SN, process the quantity
            CurrentStock stock = processNonSerializedAddition(product, request);
            results.add(stock);
        } else if (product.getNumberSn() == 2) {
            // For products with 2 SN, handle pairs according to even/odd rule
            logger.info("Processing product with 2 SN values: {}", product.getProductName());
            List<String> barcodes = new ArrayList<>(request.getProductBarcodes());
            Set<String> barcodeSet = new HashSet<>(barcodes);
            Set<String> processed = new HashSet<>();
            List<String[]> pairs = new ArrayList<>();

            for (String barcode : barcodes) {
                if (processed.contains(barcode)) continue;
                String pairBarcode = null;
                try {
                    long number = extractNumber(barcode);
                    String prefix = barcode.replaceAll("\\d+$", "");
                    if (number % 2 == 0) {
                        pairBarcode = prefix + (number - 1);
                    } else {
                        pairBarcode = prefix + (number + 1);
                    }
                } catch (Exception e) {
                    // Fallback: use generatePairedBarcode
                    pairBarcode = generatePairedBarcode(barcode);
                }
                if (barcodeSet.contains(pairBarcode)) {
                    // Both barcodes are present, pair them
                    if (!processed.contains(pairBarcode)) {
                        pairs.add(new String[]{barcode, pairBarcode});
                        processed.add(barcode);
                        processed.add(pairBarcode);
                        logger.info("Paired barcodes: {} and {}", barcode, pairBarcode);
                    }
                } else {
                    // Only one barcode present, still process as a pair
                    pairs.add(new String[]{barcode, pairBarcode});
                    processed.add(barcode);
                    logger.info("Paired barcode {} with missing pair {}", barcode, pairBarcode);
                }
            }

            // Get or find the existing stock for this product
            Optional<CurrentStock> existingStockOpt = currentStockRepository.findByBoxBarcodeAndProductName(
                    request.getBoxBarcode(), productName);
            CurrentStock stock;
            if (existingStockOpt.isEmpty()) {
                // Create new stock entry
                stock = new CurrentStock();
                stock.setBoxBarcode(request.getBoxBarcode());
                stock.setProductName(productName);
                stock.setQuantity(pairs.size()); // Each pair counts as 1 quantity
                stock.setLastUpdated(LocalDateTime.now());
            } else {
                // Update existing stock
                stock = existingStockOpt.get();
                stock.setQuantity(stock.getQuantity() + pairs.size());
                stock.setLastUpdated(LocalDateTime.now());
            }
            // Save the stock
            stock = currentStockRepository.save(stock);
            results.add(stock);

            // Process barcodes in pairs for box number assignment and logging
            for (String[] pair : pairs) {
                String barcode1 = pair[0];
                String barcode2 = pair[1];
                // Get or create a box number for this pair
                BoxNumber boxNumber = boxNumberService.createBoxNumberIfNeeded(request.getBoxBarcode(), productName, barcode1);
                Integer boxNumberValue = boxNumber != null ? boxNumber.getBoxNumber() : null;
                logger.info("Assigned box number {} to pair", boxNumberValue);
                // Process first barcode in pair (log only)
                Logs log1 = logsService.createLog(request.getBoxBarcode(), productName, barcode1, "add", request.getNote());
                log1.setBoxNumber(boxNumberValue);
                logsService.save(log1);
                try {
                    inStockService.addToStock(request.getBoxBarcode(), barcode1, productName, boxNumberValue);
                    logger.info("Added barcode {} to in_stock table", barcode1);
                } catch (Exception e) {
                    logger.error("Failed to add barcode {} to in_stock table: {}", barcode1, e.getMessage(), e);
                }
                // Process second barcode in pair (log only, only if not null and not same as first)
                if (barcode2 != null && !barcode2.equals(barcode1)) {
                    Logs log2 = logsService.createLog(request.getBoxBarcode(), productName, barcode2, "add", request.getNote());
                    log2.setBoxNumber(boxNumberValue);
                    logsService.save(log2);
                    // Also create a box number entry for the second barcode with the same box number
                    BoxNumber boxNumber2 = new BoxNumber();
                    boxNumber2.setBoxBarcode(request.getBoxBarcode());
                    boxNumber2.setProductName(productName);
                    boxNumber2.setProductBarcode(barcode2);
                    boxNumber2.setBoxNumber(boxNumberValue);
                    boxNumber2.setLastUpdated(LocalDateTime.now());
                    boxNumberRepository.save(boxNumber2);
                    try {
                        inStockService.addToStock(request.getBoxBarcode(), barcode2, productName, boxNumberValue);
                        logger.info("Added barcode {} to in_stock table", barcode2);
                    } catch (Exception e) {
                        logger.error("Failed to add barcode {} to in_stock table: {}", barcode2, e.getMessage(), e);
                    }
                }
            }
        } else {
            // For products with SN=1, process each barcode individually but aggregate in stock
            Optional<CurrentStock> existingStockOpt = currentStockRepository.findByBoxBarcodeAndProductName(
                    request.getBoxBarcode(), productName);
            
            CurrentStock stock;
            if (existingStockOpt.isEmpty()) {
                // Create new stock entry
                stock = new CurrentStock();
                stock.setBoxBarcode(request.getBoxBarcode());
                stock.setProductName(productName);
                stock.setQuantity(request.getProductBarcodes().size());
                stock.setLastUpdated(LocalDateTime.now());
            } else {
                // Update existing stock
                stock = existingStockOpt.get();
                stock.setQuantity(stock.getQuantity() + request.getProductBarcodes().size());
                stock.setLastUpdated(LocalDateTime.now());
            }
            
            // Save the stock
            stock = currentStockRepository.save(stock);
            results.add(stock);
            
            // Log each barcode individually with a box number
            for (String barcode : request.getProductBarcodes()) {
                // Get or create a box number for each barcode
                BoxNumber boxNumber = boxNumberService.createBoxNumberIfNeeded(request.getBoxBarcode(), productName, barcode);
                Integer boxNumberValue = boxNumber != null ? boxNumber.getBoxNumber() : null;
                
                // Update the stock's box number if it's not set yet
                if (stock.getBoxNumber() == null && boxNumberValue != null) {
                    stock.setBoxNumber(boxNumberValue);
                    stock = currentStockRepository.save(stock);
                }
                
                Logs log = logsService.createLog(request.getBoxBarcode(), productName, barcode, "add", request.getNote());
                log.setBoxNumber(boxNumberValue);
                logsService.save(log);
                
                // Add to in_stock table
                try {
                    inStockService.addToStock(request.getBoxBarcode(), barcode, productName, boxNumberValue);
                    logger.info("Added barcode {} to in_stock table", barcode);
                } catch (Exception e) {
                    logger.error("Failed to add barcode {} to in_stock table: {}", barcode, e.getMessage(), e);
                    // Don't throw the exception, as we want to continue with the stock addition
                }
            }
        }
        
        // Update the current stock with the highest box number
        updateCurrentStockBoxNumber(request.getBoxBarcode(), productName);
        
        return results;
    }
    
    /**
     * Process addition of non-serialized products (SN=0)
     * Creates individual log entries for each item
     */
    private CurrentStock processNonSerializedAddition(ProductCatalog product, StockAdditionDTO request) {
        String boxBarcode = request.getBoxBarcode();
        String productName = product.getProductName();
        int quantity = request.getQuantity();
        String note = request.getNote();
        
        // Check if stock already exists
        CurrentStock stock = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                .orElse(new CurrentStock());
        
        // Set basic properties if it's a new record
        if (stock.getBoxBarcode() == null) {
            stock.setBoxBarcode(boxBarcode);
            stock.setProductName(productName);
            stock.setQuantity(quantity);
        } else {
            // Add to existing quantity instead of overwriting
            logger.info("Adding {} items to existing stock of {} for {}", quantity, stock.getQuantity(), productName);
            stock.setQuantity(stock.getQuantity() + quantity);
        }
        
        stock.setBoxNumber(null); // No box number for SN=0
        stock.setLastUpdated(LocalDateTime.now());
        
        // Create a single log entry with the total quantity
        Logs log = logsService.createLog(boxBarcode, productName, null, "add", note, null, quantity);
        logsService.save(log);
        
        // Save the updated stock and log the new quantity for debugging
        CurrentStock savedStock = currentStockRepository.save(stock);
        logger.info("Saved non-serialized stock: {}, new quantity = {}", productName, savedStock.getQuantity());
        
        return savedStock;
    }
    
    /**
     * Validate the bulk addition request
     */
    private void validateBulkAdditionRequest(ProductCatalog product, StockAdditionDTO request) {
        // For serialized products (SN=1 or SN=2)
        if (product.getNumberSn() > 0) {
            // Require product barcodes
            if (request.getProductBarcodes() == null || request.getProductBarcodes().isEmpty()) {
                throw new InvalidInputException("Product barcodes are required for serialized products");
            }
            
            // Quantity must match the number of barcodes
            if (request.getQuantity() != request.getProductBarcodes().size()) {
                throw new InvalidInputException("Quantity must match the number of product barcodes for serialized products");
            }
        } else {
            // For non-serialized products (SN=0)
            // Require quantity
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new InvalidInputException("Quantity must be greater than zero for non-serialized products");
            }
        }
    }
    
    /**
     * Extract a number from a barcode string
     * @param barcode The barcode to extract a number from
     * @return The extracted number
     * @throws NumberFormatException if no number can be extracted
     */
    private long extractNumber(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            throw new NumberFormatException("Barcode is null or empty");
        }
        
        // Extract the last sequence of digits from the barcode
        String numberPart = barcode.replaceAll(".*?(\\d+)(?:-PAIR)?$", "$1");
        if (numberPart.matches("\\d+")) {
            return Long.parseLong(numberPart);
        }
        
        // No number found
        throw new NumberFormatException("No number found in barcode: " + barcode);
    }

    /**
     * Generate a paired barcode by incrementing or decrementing based on odd/even
     * Odd numbers should be paired with number+1, even numbers with number-1
     */
    public String generatePairedBarcode(String barcode) {
        try {
            // Extract the numeric suffix
            long number = extractNumber(barcode);
            String prefix = barcode.replaceAll("\\d+(?:-PAIR)?$", "");
            
            // If the number is odd, increment it to get the next even number
            if (number % 2 != 0) {
                return prefix + (number + 1);
            } else {
                // If the number is even, decrement it to get the previous odd number
                return prefix + (number - 1);
            }
        } catch (NumberFormatException e) {
            logger.error("Failed to generate paired barcode for {}: {}", barcode, e.getMessage());
            throw new InvalidInputException("Invalid barcode format. Unable to determine pair for: " + barcode);
        }
    }

    /**
     * Remove stock in bulk
     */
    @Transactional
    public List<CurrentStock> removeStockBulk(BulkRemoveDTO request) {
        // Retrieve product from catalog
        ProductCatalog product = productCatalogRepository.findById(request.getBoxBarcode())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + request.getBoxBarcode()));
        
        String productName = product.getProductName();
        
        // Validate the request based on product type
        validateBulkRemovalRequest(product, request);
        
        List<CurrentStock> results = new ArrayList<>();
        
        if (product.getNumberSn() == 0) {
            // For products without SN, process the quantity
            CurrentStock stock = processNonSerializedRemoval(product, request);
            results.add(stock);
        } else {
            // For serialized products, validate that all barcodes exist
            if (request.getProductBarcodes() == null || request.getProductBarcodes().isEmpty()) {
                throw new InvalidInputException("Product barcodes are required for serialized products");
            }
            
            // Verify each barcode exists, belongs to this product, and is still in stock
            for (String barcode : request.getProductBarcodes()) {
                if (barcode == null || barcode.isEmpty()) {
                    throw new InvalidInputException("Product barcode cannot be null or empty");
                }
                
                // Verify the barcode exists in the system
                List<Logs> logs = logsService.getLogsByProductBarcode(barcode);
                if (logs.isEmpty()) {
                    throw new ResourceNotFoundException("Product barcode not found: " + barcode);
                }
                
                // Verify the barcode belongs to this product
                boolean belongsToProduct = logs.stream()
                        .anyMatch(log -> request.getBoxBarcode().equals(log.getBoxBarcode()));
                if (!belongsToProduct) {
                    throw new InvalidInputException("Product barcode " + barcode + " does not belong to box barcode " + request.getBoxBarcode());
                }
                
                // Check if the product barcode is still in stock using InStockService
                if (!inStockService.isInStock(barcode)) {
                    throw new InvalidInputException("Product barcode " + barcode + " is not in stock. It may have been already removed, sold, lent, or marked as broken.");
                }
            }
            
            // For products with SN, process each barcode individually
            for (String barcode : request.getProductBarcodes()) {
                CurrentStock stock = removeStock(request.getBoxBarcode(), barcode, 1, request.getNote());
                results.add(stock);
            }
        }
        
        return results;
    }

    /**
     * Validate bulk removal request
     */
    private void validateBulkRemovalRequest(ProductCatalog product, BulkRemoveDTO request) {
        if (request.getQuantity() <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero");
        }
        
        if (product.getNumberSn() == 0) {
            // For non-serialized products, product barcodes should be null or empty
            if (request.getProductBarcodes() != null && !request.getProductBarcodes().isEmpty()) {
                throw new InvalidInputException("Product barcodes should not be provided for non-serialized products");
            }
        } else {
            // For serialized products, product barcodes are required
            if (request.getProductBarcodes() == null || request.getProductBarcodes().isEmpty()) {
                throw new InvalidInputException("Product barcodes are required for serialized products");
            }
            
            // Quantity must match the number of barcodes for serialized products
            if (request.getQuantity() != request.getProductBarcodes().size()) {
                throw new InvalidInputException("Quantity must match the number of product barcodes for serialized products. " +
                                              "Requested quantity: " + request.getQuantity() + 
                                              ", Number of barcodes: " + request.getProductBarcodes().size());
            }
        }
    }

    /**
     * Process removal of non-serialized products
     */
    private CurrentStock processNonSerializedRemoval(ProductCatalog product, BulkRemoveDTO request) {
        // The product parameter is not used, so we can remove it
        return removeStock(request.getBoxBarcode(), null, request.getQuantity(), request.getNote());
    }

    /**
     * Update the box number in CurrentStock to the highest box number
     */
    private void updateCurrentStockBoxNumber(String boxBarcode, String productName) {
        // Get the highest box number for this product
        Integer highestBoxNumber = boxNumberService.getHighestBoxNumber(boxBarcode, productName);
        
        // Update the current stock with the highest box number
        Optional<CurrentStock> stockOpt = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName);
        if (stockOpt.isPresent() && highestBoxNumber != null) {
            CurrentStock stock = stockOpt.get();
            stock.setBoxNumber(highestBoxNumber);
            currentStockRepository.save(stock);
        }
    }

    /**
     * Generate a unique lent ID based on employee ID and timestamp
     */
    private String generateLentId(String employeeId) {
        // Format: EMPID-YYYYMMDD-HHMMSS
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return employeeId + "-" + timestamp;
    }

    /**
     * Process bulk addition of stock with SERIALIZABLE isolation to prevent race conditions
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized void processBulkAddition(StockAdditionDTO request) {
        synchronized(STOCK_LOCK) {
            try {
                // Add delay between operations to prevent race conditions
                Thread.sleep(100);
                
        // Validate request
        if (request.getBoxBarcode() == null || request.getBoxBarcode().isEmpty()) {
            throw new InvalidInputException("Box barcode is required");
        }
        
        if (request.getProductBarcodes() == null || request.getProductBarcodes().isEmpty()) {
            throw new InvalidInputException("Product barcodes are required");
        }
        
        // Retrieve product from catalog to check if it's serialized
        ProductCatalog product = productCatalogRepository.findById(request.getBoxBarcode())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + request.getBoxBarcode()));
        
        List<String> lockedBarcodes = new ArrayList<>();
        try {
            // For serialized products, check for duplicates
            if (product.getNumberSn() > 0) {
                // First, check for duplicates within the request itself
                List<String> duplicatesInRequest = findDuplicatesInList(request.getProductBarcodes());
                if (!duplicatesInRequest.isEmpty()) {
                    throw new InvalidInputException("Duplicate product barcodes in request: " + 
                            String.join(", ", duplicatesInRequest));
                }
                
                // Then check for duplicates in the system and lock the barcodes
                List<String> unavailableBarcodes = barcodeRegistryService.checkAndLockBarcodes(request.getProductBarcodes());
                
                // If unavailable barcodes found, throw exception
                if (!unavailableBarcodes.isEmpty()) {
                    throw new InvalidInputException("The following product barcodes are already in use: " + 
                            String.join(", ", unavailableBarcodes));
                }
                
                // Keep track of locked barcodes
                lockedBarcodes.addAll(request.getProductBarcodes());
                
                // Double-check that all barcodes are still available
                for (String barcode : request.getProductBarcodes()) {
                    // Check directly with repositories to be absolutely sure
                    boolean existsInBoxNumber = boxNumberRepository.findByProductBarcode(barcode).isPresent();
                    List<Logs> logs = logsRepository.findByProductBarcode(barcode);
                    boolean hasActiveLog = false;
                    if (!logs.isEmpty()) {
                        Logs latestLog = logs.stream()
                                .max(Comparator.comparing(Logs::getTimestamp))
                                .orElse(null);
                        if (latestLog != null && "add".equals(latestLog.getOperation())) {
                            hasActiveLog = true;
                        }
                    }
                    
                    if (existsInBoxNumber || hasActiveLog) {
                        // Release all locks and throw exception
                        barcodeRegistryService.releaseBarcodes(lockedBarcodes);
                        throw new InvalidInputException("Product barcode " + barcode + " became unavailable during processing");
                    }
                }
            }
            
            // Process each product barcode one by one
            for (String productBarcode : request.getProductBarcodes()) {
                try {
                    addStock(request.getBoxBarcode(), productBarcode, 1, request.getNote());
                    // Remove from lockedBarcodes as it's been successfully processed
                    lockedBarcodes.remove(productBarcode);
                } catch (Exception e) {
                    logger.error("Error processing barcode {}: {}", productBarcode, e.getMessage(), e);
                    // Don't release this barcode's lock yet, as we'll release all remaining locks in the finally block
                    throw e; // Re-throw to trigger transaction rollback
                }
            }
        } finally {
            // Always release any remaining barcode locks
            if (!lockedBarcodes.isEmpty()) {
                logger.info("Releasing {} remaining locked barcodes", lockedBarcodes.size());
                barcodeRegistryService.releaseBarcodes(lockedBarcodes);
                    }
                }
                
                // Explicitly flush all changes
                entityManager.flush();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Operation interrupted", e);
            }
        }
    }

    /**
     * Find duplicates in a list of strings
     */
    private List<String> findDuplicatesInList(List<String> list) {
        List<String> duplicates = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        
        for (String item : list) {
            if (seen.contains(item)) {
                if (!duplicates.contains(item)) {
                    duplicates.add(item);
                }
            } else {
                seen.add(item);
            }
        }
        
        return duplicates;
    }

    /**
     * Check if a product barcode already exists in the system
     */
    private boolean isProductBarcodeExists(String productBarcode) {
        if (productBarcode == null || productBarcode.isEmpty()) {
            logger.debug("Product barcode is null or empty");
            return false;
        }
        
        logger.debug("Checking if product barcode exists: {}", productBarcode);
        
        // Check logs
        List<Logs> existingLogs = logsService.findByProductBarcode(productBarcode);
        logger.debug("Found {} logs with product barcode {}", existingLogs.size(), productBarcode);
        if (!existingLogs.isEmpty()) {
            logger.info("Product barcode {} found in logs", productBarcode);
            return true;
        }
        
        // Check box_number table
        try {
            Optional<BoxNumber> boxNumber = boxNumberService.findByProductBarcode(productBarcode);
            logger.debug("Box number present: {}", boxNumber.isPresent());
            if (boxNumber.isPresent()) {
                logger.info("Product barcode {} found in box_number", productBarcode);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error checking box_number table: {}", e.getMessage(), e);
        }
        
        // Check current_stock table
        try {
            // Use in_stock table instead of current_stock
            boolean exists = inStockRepository.existsByProductBarcode(productBarcode);
            if (exists) {
                logger.info("Product barcode {} found in in_stock", productBarcode);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error checking current_stock table: {}", e.getMessage(), e);
        }
        
        logger.debug("Product barcode {} not found in any table", productBarcode);
        return false;
    }

    /**
     * Recombine two product barcodes from different pairs into a new pair in a new box
     * This is used when individual items from different pairs need to be combined
     * For example, when one item from each of two pairs is broken, and the remaining items
     * need to be combined into a new pair in a new box
     * 
     * @param targetBoxBarcode The box barcode of the target box where the new pair will be placed
     * @param productBarcode1 The first product barcode to recombine
     * @param productBarcode2 The second product barcode to recombine
     * @param note A note describing the recombination
     * @return The updated stock for the target box
     */
    @Transactional
    public CurrentStock recombineItems(String targetBoxBarcode, String productBarcode1, String productBarcode2, String note) {
        logger.info("Recombining items: targetBoxBarcode={}, productBarcode1={}, productBarcode2={}", 
                   targetBoxBarcode, productBarcode1, productBarcode2);
        
        // Validate input
        if (targetBoxBarcode == null || targetBoxBarcode.isEmpty()) {
            throw new InvalidInputException("Target box barcode is required");
        }
        
        if (productBarcode1 == null || productBarcode1.isEmpty()) {
            throw new InvalidInputException("First product barcode is required");
        }
        
        if (productBarcode2 == null || productBarcode2.isEmpty()) {
            throw new InvalidInputException("Second product barcode is required");
        }
        
        // Get product to check if it's a paired product (SN=2)
        ProductCatalog product = productCatalogRepository.findById(targetBoxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + targetBoxBarcode));
        
        // Ensure this is a paired product (SN=2)
        if (product.getNumberSn() != 2) {
            throw new InvalidInputException("Recombination is only supported for paired products (SN=2)");
        }
        
        String productName = product.getProductName();
        
        // Verify both barcodes are available for recombination
        if (!inStockService.isInStock(productBarcode1) && !boxNumberService.isProductBarcodeAvailable(productBarcode1)) {
            throw new InvalidInputException("Product barcode " + productBarcode1 + " is not available for recombination. " +
                                          "It must be either in stock or available for reuse (removed, sold, broken, etc.)");
        }
        
        if (!inStockService.isInStock(productBarcode2) && !boxNumberService.isProductBarcodeAvailable(productBarcode2)) {
            throw new InvalidInputException("Product barcode " + productBarcode2 + " is not available for recombination. " +
                                          "It must be either in stock or available for reuse (removed, sold, broken, etc.)");
        }
        
        // If either barcode is still in stock, remove it first
        if (inStockService.isInStock(productBarcode1)) {
            logger.info("Product barcode {} is still in stock, removing it first", productBarcode1);
            removeStock(targetBoxBarcode, productBarcode1, 1, "Removing for recombination");
        }
        
        if (inStockService.isInStock(productBarcode2)) {
            logger.info("Product barcode {} is still in stock, removing it first", productBarcode2);
            removeStock(targetBoxBarcode, productBarcode2, 1, "Removing for recombination");
        }
        
        // Clean up any existing box number entries for these barcodes
        cleanupExistingBoxNumberEntries(productBarcode1);
        cleanupExistingBoxNumberEntries(productBarcode2);
        
        // Add the first item using the standard addStock method
        // This will automatically create a box number and handle all the necessary logic
        CurrentStock stock = addStock(targetBoxBarcode, productBarcode1, 1, note + " (recombined item 1)");
        Integer boxNumberValue = stock.getBoxNumber();
        
        logger.info("Added first item with box number {}", boxNumberValue);
        
        // For the second item, we need to create a box number entry with the same box number
        // and update the logs, but we don't want to increment the stock quantity again
        BoxNumber secondBoxNumber = new BoxNumber();
        secondBoxNumber.setBoxBarcode(targetBoxBarcode);
        secondBoxNumber.setProductName(productName);
        secondBoxNumber.setProductBarcode(productBarcode2);
        secondBoxNumber.setBoxNumber(boxNumberValue);
        secondBoxNumber.setLastUpdated(LocalDateTime.now());
        boxNumberRepository.save(secondBoxNumber);
        
        // Create log entry for the second barcode
        Logs log2 = logsService.createLog(targetBoxBarcode, productName, productBarcode2, "add", note + " (recombined item 2)");
        log2.setBoxNumber(boxNumberValue);
        logsService.save(log2);
        
        logger.info("Added second item with the same box number {}", boxNumberValue);
        
        // Mark both barcodes as in use
        barcodeStatusService.markBarcodeInUse(productBarcode1);
        barcodeStatusService.markBarcodeInUse(productBarcode2);
        
        // Return the updated stock
        return stock;
    }
    
    /**
     * Clean up any existing box number entries for a product barcode
     */
    private void cleanupExistingBoxNumberEntries(String productBarcode) {
        if (productBarcode == null || productBarcode.isEmpty()) {
            return;
        }
        
        // Find and delete any existing box number entries for this barcode
        Optional<BoxNumber> existingBoxNumber = boxNumberRepository.findByProductBarcode(productBarcode);
        if (existingBoxNumber.isPresent()) {
            logger.info("Removing existing box number entry for barcode: {}", productBarcode);
            boxNumberRepository.delete(existingBoxNumber.get());
            entityManager.flush(); // Ensure the delete is processed before creating a new entry
        }
    }

    /**
     * Generate an auto-filled barcode based on the last barcode
     */
    private String generateAutoFilledBarcode(String lastBarcode) {
        if (lastBarcode == null || lastBarcode.isEmpty()) {
            return null;
        }
        
        try {
            long number = extractNumber(lastBarcode);
            String prefix = lastBarcode.substring(0, lastBarcode.length() - String.valueOf(number).length());
            return String.format("%s%d", prefix, number + 1);
        } catch (Exception e) {
            logger.error("Failed to generate auto-filled barcode: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to sync stock after operations for serialized products
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void syncCurrentStockWithInStock(String boxBarcode, String productName) {
        try {
            // Get product from catalog
            ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + boxBarcode));
            
            // Only sync for serialized products
            if (product.getNumberSn() > 0) {
                // Find or create CurrentStock entry
                CurrentStock stock = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                        .orElse(new CurrentStock());
                
                // Count actual items in InStock
                List<InStock> inStockItems = inStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName);
                int actualQuantity = inStockItems.size();
                
                // For paired items (SN=2), divide quantity by 2
                if (product.getNumberSn() == 2) {
                    actualQuantity = (actualQuantity + 1) / 2; // Round up for odd numbers
                }
                
                // Update stock
                stock.setBoxBarcode(boxBarcode);
                stock.setProductName(productName);
                stock.setQuantity(actualQuantity);
                stock.setLastUpdated(LocalDateTime.now());
                
                // Update box number if available
                Integer highestBoxNumber = boxNumberService.getHighestBoxNumber(boxBarcode, productName);
                if (highestBoxNumber != null) {
                    stock.setBoxNumber(highestBoxNumber);
                }
                
                currentStockRepository.save(stock);
                logger.info("Successfully synced CurrentStock. New quantity: {}", stock.getQuantity());
            }
        } catch (Exception e) {
            logger.error("Failed to sync CurrentStock: {}", e.getMessage(), e);
            // Don't throw the exception to avoid rolling back the main operation
        }
    }

    /**
     * Return an item from a sales order back to stock (no lent logic)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void returnSalesItemToStock(String boxBarcode, String productBarcode, int quantity, String note) {
        // Validate quantity
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero");
        }

        // Retrieve product from catalog
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));

        String productName = product.getProductName();

        if (product.getNumberSn() > 0) {
            // Serialized product: add back to in_stock
            if (productBarcode == null || productBarcode.isEmpty()) {
                throw new InvalidInputException("Product barcode is required for serialized products");
            }
            // Retrieve box number for this product barcode
            Integer boxNumber = null;
            Optional<BoxNumber> boxNumberRecord = boxNumberRepository.findByProductBarcode(productBarcode);
            if (boxNumberRecord.isPresent()) {
                boxNumber = boxNumberRecord.get().getBoxNumber();
            }
            // Add to in_stock (force add for returns)
            inStockService.addToStock(boxBarcode, productBarcode, productName, boxNumber, true);

            // Synchronize CurrentStock with InStock
            syncCurrentStockWithInStock(boxBarcode, productName);

            // Log
            Logs log = logsService.createLog(boxBarcode, productName, productBarcode, "return_from_sales", note, boxNumber);
            logsService.save(log);
        } else {
            // Non-serialized: just increase quantity
            CurrentStock stock = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found for box barcode: " + boxBarcode));
            stock.setQuantity(stock.getQuantity() + quantity);
            stock.setLastUpdated(LocalDateTime.now());
            currentStockRepository.save(stock);

            // Log with correct quantity
            Logs log = logsService.createLog(boxBarcode, productName, null, "return_from_sales", note, null, quantity);
            logsService.save(log);
        }
    }
} 