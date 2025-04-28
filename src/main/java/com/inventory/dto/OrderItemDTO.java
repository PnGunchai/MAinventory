package com.inventory.dto;

import java.util.List;

public class OrderItemDTO {
    private List<ProductIdentifierDTO> products;
    private String note;        // Optional note for the addition

    // Getters and Setters
    public List<ProductIdentifierDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductIdentifierDTO> products) {
        this.products = products;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
} 