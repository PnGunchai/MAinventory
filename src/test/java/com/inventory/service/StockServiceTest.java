package com.inventory.service;

import com.inventory.InventoryManagementApplication;
import com.inventory.exception.InvalidInputException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.BoxNumber;
import com.inventory.model.CurrentStock;
import com.inventory.model.ProductCatalog;
import com.inventory.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = InventoryManagementApplication.class)
@ActiveProfiles("test")
class StockServiceTest {

    @Mock
    private CurrentStockRepository currentStockRepository;
    
    @Mock
    private ProductCatalogRepository productCatalogRepository;
    
    @Mock
    private LogsService logsService;
    
    @Mock
    private BoxNumberService boxNumberService;
    
    @Mock
    private BoxNumberRepository boxNumberRepository;
    
    @Mock
    private InStockService inStockService;
    
    @Mock
    private EntityManager entityManager;
    
    @InjectMocks
    private StockService stockService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup EntityManager mock behavior
        doNothing().when(entityManager).lock(any(), any());
        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any());
        
        // Setup BoxNumberService mock behavior
        when(boxNumberService.isProductBarcodeAvailable(any())).thenReturn(false);
        when(boxNumberService.createBoxNumberIfNeeded(any(), any(), any())).thenReturn(createMockBoxNumber());
        
        // Setup BoxNumberRepository mock behavior
        when(boxNumberRepository.existsByProductBarcode(any())).thenReturn(false);
        when(boxNumberRepository.findByProductBarcode(any())).thenReturn(Optional.empty());
        
        // Setup InStockService mock behavior
        when(inStockService.isInStock(any())).thenReturn(false);
    }
    
    private BoxNumber createMockBoxNumber() {
        BoxNumber boxNumber = new BoxNumber();
        boxNumber.setBoxNumber(1); // Changed to Integer
        boxNumber.setBoxBarcode("BOX001");
        boxNumber.setProductName("Test Product");
        return boxNumber;
    }
    
    @Test
    void addStock_WithValidInput_ShouldAddStock() {
        // Arrange
        String boxBarcode = "BOX001";
        String productBarcode = "SN001";
        int quantity = 1;
        String note = "Test note";
        
        ProductCatalog product = new ProductCatalog();
        product.setBoxBarcode(boxBarcode);
        product.setProductName("Test Product");
        product.setNumberSn(1);
        
        CurrentStock stock = new CurrentStock();
        stock.setBoxBarcode(boxBarcode);
        stock.setProductName("Test Product");
        stock.setQuantity(quantity);
        stock.setBoxNumber(1); // Changed to Integer
        stock.setLastUpdated(ZonedDateTime.now());
        
        when(productCatalogRepository.findById(boxBarcode)).thenReturn(Optional.of(product));
        when(currentStockRepository.findByBoxBarcodeAndProductName(any(), any())).thenReturn(Optional.empty());
        when(currentStockRepository.save(any(CurrentStock.class))).thenReturn(stock);
        
        // Act
        CurrentStock result = stockService.addStock(boxBarcode, productBarcode, quantity, note);
        
        // Assert
        assertNotNull(result);
        assertEquals(boxBarcode, result.getBoxBarcode());
        assertEquals("Test Product", result.getProductName());
        assertEquals(quantity, result.getQuantity());
        assertEquals(1, result.getBoxNumber()); // Changed to Integer comparison
        
        verify(productCatalogRepository).findById(boxBarcode);
        verify(currentStockRepository).findByBoxBarcodeAndProductName(boxBarcode, "Test Product");
        verify(currentStockRepository).save(any(CurrentStock.class));
        verify(logsService).createLog(
            eq(boxBarcode), 
            eq("Test Product"), 
            eq(productBarcode), 
            eq("add"), 
            eq(note), 
            eq(1), // Changed to Integer
            eq(quantity)
        );
    }
    
    @Test
    void addStock_WithInvalidBoxBarcode_ShouldThrowException() {
        // Arrange
        String boxBarcode = "INVALID";
        String productBarcode = "SN001";
        int quantity = 1;
        String note = "Test note";
        
        when(productCatalogRepository.findById(boxBarcode)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            stockService.addStock(boxBarcode, productBarcode, quantity, note);
        });
        
        verify(productCatalogRepository).findById(boxBarcode);
        verifyNoInteractions(currentStockRepository);
        verifyNoInteractions(logsService);
    }
    
    @Test
    void addStock_WithInvalidQuantity_ShouldThrowException() {
        // Arrange
        String boxBarcode = "BOX001";
        String productBarcode = "SN001";
        int quantity = 0;
        String note = "Test note";
        
        // Act & Assert
        assertThrows(InvalidInputException.class, () -> {
            stockService.addStock(boxBarcode, productBarcode, quantity, note);
        });
        
        verifyNoInteractions(productCatalogRepository);
        verifyNoInteractions(currentStockRepository);
        verifyNoInteractions(logsService);
    }
} 