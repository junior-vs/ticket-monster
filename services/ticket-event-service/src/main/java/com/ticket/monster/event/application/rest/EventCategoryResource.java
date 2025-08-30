package com.ticket.monster.event.application.rest;

import com.ticket.monster.event.application.dto.EventCategoryRequest;
import com.ticket.monster.event.application.dto.EventCategoryResponse;
import com.ticket.monster.event.application.service.EventCategoryService;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;

@Path("/event-categories")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class EventCategoryResource {

    EventCategoryService eventCategoryService;

    public EventCategoryResource(EventCategoryService eventCategoryService) {
        this.eventCategoryService = eventCategoryService;
    }


    @POST
    public Uni<RestResponse<Void>> createCategory(@Valid EventCategoryRequest request, @Context UriInfo uriInfo) {
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
public Uni<RestResponse<EventCategoryResponse>> getCategoryById(@PathParam("id") final Long id) {
    // Busca reativa pela categoria e retorna 404 se não encontrada
    return eventCategoryService.findById(id)
            .map(categoryOpt -> categoryOpt
                    .map(RestResponse::ok)
                    .orElse(RestResponse.notFound()));
}

    /**
     * Recupera uma lista categoria de evento de forma reativa.
     * Retorna 404 se não encontrada.
     */
    @GET
    public Uni<RestResponse<List<EventCategoryResponse>>> listCategories() {
        return eventCategoryService.findAll().map(RestResponse::ok);
    }

    @PUT
    @Path("/{id}")
    public Uni<RestResponse<EventCategoryResponse>> updateCategory(@PathParam("id") final Long id, @Valid final EventCategoryRequest request) {
        // Atualiza a categoria de evento de forma reativa e retorna 404 se não encontrada
        return eventCategoryService.update(id, request)
                .map(categoryOpt -> categoryOpt
                        .map(RestResponse::ok)
                        .orElse(RestResponse.notFound())
                );
    }

    @DELETE
    @Path("/{id}")
/**
 * Exclui uma categoria de evento de forma reativa.
 * Retorna 204 se excluída, 404 se não encontrada.
 */
public Uni<RestResponse<Void>> deleteCategory(@PathParam("id") final Long id) {
    // Processamento reativo para exclusão de categoria
    return eventCategoryService.delete(id)
            .map(deleted -> deleted
                    ? RestResponse.noContent()
                    : RestResponse.status(Response.Status.NOT_FOUND, null)
            );
}

}
