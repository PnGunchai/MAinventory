package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity class for lent items
 */
@Data
@Entity
@Table(name = "lent")
public class Lend {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lent_id")
    private Long lentId;
    
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
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "order_id")
    private String orderId;

    /**
     * Get the lent ID for this lent item record
     * @return The lent ID (primary key)
     */
    public Long getLentId() {
        return lentId;
    }

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

    public void setLentDate(LocalDateTime lentDate) {
        this.timestamp = lentDate;
    }

    /**
     * Get the status of the lent item
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the status of the lent item
     */
    public void setStatus(String status) {
        this.status = status;
    }
} 