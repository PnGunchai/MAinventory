package com.inventory.service;

import java.util.List;
import java.util.Optional;

/**
 * Base service interface for common CRUD operations
 * @param <T> The entity type
 * @param <ID> The ID type
 */
public interface BaseService<T, ID> {
    
    /**
     * Save an entity
     * @param entity The entity to save
     * @return The saved entity
     */
    T save(T entity);
    
    /**
     * Find an entity by ID
     * @param id The ID to find
     * @return The entity if found
     */
    Optional<T> findById(ID id);
    
    /**
     * Find all entities
     * @return All entities
     */
    List<T> findAll();
    
    /**
     * Delete an entity
     * @param entity The entity to delete
     */
    void delete(T entity);
    
    /**
     * Delete an entity by ID
     * @param id The ID of the entity to delete
     */
    void deleteById(ID id);
} 