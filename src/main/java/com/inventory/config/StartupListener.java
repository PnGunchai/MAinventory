package com.inventory.config;

import com.inventory.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupListener {
    
    @Autowired
    private SyncService syncService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Synchronize CurrentStock with ProductCatalog on startup
        syncService.syncCurrentStockWithCatalog();
    }
} 