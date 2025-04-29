package com.inventory.service;

import com.inventory.dto.LogsFilterDTO;
import com.inventory.model.Logs;
import com.inventory.repository.LogsRepository;
import com.inventory.util.PageableBuilder;
import com.inventory.util.SpecificationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Service for filtering and paginating logs
 */
@Service
public class LogsFilterService {
    
    @Autowired
    private LogsRepository logsRepository;
    
    /**
     * Find logs with filtering and pagination
     */
    public Page<Logs> findLogs(LogsFilterDTO filter) {
        Pageable pageable = PageableBuilder.build(
            filter.getPage(), 
            filter.getSize(), 
            filter.getSortBy(), 
            filter.getSortDirection()
        );
        
        Specification<Logs> spec = buildSpecification(filter);
        
        return logsRepository.findAll(spec, pageable);
    }
    
    /**
     * Build specification for filtering logs
     */
    private Specification<Logs> buildSpecification(LogsFilterDTO filter) {
        Specification<Logs> spec = Specification.where(null);
        
        if (filter.getBoxBarcode() != null && !filter.getBoxBarcode().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("boxBarcode", filter.getBoxBarcode()));
        }
        
        if (filter.getProductBarcode() != null && !filter.getProductBarcode().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("productBarcode", filter.getProductBarcode()));
        }
        
        if (filter.getProductName() != null && !filter.getProductName().isEmpty()) {
            spec = spec.and(SpecificationBuilder.like("productName", filter.getProductName()));
        }
        
        if (filter.getOperation() != null && !filter.getOperation().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("operation", filter.getOperation()));
        }
        
        if (filter.getBoxNumber() != null) {
            spec = spec.and(SpecificationBuilder.equals("boxNumber", filter.getBoxNumber()));
        }
        
        if (filter.getOrderId() != null && !filter.getOrderId().isEmpty()) {
            spec = spec.and(SpecificationBuilder.equals("orderId", filter.getOrderId()));
        }
        
        if (filter.getStartDate() != null || filter.getEndDate() != null) {
            spec = spec.and(SpecificationBuilder.dateBetween("timestamp", filter.getStartDate(), filter.getEndDate()));
        }
        
        return spec;
    }
} 