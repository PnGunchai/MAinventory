package com.inventory.service;

import com.inventory.dto.LentItemStatusDTO;
import com.inventory.dto.LentItemUpdateDTO;
import com.inventory.exception.InvalidInputException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Lend;
import com.inventory.model.Sales;
import com.inventory.repository.LendRepository;
import com.inventory.repository.SalesRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for updating lent item statuses
 */
@Service
public class LentItemUpdateService {
    
    private static final Logger logger = LoggerFactory.getLogger(LentItemUpdateService.class);
    
    @Autowired
    private LendRepository lendRepository;
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private SalesRepository salesService;
    
    /**
     * Get all lent items for a specific order
     */
    public List<Lend> getLentItemsByOrderId(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            throw new InvalidInputException("Order ID is required");
        }
        
        List<Lend> lentItems = lendRepository.findByOrderId(orderId);
        
        if (lentItems.isEmpty()) {
            throw new ResourceNotFoundException("No lent items found for order ID: " + orderId);
        }
        
        return lentItems;
    }
    
    /**
     * Update the status of multiple lent items
     */
    @Transactional
    public List<Lend> updateLentItemsStatus(LentItemUpdateDTO updateDTO) {
        if (updateDTO.getOrderId() == null || updateDTO.getOrderId().isEmpty()) {
            throw new InvalidInputException("Order ID is required");
        }
        
        if (updateDTO.getEmployeeId() == null || updateDTO.getEmployeeId().isEmpty()) {
            throw new InvalidInputException("Employee ID is required");
        }
        
        if (updateDTO.getItems() == null || updateDTO.getItems().isEmpty()) {
            throw new InvalidInputException("No items specified for update");
        }
        
        List<Lend> updatedItems = new ArrayList<>();
        
        for (LentItemStatusDTO itemDTO : updateDTO.getItems()) {
            if (itemDTO.getProductBarcode() == null || itemDTO.getProductBarcode().isEmpty()) {
                throw new InvalidInputException("Product barcode is required for each item");
            }
            
            if (itemDTO.getBoxBarcode() == null || itemDTO.getBoxBarcode().isEmpty()) {
                throw new InvalidInputException("Box barcode is required for each item");
            }
            
            if (itemDTO.getStatus() == null || itemDTO.getStatus().isEmpty()) {
                throw new InvalidInputException("Status is required for each item");
            }
            
            String status = itemDTO.getStatus().toLowerCase();
            
            if (!status.equals("returned") && !status.equals("sold")) {
                throw new InvalidInputException("Status must be either 'returned' or 'sold'");
            }
            
            // Process the status update
            Lend updatedItem = updateLentItemStatus(itemDTO, updateDTO);
            updatedItems.add(updatedItem);
        }
        
        return updatedItems;
    }
    
    /**
     * Update a single lent item's status
     */
    private Lend updateLentItemStatus(LentItemStatusDTO itemDTO, LentItemUpdateDTO updateDTO) {
        String productBarcode = itemDTO.getProductBarcode();
        String boxBarcode = itemDTO.getBoxBarcode();
        String status = itemDTO.getStatus().toLowerCase();
        
        // Find the lent item
        Lend lentItem = lendRepository.findByBoxBarcodeAndProductBarcode(boxBarcode, productBarcode)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Lent item not found with box barcode: " + boxBarcode + 
                    " and product barcode: " + productBarcode));
        
        // Verify the item belongs to the specified order
        if (!lentItem.getOrderId().equals(updateDTO.getOrderId())) {
            throw new InvalidInputException(
                "The specified item does not belong to order ID: " + updateDTO.getOrderId());
        }
        
        // Verify the item is not already processed
        if (lentItem.getStatus() != null && 
            (lentItem.getStatus().equals("returned") || lentItem.getStatus().equals("sold"))) {
            throw new InvalidInputException(
                "Lent item already processed with status: " + lentItem.getStatus());
        }
        
        String note = updateDTO.getNote() != null ? updateDTO.getNote() : "Status updated";
        
        if (status.equals("returned")) {
            // Return the item to stock
            stockService.returnLentItem(
                boxBarcode, 
                productBarcode, 
                note, 
                lentItem.getOrderId(), // Use orderId as lentId since returnLentItem expects a String
                1  // Quantity is always 1 for serialized products in this context
            );
            logger.info("Item returned to stock: {}", productBarcode);
        } else if (status.equals("sold")) {
            // Get the sales order ID from the request or generate one
            String salesOrderId = (itemDTO.getSalesOrderId() != null && !itemDTO.getSalesOrderId().isEmpty()) 
                ? itemDTO.getSalesOrderId() 
                : updateDTO.getOrderId() + "-SOLD";
            
            // Mark as sold - move from lent to sales
            stockService.moveStock(
                boxBarcode,
                productBarcode,
                1, // Always quantity 1 for serialized products
                "sales",
                updateDTO.getEmployeeId(),
                lentItem.getShopName(), // Use the original shop name
                null, // No condition for sales
                note + " (Sold from lent order: " + updateDTO.getOrderId() + ")",
                salesOrderId, // Use provided sales order ID or generated one
                false // Don't split pair for sales from lent
            );
            
            // Create sales record with isdirectsales set to false
            Sales sale = new Sales();
            sale.setBoxBarcode(boxBarcode);
            sale.setProductBarcode(productBarcode);
            sale.setEmployeeId(updateDTO.getEmployeeId());
            sale.setShopName(lentItem.getShopName());
            sale.setOrderId(salesOrderId);
            sale.setNote(note + " (Sold from lent order: " + updateDTO.getOrderId() + ")");
            sale.setIsDirectSales(false);
            // Extract numeric part from sales order ID
            Long invoiceId = Long.parseLong(salesOrderId.replaceAll("[^0-9]", ""));
            sale.setInvoiceId(invoiceId);
            salesService.save(sale);
            
            // Update the lent record status to preserve history
            lentItem.setStatus("sold");
            lentItem.setNote(lentItem.getNote() != null 
                ? lentItem.getNote() + " [Sold with order: " + salesOrderId + "]"
                : "Sold with order: " + salesOrderId);
            lendRepository.save(lentItem);
            logger.info("Item moved from lent to sales with order ID {}: {}", salesOrderId, productBarcode);
        }
        
        // Fetch and return the updated lent item
        return lendRepository.findById(lentItem.getLentId()).orElse(lentItem);
    }
} 