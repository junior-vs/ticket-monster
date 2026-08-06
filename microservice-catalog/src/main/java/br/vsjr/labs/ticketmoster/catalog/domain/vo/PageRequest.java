package br.vsjr.labs.ticketmoster.catalog.domain.vo;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 *   RN42 / FR-007, FR-008, FR-009: Paginação base 0 imutável com invariantes validados no construtor compacto
 *
 * @param page
 * @param size
 */
public record PageRequest(
        int page,
        @Positive(message = "O número da página ('page') não pode ser negativo." )
        @Size(min = 1, max = MAX_SIZE, message = "O tamanho da página ('size') deve estar entre 1 e " + MAX_SIZE) int size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    public static PageRequest of(Integer page, Integer size) {
        int p = (page == null) ? DEFAULT_PAGE : page;
        int s = (size == null) ? DEFAULT_SIZE : size;
        return new PageRequest(p, s);
    }



}
