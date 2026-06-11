package com.smartlogix.usuarios.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private int numberOfElements;
    private boolean first;
    private boolean last;
    private String sortBy;
    private String sortDir;

    public static <T> PageResponse<T> from(Page<T> pageData) {
        Sort.Order order = pageData.getSort().stream().findFirst().orElse(null);
        return PageResponse.<T>builder()
                .content(pageData.getContent())
                .page(pageData.getNumber())
                .size(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .numberOfElements(pageData.getNumberOfElements())
                .first(pageData.isFirst())
                .last(pageData.isLast())
                .sortBy(order != null ? order.getProperty() : "id")
                .sortDir(order != null ? order.getDirection().name().toLowerCase() : "asc")
                .build();
    }
}
