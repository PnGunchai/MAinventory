package com.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

/**
 * Entity class for product_catalog table
 * Contains information about products with their box barcodes and number of serial numbers
 */
@Data
@Entity
@Table(name = "product_catalog")
public class ProductCatalog {
    
    @Id
    @Column(name = "box_barcode")
    private String boxBarcode;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "number_sn", nullable = false)
    private Integer numberSn;
    
    @JsonManagedReference
    @OneToMany(mappedBy = "productCatalog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CurrentStock> currentStocks;
    
    /**
     * Override the generated getter to handle null values
     * Returns 0 instead of null if numberSn is not set
     */
    public Integer getNumberSn() {
        return numberSn != null ? numberSn : 0;
    }
} 