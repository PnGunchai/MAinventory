package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity class for lent_id table
 * Records basic information about each lending operation
 * Similar to invoice table for sales operations
 */
@Data
@Entity
@Table(name = "lent_id")
public class LentId {
    
    @Id
    @Column(name = "lent_id")
    private String lentId;
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Column(name = "shop_name", nullable = false)
    private String shopName;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "note")
    private String note;
    
    @Column(name = "status", nullable = false)
    private String status; // "active" or "completed"
    
    public void setLentId(String lentId) {
        this.lentId = lentId;
    }
} 