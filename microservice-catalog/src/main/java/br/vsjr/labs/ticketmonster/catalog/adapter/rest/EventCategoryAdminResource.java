package br.vsjr.labs.ticketmonster.catalog.adapter.rest;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.dto.CreateCategoryRequest;
import br.vsjr.labs.ticketmonster.catalog.adapter.in.dto.UpdateCategoryRequest;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.CreateCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.DeleteCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.UpdateCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryId;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.UUID;

@Path("/api/v1/admin/event-categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ROLE_ADMIN")
public class EventCategoryAdminResource {

    private final CreateCategoryUseCase createUseCase;
    private final UpdateCategoryUseCase updateUseCase;
    private final DeleteCategoryUseCase deleteUseCase;

    public EventCategoryAdminResource(CreateCategoryUseCase createUseCase, UpdateCategoryUseCase updateUseCase, DeleteCategoryUseCase deleteUseCase

    ) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;

    }


    @POST
    @Operation(summary = "Cadastrar nova categoria de evento (Admin)")
    @APIResponse(responseCode = "201", description = "Categoria criada com sucesso")
    @APIResponse(responseCode = "400", description = "Dados de requisição inválidos (Problem Details)")
    @APIResponse(responseCode = "409", description = "Descrição de categoria já cadastrada (RN06 Conflict)")
    public Uni<RestResponse<CategoryResponse>> createCategory(@Valid CreateCategoryRequest request) {
        return Uni.createFrom().item(null);

    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Alterar descrição de categoria existente (Admin)")
    @APIResponse(responseCode = "200", description = "Categoria atualizada com sucesso")
    @APIResponse(responseCode = "404", description = "Categoria não encontrada")
    @APIResponse(responseCode = "409", description = "Descrição conflitante com outra categoria cadastrada")
    public Uni<RestResponse<CategoryResponse>> updateCategory(@PathParam("id") UUID id, @Valid UpdateCategoryRequest request) {
        return Uni.createFrom().item(null);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir categoria sem eventos vinculados (Admin)")
    @APIResponse(responseCode = "204", description = "Categoria excluída com sucesso")
    @APIResponse(responseCode = "404", description = "Categoria não encontrada")
    @APIResponse(responseCode = "409", description = "Exclusão impedida: categoria possui eventos associados")
    public Uni<RestResponse<?>> deleteCategory(@PathParam("id") UUID id) {
        var categoryId = new EventCategoryId(id);
        return deleteUseCase.deleteCategory(categoryId)
                .onItem().transform(v -> RestResponse.ResponseBuilder.noContent().build());
    }
}