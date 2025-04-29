package com.inventory.service;

import com.inventory.model.Sales;
import com.inventory.model.Invoice;
import com.inventory.repository.SalesRepository;
import com.inventory.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.inventory.dto.OrderSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;

import java.time.LocalDateTime;

@Service
public class SalesService extends BaseServiceImpl<Sales, Long, SalesRepository> {
    
    private static final Logger logger = LoggerFactory.getLogger(SalesService.class);
    
    @Autowired
    private SalesRepository salesRepository;
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    public SalesService(SalesRepository repository) {
        super(repository);
    }
    
    /**
     * Create a sales record
     * @param quantity For serialized products, should always be 1. For non-serialized products, can be any positive number.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Sales createSalesRecord(String boxBarcode, String productName, String productBarcode, 
                                  String employeeId, String shopName, String orderId, String note, 
                                  Integer boxNumber, Integer quantity) {
        
        // Log all parameters to debug
        logger.info("SalesService.createSalesRecord: Creating sales record with parameters:");
        logger.info("boxBarcode={}", boxBarcode);
        logger.info("productName={}", productName);
        logger.info("productBarcode={}", productBarcode);
        logger.info("employeeId={}", employeeId);
        logger.info("shopName={}", shopName);
        logger.info("orderId={}", orderId);
        logger.info("boxNumber={}", boxNumber);
        logger.info("quantity={}", quantity);
        
        try {
            // Validate required parameters
            if (boxBarcode == null || boxBarcode.trim().isEmpty()) {
                throw new IllegalArgumentException("Box barcode is required");
            }
            if (productName == null || productName.trim().isEmpty()) {
                throw new IllegalArgumentException("Product name is required");
            }
            if (employeeId == null || employeeId.trim().isEmpty()) {
                employeeId = "SYSTEM";  // Default employee ID
                logger.info("Using default employee ID: SYSTEM");
            }
            if (shopName == null || shopName.trim().isEmpty()) {
                shopName = "DEFAULT";  // Default shop name
                logger.info("Using default shop name: DEFAULT");
            }
            if (orderId == null || orderId.trim().isEmpty()) {
                orderId = "ORDER-" + System.currentTimeMillis();  // Generate a random order ID
                logger.info("Generated order ID: {}", orderId);
            }
            if (quantity == null || quantity <= 0) {
                quantity = 1;  // Default quantity
                logger.info("Using default quantity: 1");
            }
            
            // Create or get invoice - Using orderId as the invoice number
            Invoice invoice = getOrCreateInvoice(orderId, employeeId, shopName);
            // Always set the user note in Invoice
            invoice.setNote(note);
            invoiceRepository.save(invoice);
            logger.info("Created/Retrieved invoice: {} with ID: {}", invoice.getInvoice(), invoice.getInvoiceId());
            
            // Create sales record
            Sales sales = new Sales();
            sales.setInvoiceId(invoice.getInvoiceId());
            sales.setBoxBarcode(boxBarcode);
            sales.setProductName(productName);
            sales.setProductBarcode(productBarcode);
            sales.setEmployeeId(employeeId);
            sales.setShopName(shopName);
            sales.setTimestamp(LocalDateTime.now());
            sales.setBoxNumber(boxNumber);
            sales.setNote(note);
            sales.setOrderId(orderId);
            sales.setQuantity(quantity);
            
            Sales savedSales = salesRepository.save(sales);
            logger.info("Saved sales record with ID: {}", savedSales.getSalesId());
            
            // Force flush to ensure immediate persistence
            salesRepository.flush();
            
            return savedSales;
        } catch (Exception e) {
            logger.error("ERROR creating sales record: {}", e.getMessage());
            e.printStackTrace();
            throw e;  // Rethrow to allow transaction handling to work
        }
    }
    
    /**
     * Get or create an invoice
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private Invoice getOrCreateInvoice(String orderId, String employeeId, String shopName) {
        logger.info("getOrCreateInvoice: orderId={}, employeeId={}, shopName={}", orderId, employeeId, shopName);
        
        try {
            // Check if invoice already exists
            Invoice invoice = null;
            if (orderId != null && !orderId.isEmpty()) {
                logger.info("Looking for existing invoice: {}", orderId);
                invoice = invoiceRepository.findByInvoice(orderId)
                        .stream().findFirst().orElse(null);
                
                if (invoice != null) {
                    logger.info("Found existing invoice: {} with ID: {}", invoice.getInvoice(), invoice.getInvoiceId());
                } else {
                    logger.info("No existing invoice found with number: {}", orderId);
                }
            }
            
            if (invoice == null) {
                // Create new invoice
                invoice = new Invoice();
                String finalInvoiceNumber = orderId != null && !orderId.isEmpty() ? 
                                          orderId : "INV-" + System.currentTimeMillis();
                invoice.setInvoice(finalInvoiceNumber);
                invoice.setEmployeeId(employeeId != null ? employeeId : "SYSTEM");
                invoice.setShopName(shopName != null ? shopName : "DEFAULT");
                
                // Initialize timestamp fields
                LocalDateTime now = LocalDateTime.now();
                invoice.setTimestamp(now);
                invoice.setLastModified(now);  // Initial creation is also the last modification
                
                // Initialize edit tracking
                invoice.setEditCount(0);  // No edits yet
                invoice.setEditHistory(null);  // No edit history yet
                
                invoice = invoiceRepository.save(invoice);
                logger.info("Created new invoice: {} with ID: {}", invoice.getInvoice(), invoice.getInvoiceId());
            }
            
            return invoice;
        } catch (Exception e) {
            logger.error("ERROR in getOrCreateInvoice: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // For serialized products (with boxNumber)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Sales createSalesRecord(String boxBarcode, String productName, String productBarcode, 
                                 String employeeId, String shopName, String orderId, String note,
                                 Integer boxNumber) {
        // For serialized products, quantity is always 1
        return createSalesRecord(boxBarcode, productName, productBarcode, employeeId, shopName, 
                               orderId, note, boxNumber, 1);
    }

    // For legacy compatibility (no boxNumber)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Sales createSalesRecord(String boxBarcode, String productName, String productBarcode, 
                                 String employeeId, String shopName, String orderId, String note) {
        // For serialized products, quantity is always 1
        return createSalesRecord(boxBarcode, productName, productBarcode, employeeId, shopName, 
                               orderId, note, null, 1);
    }

    /**
     * Create sales record for non-serialized product with specified quantity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Sales createNonSerializedSalesRecord(String boxBarcode, String productName, 
                                              String employeeId, String shopName, String orderId, 
                                              String note, int quantity) {
        return createSalesRecord(boxBarcode, productName, null, employeeId, shopName, 
                               orderId, note, null, quantity);
    }

    /**
     * Save a sales record
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Sales save(Sales sales) {
        // Add detailed debug logging with stack trace - this will help us identify where the call is coming from
        logger.info("SalesService.save - Initial isDirectSales value: {}", sales.getIsDirectSales());
        if (sales.getIsDirectSales() != null) {
            // Get stack trace to identify caller
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            if (stackTraceElements.length > 3) {
                logger.info("Called from: {}.{}", 
                    stackTraceElements[3].getClassName(), 
                    stackTraceElements[3].getMethodName());
            }
        }
        
        // Store the initial value to detect changes
        Boolean initialIsDirectSales = sales.getIsDirectSales();
        
        // CRITICAL FIX: Only set isDirectSales based on note content if it wasn't explicitly set
        // This ensures we NEVER override an explicitly set value
        if (initialIsDirectSales == null) {
            logger.info("isDirectSales not explicitly set, checking note content");
            // Only check for lent-to-sales conversion in the note content if not explicitly set
            if (sales.getNote() != null && sales.getNote().contains("from lent order")) {
                sales.setIsDirectSales(false);
                logger.info("Setting isDirectSales=false based on note content");
            } else {
                sales.setIsDirectSales(true); // Default to direct sales
                logger.info("Setting isDirectSales=true (default)");
            }
        } else {
            logger.info("Using explicitly set isDirectSales={}", initialIsDirectSales);
            // No code here that would modify the value - ensuring the explicit value is preserved
        }
        
        // Log the final value before saving
        logger.info("SalesService.save - Final isDirectSales value: {} (from initial: {})", 
            sales.getIsDirectSales(), initialIsDirectSales);
        
        Sales savedSales = salesRepository.save(sales);
        
        // Verify the saved value matches what we expected
        logger.info("After save operation - isDirectSales value: {}", savedSales.getIsDirectSales());
        
        return savedSales;
    }

    public Page<Sales> findAll(Pageable pageable) {
        return salesRepository.findAll(pageable);
    }

    public Sales createSales(Sales sales) {
        // Set default values
        sales.setSaleDateTime(LocalDateTime.now());
        
        // Store the initial value to detect changes
        Boolean initialIsDirectSales = sales.getIsDirectSales();
        logger.info("SalesService.createSales - Initial isDirectSales value: {}", initialIsDirectSales);
        
        // CRITICAL FIX: Only set isDirectSales based on note content if it wasn't explicitly set
        // This ensures we NEVER override an explicitly set value
        if (initialIsDirectSales == null) {
            // Only check note content for lent-to-sales detection if not explicitly set
            if (sales.getNote() != null && sales.getNote().contains("from lent order")) {
                sales.setIsDirectSales(false);
                logger.info("Setting isDirectSales=false based on note content");
            } else {
                sales.setIsDirectSales(true); // Default to direct sales
                logger.info("Setting isDirectSales=true (default)");
            }
        } else {
            logger.info("Using explicitly set isDirectSales={}", initialIsDirectSales);
            // No code here that would modify the value - ensuring the explicit value is preserved
        }
        
        // Log final value
        logger.info("SalesService.createSales - Final isDirectSales value: {} (from initial: {})", 
            sales.getIsDirectSales(), initialIsDirectSales);
        
        // Create or get invoice using the order ID
        Invoice invoice = getOrCreateInvoice(
            sales.getOrderId(), 
            sales.getEmployeeId(), 
            sales.getShopName()
        );
        sales.setInvoiceId(invoice.getInvoiceId());
        
        Sales savedSales = salesRepository.save(sales);
        logger.info("After save operation - isDirectSales value: {}", savedSales.getIsDirectSales());
        
        return savedSales;
    }
} 