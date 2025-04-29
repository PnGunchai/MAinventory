package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity class for sales table
 * Records products that have been sold
 */
@Data
@Entity
@Table(name = "sales")
public class Sales {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_id")
    private Long salesId;
    
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;
    
    @Column(name = "box_barcode", nullable = false)
    private String boxBarcode;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "product_barcode")
    private String productBarcode;
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "box_number")
    private Integer boxNumber;

    @Column(name = "note")
    private String note;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "isdirectsales")
    private Boolean isDirectSales = true;
    
    // Renamed method to avoid conflict with Lombok-generated setter
    public void setSaleDateTime(LocalDateTime saleDate) {
        this.timestamp = saleDate;
    }

    /**
     * Builder for Sales entity
     */
    public static class Builder {
        private Sales sales = new Sales();
        
        public Builder boxBarcode(String boxBarcode) {
            sales.setBoxBarcode(boxBarcode);
            return this;
        }
        
        public Builder productName(String productName) {
            sales.setProductName(productName);
            return this;
        }
        
        public Builder productBarcode(String productBarcode) {
            sales.setProductBarcode(productBarcode);
            return this;
        }
        
        public Builder employeeId(String employeeId) {
            sales.setEmployeeId(employeeId);
            return this;
        }
        
        public Builder shopName(String shopName) {
            sales.setShopName(shopName);
            return this;
        }
        
        public Builder note(String note) {
            sales.setNote(note);
            return this;
        }
        
        public Builder boxNumber(Integer boxNumber) {
            sales.setBoxNumber(boxNumber);
            return this;
        }
        
        public Builder quantity(int quantity) {
            sales.setQuantity(quantity);
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            sales.setTimestamp(timestamp);
            return this;
        }
        
        public Builder orderId(String orderId) {
            sales.setOrderId(orderId);
            return this;
        }
        
        public Sales build() {
            return sales;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
} 