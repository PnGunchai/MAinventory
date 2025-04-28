package com.inventory.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO for pagination response
 */
@Data
public class PageResponseDTO<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    
    /**
     * Create a PageResponseDTO from a Page
     */
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        PageResponseDTO<T> response = new PageResponseDTO<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        return response;
    }
} 