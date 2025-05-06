package com.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

/**
 * Entity for in_stock table
 */
@Entity
@Table(name = "in_stock")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "product_barcode", unique = true)
    private String productBarcode;
    
    @Column(name = "box_barcode", nullable = false)
    private String boxBarcode;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "box_number")
    private Integer boxNumber;
    
    @Column(name = "added_timestamp")
    private ZonedDateTime addedTimestamp;
} 