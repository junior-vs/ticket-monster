package br.vsjr.labs.ticketmoster.catalog.adapter.rest;

import br.vsjr.labs.ticketmoster.catalog.adapter.in.dto.EventCategoryVO;
import br.vsjr.labs.ticketmoster.catalog.application.mapper.EventCategoryMapper;
import br.vsjr.labs.ticketmoster.catalog.application.port.in.CreateCategoryUseCase;
import br.vsjr.labs.ticketmoster.catalog.application.port.in.DeleteCategoryUseCase;
import br.vsjr.labs.ticketmoster.catalog.application.port.in.UpdateCategoryUseCase;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategoryDescription;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategoryId;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;
import java.util.UUID;

@Path("/api/v1/admin/event-categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventCategoryAdminResource {

    private final CreateCategoryUseCase createUseCase;
    private final UpdateCategoryUseCase updateUseCase;
    private final DeleteCategoryUseCase deleteUseCase;
    private final EventCategoryMapper mapper;

    public EventCategoryAdminResource(CreateCategoryUseCase createUseCase, UpdateCategoryUseCase updateUseCase, DeleteCategoryUseCase deleteUseCase, EventCategoryMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.mapper = mapper;
    }



    @POST
    @Operation(summary = "Cadastrar nova categoria de evento (Admin)")
    @APIResponse(responseCode = "201", description = "Categoria criada com sucesso")
    @APIResponse(responseCode = "400", description = "Dados de requisição inválidos (Problem Details)")
    @APIResponse(responseCode = "409", description = "Descrição de categoria já cadastrada (RN06 Conflict)")
    public Uni<RestResponse<EventCategoryVO>> createCategory(@Valid EventCategoryVO request) {
        var eventCategory = mapper.toDomain(request);
        return createUseCase.createCategory(eventCategory)
                .onItem().transform(category -> {
                    var responseDto = mapper.toVO(category);

                    return RestResponse.ResponseBuilder
                            .create(Response.Status.CREATED, responseDto)
                            .location(URI.create("/v1/event-categories/" + category.id().value()))
                            .build();
                });
    }
    @PUT
    @Path("/{id}")
    @Operation(summary = "Alterar descrição de categoria existente (Admin)")
    @APIResponse(responseCode = "200", description = "Categoria atualizada com sucesso")
    @APIResponse(responseCode = "404", description = "Categoria não encontrada")
    @APIResponse(responseCode = "409", description = "Descrição conflitante com outra categoria cadastrada")
    public Uni<RestResponse<EventCategoryVO>> updateCategory(@PathParam("id") UUID id, @Valid EventCategoryVO request) {
        var categoryId = new EventCategoryId(id);
        var description = new EventCategoryDescription(request.description());
        return updateUseCase.updateCategory(categoryId, description)
                .onItem().transform(category -> {
                    var responseDto = mapper.toVO(category);

                    return RestResponse.ResponseBuilder
                            .create(Response.Status.CREATED, responseDto)
                            .location(URI.create("/v1/event-categories/" + category.id().value()))
                            .build();
                });
    }
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir categoria sem eventos vinculados (Admin)")
    @APIResponse(responseCode = "204", description = "Categoria excluída com sucesso")
    @APIResponse(responseCode = "404", description = "Categoria não encontrada")
    @APIResponse(responseCode = "409", description = "Exclusão impedida: categoria possui eventos associados")
    public Uni<RestResponse<?>>  deleteCategory(@PathParam("id") UUID id) {
        var categoryId = new EventCategoryId(id);
        return deleteUseCase.deleteCategory(categoryId)
                .onItem().transform(v -> RestResponse.ResponseBuilder.noContent().build());
    }
}