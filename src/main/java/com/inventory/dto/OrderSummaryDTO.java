package com.inventory.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * DTO for order summary information
 */
@Data
public class OrderSummaryDTO {
    private String orderId;
    private String employeeId;
    private String shopName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Bangkok")
    private OffsetDateTime timestamp;
    private String status;
    private Integer totalItems;
    private Integer processedItems;
    private String note;
} 