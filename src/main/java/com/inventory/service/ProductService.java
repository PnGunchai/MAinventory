package com.inventory.service;

import com.inventory.exception.ResourceNotFoundException;
import com.inventory.exception.InvalidInputException;
import com.inventory.model.ProductCatalog;
import com.inventory.model.Logs;
import com.inventory.repository.ProductCatalogRepository;
import com.inventory.repository.LogsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

/**
 * Service for product catalog operations
 */
@Service
public class ProductService {

    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private LogsRepository logsRepository;
    
    @Autowired
    private LogsService logsService;
    
    @Autowired
    private SyncService syncService;
    
    // Valid values for numberSn
    private static final List<Integer> VALID_SN_VALUES = Arrays.asList(0, 1, 2);
    
    /**
     * Add a new product to the catalog
     */
    @Transactional
    public ProductCatalog addProduct(String boxBarcode, String productName, Integer numberSn) {
        // Validate numberSn
        validateNumberSn(numberSn);
        
        // Check if product already exists
        if (productCatalogRepository.existsById(boxBarcode)) {
            throw new InvalidInputException("Product with box barcode " + boxBarcode + " already exists");
        }
        
        ProductCatalog product = new ProductCatalog();
        product.setBoxBarcode(boxBarcode);
        product.setProductName(productName);
        product.setNumberSn(numberSn);
        
        ProductCatalog savedProduct = productCatalogRepository.save(product);
        
        // Create corresponding stock entry with zero quantity
        syncService.createOrUpdateStock(boxBarcode, productName);
        
        return savedProduct;
    }
    
    /**
     * Add a new product with special logging behavior based on number_sn
     */
    @Transactional
    public ProductCatalog addProductWithLogs(ProductCatalog product, String productBarcode) {
        // Validate numberSn
        validateNumberSn(product.getNumberSn());
        
        // Check if product already exists
        if (productCatalogRepository.existsById(product.getBoxBarcode())) {
            throw new InvalidInputException("Product with box barcode " + product.getBoxBarcode() + " already exists");
        }
        
        // Save the product to catalog
        ProductCatalog savedProduct = productCatalogRepository.save(product);
        
        // Create logs based on number_sn
        switch (product.getNumberSn()) {
            case 0:
                // For products with no serial number, use "StP" as barcode
                logsService.createLog(product.getBoxBarcode(), product.getProductName(), "StP", "add");
                break;
                
            case 1:
                // For products with one serial number, use the actual barcode
                logsService.createLog(product.getBoxBarcode(), product.getProductName(), productBarcode, "add");
                break;
                
            case 2:
                // For products with two serial numbers, create two logs
                logsService.createLog(product.getBoxBarcode(), product.getProductName(), productBarcode, "add");
                
                // Create second log with modified barcode
                String secondBarcode;
                try {
                    long barcodeValue = Long.parseLong(productBarcode);
                    if (barcodeValue % 2 == 0) {
                        // Even barcode, subtract 1
                        secondBarcode = String.valueOf(barcodeValue - 1);
                    } else {
                        // Odd barcode, add 1
                        secondBarcode = String.valueOf(barcodeValue + 1);
                    }
                    logsService.createLog(product.getBoxBarcode(), product.getProductName(), secondBarcode, "add");
                } catch (NumberFormatException e) {
                    // If barcode is not a number, just use the original
                    logsService.createLog(product.getBoxBarcode(), product.getProductName(), productBarcode, "add");
                }
                break;
        }
        
        return savedProduct;
    }
    
    /**
     * Get a product by box barcode
     */
    public ProductCatalog getProduct(String boxBarcode) {
        return productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));
    }
    
    /**
     * Get all products with sorting and pagination
     */
    public List<ProductCatalog> getAllProducts(int page, int size, String sort, String direction) {
        org.springframework.data.domain.Sort.Direction sortDirection = 
            direction.equalsIgnoreCase("desc") ? 
            org.springframework.data.domain.Sort.Direction.DESC : 
            org.springframework.data.domain.Sort.Direction.ASC;
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(
                page, 
                size, 
                org.springframework.data.domain.Sort.by(sortDirection, sort)
            );
        
        return productCatalogRepository.findAll(pageable).getContent();
    }
    
    /**
     * Update a product
     */
    @Transactional
    public ProductCatalog updateProduct(String boxBarcode, String productName, Integer numberSn) {
        // Validate numberSn if provided
        if (numberSn != null) {
            validateNumberSn(numberSn);
        }
        
        ProductCatalog product = getProduct(boxBarcode);
        
        if (productName != null) {
            product.setProductName(productName);
        }
        
        if (numberSn != null) {
            product.setNumberSn(numberSn);
        }
        
        return productCatalogRepository.save(product);
    }
    
    /**
     * Delete a product
     */
    @Transactional
    public void deleteProduct(String boxBarcode) {
        ProductCatalog product = getProduct(boxBarcode);
        productCatalogRepository.delete(product);
    }
    
    /**
     * Validate that numberSn is one of the allowed values (0, 1, 2)
     */
    private void validateNumberSn(Integer numberSn) {
        if (numberSn == null || !VALID_SN_VALUES.contains(numberSn)) {
            throw new InvalidInputException("Invalid number_sn value: " + numberSn + ". Must be 0, 1, or 2.");
        }
    }
} 