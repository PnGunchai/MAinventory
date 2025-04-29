package com.inventory.dto;

public class InventoryMovementDTO {
    private String boxBarcode;
    private String productName;
    private Integer addCount;
    private Integer removeCount;
    private Integer salesCount;
    private Integer lentCount;
    private Integer returnedCount;
    private Integer brokenCount;
    private Integer netMovement;
    
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
    
    public Integer getAddCount() {
        return addCount;
    }
    
    public void setAddCount(Integer addCount) {
        this.addCount = addCount;
    }
    
    public Integer getRemoveCount() {
        return removeCount;
    }
    
    public void setRemoveCount(Integer removeCount) {
        this.removeCount = removeCount;
    }
    
    public Integer getSalesCount() {
        return salesCount;
    }
    
    public void setSalesCount(Integer salesCount) {
        this.salesCount = salesCount;
    }
    
    public Integer getLentCount() {
        return lentCount;
    }
    
    public void setLentCount(Integer lentCount) {
        this.lentCount = lentCount;
    }
    
    public Integer getReturnedCount() {
        return returnedCount;
    }
    
    public void setReturnedCount(Integer returnedCount) {
        this.returnedCount = returnedCount;
    }
    
    public Integer getBrokenCount() {
        return brokenCount;
    }
    
    public void setBrokenCount(Integer brokenCount) {
        this.brokenCount = brokenCount;
    }
    
    public Integer getNetMovement() {
        return netMovement;
    }
    
    public void setNetMovement(Integer netMovement) {
        this.netMovement = netMovement;
    }
} 