package com.inventory.service;

import com.inventory.exception.InvalidInputException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.InStock;
import com.inventory.model.ProductCatalog;
import com.inventory.repository.InStockRepository;
import com.inventory.repository.ProductCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing in_stock table
 */
@Service
public class InStockService {
    
    private static final Logger logger = LoggerFactory.getLogger(InStockService.class);
    
    @Autowired
    private InStockRepository inStockRepository;
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    /**
     * Add a product to in_stock
     */
    @Transactional
    public InStock addToStock(String boxBarcode, String productBarcode, String productName, Integer boxNumber) {
        return addToStock(boxBarcode, productBarcode, productName, boxNumber, false);
    }

    /**
     * Add a product to in_stock with option to force add (for returns)
     */
    @Transactional
    public InStock addToStock(String boxBarcode, String productBarcode, String productName, Integer boxNumber, boolean force) {
        // Validate inputs
        if (boxBarcode == null || boxBarcode.isEmpty()) {
            throw new InvalidInputException("Box barcode is required");
        }
        
        if (productName == null || productName.isEmpty()) {
            throw new InvalidInputException("Product name is required");
        }
        
        // For serialized products, product barcode is required
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with box barcode: " + boxBarcode));
        
        if (product.getNumberSn() > 0 && (productBarcode == null || productBarcode.isEmpty())) {
            throw new InvalidInputException("Product barcode is required for serialized products");
        }
        
        // Check if product barcode already exists in stock (for serialized products)
        // Skip this check if force is true (for returns)
        if (!force && product.getNumberSn() > 0 && productBarcode != null && !productBarcode.isEmpty()) {
            Optional<InStock> existingStock = inStockRepository.findByProductBarcode(productBarcode);
            if (existingStock.isPresent()) {
                throw new InvalidInputException("Product barcode already exists in stock: " + productBarcode);
            }
        }
        
        // If force is true and item exists, remove it first
        if (force && productBarcode != null && !productBarcode.isEmpty()) {
            inStockRepository.deleteByProductBarcode(productBarcode);
        }
        
        // Create new in_stock entry
        InStock inStock = new InStock();
        inStock.setBoxBarcode(boxBarcode);
        inStock.setProductName(productName);
        inStock.setBoxNumber(boxNumber);
        inStock.setAddedTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Bangkok")));
        
        // Set product barcode for serialized products
        if (product.getNumberSn() > 0 && productBarcode != null && !productBarcode.isEmpty()) {
            inStock.setProductBarcode(productBarcode);
        }
        
        // Save and return
        return inStockRepository.save(inStock);
    }
    
    /**
     * Remove a product from in_stock
     */
    @Transactional
    public void removeFromStock(String productBarcode) {
        if (productBarcode == null || productBarcode.isEmpty()) {
            throw new InvalidInputException("Product barcode is required");
        }
        
        // Check if product exists in stock
        if (!inStockRepository.existsByProductBarcode(productBarcode)) {
            throw new ResourceNotFoundException("Product barcode not found in stock: " + productBarcode);
        }
        
        // Remove from stock
        inStockRepository.deleteByProductBarcode(productBarcode);
        logger.info("Removed product from stock: {}", productBarcode);
    }
    
    /**
     * Check if a product is in stock
     */
    public boolean isInStock(String productBarcode) {
        if (productBarcode == null || productBarcode.isEmpty()) {
            return false;
        }
        
        return inStockRepository.existsByProductBarcode(productBarcode);
    }
    
    /**
     * Get all products in stock
     */
    public List<InStock> getAllInStock() {
        return inStockRepository.findAll();
    }
    
    /**
     * Get products in stock by box barcode
     */
    public List<InStock> getInStockByBoxBarcode(String boxBarcode) {
        return inStockRepository.findByBoxBarcode(boxBarcode);
    }
    
    /**
     * Find products in stock by box barcode
     */
    public List<InStock> findByBoxBarcode(String boxBarcode) {
        return inStockRepository.findByBoxBarcode(boxBarcode);
    }
    
    /**
     * Get product in stock by product barcode
     */
    public InStock getInStockByProductBarcode(String productBarcode) {
        return inStockRepository.findByProductBarcode(productBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product barcode not found in stock: " + productBarcode));
    }
    
    /**
     * Count products in stock by box barcode and product name
     */
    public long countInStockByBoxBarcodeAndProductName(String boxBarcode, String productName) {
        return inStockRepository.countByBoxBarcodeAndProductName(boxBarcode, productName);
    }

    /**
     * Check if a product is in stock and get its details
     * @return The InStock object if found, null if not in stock
     */
    public InStock getInStockDetails(String productBarcode) {
        if (productBarcode == null || productBarcode.isEmpty()) {
            return null;
        }
        
        Optional<InStock> inStock = inStockRepository.findByProductBarcode(productBarcode);
        return inStock.orElse(null);
    }

    /**
     * Get paginated and searchable in-stock items
     */
    public Page<InStock> getInStockPage(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return inStockRepository.searchInStock(search, pageable);
        } else {
            return inStockRepository.findAll(pageable);
        }
    }
} 