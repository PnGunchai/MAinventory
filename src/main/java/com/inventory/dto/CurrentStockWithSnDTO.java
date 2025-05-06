package com.inventory.dto;

import lombok.Data;
import java.time.ZonedDateTime;

/**
 * DTO for current stock with SN type
 */
@Data
public class CurrentStockWithSnDTO {
    private Long stockId;
    private String boxBarcode;
    private String productName;
    private Integer totalQuantity;
    private ZonedDateTime lastUpdated;
    private Integer boxNumber;
    private Integer numberSn;
} 