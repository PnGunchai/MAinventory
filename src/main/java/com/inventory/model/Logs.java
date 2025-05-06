package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

/**
 * Entity class for logs table
 * Records all operations (add, remove, sales, lent, returned, broken)
 */
@Data
@Entity
@Table(name = "logs")
public class Logs {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "logs_id")
    private Long logsId;
    
    @Column(name = "box_barcode", nullable = false)
    private String boxBarcode;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "product_barcode")
    private String productBarcode;
    
    @Column(name = "operation", nullable = false)
    private String operation;
    
    @Column(name = "timestamp")
    private ZonedDateTime timestamp;
    
    @Column(name = "note")
    private String note;
    
    @Column(name = "box_number")
    private Integer boxNumber;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "quantity", nullable = false, columnDefinition = "integer default 1")
    private Integer quantity = 1;

    public Integer getBoxNumber() {
        return boxNumber;
    }

    public void setBoxNumber(Integer boxNumber) {
        this.boxNumber = boxNumber;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
} 