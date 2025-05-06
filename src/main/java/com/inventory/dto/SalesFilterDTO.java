package com.inventory.dto;

import lombok.Data;
import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Bangkok")
    private ZonedDateTime startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Bangkok")
    private ZonedDateTime endDate;
    
    // Pagination parameters
    private int page = 0;
    private int size = 20;
    private String sortBy = "timestamp";
    private String sortDirection = "desc";
} 