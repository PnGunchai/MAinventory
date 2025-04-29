package com.inventory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Base service implementation for common CRUD operations
 * @param <T> The entity type
 * @param <ID> The ID type
 * @param <R> The repository type
 */
public abstract class BaseServiceImpl<T, ID, R extends JpaRepository<T, ID>> implements BaseService<T, ID> {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final R repository;
    
    public BaseServiceImpl(R repository) {
        this.repository = repository;
    }
    
    @Override
    @Transactional
    public T save(T entity) {
        logger.debug("Saving entity: {}", entity);
        return repository.save(entity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        logger.debug("Finding entity by ID: {}", id);
        return repository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        logger.debug("Finding all entities");
        return repository.findAll();
    }
    
    @Override
    @Transactional
    public void delete(T entity) {
        logger.debug("Deleting entity: {}", entity);
        repository.delete(entity);
    }
    
    @Override
    @Transactional
    public void deleteById(ID id) {
        logger.debug("Deleting entity by ID: {}", id);
        repository.deleteById(id);
    }
} 