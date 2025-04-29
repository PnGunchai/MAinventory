package com.inventory.dto;

import lombok.Data;

/**
 * DTO for filtering current stock
 */
@Data
public class CurrentStockFilterDTO {
    private String boxBarcode;
    private String productName;
    private Integer boxNumber;
    private Integer minQuantity;
    private Integer maxQuantity;
    
    // Pagination parameters
    private int page = 0;
    private int size = 20;
    private String sortBy = "lastUpdated";
    private String sortDirection = "desc";
} 