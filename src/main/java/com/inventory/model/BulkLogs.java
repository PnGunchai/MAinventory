package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Entity class for bulk_logs table
 * Daily summary of operations
 */
@Data
@Entity
@Table(name = "bulk_logs")
public class BulkLogs {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bulk_id")
    private Long bulkId;
    
    @Column(name = "box_barcode", nullable = false)
    private String boxBarcode;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "operation", nullable = false)
    private String operation;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
} 