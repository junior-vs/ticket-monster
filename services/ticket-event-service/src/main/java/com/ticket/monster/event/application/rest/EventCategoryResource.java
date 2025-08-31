package com.ticket.monster.event.application.rest;

import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;

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
public class EventCategoryResource {

    EventCategoryService eventCategoryService;

    public EventCategoryResource(EventCategoryService eventCategoryService) {
        this.eventCategoryService = eventCategoryService;
    }

    @POST
    @WithTransaction
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
    @WithSession
    public Uni<RestResponse<EventCategoryResponse>> getCategoryById(@PathParam("id") final Long id) {
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
    public Uni<RestResponse<List<EventCategoryResponse>>> listCategories() {
        return eventCategoryService.findAll().map(RestResponse::ok);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    public Uni<RestResponse<EventCategoryResponse>> updateCategory(@PathParam("id") final Long id,
            @Valid final EventCategoryRequest request) {
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
    public Uni<RestResponse<Void>> deleteCategory(@PathParam("id") final Long id) {
        // Processamento reativo para exclusão de categoria
        return eventCategoryService.delete(id)
                .map(deleted -> deleted
                        ? RestResponse.noContent()
                        : RestResponse.status(Response.Status.NOT_FOUND, null));
    }

}
