package com.inventory.dto;

import lombok.Data;

/**
 * DTO for a single lent item status update
 */
@Data
public class LentItemStatusDTO {
    /**
     * Product barcode of the lent item
     */
    private String productBarcode;
    
    /**
     * Box barcode of the lent item (required for updating status)
     */
    private String boxBarcode;
    
    /**
     * New status for the item: "returned" or "sold"
     */
    private String status;
    
    /**
     * Sales order ID, required when status is "sold"
     * If not provided, a default one will be generated based on the original order ID
     */
    private String salesOrderId;
}
