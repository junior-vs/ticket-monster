package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest;

import java.net.URI;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.CategoryResponse;
import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.CreateCategoryRequest;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.CreateCategoryUseCase;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    /** Caminho base administrativo (contracts/event-categories-api.yaml). */
    public static final String BASE_PATH = "/api/v1/admin/event-categories";

    private final CreateCategoryUseCase createCategoryUseCase;

    @Inject
    public EventCategoryAdminResource(CreateCategoryUseCase createCategoryUseCase) {
        this.createCategoryUseCase = createCategoryUseCase;
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
    public Uni<Response> create(CreateCategoryRequest request) {
        return createCategoryUseCase.create(request.description())
                .map(category -> Response
                        .created(URI.create(BASE_PATH + "/" + category.id()))
                        .entity(CategoryResponse.from(category))
                        .build());
    }
}
