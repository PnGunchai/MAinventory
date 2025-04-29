package com.inventory.service;

import com.inventory.dto.ProductIdentifierDTO;
import com.inventory.dto.BrokenOrderDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.ProductCatalog;
import com.inventory.model.BrokenId;
import com.inventory.repository.ProductCatalogRepository;
import com.inventory.repository.LogsRepository;
import com.inventory.repository.BrokenIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for broken order operations
 */
@Service
public class BrokenOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(BrokenOrderService.class);
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private LogsRepository logsRepository;
    
    @Autowired
    private BrokenIdRepository brokenIdRepository;
    
    /**
     * Process a broken order with mixed product types
     */
    @Transactional
    public void processBrokenOrder(BrokenOrderDTO orderDTO) {
        List<ProductIdentifierDTO> allProducts = orderDTO.getAllProducts();
        
        if (allProducts.isEmpty()) {
            throw new InvalidInputException("No products specified in the broken order");
        }
        
        // Validate condition is provided
        if (orderDTO.getCondition() == null || orderDTO.getCondition().isEmpty()) {
            throw new InvalidInputException("Condition is required for broken orders");
        }
        
        // Create and save BrokenId record
        String brokenOrderId = orderDTO.getOrderId();
        if (brokenOrderId != null && !brokenOrderId.isEmpty()) {
            // Check if BrokenId record already exists
            if (!brokenIdRepository.findById(brokenOrderId).isPresent()) {
                BrokenId brokenId = new BrokenId();
                brokenId.setBrokenId(brokenOrderId);
                brokenId.setTimestamp(LocalDateTime.now());
                brokenId.setNote(orderDTO.getNote());
                
                // Save the broken ID record
                brokenIdRepository.save(brokenId);
                logger.info("Created broken order ID record: {}", brokenOrderId);
            } else {
                logger.info("BrokenId record already exists for order ID: {}", brokenOrderId);
            }
        }
        
        // Process each product identifier
        for (ProductIdentifierDTO product : allProducts) {
            if (product.getIdentifier() == null || product.getIdentifier().isEmpty()) {
                throw new InvalidInputException("Product identifier cannot be empty");
            }
            
            processProductIdentifier(product, orderDTO);
        }
    }
    
    /**
     * Process a single product identifier
     */
    private void processProductIdentifier(ProductIdentifierDTO product, BrokenOrderDTO orderDTO) {
        String identifier = product.getIdentifier();
        Boolean productSplitPair = product.getSplitPair();
        
        logger.info("Processing product for broken: {} with splitPair={}", identifier, productSplitPair);
        
        // Check if it's a quantity format (BOX001:5)
        if (identifier.contains(":")) {
            processNonSerializedProduct(identifier, orderDTO);
        } else {
            // It's a serialized product barcode
            processSerializedProduct(identifier, orderDTO, productSplitPair);
        }
    }
    
    /**
     * Process a non-serialized product with quantity
     */
    private void processNonSerializedProduct(String identifier, BrokenOrderDTO orderDTO) {
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
        
        logger.info("Processing non-serialized product: {} with quantity: {}", boxBarcode, quantity);
        
        // Move the stock to broken
        stockService.moveStock(
            boxBarcode,
            null, // No product barcode for non-serialized products
            quantity,
            "broken",
            orderDTO.getEmployeeId(),
            null, // No shop name for broken items
            orderDTO.getCondition(), // Condition is required for broken items
            orderDTO.getNote(),
            orderDTO.getOrderId() // Pass the order ID (optional for broken)
        );
    }
    
    /**
     * Process a serialized product
     */
    private void processSerializedProduct(String productBarcode, BrokenOrderDTO orderDTO, Boolean productSplitPair) {
        // Find the box barcode for this product barcode
        String boxBarcode = findBoxBarcodeForProductBarcode(productBarcode);
        
        // Always use splitPair=true for broken items, regardless of what was passed in the request
        // This ensures we only mark the specific barcode as broken without affecting its pair
        boolean splitPair = true;
        
        logger.info("Moving product barcode {} to broken with forced splitPair=true", productBarcode);
        
        // Move the stock to broken
        stockService.moveStock(
            boxBarcode,
            productBarcode,
            1, // Always quantity 1 for serialized products
            "broken",
            orderDTO.getEmployeeId(),
            null, // No shop name for broken items
            orderDTO.getCondition(), // Condition is required for broken items
            orderDTO.getNote(),
            orderDTO.getOrderId(), // Pass the order ID (optional for broken)
            splitPair // Always true to ensure only the specified barcode is marked as broken
        );
    }
    
    /**
     * Find the box barcode for a product barcode
     */
    private String findBoxBarcodeForProductBarcode(String productBarcode) {
        // Query the logs to find the most recent entry for this product barcode
        return logsRepository.findBoxBarcodeByProductBarcode(productBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product barcode not found: " + productBarcode));
    }
} 