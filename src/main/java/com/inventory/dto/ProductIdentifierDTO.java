package com.inventory.dto;

import lombok.Data;

/**
 * DTO for a single product in a sales order
 * Can represent either a serialized product barcode or a non-serialized product with quantity (BOX001:5)
 */
@Data
public class ProductIdentifierDTO {
    private String identifier;  // The product barcode or box:quantity format
    private Boolean splitPair;  // Whether to split a pair for this specific product (only applicable for SN=2 products)
    private String destination; // For lent items: "lent" (default), "return", or "sales"
    
    // Default constructor
    public ProductIdentifierDTO() {
    }
    
    // Constructor with identifier only
    public ProductIdentifierDTO(String identifier) {
        this.identifier = identifier;
        this.splitPair = true; // Changed to true
        this.destination = null; // Default to null (will be treated as "lent" in lent order processing)
    }
    
    // Constructor with identifier and splitPair
    public ProductIdentifierDTO(String identifier, Boolean splitPair) {
        this.identifier = identifier;
        this.splitPair = true; // Force true regardless of input
        this.destination = null; // Default to null (will be treated as "lent" in lent order processing)
    }
    
    // Full constructor with all fields
    public ProductIdentifierDTO(String identifier, Boolean splitPair, String destination) {
        this.identifier = identifier;
        this.splitPair = true; // Force true regardless of input
        this.destination = destination;
    }

    // Override the getSplitPair method to always return true
    public Boolean getSplitPair() {
        return true;
    }
} 