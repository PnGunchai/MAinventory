package com.inventory.repository;

import com.inventory.model.ProductCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for ProductCatalog entity
 */
@Repository
public interface ProductCatalogRepository extends JpaRepository<ProductCatalog, String> {
    // Custom query methods can be added here
} 