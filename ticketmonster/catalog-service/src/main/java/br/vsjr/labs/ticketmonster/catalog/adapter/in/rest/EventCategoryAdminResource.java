package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.CreateCategoryRequest;
import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.UpdateCategoryRequest;
import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.mapper.EventCategoryMapper;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.CreateCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.DeleteCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.UpdateCategoryUseCase;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

/**
 * Adaptador de entrada REST da administracao de categorias de eventos.
 *
 * <p>Endpoints administrativos (US-CAT-09): apenas escrita, protegida por
 * {@code ROLE_ADMIN} (AGENTS.md secao 8). A consulta publica e tratada em
 * {@code EventCategoryResource} (US4).
 */
@Path(EventCategoryAdminResource.BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventCategoryAdminResource {

    /**
     * Caminho base administrativo (contracts/event-categories-api.yaml).
     */
    public static final String BASE_PATH = "/api/v1/admin/event-categories";

    final CreateCategoryUseCase createCategoryUseCase;
    final UpdateCategoryUseCase updateCategoryUseCase;
    final DeleteCategoryUseCase deleteCategoryUseCase;
    final EventCategoryMapper dtoMapper;

    @Inject
    public EventCategoryAdminResource(CreateCategoryUseCase createCategoryUseCase,
                                      UpdateCategoryUseCase updateCategoryUseCase,
                                      DeleteCategoryUseCase deleteCategoryUseCase,
                                      EventCategoryMapper dtoMapper) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Cadastra uma nova categoria de evento.
     *
     * <p>FR-001 - sucesso responde {@code 201 Created} com o recurso criado e o
     * header {@code Location}. Erros sao mapeados para RFC 7807 (FR-005): 400 para
     * descricao invalida e 409 para descricao duplicada (RN06).
     *
     * @param request corpo com a descricao informada
     * @return resposta {@code 201 Created} com a categoria persistida
     */
    @POST
    @RolesAllowed("ROLE_ADMIN")
    @WithTransaction
    public Uni<Response> create(@Valid CreateCategoryRequest request) {
        return createCategoryUseCase.create(request.description())
                .map(category -> Response
                        .created(URI.create(BASE_PATH + "/" + category.id()))
                        .entity(dtoMapper.toResponse(category))
                        .build());
    }

    /**
     * Altera a descricao de uma categoria existente.
     *
     * <p>FR-001 - sucesso responde {@code 200 OK} com o recurso atualizado. Erros
     * RFC 7807: 400 descricao invalida, 404 id inexistente e 409 descricao ja
     * utilizada por outra categoria (RN06).
     *
     * @param id      identificador da categoria
     * @param request corpo com a nova descricao
     * @return resposta {@code 200 OK} com a categoria atualizada
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @WithTransaction
    public Uni<Response> update(@PathParam("id") UUID id, @Valid UpdateCategoryRequest request) {
        return updateCategoryUseCase.update(id, request.description())
                .map(category -> Response.ok(dtoMapper.toResponse(category)).build());
    }

    /**
     * Exclui uma categoria sem eventos associados.
     *
     * <p>FR-004 - sucesso responde {@code 204 No Content}. Erros RFC 7807: 404 id
     * inexistente e 409 categoria com eventos vinculados (RN04).
     *
     * @param id identificador da categoria
     * @return resposta {@code 204 No Content}
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("ROLE_ADMIN")
    @WithTransaction
    public Uni<Response> delete(@PathParam("id") UUID id) {
        return deleteCategoryUseCase.delete(id)
                .replaceWith(Response.noContent().build());
    }
}
