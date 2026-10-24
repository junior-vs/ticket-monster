package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest;

import java.util.List;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.CategoryResponse;
import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.mapper.EventCategoryDtoMapper;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.ListCategoriesUseCase;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Adaptador de entrada REST da consulta publica de categorias de eventos.
 *
 * <p>FR-001 / FR-007 - leitura publica (sem token) da listagem ordenada por
 * descricao, servida por Cache-Aside em Redis.
 */
@Path(EventCategoryResource.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class EventCategoryResource {

    /** Caminho base publico (contracts/event-categories-api.yaml). */
    public static final String BASE_PATH = "/api/v1/event-categories";

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final EventCategoryDtoMapper dtoMapper;

    @Inject
    public EventCategoryResource(ListCategoriesUseCase listCategoriesUseCase, EventCategoryDtoMapper dtoMapper) {
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Lista todas as categorias cadastradas, ordenadas alfabeticamente.
     *
     * @return resposta {@code 200 OK} com a lista de categorias
     */
    @GET
    @WithTransaction
    public Uni<List<CategoryResponse>> list() {
        return listCategoriesUseCase.listAll()
                .map(dtoMapper::toResponseList);
    }
}
