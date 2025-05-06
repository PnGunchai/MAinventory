package com.inventory.util;

import com.inventory.exception.InvalidInputException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 * Utility class for common inventory operations
 */
public class InventoryUtils {
    
    /**
     * Validate that a string is not null or empty
     * @param value The string to validate
     * @param fieldName The name of the field for error messages
     * @throws InvalidInputException if the string is null or empty
     */
    public static void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + " cannot be empty");
        }
    }
    
    /**
     * Validate that a number is greater than zero
     * @param value The number to validate
     * @param fieldName The name of the field for error messages
     * @throws InvalidInputException if the number is not greater than zero
     */
    public static void validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new InvalidInputException(fieldName + " must be greater than zero");
        }
    }
    
    /**
     * Generate a unique order ID
     */
    public static String generateOrderId(String prefix) {
        String timestamp = ZonedDateTime.now(ZoneId.of("Asia/Bangkok")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        
        return prefix + "-" + timestamp + "-" + random;
    }
    
    /**
     * Validate employee ID format
     * Employee ID can be any non-empty string
     */
    public static boolean isValidEmployeeId(String employeeId) {
        return employeeId != null && !employeeId.trim().isEmpty();
    }
    
    /**
     * Format employee ID to ensure it follows the standard format
     */
    public static String formatEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new InvalidInputException("Employee ID cannot be empty");
        }
        
        return employeeId.trim();
    }
    
    /**
     * Safely get a value from an array with bounds checking
     * @param array The array to get the value from
     * @param index The index to get
     * @param defaultValue The default value to return if the index is out of bounds
     * @return The value at the index or the default value
     */
    public static <T> T safeArrayGet(T[] array, int index, T defaultValue) {
        if (array == null || index < 0 || index >= array.length) {
            return defaultValue;
        }
        return array[index];
    }
} 