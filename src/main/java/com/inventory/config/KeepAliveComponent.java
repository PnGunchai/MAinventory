package com.inventory.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class KeepAliveComponent implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        // Comment out this line to allow normal application lifecycle
        // Thread.currentThread().join();
        
        // Just log that the application has started
        System.out.println("Application started successfully");
    }
} 