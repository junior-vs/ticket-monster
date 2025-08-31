package com.ticket.monster.event.application.rest;

import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;

import com.ticket.monster.event.application.dto.EventRequest;
import com.ticket.monster.event.application.dto.EventResponse;
import com.ticket.monster.event.application.service.EventService;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.inject.Inject;
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


@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Event", description = "APIs para gerenciamento de eventos")
public class EventResource {

    EventService eventService;

    @Inject
    public EventResource(final EventService eventService) {
        this.eventService = eventService;
    }


    @POST
    @WithTransaction
    @Operation(summary = "Cria um novo evento", description = "Cria um novo evento e retorna o local do recurso criado.")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Evento criado com sucesso"),
        @APIResponse(responseCode = "400", description = "Dados inválidos"),
        @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public Uni<RestResponse<Void>> create(
            @Parameter(description = "Dados do evento", required = true)
            @Valid EventRequest request,
            @Context UriInfo uriInfo) {
        return eventService.create(request)
                .map(event -> RestResponse.created(
                        uriInfo.getAbsolutePathBuilder()
                                .path(String.valueOf(event.id()))
                                .build()));
    }


    @GET
    @Path("/{id}")
    @WithSession
    @Operation(summary = "Busca evento por ID", description = "Recupera um evento pelo seu identificador.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Evento encontrado",
            content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @APIResponse(responseCode = "404", description = "Evento não encontrado"),
        @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public Uni<RestResponse<EventResponse>> getById(
            @Parameter(description = "ID do evento", required = true, example = "1")
            @PathParam("id") Long id) {
        return eventService.findById(id)
                .map(event -> RestResponse.ok(event))
                .onItem()
                .ifNull()
                .continueWith(() -> RestResponse.status(Response.Status.NOT_FOUND));
    }


    @GET
    @WithSession
    @Operation(summary = "Lista todos os eventos", description = "Retorna uma lista de todos os eventos cadastrados.")
    @APIResponse(responseCode = "200", description = "Lista de eventos",
        content = @Content(schema = @Schema(implementation = EventResponse.class)))
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Uni<RestResponse<List<EventResponse>>> listAll() {
        return eventService.findAll().map(RestResponse::ok);
    }


    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza um evento", description = "Atualiza os dados de um evento existente.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Evento atualizado",
            content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @APIResponse(responseCode = "404", description = "Evento não encontrado"),
        @APIResponse(responseCode = "400", description = "Dados inválidos"),
        @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public Uni<RestResponse<EventResponse>> update(
            @Parameter(description = "ID do evento", required = true, example = "1")
            @PathParam("id") Long id,
            @Parameter(description = "Dados do evento", required = true)
            EventRequest dto) {
       return eventService.update(id, dto)
                .map(RestResponse::ok);
    }


    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove um evento", description = "Remove um evento pelo seu identificador.")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Evento removido com sucesso"),
        @APIResponse(responseCode = "404", description = "Evento não encontrado"),
        @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public Uni<RestResponse<Void>> delete(
            @Parameter(description = "ID do evento", required = true, example = "1")
            @PathParam("id") Long id) {
       return eventService.delete(id).map(RestResponse::ok);
    }

}
