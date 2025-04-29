package com.inventory.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for lent order operations
 */
@Data
public class LentOrderDTO {
    private String orderId;
    private String employeeId;
    private String shopName;
    private String note;
    
    // Original field - maintained for backward compatibility
    private List<String> productIdentifiers;
    
    // New field for product-specific splitPair settings
    private List<ProductIdentifierDTO> products;
    
    // Global splitPair setting - used as default if product-specific setting is not provided
    private Boolean splitPair;
    
    /**
     * Get the combined list of products, merging both productIdentifiers and products fields
     * This ensures backward compatibility while supporting the new product-specific splitPair feature
     */
    public List<ProductIdentifierDTO> getAllProducts() {
        List<ProductIdentifierDTO> allProducts = new ArrayList<>();
        
        // Add products from the new field if available
        if (products != null && !products.isEmpty()) {
            allProducts.addAll(products);
        }
        
        // Add products from the old field if available, converting them to ProductIdentifierDTO objects
        if (productIdentifiers != null && !productIdentifiers.isEmpty()) {
            for (String identifier : productIdentifiers) {
                allProducts.add(new ProductIdentifierDTO(identifier, splitPair));
            }
        }
        
        return allProducts;
    }
} 