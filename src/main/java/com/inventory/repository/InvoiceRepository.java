package com.inventory.repository;

import com.inventory.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository interface for Invoice entity
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    /**
     * Find invoices by invoice number
     */
    List<Invoice> findByInvoice(String invoice);
    
    /**
     * Find invoices by employee ID
     */
    List<Invoice> findByEmployeeId(String employeeId);
    
    /**
     * Find invoices by shop name
     */
    List<Invoice> findByShopName(String shopName);
} 