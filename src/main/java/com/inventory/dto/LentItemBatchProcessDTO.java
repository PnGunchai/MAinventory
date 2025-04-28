package com.inventory.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * DTO for batch processing of lent items with different destinations
 */
@Data
public class LentItemBatchProcessDTO {
    
    /**
     * Employee ID of the person processing the items
     */
    private String employeeId;
    
    /**
     * Shop name (required for sales)
     */
    private String shopName;
    
    /**
     * Optional note for the operation
     */
    private String note;
    
    /**
     * Map of product identifiers to their split destinations
     * Key: Product identifier (boxBarcode for non-serialized, productBarcode for serialized)
     * Value: Map of destination ("return" or "sales") to quantity
     * Example: {"BOX001": {"return": 2, "sales": 3}}
     */
    private Map<String, Map<String, Integer>> splitDestinations;
    
    /**
     * List of product barcodes to return to stock (for backward compatibility)
     */
    private List<String> returnToStock;
    
    /**
     * List of product barcodes to move to sales (for backward compatibility)
     */
    private List<String> moveToSales;
    
    /**
     * Custom sales order ID for items moved to sales
     * If not provided, defaults to "SALES-FROM-LENT-{orderId}"
     */
    private String salesOrderId;
    
    /**
     * List of product barcodes to keep as lent
     * (optional, any items not in other lists are assumed to remain lent)
     */
    private List<String> keepAsLent;
    
    /**
     * Optional condition for items being marked as broken
     */
    private String condition;
    
    /**
     * List of product barcodes to mark as broken
     */
    private List<String> markAsBroken;
    
    /**
     * Whether to split pairs when processing items
     * If true, only the specified barcodes will be processed
     * If false, paired items will be processed together
     */
    private boolean splitPairs = false;
    
    /**
     * Whether this is a direct sales transaction
     * For lent items being moved to sales, this should be false
     */
    private Boolean isDirectSales = false;
} 