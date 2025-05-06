package com.inventory.service;

import com.inventory.model.Lend;
import com.inventory.repository.LendRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LendService {

    private static final Logger logger = LoggerFactory.getLogger(LendService.class);

    @Autowired
    private LendRepository lendRepository;
    
    /**
     * Create a lend record with quantity
     * @param quantity For serialized products, should always be 1. For non-serialized products, can be any positive number.
     */
    @Transactional
    public Lend createLendRecord(String boxBarcode, String productName, String productBarcode, 
                               String employeeId, String shopName, String note, Integer boxNumber, 
                               Integer quantity, String orderId) {
        
        Lend lend = new Lend();
        lend.setBoxBarcode(boxBarcode);
        lend.setProductName(productName);
        lend.setProductBarcode(productBarcode);
        lend.setEmployeeId(employeeId);
        lend.setShopName(shopName);
        lend.setTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Bangkok")));
        lend.setBoxNumber(boxNumber);
        lend.setNote(note);
        lend.setQuantity(quantity);
        lend.setOrderId(orderId);
        lend.setStatus("lent"); // Default status for new lent items
        
        logger.info("Creating lend record with orderId: {}", orderId);
        return lendRepository.save(lend);
    }
    
    /**
     * Create a lend record for serialized product with boxNumber and orderId
     */
    @Transactional
    public Lend createLendRecord(String boxBarcode, String productName, String productBarcode, 
                               String employeeId, String shopName, String note, Integer boxNumber, String orderId) {
        // For serialized products, quantity is always 1
        return createLendRecord(boxBarcode, productName, productBarcode, employeeId, shopName, 
                              note, boxNumber, 1, orderId);
    }

    /**
     * Create a lend record for serialized product with boxNumber (legacy support)
     */
    @Transactional
    public Lend createLendRecord(String boxBarcode, String productName, String productBarcode, 
                               String employeeId, String shopName, String note, Integer boxNumber) {
        // For serialized products, quantity is always 1
        // Use a generated order ID as fallback
        return createLendRecord(boxBarcode, productName, productBarcode, employeeId, shopName, 
                              note, boxNumber, 1, generateDefaultOrderId());
    }

    /**
     * Create a lend record without boxNumber (legacy support)
     */
    @Transactional
    public Lend createLendRecord(String boxBarcode, String productName, String productBarcode, 
                               String employeeId, String shopName, String note) {
        // For serialized products, quantity is always 1
        // Use a generated order ID as fallback
        return createLendRecord(boxBarcode, productName, productBarcode, employeeId, shopName, 
                              note, null, 1, generateDefaultOrderId());
    }
    
    /**
     * Create a lend record for non-serialized product with specified quantity
     */
    @Transactional
    public Lend createNonSerializedLendRecord(String boxBarcode, String productName, 
                                           String employeeId, String shopName, String note, 
                                           int quantity, String orderId) {
        return createLendRecord(boxBarcode, productName, null, employeeId, shopName, 
                              note, null, quantity, orderId);
    }

    /**
     * Generate a default order ID for lent items if none provided
     */
    private String generateDefaultOrderId() {
        return "LENT-" + System.currentTimeMillis();
    }

    /**
     * Find lent items by box barcode and product name
     */
    public List<Lend> findByBoxBarcodeAndProductName(String boxBarcode, String productName) {
        return lendRepository.findByBoxBarcodeAndProductName(boxBarcode, productName);
    }

    /**
     * Delete a lent item
     */
    @Transactional
    public void delete(Lend lend) {
        lendRepository.delete(lend);
    }

    /**
     * Save a lend record
     */
    @Transactional
    public Lend save(Lend lend) {
        return lendRepository.save(lend);
    }
} 