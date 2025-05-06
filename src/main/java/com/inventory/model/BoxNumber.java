package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

/**
 * Entity class for box_number table
 * Auto-running box numbers for products
 */
@Data
@Entity
@Table(name = "box_number")
public class BoxNumber {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "box_barcode", nullable = false)
    private String boxBarcode;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "product_barcode")
    private String productBarcode;
    
    @Column(name = "box_number", nullable = false)
    private Integer boxNumber;
    
    @Column(name = "last_updated")
    private ZonedDateTime lastUpdated;
    
    // Getters and setters
    public Integer getBoxNumber() {
        return boxNumber;
    }
    
    public void setBoxNumber(Integer boxNumber) {
        this.boxNumber = boxNumber;
    }
    
    public String getBoxBarcode() {
        return boxBarcode;
    }
    
    public void setBoxBarcode(String boxBarcode) {
        this.boxBarcode = boxBarcode;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductBarcode() {
        return productBarcode;
    }
    
    public void setProductBarcode(String productBarcode) {
        this.productBarcode = productBarcode;
    }
    
    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
} 