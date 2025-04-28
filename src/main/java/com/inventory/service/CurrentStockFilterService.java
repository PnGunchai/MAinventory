package com.inventory.service;

import com.inventory.dto.CurrentStockFilterDTO;
import com.inventory.model.CurrentStock;
import com.inventory.repository.CurrentStockRepository;
import com.inventory.util.PageableBuilder;
import com.inventory.util.SpecificationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Service for filtering and paginating current stock
 */
@Service
public class CurrentStockFilterService {
    
    @Autowired
    private CurrentStockRepository currentStockRepository;
    
    /**
     * Find current stock with filtering and pagination
     */
    public Page<CurrentStock> findCurrentStock(CurrentStockFilterDTO filter) {
        Pageable pageable = PageableBuilder.build(
            filter.getPage(), 
            filter.getSize(), 
            filter.getSortBy(), 
            filter.getSortDirection()
        );
        
        Specification<CurrentStock> spec = buildSpecification(filter);
        
        return currentStockRepository.findAll(spec, pageable);
    }
    
    /**
     * Build specification for filtering current stock
     */
    private Specification<CurrentStock> buildSpecification(CurrentStockFilterDTO filter) {
        Specification<CurrentStock> spec = Specification.where(null);
        
        if (filter.getBoxBarcode() != null && !filter.getBoxBarcode().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("boxBarcode", filter.getBoxBarcode()));
        }
        
        if (filter.getProductName() != null && !filter.getProductName().isEmpty()) {
            spec = spec.and(SpecificationBuilder.like("productName", filter.getProductName()));
        }
        
        if (filter.getBoxNumber() != null) {
            spec = spec.and(SpecificationBuilder.equals("boxNumber", filter.getBoxNumber()));
        }
        
        if (filter.getMinQuantity() != null) {
            spec = spec.and(SpecificationBuilder.greaterThanOrEqual("quantity", filter.getMinQuantity()));
        }
        
        if (filter.getMaxQuantity() != null) {
            spec = spec.and(SpecificationBuilder.lessThanOrEqual("quantity", filter.getMaxQuantity()));
        }
        
        return spec;
    }
} 