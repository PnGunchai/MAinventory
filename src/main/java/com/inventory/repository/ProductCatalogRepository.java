package com.inventory.repository;

import com.inventory.model.ProductCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for ProductCatalog entity
 */
@Repository
public interface ProductCatalogRepository extends JpaRepository<ProductCatalog, String> {
    @Query("SELECT p FROM ProductCatalog p WHERE " +
           "LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.boxBarcode) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ProductCatalog> searchProducts(@Param("search") String search, Pageable pageable);

    boolean existsByProductNameIgnoreCase(String productName);

    // Custom query methods can be added here
} 