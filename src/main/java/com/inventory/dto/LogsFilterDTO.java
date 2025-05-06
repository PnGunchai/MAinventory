package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * DTO for filtering logs
 */
@Data
@Schema(description = "Filter criteria for logs")
public class LogsFilterDTO {
    @Schema(description = "Box barcode to filter by")
    private String boxBarcode;

    @Schema(description = "Product barcode to filter by")
    private String productBarcode;

    @Schema(description = "Product name to filter by")
    private String productName;

    @Schema(description = "Operation type to filter by")
    private String operation;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Bangkok")
    @Schema(description = "Start date for date range filter (ISO format)")
    private ZonedDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Bangkok")
    @Schema(description = "End date for date range filter (ISO format)")
    private ZonedDateTime endDate;

    @Schema(description = "Box number to filter by")
    private Integer boxNumber;

    @Schema(description = "Order ID to filter by")
    private String orderId;
    
    // Pagination parameters
    @Schema(description = "Page number (0-based)", example = "0")
    @Min(value = 0, message = "Page number cannot be negative")
    private int page = 0;

    @Schema(description = "Page size", example = "20")
    @Min(value = 1, message = "Page size must be greater than 0")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private int size = 20;

    @Schema(description = "Field to sort by", example = "timestamp")
    @Pattern(regexp = "^(timestamp|boxBarcode|productBarcode|productName|operation|boxNumber|orderId)$", 
            message = "Invalid sort field")
    private String sortBy = "timestamp";

    @Schema(description = "Sort direction", example = "desc")
    @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
    private String sortDirection = "desc";

    @AssertTrue(message = "End date must be after start date")
    private boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
} 