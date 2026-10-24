package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.mapper;

import java.util.List;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.CategoryResponse;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapeamento entre o agregado de dominio {@link EventCategory} e os DTOs REST.
 *
 * <p>Bean CDI injetavel nos adaptadores de entrada, mantendo o dominio livre de
 * convencoes de transporte: o agregado expoe acessores fluentes
 * ({@code id()}, {@code description()}, {@code createdAt()}) e a projecao para o
 * contrato {@code CategoryResponse} acontece nesta borda.
 */
@ApplicationScoped
public class EventCategoryDtoMapper {

    /**
     * Projeta o agregado de dominio para a representacao de transporte.
     *
     * @param category categoria de dominio
     * @return DTO de resposta
     */
    public CategoryResponse toResponse(EventCategory category) {
        return new CategoryResponse(category.id(), category.description().value(), category.createdAt());
    }

    /**
     * Projeta uma colecao de categorias para a representacao de transporte.
     *
     * @param categories categorias de dominio, ja ordenadas
     * @return lista de DTOs de resposta na mesma ordem
     */
    public List<CategoryResponse> toResponseList(List<EventCategory> categories) {
        return categories.stream().map(this::toResponse).toList();
    }
}
