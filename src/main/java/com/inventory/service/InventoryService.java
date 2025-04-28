package com.inventory.service;

import com.inventory.model.CurrentStock;
import com.inventory.model.ProductCatalog;
import com.inventory.repository.CurrentStockRepository;
import com.inventory.repository.ProductCatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for inventory management operations
 */
@Service
public class InventoryService {
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private CurrentStockRepository currentStockRepository;
    
    /**
     * Add a new product to the catalog
     */
    @Transactional
    public ProductCatalog addProductToCatalog(ProductCatalog product) {
        return productCatalogRepository.save(product);
    }
    
    /**
     * Get all products from catalog
     */
    public List<ProductCatalog> getAllProducts() {
        return productCatalogRepository.findAll();
    }
    
    /**
     * Get product by box barcode
     */
    public ProductCatalog getProductByBoxBarcode(String boxBarcode) {
        return productCatalogRepository.findById(boxBarcode).orElse(null);
    }
    
    /**
     * Add stock for a product
     */
    @Transactional
    public CurrentStock addStock(String boxBarcode, String productName, int quantity) {
        // Check if product exists in catalog
        ProductCatalog product = productCatalogRepository.findById(boxBarcode)
                .orElseThrow(() -> new RuntimeException("Product not found in catalog"));
        
        // Check if stock already exists
        Optional<CurrentStock> stockOptional = currentStockRepository.findByBoxBarcodeAndProductName(boxBarcode, productName);
        CurrentStock stock;
        
        if (stockOptional.isEmpty()) {
            // Create new stock entry
            stock = new CurrentStock();
            stock.setBoxBarcode(boxBarcode);
            stock.setProductName(productName);
            stock.setQuantity(quantity);
        } else {
            // Update existing stock
            stock = stockOptional.get();
            stock.setQuantity(stock.getQuantity() + quantity);
        }
        
        return currentStockRepository.save(stock);
    }
    
    /**
     * Get current stock for all products
     */
    public List<CurrentStock> getAllStock() {
        return currentStockRepository.findAll();
    }
    
    /**
     * Get stock by box barcode
     */
    public List<CurrentStock> getStockByBoxBarcode(String boxBarcode) {
        return currentStockRepository.findByBoxBarcode(boxBarcode);
    }
} 