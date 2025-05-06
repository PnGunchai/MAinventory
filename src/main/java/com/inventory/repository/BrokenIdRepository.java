package com.inventory.repository;

import com.inventory.model.BrokenId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface BrokenIdRepository extends JpaRepository<BrokenId, String> {
    
    /**
     * Find broken IDs by timestamp between start and end time
     */
    List<BrokenId> findByTimestampBetween(ZonedDateTime startTime, ZonedDateTime endTime);
    
    /**
     * Find broken IDs containing note
     */
    List<BrokenId> findByNoteContaining(String noteFragment);
} 