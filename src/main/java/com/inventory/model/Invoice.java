package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity class for invoice table
 * Records invoice information for sales
 */
@Data
@Entity
@Table(name = "invoice")
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;
    
    @Column(name = "invoice", nullable = false)
    private String invoice;
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Column(name = "shop_name", nullable = false)
    private String shopName;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "edit_count")
    private Integer editCount = 0;

    @Column(name = "edit_history", columnDefinition = "TEXT")
    private String editHistory;

    @Column(name = "note")
    private String note;

    /**
     * Add an edit record to the history
     * @param operation The type of edit (ADD_ITEMS, REMOVE_ITEM, UPDATE_NOTES)
     * @param details Description of what was changed
     */
    public void addEditHistory(String operation, String details) {
        LocalDateTime now = LocalDateTime.now();
        String newEdit = String.format("[%s] %s: %s", now, operation, details);
        
        if (editHistory == null || editHistory.isEmpty()) {
            editHistory = newEdit;
        } else {
            editHistory = newEdit + "\n" + editHistory;  // Prepend new edit
        }
        
        this.lastModified = now;
        this.editCount = (this.editCount != null ? this.editCount : 0) + 1;
    }
} 