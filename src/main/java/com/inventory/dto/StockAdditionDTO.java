package com.inventory.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO for bulk stock addition operations
 */
@Data
public class StockAdditionDTO {
    private String boxBarcode;
    private List<String> productBarcodes;
    private Integer quantity;
    private String note;
} 