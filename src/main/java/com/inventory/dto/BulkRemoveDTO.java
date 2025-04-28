package com.inventory.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO for bulk remove operations
 */
@Data
public class BulkRemoveDTO {
    private String boxBarcode;
    private List<String> productBarcodes;
    private Integer quantity;
    private String note;
} 