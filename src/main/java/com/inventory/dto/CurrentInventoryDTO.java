package com.inventory.dto;

import java.time.LocalDateTime;

public class CurrentInventoryDTO {
    private String boxBarcode;
    private String productName;
    private String productBarcode;
    private Integer quantity;
    private Integer boxNumber;
    private LocalDateTime lastUpdated;
    private Long daysInInventory;
    
    // Getters and setters
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
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Integer getBoxNumber() {
        return boxNumber;
    }
    
    public void setBoxNumber(Integer boxNumber) {
        this.boxNumber = boxNumber;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public Long getDaysInInventory() {
        return daysInInventory;
    }
    
    public void setDaysInInventory(Long daysInInventory) {
        this.daysInInventory = daysInInventory;
    }
} 