package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity class for broken table
 * Records products that are broken
 */
@Data
@Entity
@Table(name = "broken")
public class Broken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broken_id")
    private Long brokenId;
    
    @Column(name = "box_barcode", nullable = false)
    private String boxBarcode;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "product_barcode")
    private String productBarcode;
    
    @Column(name = "condition")
    private String condition;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "box_number")
    private Integer boxNumber;

    @Column(name = "note")
    private String note;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "order_id")
    private String orderId;

    public Integer getBoxNumber() {
        return boxNumber;
    }

    public void setBoxNumber(Integer boxNumber) {
        this.boxNumber = boxNumber;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setReportDate(LocalDateTime reportDate) {
        this.timestamp = reportDate;
    }
} 