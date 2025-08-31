package com.ticket.monster.event.application.rest;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.jboss.resteasy.reactive.RestResponse;

import com.ticket.monster.event.application.dto.ErrorResponse;
import com.ticket.monster.event.application.dto.EventCategoryRequest;
import com.ticket.monster.event.application.dto.EventCategoryResponse;
import com.ticket.monster.event.application.service.EventCategoryService;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/event-categories")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Event Category", description = "APIs para gerenciamento de categorias de eventos")
public class EventCategoryResource {

    EventCategoryService eventCategoryService;

    public EventCategoryResource(EventCategoryService eventCategoryService) {
        this.eventCategoryService = eventCategoryService;
    }

    @POST
    @WithTransaction
    @Operation(summary = "Cria uma nova categoria de evento", description = "Cria uma nova categoria de evento e retorna o local do recurso criado.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Categoria criada com sucesso"),
            @APIResponse(responseCode = "400", description = "Dados inválidos"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<RestResponse<Void>> createCategory(
            @Parameter(description = "Dados da categoria de evento", required = true) @Valid EventCategoryRequest request,
            @Context UriInfo uriInfo) {
        return eventCategoryService.create(request)
                .map(category -> RestResponse.created(
                        uriInfo.getAbsolutePathBuilder()
                                .path(String.valueOf(category.id()))
                                .build()));
    }

    /**
     * Recupera uma categoria de evento pelo ID de forma reativa.
     * Retorna 404 se não encontrada.
     */

    @GET
    @Path("/{id}")
    @WithSession
    @Operation(summary = "Busca categoria de evento por ID", description = "Recupera uma categoria de evento pelo seu identificador.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Categoria encontrada", content = @Content(schema = @Schema(implementation = EventCategoryResponse.class))),
            @APIResponse(responseCode = "404", description = "Categoria não encontrada"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<RestResponse<EventCategoryResponse>> getCategoryById(
            @Parameter(description = "ID da categoria de evento", required = true, example = "1") @PathParam("id") final Long id) {
        // Busca reativa pela categoria e retorna 404 se não encontrada
        return eventCategoryService.findByIdOptional(id)
                .map(categoryOpt -> categoryOpt
                        .map(RestResponse::ok)
                        .orElse(RestResponse.notFound()));
    }

    /**
     * Recupera uma lista categoria de evento de forma reativa.
     * Retorna 404 se não encontrada.
     */

    @GET
    @WithSession
    @Operation(summary = "Lista todas as categorias de evento", description = "Retorna uma lista de todas as categorias de evento cadastradas.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de categorias", content = @Content(schema = @Schema(implementation = EventCategoryResponse.class))),
            @APIResponse(responseCode = "404", description = "Categorias não encontradas"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<RestResponse<List<EventCategoryResponse>>> listCategories() {
        return eventCategoryService.findAll().map(RestResponse::ok);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Atualiza uma categoria de evento", description = "Atualiza os dados de uma categoria de evento existente.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Categoria atualizada", content = @Content(schema = @Schema(implementation = EventCategoryResponse.class))),
            @APIResponse(responseCode = "404", description = "Categoria não encontrada"),
            @APIResponse(responseCode = "400", description = "Dados inválidos"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<RestResponse<EventCategoryResponse>> updateCategory(
            @Parameter(description = "ID da categoria de evento", required = true, example = "1") @PathParam("id") final Long id,
            @Parameter(description = "Dados da categoria de evento", required = true) @Valid final EventCategoryRequest request) {
        // Atualiza a categoria de evento de forma reativa e retorna 404 se não
        // encontrada
        return eventCategoryService.update(id, request)
                .map(categoryOpt -> categoryOpt
                        .map(RestResponse::ok)
                        .orElse(RestResponse.notFound()));
    }

    @DELETE
    @WithTransaction
    @Path("/{id}")
    @Operation(summary = "Remove uma categoria de evento", description = "Remove uma categoria de evento pelo seu identificador.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Categoria removida com sucesso"),
            @APIResponse(responseCode = "404", description = "Categoria não encontrada"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<RestResponse<Void>> deleteCategory(
            @Parameter(description = "ID da categoria de evento", required = true, example = "1") @PathParam("id") final Long id) {
        // Processamento reativo para exclusão de categoria
        return eventCategoryService.delete(id)
                .map(deleted -> deleted
                        ? RestResponse.noContent()
                        : RestResponse.status(Response.Status.NOT_FOUND, null));
    }

}
