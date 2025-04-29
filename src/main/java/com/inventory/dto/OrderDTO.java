package com.inventory.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO for order operations (sales, lent, broken)
 */
@Data
public class OrderDTO {
    private String orderId;
    private String destination; // "sales", "lent", or "broken"
    private String employeeId;
    private String shopName;
    private String condition; // For broken items
    private String note;
    private List<String> productIdentifiers; // Can be product barcodes or box barcodes with quantity
} 