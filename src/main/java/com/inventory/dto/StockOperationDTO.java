package com.inventory.dto;

import lombok.Data;

/**
 * DTO for stock operations (add, remove, sales, lent, returned, broken)
 */
@Data
public class StockOperationDTO {
    private String boxBarcode;
    private String productName;
    private String productBarcode;
    private Integer quantity;
    private String operation;
    private String employeeId;
    private String shopName;
    private String condition;
    private String invoiceNumber;
} 