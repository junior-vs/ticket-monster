package com.ticket.monster.event.application.rest;

import com.ticket.monster.event.application.dto.EventCategoryRequest;
import com.ticket.monster.event.application.dto.EventRequest;
import com.ticket.monster.event.application.dto.EventResponse;
import com.ticket.monster.event.application.service.EventCategoryService;
import com.ticket.monster.event.application.service.EventService;
import com.ticket.monster.event.domain.model.Event;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    EventService eventService;


    @Inject
    public EventResource(EventService eventService) {
        this.eventService = eventService;
    }

    @POST
    public Response create(@Valid EventRequest request) {

        // use mutiny to handle asynchronous processing
        EventResponse event = eventService.create(request);
        return Response.status(Response.Status.CREATED).entity(event.id()).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        return eventService.findById(id)
                .map(event -> Response.ok(event).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).entity("Evento não encontrado").build());
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, EventRequest dto) {
        Event event = eventService.update(id, dto);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Evento não encontrado").build();
        }
        return Response.ok(event).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = eventService.delete(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).entity("Evento não encontrado").build();
        }
        return Response.noContent().build();
    }


}

