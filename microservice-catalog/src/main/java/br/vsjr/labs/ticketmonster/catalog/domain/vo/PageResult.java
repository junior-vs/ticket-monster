package br.vsjr.labs.ticketmonster.catalog.domain.vo;

import java.util.List;

/**
 * Record genérico para transportar resultados paginados e metadados
 *
 * @param content
 * @param page
 * @param size
 * @param totalElements
 * @param totalPages
 * @param <T>
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResult<T> of(List<T> items, int page, int size, long totalElements) {
        int totalPages = (size == 0) ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResult<>(items, page, size, totalElements, totalPages);
    }

}
