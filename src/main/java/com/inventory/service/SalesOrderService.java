package com.inventory.service;

import com.inventory.dto.ProductIdentifierDTO;
import com.inventory.dto.SalesOrderDTO;
import com.inventory.dto.OrderItemDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.ProductCatalog;
import com.inventory.model.Sales;
import com.inventory.model.Invoice;
import com.inventory.repository.ProductCatalogRepository;
import com.inventory.repository.LogsRepository;
import com.inventory.repository.InvoiceRepository;
import com.inventory.repository.SalesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for sales order operations
 */
@Service
public class SalesOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(SalesOrderService.class);
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private LogsRepository logsRepository;

    @Autowired
    private InStockService inStockService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SalesRepository salesRepository;
    
    /**
     * Process a sales order with mixed product types
     */
    @Transactional
    public void processSalesOrder(SalesOrderDTO orderDTO) {
        List<ProductIdentifierDTO> allProducts = orderDTO.getAllProducts();
        
        if (allProducts.isEmpty()) {
            throw new InvalidInputException("No products specified in the order");
        }
        
        // Validate shopName is provided
        if (orderDTO.getShopName() == null || orderDTO.getShopName().isEmpty()) {
            throw new InvalidInputException("Shop name is required for sales orders");
        }
        
        // Validate orderId is provided
        if (orderDTO.getOrderId() == null || orderDTO.getOrderId().isEmpty()) {
            throw new InvalidInputException("Order ID is required for sales orders");
        }

        // Check for duplicate order ID
        if (!invoiceRepository.findByInvoice(orderDTO.getOrderId()).isEmpty()) {
            throw new InvalidInputException("Sales order ID already exists: " + orderDTO.getOrderId());
        }

        // --- NEW: Create and save Invoice with note ---
        Invoice invoice = new Invoice();
        invoice.setInvoice(orderDTO.getOrderId());
        invoice.setEmployeeId(orderDTO.getEmployeeId());
        invoice.setShopName(orderDTO.getShopName());
        invoice.setTimestamp(java.time.LocalDateTime.now());
        invoice.setLastModified(java.time.LocalDateTime.now());
        invoice.setNote(orderDTO.getNote());
        invoice.setEditCount(0);
        invoice.setEditHistory(null);
        invoiceRepository.save(invoice);
        // --- END NEW ---
        
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
    private void processProductIdentifier(ProductIdentifierDTO product, SalesOrderDTO orderDTO) {
        String identifier = product.getIdentifier();
        
        logger.info("Processing product: {}", identifier);
        
        // Check if it's a quantity format (BOX001:5)
        if (identifier.contains(":")) {
            processNonSerializedProduct(identifier, orderDTO);
        } else {
            // It's a serialized product barcode
            processSerializedProduct(identifier, orderDTO, true); // Always true to handle items individually
        }
    }
    
    /**
     * Process a non-serialized product with quantity
     */
    private void processNonSerializedProduct(String identifier, SalesOrderDTO orderDTO) {
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
        ProductCatalog productCatalog = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));
        
        if (productCatalog.getNumberSn() != 0) {
            throw new InvalidInputException("Quantity format only valid for non-serialized products: " + boxBarcode);
        }
        
        logger.info("Processing non-serialized product: {} with quantity: {}", boxBarcode, quantity);
        
        // Move the stock to sales
        stockService.moveStock(
            boxBarcode,
            null, // No product barcode for non-serialized products
            quantity,
            "sales",
            orderDTO.getEmployeeId(),
            orderDTO.getShopName(),
            null, // No condition for sales
            orderDTO.getNote(),
            orderDTO.getOrderId(), // Pass the order ID
            false, // splitPair is false for non-serialized products
            orderDTO.getIsDirectSales() // Pass the isDirectSales flag
        );
    }
    
    /**
     * Process a serialized product
     */
    private void processSerializedProduct(String productBarcode, SalesOrderDTO orderDTO, Boolean productSplitPair) {
        // Find the box barcode for this product barcode
        String boxBarcode = findBoxBarcodeForProductBarcode(productBarcode);
        
        // Get product from catalog
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));

        logger.info("Moving product barcode {} to sales", productBarcode);
        
        // Move the stock to sales
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
            true, // Always true to handle items individually
            orderDTO.getIsDirectSales() // Pass the isDirectSales flag
        );
    }
    
    /**
     * Legacy method to maintain backward compatibility
     */
    private void processSerializedProduct(String productBarcode, SalesOrderDTO orderDTO) {
        processSerializedProduct(productBarcode, orderDTO, orderDTO.getSplitPair());
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
     * Process products specifically for order editing
     */
    private void processProductForOrderEdit(ProductIdentifierDTO product, String orderId, String employeeId, String shopName, String note) {
        // Create a temporary SalesOrderDTO with edit-specific settings
        SalesOrderDTO editOrderDTO = new SalesOrderDTO();
        editOrderDTO.setOrderId(orderId);
        editOrderDTO.setEmployeeId(employeeId);
        editOrderDTO.setShopName(shopName);
        editOrderDTO.setNote(note);
        editOrderDTO.setSplitPair(true); // Always true to handle items individually
        editOrderDTO.setIsDirectSales(false); // Not direct sales for edits
        
        // Reuse existing product processing logic
        processProductIdentifier(product, editOrderDTO);
    }

    /**
     * Add items to an existing sales order
     */
    @Transactional
    public void addItemsToOrder(String orderId, OrderItemDTO itemDTO) {
        // Validate order exists and get its details
        Invoice invoice = invoiceRepository.findByInvoice(orderId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found: " + orderId));

        // Track the edit in invoice
        StringBuilder editDetails = new StringBuilder("Added items: ");
        for (ProductIdentifierDTO product : itemDTO.getProducts()) {
            editDetails.append(product.getIdentifier()).append(", ");
        }
        if (itemDTO.getNote() != null && !itemDTO.getNote().isEmpty()) {
            editDetails.append("Note: ").append(itemDTO.getNote());
        }
        invoice.addEditHistory("ADD_ITEMS", editDetails.toString().replaceAll(", $", ""));
        invoiceRepository.save(invoice);

        // Process each product using the dedicated edit method
        for (ProductIdentifierDTO product : itemDTO.getProducts()) {
            processProductForOrderEdit(
                product,
                orderId,
                invoice.getEmployeeId(),
                invoice.getShopName(),
                itemDTO.getNote()
            );
        }
    }

    /**
     * Remove an item from an existing sales order
     */
    @Transactional
    public void removeItemFromOrder(String orderId, String identifier) {
        // Validate order exists and get its details
        Invoice invoice = invoiceRepository.findByInvoice(orderId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found: " + orderId));

        // Find the sales record for this product
        Sales salesRecord;
        
        // Check if it's a non-serial product first (contains ":")
        if (identifier.contains(":")) {
            String boxBarcode = identifier.split(":")[0];
            salesRecord = salesRepository.findByOrderIdAndBoxBarcode(orderId, boxBarcode)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Box barcode %s not found in order %s", boxBarcode, orderId)));
        } else {
            // For serial products, use product barcode
            salesRecord = salesRepository.findByOrderIdAndProductBarcode(orderId, identifier)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Product barcode %s not found in order %s", identifier, orderId)));
        }

        // Track the edit in invoice
        invoice.addEditHistory("REMOVE_ITEM", 
            String.format("Removed item: %s (Box: %s%s)", 
                salesRecord.getProductName(), 
                salesRecord.getBoxBarcode(),
                salesRecord.getProductBarcode() != null ? 
                    ", Product: " + salesRecord.getProductBarcode() : 
                    ", Quantity: " + salesRecord.getQuantity()));
        invoiceRepository.save(invoice);

        // Move the item back to stock
        stockService.returnSalesItemToStock(
            salesRecord.getBoxBarcode(),
            salesRecord.getProductBarcode(),
            salesRecord.getQuantity() != null ? salesRecord.getQuantity() : 1,
            "Removed from sales order " + orderId
        );

        // Delete the sales record
        salesRepository.delete(salesRecord);
    }

    /**
     * Update notes for an existing sales order
     */
    @Transactional
    public void updateOrderNotes(String orderId, String note) {
        // Validate order exists
        Invoice invoice = invoiceRepository.findByInvoice(orderId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found: " + orderId));

        // Track the edit in invoice
        invoice.addEditHistory("UPDATE_NOTES", "Updated order notes: " + note);
        // Append the new note to the existing note, prepending a timestamp
        String oldNote = invoice.getNote();
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String noteWithTimestamp = "[" + timestamp + "] " + note;
        String newNote = (oldNote != null && !oldNote.isEmpty()) ? oldNote + "\n" + noteWithTimestamp : noteWithTimestamp;
        invoice.setNote(newNote);
        invoiceRepository.save(invoice);

        // Update notes for all sales records in this order
        List<Sales> salesRecords = salesRepository.findByOrderId(orderId);
        for (Sales record : salesRecords) {
            record.setNote(note);
            salesRepository.save(record);
        }
    }
} 