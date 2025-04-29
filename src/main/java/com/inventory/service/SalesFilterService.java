package com.inventory.service;

import com.inventory.dto.SalesFilterDTO;
import com.inventory.model.Sales;
import com.inventory.repository.SalesRepository;
import com.inventory.util.PageableBuilder;
import com.inventory.util.SpecificationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Service for filtering and paginating sales
 */
@Service
public class SalesFilterService {
    
    @Autowired
    private SalesRepository salesRepository;
    
    /**
     * Find sales with filtering and pagination
     */
    public Page<Sales> findSales(SalesFilterDTO filter) {
        Pageable pageable = PageableBuilder.build(
            filter.getPage(), 
            filter.getSize(), 
            filter.getSortBy(), 
            filter.getSortDirection()
        );
        
        Specification<Sales> spec = buildSpecification(filter);
        
        return salesRepository.findAll(spec, pageable);
    }
    
    /**
     * Build specification for filtering sales
     */
    private Specification<Sales> buildSpecification(SalesFilterDTO filter) {
        Specification<Sales> spec = Specification.where(null);
        
        if (filter.getOrderId() != null && !filter.getOrderId().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("orderId", filter.getOrderId()));
        }
        
        if (filter.getEmployeeId() != null && !filter.getEmployeeId().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("employeeId", filter.getEmployeeId()));
        }
        
        if (filter.getShopName() != null && !filter.getShopName().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("shopName", filter.getShopName()));
        }
        
        if (filter.getBoxBarcode() != null && !filter.getBoxBarcode().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("boxBarcode", filter.getBoxBarcode()));
        }
        
        if (filter.getProductBarcode() != null && !filter.getProductBarcode().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("productBarcode", filter.getProductBarcode()));
        }
        
        if (filter.getProductName() != null && !filter.getProductName().isEmpty()) {
            spec = spec.and(SpecificationBuilder.like("productName", filter.getProductName()));
        }
        
        if (filter.getStartDate() != null || filter.getEndDate() != null) {
            spec = spec.and(SpecificationBuilder.dateBetween("timestamp", filter.getStartDate(), filter.getEndDate()));
        }
        
        return spec;
    }
} 