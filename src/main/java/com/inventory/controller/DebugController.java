package com.inventory.controller;

import com.inventory.model.Sales;
import com.inventory.service.SalesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private SalesService salesService;

    @PostMapping("/test-sales-save")
    public ResponseEntity<?> testSalesSave(@RequestParam(required = false) Boolean isDirectSales,
                                           @RequestParam(defaultValue = "Test note") String note) {
        // Create a test sales object
        Sales sale = new Sales();
        sale.setBoxBarcode("TEST001");
        sale.setProductName("Test Product");
        sale.setEmployeeId("TEST");
        sale.setShopName("Test Shop");
        sale.setOrderId("TEST-ORDER-" + System.currentTimeMillis());
        sale.setTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Bangkok")));
        sale.setQuantity(1);
        
        // Set note to test behavior with lent items
        if (note.contains("lent")) {
            sale.setNote("Batch processing (from lent order: L001)");
        } else {
            sale.setNote(note);
        }
        
        // Set isDirectSales if provided
        if (isDirectSales != null) {
            sale.setIsDirectSales(isDirectSales);
        }
        
        // Save the sales object
        Sales savedSale = salesService.save(sale);
        
        // Return the result
        return ResponseEntity.ok(savedSale);
    }
} 