package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity class for current_stock table
 * Represents the current inventory of products in stock
 */
@Data
@Entity
@Table(name = "current_stock")
public class CurrentStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long stockId;
    
    @Column(name = "box_barcode", nullable = false)
    private String boxBarcode;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "box_number")
    private Integer boxNumber;

    public Integer getBoxNumber() {
        return boxNumber;
    }

    public void setBoxNumber(Integer boxNumber) {
        this.boxNumber = boxNumber;
    }
} 