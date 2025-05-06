package com.inventory.service;

import com.inventory.model.Broken;
import com.inventory.model.BrokenId;
import com.inventory.repository.BrokenRepository;
import com.inventory.repository.BrokenIdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.ZoneId;

@Service
public class BrokenService {

    private static final Logger logger = LoggerFactory.getLogger(BrokenService.class);

    @Autowired
    private BrokenRepository brokenRepository;
    
    @Autowired
    private BrokenIdRepository brokenIdRepository;
    
    /**
     * Create a broken record with quantity
     * @param quantity For serialized products, should always be 1. For non-serialized products, can be any positive number.
     */
    @Transactional
    public Broken createBrokenRecord(String boxBarcode, String productName, String productBarcode, 
                                   String condition, String note, Integer boxNumber, Integer quantity, String orderId) {
        
        // Create and save BrokenId record if provided
        if (orderId != null && !orderId.isEmpty()) {
            try {
                // Check if the BrokenId record already exists
                if (!brokenIdRepository.findById(orderId).isPresent()) {
                    BrokenId brokenId = new BrokenId();
                    brokenId.setBrokenId(orderId);
                    brokenId.setTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Bangkok")));
                    brokenId.setNote(note);
                    
                    brokenIdRepository.save(brokenId);
                    logger.info("Created broken ID record: {}", orderId);
                } else {
                    logger.info("BrokenId record already exists for order ID: {}", orderId);
                }
            } catch (Exception e) {
                logger.warn("Failed to create BrokenId record: {}", e.getMessage());
                // Continue processing even if BrokenId creation fails
            }
        }
        
        Broken broken = new Broken();
        broken.setBoxBarcode(boxBarcode);
        broken.setProductName(productName);
        broken.setProductBarcode(productBarcode);
        broken.setCondition(condition);
        broken.setTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Bangkok")));
        broken.setBoxNumber(boxNumber);
        broken.setNote(note);
        broken.setQuantity(quantity);
        broken.setOrderId(orderId);
        
        return brokenRepository.save(broken);
    }

    /**
     * Create a broken record for serialized product with boxNumber
     */
    @Transactional
    public Broken createBrokenRecord(String boxBarcode, String productName, String productBarcode, 
                                   String condition, String note, Integer boxNumber) {
        // For serialized products, quantity is always 1
        // Use generated order ID for backward compatibility
        return createBrokenRecord(boxBarcode, productName, productBarcode, condition, note, 
                                boxNumber, 1, generateDefaultOrderId());
    }

    /**
     * Create a broken record for serialized product with boxNumber and orderId
     */
    @Transactional
    public Broken createBrokenRecord(String boxBarcode, String productName, String productBarcode, 
                                   String condition, String note, Integer boxNumber, Integer quantity) {
        // Pass the generated order ID for backward compatibility
        return createBrokenRecord(boxBarcode, productName, productBarcode, condition, note, 
                                boxNumber, quantity, generateDefaultOrderId());
    }

    /**
     * Create a broken record without boxNumber (legacy support)
     */
    @Transactional
    public Broken createBrokenRecord(String boxBarcode, String productName, String productBarcode, 
                                   String condition, String note) {
        // For serialized products, quantity is always 1
        return createBrokenRecord(boxBarcode, productName, productBarcode, condition, note, 
                                null, 1, generateDefaultOrderId());
    }
    
    /**
     * Create a broken record for non-serialized product with specified quantity
     */
    @Transactional
    public Broken createNonSerializedBrokenRecord(String boxBarcode, String productName, 
                                               String condition, String note, int quantity, String orderId) {
        return createBrokenRecord(boxBarcode, productName, null, condition, note, 
                                null, quantity, orderId);
    }

    /**
     * Generate a default order ID for broken items if none provided
     */
    private String generateDefaultOrderId() {
        return "BROKEN-" + System.currentTimeMillis();
    }

    /**
     * Save a broken record
     */
    @Transactional
    public Broken save(Broken broken) {
        return brokenRepository.save(broken);
    }
} 