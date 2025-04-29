package com.inventory.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility class for building pageable objects for pagination and sorting
 */
public class PageableBuilder {
    
    /**
     * Create a pageable object with default sorting
     */
    public static Pageable build(int page, int size) {
        return PageRequest.of(page, size);
    }
    
    /**
     * Create a pageable object with sorting
     */
    public static Pageable build(int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(sortBy);
        
        if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        
        return PageRequest.of(page, size, sort);
    }
    
    /**
     * Create a pageable object with multiple sort fields
     */
    public static Pageable build(int page, int size, String[] sortFields, String[] sortDirections) {
        if (sortFields == null || sortFields.length == 0) {
            return PageRequest.of(page, size);
        }
        
        Sort sort = null;
        
        for (int i = 0; i < sortFields.length; i++) {
            String field = sortFields[i];
            String direction = (sortDirections != null && i < sortDirections.length) ? sortDirections[i] : "asc";
            
            Sort.Order order = direction.equalsIgnoreCase("desc") ? 
                Sort.Order.desc(field) : Sort.Order.asc(field);
            
            if (sort == null) {
                sort = Sort.by(order);
            } else {
                sort = sort.and(Sort.by(order));
            }
        }
        
        return PageRequest.of(page, size, sort);
    }
} 