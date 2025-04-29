package com.inventory.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO for updating the status of lent items
 */
@Data
public class LentItemUpdateDTO {
    /**
     * The original order ID of the lent order
     */
    private String orderId;
    
    /**
     * The employee who is handling this update
     */
    private String employeeId;
    
    /**
     * Optional note about this update
     */
    private String note;
    
    /**
     * List of items to update with their new status
     */
    private List<LentItemStatusDTO> items;
} 