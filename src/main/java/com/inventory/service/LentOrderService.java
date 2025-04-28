package com.inventory.service;

import com.inventory.dto.ProductIdentifierDTO;
import com.inventory.dto.LentOrderDTO;
import com.inventory.dto.LentItemBatchProcessDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.ProductCatalog;
import com.inventory.model.Lend;
import com.inventory.model.LentId;
import com.inventory.model.Logs;
import com.inventory.model.Sales;
import com.inventory.model.Invoice;
import com.inventory.repository.ProductCatalogRepository;
import com.inventory.repository.LogsRepository;
import com.inventory.repository.LendRepository;
import com.inventory.repository.LentIdRepository;
import com.inventory.repository.InvoiceRepository;
import com.inventory.repository.CurrentStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import com.inventory.dto.OrderSummaryDTO;
import org.springframework.data.domain.PageImpl;
import java.util.stream.Collectors;
import com.inventory.repository.CurrentStockRepository;
import java.util.Map;
import com.inventory.model.Sales;
import com.inventory.model.Invoice;
import java.util.Optional;

/**
 * Service for lent order operations
 */
@Service
public class LentOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(LentOrderService.class);
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private LogsRepository logsRepository;
    
    @Autowired
    private LendRepository lendRepository;
    
    @Autowired
    private LentIdRepository lentIdRepository;
    
    @Autowired
    private LogsService logsService;
    
    @Autowired
    private CurrentStockRepository currentStockRepository;
    
    @Autowired
    private LendService lendService;
    
    @Autowired
    private SalesService salesService;
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    /**
     * Process a lent order with mixed product types
     */
    @Transactional
    public void processLentOrder(LentOrderDTO orderDTO) {
        List<ProductIdentifierDTO> allProducts = orderDTO.getAllProducts();
        
        if (allProducts.isEmpty()) {
            throw new InvalidInputException("No products specified in the lent order");
        }
        
        // Validate shopName is provided
        if (orderDTO.getShopName() == null || orderDTO.getShopName().isEmpty()) {
            throw new InvalidInputException("Shop name is required for lent orders");
        }
        
        // Validate employeeId is provided
        if (orderDTO.getEmployeeId() == null || orderDTO.getEmployeeId().isEmpty()) {
            throw new InvalidInputException("Employee ID is required for lent orders");
        }
        
        // Validate orderId is provided
        if (orderDTO.getOrderId() == null || orderDTO.getOrderId().isEmpty()) {
            throw new InvalidInputException("Order ID is required for lent orders");
        }

        // Check for duplicate order ID
        if (lentIdRepository.findById(orderDTO.getOrderId()).isPresent()) {
            throw new InvalidInputException("Lent order ID already exists: " + orderDTO.getOrderId());
        }
        
        // Create LentId record for the order
        createLentIdRecord(orderDTO);
        
        // Process each product identifier
        for (ProductIdentifierDTO product : allProducts) {
            if (product.getIdentifier() == null || product.getIdentifier().isEmpty()) {
                throw new InvalidInputException("Product identifier cannot be empty");
            }
            
            processProductIdentifier(product, orderDTO);
        }
    }
    
    /**
     * Create a LentId record for the lent order
     */
    private void createLentIdRecord(LentOrderDTO orderDTO) {
        logger.info("Creating LentId record for order ID: {}", orderDTO.getOrderId());
        
        // Check if LentId record already exists
        if (lentIdRepository.findById(orderDTO.getOrderId()).isPresent()) {
            logger.info("LentId record already exists for order ID: {}", orderDTO.getOrderId());
            return;
        }
        
        // Create new LentId record
        LentId lentId = new LentId();
        lentId.setLentId(orderDTO.getOrderId());
        lentId.setEmployeeId(orderDTO.getEmployeeId());
        lentId.setShopName(orderDTO.getShopName());
        lentId.setTimestamp(LocalDateTime.now());
        lentId.setNote(orderDTO.getNote());
        lentId.setStatus("active");
        
        lentIdRepository.save(lentId);
        logger.info("Successfully created LentId record for order ID: {}", orderDTO.getOrderId());
    }
    
    /**
     * Process a single product identifier
     */
    private void processProductIdentifier(ProductIdentifierDTO product, LentOrderDTO orderDTO) {
        String identifier = product.getIdentifier();
        Boolean productSplitPair = product.getSplitPair();
        // Get the destination or default to "lent"
        String destination = product.getDestination() != null ? product.getDestination() : "lent";
        
        logger.info("Processing product for lent order: {} with splitPair={}, destination={}", 
                    identifier, productSplitPair, destination);
        
        // Check if it's a quantity format (BOX001:5)
        if (identifier.contains(":")) {
            processNonSerializedProduct(identifier, orderDTO, destination);
        } else {
            // It's a serialized product barcode
            processSerializedProduct(identifier, orderDTO, productSplitPair, destination);
        }
    }
    
    /**
     * Process a non-serialized product with quantity
     */
    private void processNonSerializedProduct(String identifier, LentOrderDTO orderDTO, String destination) {
        String[] parts = identifier.split(":");
        if (parts.length != 2) {
            throw new InvalidInputException("Invalid format for non-serialized product: " + identifier);
        }
        
        String boxBarcode = parts[0];
        int quantity;
        
        try {
            quantity = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Invalid quantity in: " + identifier);
        }
        
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero");
        }
        
        // Verify this is a non-serialized product
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));
        
        if (product.getNumberSn() != 0) {
            throw new InvalidInputException("Quantity format only valid for non-serialized products: " + boxBarcode);
        }
        
        logger.info("Processing non-serialized product: {} with quantity: {} for destination: {}", 
                   boxBarcode, quantity, destination);
        
        if ("return".equals(destination)) {
            // Non-serialized products can't be "returned" in this context because they were never lent
            throw new InvalidInputException("Cannot return non-serialized products that weren't previously lent");
        } else if ("sales".equals(destination)) {
            // Move directly to sales
            stockService.moveStock(
                boxBarcode,
                null, // No product barcode for non-serialized products
                quantity,
                "sales",
                orderDTO.getEmployeeId(),
                orderDTO.getShopName(),
                null, // No condition for sales
                orderDTO.getNote(),
                orderDTO.getOrderId() // Pass the order ID
            );
        } else {
            // Create a non-serialized lent record with null boxNumber to ensure non-serialized products 
            // don't have a box number in the database
            lendService.createNonSerializedLendRecord(
                boxBarcode,
                product.getProductName(),
                orderDTO.getEmployeeId(),
                orderDTO.getShopName(),
                orderDTO.getNote(),
                quantity,
                orderDTO.getOrderId()
            );
            
            // Create log entry for the lent item with null boxNumber
            Logs lentLog = logsService.createLog(
                boxBarcode,
                product.getProductName(),
                null, // No product barcode for non-serialized products
                "lent",
                orderDTO.getNote(),
                null, // Explicitly set boxNumber to null for non-serialized products
                quantity
            );
            lentLog.setOrderId(orderDTO.getOrderId());
            logsService.save(lentLog);
            
            // Update current stock quantity
            try {
                // Find current stock for this product
                com.inventory.model.CurrentStock stock = currentStockRepository.findByBoxBarcodeAndProductName(
                    boxBarcode, product.getProductName())
                    .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
                
                // Deduct the quantity
                stock.setQuantity(stock.getQuantity() - quantity);
                stock.setLastUpdated(LocalDateTime.now());
                currentStockRepository.save(stock);
                
                logger.info("Updated current stock for {} to {}", boxBarcode, stock.getQuantity());
            } catch (Exception e) {
                logger.error("Error updating stock: {}", e.getMessage(), e);
                throw e;
            }
        }
    }
    
    /**
     * Process a serialized product
     */
    private void processSerializedProduct(String productBarcode, LentOrderDTO orderDTO, Boolean productSplitPair, String destination) {
        // Find the box barcode for this product barcode
        String boxBarcode = findBoxBarcodeForProductBarcode(productBarcode);
        
        // Get the product from catalog to get product name
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + boxBarcode));
        
        // Use product-specific splitPair if provided, otherwise fall back to order's global splitPair,
        // and if that's also not provided, default to false
        boolean splitPair = (productSplitPair != null) ? productSplitPair : 
                           (orderDTO.getSplitPair() != null) ? orderDTO.getSplitPair() : false;
        
        logger.info("Processing product barcode {} with splitPair={}, destination={}", 
                    productBarcode, splitPair, destination);
        
        if ("return".equals(destination)) {
            // First move to lent, then immediately return
            // This ensures proper tracking in the system
            stockService.moveStock(
                boxBarcode,
                productBarcode,
                1, // Always quantity 1 for serialized products
                "lent",
                orderDTO.getEmployeeId(),
                orderDTO.getShopName(),
                null, // No condition for lent
                orderDTO.getNote() + " (Auto-returned)",
                orderDTO.getOrderId(), // Pass the order ID
                splitPair, // Pass the splitPair flag
                false  // Not a direct sale
            );
            
            // Then return it
            stockService.returnLentItem(
                boxBarcode,
                productBarcode,
                String.format("Returned from lent (order: %s)", orderDTO.getOrderId()),
                null, // No specific lentId
                1    // Quantity is always 1 for serialized products
            );
        } else if ("sales".equals(destination)) {
            // Move directly to sales
            stockService.moveStock(
                boxBarcode,
                productBarcode,
                1, // Always quantity 1 for serialized products
                "sales",
                orderDTO.getEmployeeId(),
                orderDTO.getShopName(),
                null, // No condition for sales
                orderDTO.getNote(),
                orderDTO.getOrderId(), // Pass the order ID
                splitPair, // Pass the splitPair flag
                false  // Not a direct sale
            );
        } else {
            // Default: move to lent
            stockService.moveStock(
                boxBarcode,
                productBarcode,
                1, // Always quantity 1 for serialized products
                "lent",
                orderDTO.getEmployeeId(),
                orderDTO.getShopName(),
                null, // No condition for lent
                orderDTO.getNote(),
                orderDTO.getOrderId(), // Pass the order ID
                splitPair, // Pass the splitPair flag
                false  // Not a direct sale
            );
        }
    }
    
    /**
     * Find the box barcode for a product barcode
     */
    private String findBoxBarcodeForProductBarcode(String productBarcode) {
        // Query the logs to find the most recent entry for this product barcode
        return logsRepository.findBoxBarcodeByProductBarcode(productBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product barcode not found: " + productBarcode));
    }
    
    /**
     * Get all lent items for a specific order ID
     * 
     * @param orderId The order ID to fetch lent items for
     * @return List of lent items with their details
     */
    public List<Lend> getLentItemsByOrderId(String orderId) {
        // Validate the order ID
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new InvalidInputException("Order ID is required");
        }
        
        logger.info("Fetching lent items for order ID: {}", orderId);
        
        // Query the lend repository for items with this order ID
        List<Lend> lentItems = lendRepository.findByOrderId(orderId);
        
        logger.info("Found {} lent items for order ID: {}", lentItems.size(), orderId);
        
        return lentItems;
    }
    
    /**
     * Process multiple lent items from an order with different destinations
     * 
     * @param orderId The order ID to process items for
     * @param request The request containing items and their destinations
     */
    public void processBatchLentItems(String orderId, LentItemBatchProcessDTO request) {
        // Validate the order ID
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new InvalidInputException("Order ID is required");
        }
        
        logger.info("Batch processing lent items for order ID: {}", orderId);
        
        boolean hasItems = false;

        // Process split destinations if provided
        if (request.getSplitDestinations() != null && !request.getSplitDestinations().isEmpty()) {
            hasItems = true;
            processSplitDestinations(orderId, request);
        }
        
        // Handle legacy format for backward compatibility
        if (request.getReturnToStock() != null && !request.getReturnToStock().isEmpty()) {
            hasItems = true;
            processReturnToStock(orderId, request);
        }
        
        if (request.getMoveToSales() != null && !request.getMoveToSales().isEmpty()) {
            hasItems = true;
            processMovingToSales(orderId, request);
        }
        
        if (request.getMarkAsBroken() != null && !request.getMarkAsBroken().isEmpty()) {
            hasItems = true;
            processMarkingAsBroken(orderId, request);
        }
        
        if (!hasItems) {
            logger.warn("No items provided for batch processing");
        }
    }
    
    /**
     * Process items with split destinations
     */
    @Transactional(propagation = Propagation.REQUIRED)
    private void processSplitDestinations(String orderId, LentItemBatchProcessDTO request) {
        logger.info("Processing split destinations for order ID: {}", orderId);
        
        for (Map.Entry<String, Map<String, Integer>> entry : request.getSplitDestinations().entrySet()) {
            String identifier = entry.getKey();
            Map<String, Integer> destinations = entry.getValue();
            
            // For non-serialized products
            if (identifier.contains(":")) {
                processNonSerializedSplitDestinations(identifier.split(":")[0], destinations, orderId, request);
            } else {
                // For serialized products, we don't support splitting as they are individual units
                throw new InvalidInputException("Split destinations are only supported for non-serialized products");
            }
        }
    }

    /**
     * Process non-serialized product with split destinations
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processNonSerializedSplitDestinations(String boxBarcode, Map<String, Integer> destinations, 
                                                     String orderId, LentItemBatchProcessDTO request) {
        // Find the lent record
        List<Lend> lentItems = lendRepository.findByBoxBarcodeAndOrderId(boxBarcode, orderId);
        
        if (lentItems.isEmpty()) {
            throw new ResourceNotFoundException("Lent item not found with box barcode: " + boxBarcode + 
                " and order ID: " + orderId);
        }
        
        // Get the active lent record
        Lend lentItem = lentItems.stream()
                .filter(item -> item.getProductBarcode() == null && "lent".equals(item.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active lent item not found with box barcode: " + boxBarcode + " and order ID: " + orderId));
        
        // Get the original quantity
        int originalQuantity = lentItem.getQuantity();
        
        // Calculate total requested quantity
        int totalRequestedQuantity = destinations.values().stream().mapToInt(Integer::intValue).sum();
        
        // Verify total requested quantity doesn't exceed original quantity
        if (totalRequestedQuantity > originalQuantity) {
            throw new InvalidInputException("Total requested quantity " + totalRequestedQuantity + 
                " exceeds lent quantity " + originalQuantity);
        }
        
        // Verify this is a non-serialized product
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + boxBarcode));
        
        if (product.getNumberSn() > 0) {
            throw new InvalidInputException("Product " + boxBarcode + " is a serialized product");
        }
        
        // Process each destination
        int remainingQuantity = originalQuantity;
        
        // Process returns first
        if (destinations.containsKey("return")) {
            int returnQuantity = destinations.get("return");
            if (returnQuantity > 0) {
                // Return items to stock
                stockService.returnLentItem(
                    boxBarcode,
                    null,
                    String.format("Returned %d from lent (order: %s)", returnQuantity, orderId),
                    orderId,
                    returnQuantity  // Pass the return quantity explicitly
                );
                
                // Create record for returned portion
                Lend returnedPortion = new Lend();
                returnedPortion.setBoxBarcode(boxBarcode);
                returnedPortion.setProductName(lentItem.getProductName());
                returnedPortion.setEmployeeId(request.getEmployeeId());
                returnedPortion.setShopName(lentItem.getShopName());
                returnedPortion.setTimestamp(LocalDateTime.now());
                returnedPortion.setQuantity(returnQuantity);
                returnedPortion.setOrderId(orderId);
                returnedPortion.setStatus("returned");
                returnedPortion.setNote("Split return: " + returnQuantity + " of " + originalQuantity);
                lendRepository.save(returnedPortion);
                
                remainingQuantity -= returnQuantity;
            }
        }
        
        // Process sales
        if (destinations.containsKey("sales")) {
            int salesQuantity = destinations.get("sales");
            if (salesQuantity > 0) {
                // Create sales order ID if needed
                String salesOrderId = (request.getSalesOrderId() != null && !request.getSalesOrderId().trim().isEmpty()) 
                    ? request.getSalesOrderId() 
                    : "SALES-FROM-LENT-" + orderId;
                
                // Move items to sales
                stockService.moveFromLentToSales(
                    boxBarcode,
                    null,
                    salesQuantity,
                    request.getEmployeeId(),
                    request.getShopName(),
                    request.getNote() != null ? request.getNote() : "Moved from lent to sales",
                    salesOrderId,
                    orderId,
                    request.getIsDirectSales()
                );
                
                // Create record for sold portion
                Lend soldPortion = new Lend();
                soldPortion.setBoxBarcode(boxBarcode);
                soldPortion.setProductName(lentItem.getProductName());
                soldPortion.setEmployeeId(request.getEmployeeId());
                soldPortion.setShopName(lentItem.getShopName());
                soldPortion.setTimestamp(LocalDateTime.now());
                soldPortion.setQuantity(salesQuantity);
                soldPortion.setOrderId(orderId);
                soldPortion.setStatus("lent to sales");
                soldPortion.setNote("Split sale: " + salesQuantity + " of " + originalQuantity);
                lendRepository.save(soldPortion);
                
                remainingQuantity -= salesQuantity;
            }
        }
        
        // Update original record if there are remaining items
        if (remainingQuantity > 0) {
            lentItem.setQuantity(remainingQuantity);
            lentItem.setNote(lentItem.getNote() != null ? 
                lentItem.getNote() + String.format(" [Split: %d remaining]", remainingQuantity) : 
                String.format("Split: %d remaining", remainingQuantity));
            lendRepository.save(lentItem);
        } else {
            // If no remaining items, mark the original record as fully processed
            lentItem.setQuantity(0);
            lentItem.setStatus("processed");
            lentItem.setNote(lentItem.getNote() != null ? 
                lentItem.getNote() + " [Fully processed]" : 
                "Fully processed");
            lendRepository.save(lentItem);
        }
        
        // Update order status
        updateOrderStatus(orderId);
    }
    
    /**
     * Process items that should be returned to stock
     */
    @Transactional(noRollbackFor = Exception.class)
    private void processReturnToStock(String orderId, LentItemBatchProcessDTO request) {
        if (request.getReturnToStock() == null || request.getReturnToStock().isEmpty()) {
            return;
        }
        
        logger.info("Processing {} items to return to stock", request.getReturnToStock().size());
        
        Set<String> processedBarcodes = new HashSet<>();
        
        for (String identifier : request.getReturnToStock()) {
            // Skip if already processed as part of a pair
            if (processedBarcodes.contains(identifier)) {
                logger.info("Skipping already processed identifier: {}", identifier);
                continue;
            }
            
            try {
                logger.info("Processing return for identifier: {}", identifier);
                
                // Check if it's a quantity format (BOX001:5)
                if (identifier.contains(":")) {
                    processNonSerializedReturn(identifier, orderId, request);
                } else {
                    // Find the lent record first to verify it exists and is active
                    Lend lentItem = lendRepository.findByProductBarcodeAndOrderId(identifier, orderId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Lent item not found with barcode: " + identifier + " and order ID: " + orderId));
                    
                    if (!"lent".equals(lentItem.getStatus())) {
                        logger.warn("Skipping return of item {} as it's not in 'lent' status. Current status: {}", 
                                  identifier, lentItem.getStatus());
                        continue;
                    }
                    
                    // It's a serialized product barcode
                    processSerializedReturn(identifier, orderId, request, processedBarcodes);
                }
                
                logger.info("Successfully processed return for item {}", identifier);
            } catch (Exception e) {
                logger.error("Failed to return item {} to stock: {}", identifier, e.getMessage(), e);
                // Continue with the next item rather than failing the entire batch
            }
        }
        
        // Update order status after all items are processed
        updateOrderStatus(orderId);
    }
    
    /**
     * Process return of a non-serialized product
     */
    @Transactional(noRollbackFor = Exception.class)
    private void processNonSerializedReturn(String identifier, String orderId, LentItemBatchProcessDTO request) {
        logger.info("Processing non-serialized return for identifier: {}", identifier);
        
        String[] parts = identifier.split(":");
        if (parts.length != 2) {
            throw new InvalidInputException("Invalid format for non-serialized product: " + identifier);
        }
        
        String boxBarcode = parts[0];
        int returnQuantity = Integer.parseInt(parts[1]);
        
        // Find the lent record
        List<Lend> lentItems = lendRepository.findByBoxBarcodeAndOrderId(boxBarcode, orderId);
        
        if (lentItems.isEmpty()) {
            throw new ResourceNotFoundException("Lent item not found with box barcode: " + boxBarcode);
        }
        
        // Get the active lent record
        Lend lentItem = lentItems.stream()
                .filter(item -> item.getProductBarcode() == null && "lent".equals(item.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active lent item not found with box barcode: " + boxBarcode));

        // Create return record
        Lend returnRecord = new Lend();
        returnRecord.setBoxBarcode(boxBarcode);
        returnRecord.setProductName(lentItem.getProductName());
        returnRecord.setProductBarcode(null);
        returnRecord.setEmployeeId(request.getEmployeeId());
        returnRecord.setShopName(lentItem.getShopName());
        returnRecord.setTimestamp(LocalDateTime.now());
        returnRecord.setQuantity(returnQuantity);
        returnRecord.setOrderId(orderId);
        returnRecord.setStatus("returned");
        returnRecord.setNote("Split return from original lent quantity: " + lentItem.getQuantity());
        lendRepository.save(returnRecord);

        // Return items to stock with correct quantity
        stockService.returnLentItem(
            boxBarcode,
            null,
            String.format("Returned %d from lent (order: %s)", returnQuantity, orderId),
            orderId,
            returnQuantity  // Pass the return quantity explicitly
        );

        // Update the original lent record
        lentItem.setStatus("processed");
        lentItem.setQuantity(0);
        lentItem.setNote(lentItem.getNote() != null ? 
            lentItem.getNote() + " [Fully processed]" : 
            "Fully processed");
        lendRepository.save(lentItem);

        // Update order status
        updateOrderStatus(orderId);
        
        logger.info("Successfully processed non-serialized return for {}", boxBarcode);
    }
    
    /**
     * Process return of a serialized product
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    private void processSerializedReturn(String productBarcode, String orderId, LentItemBatchProcessDTO request, Set<String> processedBarcodes) {
        logger.info("Processing serialized return for product: {}", productBarcode);
        
        // Find the latest lent record for this product
        Lend lentItem = lendRepository.findLatestByProductBarcodeAndOrderId(productBarcode, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lent item not found with barcode: " + productBarcode + " and order ID: " + orderId));
        
        // Check if the item is already returned
        if ("returned".equals(lentItem.getStatus())) {
            logger.info("Item {} is already returned", productBarcode);
            return;
        }
        
        // Return the item to stock
        stockService.returnLentItem(
            lentItem.getBoxBarcode(),
            productBarcode,
            String.format("Returned from lent (order: %s)", orderId),
            orderId, // Use orderId as lentId
            1    // Quantity is always 1 for serialized products
        );
        
        // Update the lent record status
        lentItem.setStatus("returned");
        lentItem.setNote(lentItem.getNote() != null ? 
            lentItem.getNote() + " [Returned to stock]" : 
            "Returned to stock");
        lendRepository.save(lentItem);
        
        // Mark this barcode as processed
        processedBarcodes.add(productBarcode);
        
        logger.info("Successfully returned serialized product {} to stock", productBarcode);
    }
    
    /**
     * Process items that should be moved to sales
     */
    @Transactional(propagation = Propagation.REQUIRED, noRollbackFor = Exception.class)
    private void processMovingToSales(String orderId, LentItemBatchProcessDTO request) {
        // Validate shop name is provided for sales
        if (request.getShopName() == null || request.getShopName().trim().isEmpty()) {
            throw new InvalidInputException("Shop name is required when moving items to sales");
        }
        
        logger.info("Processing {} items to move to sales", request.getMoveToSales().size());
        
        // Create a new sales order ID if needed
        String salesOrderId = (request.getSalesOrderId() != null && !request.getSalesOrderId().trim().isEmpty()) 
                ? request.getSalesOrderId() 
                : "SALES-FROM-LENT-" + orderId;
        
        logger.info("Using sales order ID: {}", salesOrderId);
        
        // Keep track of processed barcodes to avoid double-processing pairs
        Set<String> processedBarcodes = new HashSet<>();
        
        for (String identifier : request.getMoveToSales()) {
            // Skip if already processed as part of a pair
            if (processedBarcodes.contains(identifier)) {
                logger.info("Skipping already processed identifier: {}", identifier);
                continue;
            }
            
            try {
                // Check if it's a quantity format (BOX001:5)
                if (identifier.contains(":")) {
                    processNonSerializedSales(identifier, orderId, request, salesOrderId);
                } else {
                    // It's a serialized product barcode
                    processSerializedSales(identifier, orderId, request, salesOrderId, processedBarcodes);
                }
                
                logger.info("Successfully processed sales for item {}", identifier);
            } catch (Exception e) {
                logger.error("Failed to move item {} to sales: {}", identifier, e.getMessage(), e);
                // Continue with the next item rather than failing the entire batch
            }
        }
    }
    
    /**
     * Process sales of a non-serialized product
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    private void processNonSerializedSales(String identifier, String orderId, LentItemBatchProcessDTO request, String salesOrderId) {
        logger.info("Processing non-serialized sales for identifier: {}", identifier);
        
        // Parse the identifier (format: BOX001:5)
        String[] parts = identifier.split(":");
        if (parts.length != 2) {
            logger.error("Invalid format for non-serialized product. Expected BOX001:5, got: {}", identifier);
            throw new InvalidInputException("Invalid format for non-serialized product: " + identifier + 
                ". Expected format: BOXBARCODE:QUANTITY");
        }
        
        String boxBarcode = parts[0];
        int requestedQuantity;
        
        try {
            requestedQuantity = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            logger.error("Invalid quantity in identifier: {}", identifier);
            throw new InvalidInputException("Invalid quantity in: " + identifier + 
                ". Quantity must be a valid number.");
        }
        
        if (requestedQuantity <= 0) {
            logger.error("Invalid quantity {} in identifier: {}", requestedQuantity, identifier);
            throw new InvalidInputException("Quantity must be greater than zero");
        }
        
        logger.info("Looking for lent record with box barcode: {} and order ID: {}", boxBarcode, orderId);
        
        // Find all lent records for this box barcode and order
        List<Lend> lentItems = lendRepository.findByBoxBarcodeAndOrderId(boxBarcode, orderId);
        
        if (lentItems.isEmpty()) {
            logger.error("No lent records found for box barcode: {} and order ID: {}", boxBarcode, orderId);
            throw new ResourceNotFoundException("Lent item not found with box barcode: " + boxBarcode + " and order ID: " + orderId);
        }
        
        // Get the original lent record (the one with the initial quantity)
        Lend originalLentItem = lentItems.stream()
                .filter(item -> item.getProductBarcode() == null)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Original lent item not found with box barcode: " + boxBarcode + " and order ID: " + orderId));

        // Get the initial quantity from the logs table
        List<Logs> logs = logsRepository.findByBoxBarcodeAndOrderId(boxBarcode, orderId);
        int initialQuantity = logs.stream()
                .filter(log -> "lent".equals(log.getOperation()))
                .mapToInt(Logs::getQuantity)
                .findFirst()
                .orElse(0);

        if (initialQuantity == 0) {
            throw new ResourceNotFoundException("Could not find initial lent quantity in logs for box barcode: " + 
                boxBarcode + " and order ID: " + orderId);
        }

        // Calculate total processed quantity (returned + to be sold)
        int returnedQuantity = lentItems.stream()
                .filter(item -> "returned".equals(item.getStatus()))
                .mapToInt(Lend::getQuantity)
                .sum();

        int soldQuantity = lentItems.stream()
                .filter(item -> "lent to sales".equals(item.getStatus()))
                .mapToInt(Lend::getQuantity)
                .sum();
                
        if (returnedQuantity + soldQuantity + requestedQuantity > initialQuantity) {
            throw new InvalidInputException(String.format(
                "Total processed quantity (%d returned + %d already sold + %d to sell = %d) exceeds original lent quantity %d",
                returnedQuantity, soldQuantity, requestedQuantity, returnedQuantity + soldQuantity + requestedQuantity, initialQuantity));
        }

        // Get product from catalog
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + boxBarcode));

        // Create sales record
        Sales sale = new Sales();
        sale.setBoxBarcode(boxBarcode);
        sale.setProductName(originalLentItem.getProductName());
        sale.setProductBarcode(null);
        sale.setEmployeeId(request.getEmployeeId());
        sale.setShopName(request.getShopName());
        sale.setOrderId(salesOrderId);
        sale.setNote("Moved from lent order: " + orderId);
        sale.setBoxNumber(null);
        sale.setIsDirectSales(request.getIsDirectSales());
        sale.setQuantity(requestedQuantity);
        sale.setTimestamp(LocalDateTime.now());

        // Get or create invoice
        Invoice invoice = getOrCreateInvoice(salesOrderId, request.getEmployeeId(), request.getShopName());
        sale.setInvoiceId(invoice.getInvoiceId());
        salesService.save(sale);

        // Create log entry for the sale
        Logs saleLog = new Logs();
        saleLog.setBoxBarcode(boxBarcode);
        saleLog.setProductName(originalLentItem.getProductName());
        saleLog.setProductBarcode(null);
        saleLog.setQuantity(requestedQuantity);
        saleLog.setOperation("moved_from_lent_to_sales");
        saleLog.setNote("Moved from lent order: " + orderId);
        saleLog.setOrderId(salesOrderId);
        saleLog.setTimestamp(LocalDateTime.now());
        logsRepository.save(saleLog);

        // Create record for sold portion
        Lend soldPortion = new Lend();
        soldPortion.setBoxBarcode(boxBarcode);
        soldPortion.setProductName(originalLentItem.getProductName());
        soldPortion.setEmployeeId(request.getEmployeeId());
        soldPortion.setShopName(request.getShopName());
        soldPortion.setTimestamp(LocalDateTime.now());
        soldPortion.setQuantity(requestedQuantity);
        soldPortion.setOrderId(orderId);
        soldPortion.setStatus("lent to sales");
        soldPortion.setNote("Split sale: " + requestedQuantity + " of " + initialQuantity);
        lendRepository.save(soldPortion);

        // Update order status
        updateOrderStatus(orderId);
        
        logger.info("Successfully processed non-serialized sales for {}", boxBarcode);
    }
    
    /**
     * Process sales of a serialized product
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    private void processSerializedSales(String productBarcode, String orderId, LentItemBatchProcessDTO request, 
                                      String salesOrderId, Set<String> processedBarcodes) {
        // Find the lent record for this product
        Lend lentItem = lendRepository.findByProductBarcodeAndOrderId(productBarcode, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lent item not found with barcode: " + productBarcode + " and order ID: " + orderId));
        
        // Get product from catalog
        ProductCatalog product = productCatalogRepository.findById(lentItem.getBoxBarcode())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + lentItem.getBoxBarcode()));
        
        // Use moveFromLentToSales for consistent behavior with non-serialized products
        stockService.moveFromLentToSales(
            lentItem.getBoxBarcode(),
            productBarcode,
            1, // Always quantity 1 for serialized products
            request.getEmployeeId(),
            request.getShopName(),
            request.getNote() != null ? request.getNote() : "Moved from lent to sales",
            salesOrderId, // Use the provided or generated sales order ID 
            orderId, // Original lent order ID
            request.getIsDirectSales() // Use the value from the DTO
        );
        
        // Double-check the lent record to ensure it has the "lent to sales" status
        Lend updatedLentItem = lendRepository.findById(lentItem.getLentId()).orElse(null);
        if (updatedLentItem != null && !"lent to sales".equals(updatedLentItem.getStatus())) {
            logger.info("Ensuring lent record status is 'lent to sales' for UI consistency");
            updatedLentItem.setStatus("lent to sales");
            lendRepository.save(updatedLentItem);
            lendRepository.flush(); // Force immediate persistence
        }
        
        // Mark this barcode as processed
        processedBarcodes.add(productBarcode);
        
        // Update order status after processing the item
        updateOrderStatus(orderId);
    }
    
    /**
     * Process items that should be marked as broken
     */
    @Transactional(noRollbackFor = Exception.class)
    private void processMarkingAsBroken(String orderId, LentItemBatchProcessDTO request) {
        // Validate condition is provided for broken items
        if (request.getCondition() == null || request.getCondition().trim().isEmpty()) {
            throw new InvalidInputException("Condition is required when marking items as broken");
        }
        
        logger.info("Processing {} items to mark as broken", request.getMarkAsBroken().size());
        
        // Create a broken order ID
        String brokenOrderId = "BROKEN-FROM-LENT-" + orderId;
        
        // Keep track of processed barcodes to avoid double-processing pairs
        Set<String> processedBarcodes = new HashSet<>();
        
        for (String identifier : request.getMarkAsBroken()) {
            // Skip if already processed as part of a pair
            if (processedBarcodes.contains(identifier)) {
                logger.info("Skipping already processed identifier: {}", identifier);
                continue;
            }
            
            try {
                // Check if it's a quantity format (BOX001:5)
                if (identifier.contains(":")) {
                    processNonSerializedBroken(identifier, orderId, request, brokenOrderId);
                } else {
                    // It's a serialized product barcode
                    processSerializedBroken(identifier, orderId, request, brokenOrderId, processedBarcodes);
                }
                
                logger.info("Successfully processed broken mark for item {}", identifier);
            } catch (Exception e) {
                logger.error("Failed to mark item {} as broken: {}", identifier, e.getMessage(), e);
                // Continue with the next item rather than failing the entire batch
            }
        }
    }
    
    /**
     * Process marking a non-serialized product as broken
     */
    private void processNonSerializedBroken(String identifier, String orderId, LentItemBatchProcessDTO request, String brokenOrderId) {
        String[] parts = identifier.split(":");
        if (parts.length != 2) {
            throw new InvalidInputException("Invalid format for non-serialized product: " + identifier);
        }
        
        String boxBarcode = parts[0];
        int quantity;
        
        try {
            quantity = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Invalid quantity in: " + identifier);
        }
        
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero");
        }
        
        // Find lent records for this non-serialized product by box barcode and order ID
        List<Lend> lentItems = lendRepository.findByBoxBarcodeAndOrderId(boxBarcode, orderId);
        
        if (lentItems.isEmpty()) {
            throw new ResourceNotFoundException("Lent item not found with box barcode: " + boxBarcode + " and order ID: " + orderId);
        }
        
        // There should be only one record for a non-serialized product with the given box barcode
        Lend lentItem = lentItems.stream()
                .filter(item -> item.getProductBarcode() == null && "lent".equals(item.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lent item not found with box barcode: " + boxBarcode + " and order ID: " + orderId));
        
        // Verify this is a non-serialized product
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + boxBarcode));
        
        if (product.getNumberSn() != 0) {
            throw new InvalidInputException("Invalid product type for quantity format: " + boxBarcode);
        }
        
        // Move the item to broken
        stockService.moveStock(
            boxBarcode,
            null, // No product barcode for non-serialized products
            quantity,
            "broken",
            request.getEmployeeId(),
            null, // No shop name for broken items
            request.getCondition(),
            request.getNote() != null ? request.getNote() : "Marked as broken from lent",
            brokenOrderId // Use the generated broken order ID
        );
        
        // Update the lent record status
        lentItem.setStatus("broken");
        lendRepository.save(lentItem);
        
        // Update order status after processing the broken item
        updateOrderStatus(orderId);
        
        logger.info("Successfully marked non-serialized item {} with quantity {} as broken", boxBarcode, quantity);
    }
    
    /**
     * Process marking a serialized product as broken
     */
    private void processSerializedBroken(String productBarcode, String orderId, LentItemBatchProcessDTO request, 
                                       String brokenOrderId, Set<String> processedBarcodes) {
        // Find the lent record for this product
        Lend lentItem = lendRepository.findByProductBarcodeAndOrderId(productBarcode, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lent item not found with barcode: " + productBarcode + " and order ID: " + orderId));
        
        // Get product from catalog
        ProductCatalog product = productCatalogRepository.findById(lentItem.getBoxBarcode())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + lentItem.getBoxBarcode()));
        
        // Process this product's broken status
        stockService.moveStock(
            lentItem.getBoxBarcode(),
            productBarcode,
            1, // Always quantity 1 for serialized products
            "broken",
            request.getEmployeeId(),
            null, // No shop name for broken items
            request.getCondition(),
            request.getNote() != null ? request.getNote() : "Marked as broken from lent",
            brokenOrderId, // Use the generated broken order ID
            true, // Always split pairs
            false  // Not a direct sale
        );
        
        // Update the lent record status
        lentItem.setStatus("broken");
        lendRepository.save(lentItem);
        
        // Mark this barcode as processed
        processedBarcodes.add(productBarcode);
        
        // Update order status after processing the item
        updateOrderStatus(orderId);
    }
    
    /**
     * Update order status based on its items' statuses
     * Order is considered "completed" only when all items are no longer in "active" status
     */
    @Transactional
    private void updateOrderStatus(String orderId) {
        logger.info("Updating order status for order ID: {}", orderId);
        
        // Get all items for this order
        List<Lend> items = lendRepository.findByOrderId(orderId);
        
        if (items.isEmpty()) {
            logger.warn("No items found for order ID: {}", orderId);
            return;
        }
        
        // Check if any items are still in active lent status
        boolean hasActiveItems = items.stream()
                .anyMatch(item -> item.getStatus() == null || 
                                item.getStatus().equals("active") || 
                                item.getStatus().equals("lent"));
        
        // Get the order record
        LentId order = lentIdRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        
        // Calculate total quantities for different statuses
        int totalQuantity = items.stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
        
        int processedQuantity = items.stream()
                .filter(item -> "processed".equals(item.getStatus()) ||
                              "returned".equals(item.getStatus()) ||
                              "lent to sales".equals(item.getStatus()) ||
                              "broken".equals(item.getStatus()))
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
        
        // Update order status based on processed items
        if (!hasActiveItems && processedQuantity >= totalQuantity) {
            order.setStatus("completed");
            logger.info("Setting order {} status to 'completed' as all items are processed", orderId);
        } else {
            order.setStatus("active");
            logger.info("Setting order {} status to 'active' as some items are still active", orderId);
        }
        
        lentIdRepository.save(order);
    }

    /**
     * Get all lent orders with optional status filtering and pagination
     * 
     * @param status Status filter ("active", "completed", or "all")
     * @param pageable Pagination information
     * @return Page of lent orders
     */
    public Page<OrderSummaryDTO> getLentOrdersSummary(String status, Pageable pageable) {
        logger.info("Fetching lent orders summary with status: {} and pagination: {}", status, pageable);
        
        // Get lent orders based on status
        Page<LentId> lentPage = (status != null && !status.equals("all")) 
            ? lentIdRepository.findByStatus(status, pageable)
            : lentIdRepository.findAll(pageable);
        
        // Convert to summaries with total items
        List<OrderSummaryDTO> summaries = lentPage.getContent().stream()
            .map(lentId -> {
                OrderSummaryDTO summary = new OrderSummaryDTO();
                summary.setOrderId(lentId.getLentId());
                summary.setEmployeeId(lentId.getEmployeeId());
                summary.setShopName(lentId.getShopName());
                summary.setStatus(lentId.getStatus()); // Will show "active" or "completed"
                summary.setTimestamp(lentId.getTimestamp());
                summary.setNote(lentId.getNote());
                
                // Calculate total items from associated lent items
                List<Lend> lentItems = lendRepository.findByOrderId(lentId.getLentId());
                int totalItems = lentItems.stream()
                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 1)
                    .sum();
                summary.setTotalItems(totalItems);
                
                return summary;
            })
            .collect(Collectors.toList());
        
        return new PageImpl<>(summaries, pageable, lentPage.getTotalElements());
    }

    /**
     * Update lent record status directly using JDBC
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLentStatus(String productBarcode, String orderId, String status) {
        if (orderId == null || orderId.isEmpty()) {
            logger.warn("Cannot update lent status: orderId is null or empty");
            return;
        }
        
        try {
            Query query;
            
            if (productBarcode == null || productBarcode.isEmpty()) {
                // For non-serialized products (where product_barcode is NULL)
                logger.info("Updating lent status for non-serialized product with orderId={}, status={}", orderId, status);
                query = entityManager.createNativeQuery(
                    "UPDATE lend SET status = :status WHERE product_barcode IS NULL AND order_id = :orderId");
                query.setParameter("status", status);
                query.setParameter("orderId", orderId);
            } else {
                // For serialized products
                logger.info("Updating lent status for productBarcode={}, orderId={}, status={}", productBarcode, orderId, status);
                query = entityManager.createNativeQuery(
                    "UPDATE lend SET status = :status WHERE product_barcode = :productBarcode AND order_id = :orderId");
                query.setParameter("status", status);
                query.setParameter("productBarcode", productBarcode);
                query.setParameter("orderId", orderId);
            }
            
            int updatedRows = query.executeUpdate();
            logger.info("Updated {} lent records with status '{}'", updatedRows, status);
            
            // Force flush to ensure immediate persistence
            entityManager.flush();
        } catch (Exception e) {
            logger.error("Failed to update lent status: {}", e.getMessage(), e);
        }
    }

    /**
     * Get all lent orders with optional status filtering and pagination
     * 
     * @param status Status filter ("active", "completed", or "all")
     * @param pageable Pagination information
     * @return Page of lent orders
     */
    public Page<LentId> getLentOrders(String status, Pageable pageable) {
        logger.info("Fetching lent orders with status: {}, page: {}, size: {}", 
                   status, pageable.getPageNumber(), pageable.getPageSize());
        
        // If status is "all", return all orders
        if (status.equals("all")) {
            return lentIdRepository.findAll(pageable);
        }
        
        // Otherwise, filter by status
        return lentIdRepository.findByStatus(status, pageable);
    }

    private Invoice getOrCreateInvoice(String salesOrderId, String employeeId, String shopName) {
        // Try to find existing invoice
        List<Invoice> existingInvoices = invoiceRepository.findByInvoice(salesOrderId);
        if (!existingInvoices.isEmpty()) {
            return existingInvoices.get(0);
        }

        // Create new invoice if none exists
        Invoice invoice = new Invoice();
        invoice.setInvoice(salesOrderId);
        invoice.setEmployeeId(employeeId);
        invoice.setShopName(shopName);
        invoice.setTimestamp(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }
} 