package com.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

/**
 * Entity class for broken_id table
 * Records basic information about each broken item report
 */
@Data
@Entity
@Table(name = "broken_id")
public class BrokenId {
    
    @Id
    @Column(name = "broken_id")
    private String brokenId;
    
    @Column(name = "timestamp", nullable = false)
    private ZonedDateTime timestamp;
    
    @Column(name = "note")
    private String note;
} 