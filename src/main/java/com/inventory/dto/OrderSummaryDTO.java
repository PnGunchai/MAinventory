package com.inventory.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO for order summary information
 */
@Data
public class OrderSummaryDTO {
    private String orderId;
    private String employeeId;
    private String shopName;
    private LocalDateTime timestamp;
    private String status;
    private Integer totalItems;
    private Integer processedItems;
    private String note;
} 