package com.inventory.dto;

import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public class CurrentInventoryDTO {
    private String boxBarcode;
    private String productName;
    private String productBarcode;
    private Integer quantity;
    private Integer boxNumber;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Bangkok")
    private ZonedDateTime lastUpdated;
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
    
    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public Long getDaysInInventory() {
        return daysInInventory;
    }
    
    public void setDaysInInventory(Long daysInInventory) {
        this.daysInInventory = daysInInventory;
    }
} 