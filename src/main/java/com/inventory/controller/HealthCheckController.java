package com.inventory.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for health check endpoint
 */
@RestController
public class HealthCheckController {
    
    /**
     * Simple health check endpoint that returns 200 OK when the application is running
     */
    @GetMapping("/healthz")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
} 