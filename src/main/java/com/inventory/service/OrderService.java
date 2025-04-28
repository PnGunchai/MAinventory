package com.inventory.service;

import com.inventory.dto.OrderDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.ProductCatalog;
import com.inventory.model.LentId;
import com.inventory.model.BrokenId;
import com.inventory.model.Invoice;
import com.inventory.repository.ProductCatalogRepository;
import com.inventory.repository.LogsRepository;
import com.inventory.repository.LentIdRepository;
import com.inventory.repository.BrokenIdRepository;
import com.inventory.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Service for order operations
 */
@Service
public class OrderService {
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private LogsRepository logsRepository;
    
    @Autowired
    private LentIdRepository lentIdRepository;
    
    @Autowired
    private BrokenIdRepository brokenIdRepository;
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    /**
     * Process an order with mixed product types
     */
    @Transactional
    public void processOrder(OrderDTO orderDTO) {
        if (orderDTO.getProductIdentifiers() == null || orderDTO.getProductIdentifiers().isEmpty()) {
            throw new InvalidInputException("Product identifiers cannot be empty");
        }
        
        // Validate destination
        String destination = orderDTO.getDestination().toLowerCase();
        if (!destination.equals("sales") && !destination.equals("lent") && !destination.equals("broken")) {
            throw new InvalidInputException("Invalid destination: " + destination);
        }
        
        // Handle order ID based on destination
        handleOrderId(orderDTO);
        
        // Validate required fields based on destination
        validateOrderFields(orderDTO);
        
        // Create operation record based on destination
        createOperationRecord(orderDTO);
        
        // Process each product identifier
        for (String identifier : orderDTO.getProductIdentifiers()) {
            processProductIdentifier(identifier, orderDTO);
        }
    }
    
    /**
     * Handle order ID based on destination
     * - For sales and lent: require manual input
     * - For broken: auto-generate if not provided
     */
    private void handleOrderId(OrderDTO orderDTO) {
        String destination = orderDTO.getDestination().toLowerCase();
        
        if (destination.equals("sales") || destination.equals("lent")) {
            // For sales and lent, order ID must be provided by user
            if (orderDTO.getOrderId() == null || orderDTO.getOrderId().isEmpty()) {
                throw new InvalidInputException("Order ID is required for " + destination + " orders");
            }
        } else if (destination.equals("broken")) {
            // For broken items, auto-generate ID if not provided
            if (orderDTO.getOrderId() == null || orderDTO.getOrderId().isEmpty()) {
                orderDTO.setOrderId(generateBrokenItemId());
            }
        }
    }
    
    /**
     * Generate a unique ID for broken items
     */
    private String generateBrokenItemId() {
        String prefix = "DMG";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        
        return prefix + "-" + timestamp + "-" + random;
    }
    
    /**
     * Validate required fields based on destination
     */
    private void validateOrderFields(OrderDTO orderDTO) {
        String destination = orderDTO.getDestination().toLowerCase();
        
        if ((destination.equals("sales") || destination.equals("lent")) && 
            (orderDTO.getEmployeeId() == null || orderDTO.getEmployeeId().isEmpty())) {
            throw new InvalidInputException("Employee ID is required for sales and lent destinations");
        }
        
        if ((destination.equals("sales") || destination.equals("lent")) && 
            (orderDTO.getShopName() == null || orderDTO.getShopName().isEmpty())) {
            throw new InvalidInputException("Shop name is required for sales and lent destinations");
        }
        
        if (destination.equals("broken") && 
            (orderDTO.getCondition() == null || orderDTO.getCondition().isEmpty())) {
            throw new InvalidInputException("Condition is required for broken destination");
        }
    }
    
    /**
     * Process a single product identifier
     */
    private void processProductIdentifier(String identifier, OrderDTO orderDTO) {
        // Check if it's a quantity format (BOX001:5)
        if (identifier.contains(":")) {
            processNonSerializedProduct(identifier, orderDTO);
        } else {
            // It's a serialized product barcode
            processSerializedProduct(identifier, orderDTO);
        }
    }
    
    /**
     * Process a non-serialized product with quantity
     */
    private void processNonSerializedProduct(String identifier, OrderDTO orderDTO) {
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
        
        // Move the stock to the specified destination
        stockService.moveStock(
            boxBarcode,
            null, // No product barcode for non-serialized products
            quantity,
            orderDTO.getDestination(),
            orderDTO.getEmployeeId(),
            orderDTO.getShopName(),
            orderDTO.getCondition(),
            orderDTO.getNote(),
            orderDTO.getOrderId() // Pass the order ID
        );
    }
    
    /**
     * Process a serialized product
     */
    private void processSerializedProduct(String productBarcode, OrderDTO orderDTO) {
        // Find the box barcode for this product barcode
        String boxBarcode = findBoxBarcodeForProductBarcode(productBarcode);
        
        // Move the stock to the specified destination
        stockService.moveStock(
            boxBarcode,
            productBarcode,
            1, // Always quantity 1 for serialized products
            orderDTO.getDestination(),
            orderDTO.getEmployeeId(),
            orderDTO.getShopName(),
            orderDTO.getCondition(),
            orderDTO.getNote(),
            orderDTO.getOrderId() // Pass the order ID
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
    
    /**
     * Create operation record based on destination
     */
    private void createOperationRecord(OrderDTO orderDTO) {
        String destination = orderDTO.getDestination().toLowerCase();
        
        if (destination.equals("lent")) {
            // Create LentId record
            LentId lentId = new LentId();
            lentId.setLentId(orderDTO.getOrderId());
            lentId.setEmployeeId(orderDTO.getEmployeeId());
            lentId.setShopName(orderDTO.getShopName());
            lentId.setTimestamp(LocalDateTime.now());
            lentId.setNote(orderDTO.getNote());
            lentId.setStatus("active");
            
            lentIdRepository.save(lentId);
        } else if (destination.equals("broken")) {
            // Create BrokenId record
            BrokenId brokenId = new BrokenId();
            brokenId.setBrokenId(orderDTO.getOrderId());
            brokenId.setTimestamp(LocalDateTime.now());
            brokenId.setNote(orderDTO.getNote());
            
            brokenIdRepository.save(brokenId);
        }
        // For sales, we assume the Invoice table is handled elsewhere
    }
    
    /**
     * Get all sales orders with pagination
     */
    public Page<Invoice> getSalesOrders(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }
} 