package com.inventory.util;

import org.springframework.data.jpa.domain.Specification;
import java.time.ZonedDateTime;

/**
 * Utility class for building JPA specifications for dynamic filtering
 */
public class SpecificationBuilder {
    
    /**
     * Create a specification for equal condition
     */
    public static <T> Specification<T> equals(String fieldName, Object value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get(fieldName), value);
        };
    }
    
    /**
     * Create a specification for like condition
     */
    public static <T> Specification<T> like(String fieldName, String value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(fieldName)),
                "%" + value.toLowerCase() + "%"
            );
        };
    }
    
    /**
     * Create a specification for greater than or equal condition
     */
    public static <T> Specification<T> greaterThanOrEqual(String fieldName, Comparable value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return null;
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), value);
        };
    }
    
    /**
     * Create a specification for less than or equal condition
     */
    public static <T> Specification<T> lessThanOrEqual(String fieldName, Comparable value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return null;
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), value);
        };
    }
    
    /**
     * Create a specification for between condition
     */
    public static <T> Specification<T> between(String fieldName, Comparable startValue, Comparable endValue) {
        return (root, query, criteriaBuilder) -> {
            if (startValue == null && endValue == null) {
                return null;
            }
            if (startValue == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), endValue);
            }
            if (endValue == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), startValue);
            }
            return criteriaBuilder.between(root.get(fieldName), startValue, endValue);
        };
    }
    
    /**
     * Create a specification for date between condition
     */
    public static <T> Specification<T> dateBetween(String fieldName, ZonedDateTime startDate, ZonedDateTime endDate) {
        return between(fieldName, startDate, endDate);
    }
} 