package com.inventory.repository;

import com.inventory.model.LentId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface LentIdRepository extends JpaRepository<LentId, String> {
    
    /**
     * Find lent IDs by employee ID
     */
    List<LentId> findByEmployeeId(String employeeId);
    
    /**
     * Find lent IDs by shop name
     */
    List<LentId> findByShopName(String shopName);
    
    /**
     * Find lent IDs by status with pagination
     */
    Page<LentId> findByStatus(String status, Pageable pageable);
    
    /**
     * Find lent IDs by status without pagination
     */
    List<LentId> findByStatus(String status);
    
    /**
     * Find lent IDs by employee ID and status
     */
    List<LentId> findByEmployeeIdAndStatus(String employeeId, String status);
    
    /**
     * Find lent IDs by shop name and status
     */
    List<LentId> findByShopNameAndStatus(String shopName, String status);
    
    /**
     * Find lent IDs by timestamp between start and end time
     */
    List<LentId> findByTimestampBetween(ZonedDateTime startTime, ZonedDateTime endTime);

    /**
     * Search lent IDs by terms in lentId, employeeId, or shopName
     */
    @Query("SELECT l FROM LentId l WHERE " +
           "LOWER(l.lentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.employeeId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.shopName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<LentId> findBySearchTerms(@Param("search") String search, Pageable pageable);

    /**
     * Search lent IDs by terms in lentId, employeeId, or shopName and status
     */
    @Query("SELECT l FROM LentId l WHERE " +
           "(LOWER(l.lentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.employeeId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.shopName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "l.status = :status")
    Page<LentId> findByStatusAndSearchTerms(@Param("status") String status, @Param("search") String search, Pageable pageable);
} 