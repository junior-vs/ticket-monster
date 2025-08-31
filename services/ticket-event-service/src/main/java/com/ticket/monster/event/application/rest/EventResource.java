package com.ticket.monster.event.application.rest;

import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;

import com.ticket.monster.event.application.dto.EventRequest;
import com.ticket.monster.event.application.dto.EventResponse;
import com.ticket.monster.event.application.service.EventService;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
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
public class EventResource {

    EventService eventService;

    @Inject
    public EventResource(final EventService eventService) {
        this.eventService = eventService;
    }

    @POST
    @WithTransaction
    public Uni<RestResponse<Void>> create(@Valid EventRequest request, @Context UriInfo uriInfo) {
        return eventService.create(request)
                .map(event -> RestResponse.created(
                        uriInfo.getAbsolutePathBuilder()
                                .path(String.valueOf(event.id()))
                                .build()));
    }

    @GET
    @Path("/{id}")
    @WithSession
    public Uni<RestResponse<EventResponse>> getById(@PathParam("id") Long id) {
        return eventService.findById(id)
                .map(event -> RestResponse.ok(event))
                .onItem()
                .ifNull()
                .continueWith(() -> RestResponse.status(Response.Status.NOT_FOUND));

    }

    @GET
    @WithSession
    public Uni<RestResponse<List<EventResponse>>> listAll() {
        return eventService.findAll().map(RestResponse::ok);

    }

    @PUT
    @Path("/{id}")
    public Uni<RestResponse<EventResponse>> update(@PathParam("id") Long id, EventRequest dto) {
       return eventService.update(id, dto)
                .map(RestResponse::ok);

    }

    @DELETE
    @Path("/{id}")
    public Uni<RestResponse<Void>> delete(@PathParam("id") Long id) {
       return eventService.delete(id).map(RestResponse::ok);
    }

}
