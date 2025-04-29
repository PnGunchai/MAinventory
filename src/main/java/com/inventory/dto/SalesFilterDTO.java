package com.inventory.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO for filtering sales records
 */
@Data
public class SalesFilterDTO {
    private String orderId;
    private String employeeId;
    private String shopName;
    private String boxBarcode;
    private String productBarcode;
    private String productName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Pagination parameters
    private int page = 0;
    private int size = 20;
    private String sortBy = "timestamp";
    private String sortDirection = "desc";
} 