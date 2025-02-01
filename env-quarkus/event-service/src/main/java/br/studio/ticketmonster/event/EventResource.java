package br.studio.ticketmonster.event;

import java.net.URI;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    EventService eventService;

    public EventResource(EventService eventService) {
        this.eventService = eventService;
    }

    @POST
    @WithTransaction
    public Uni<RestResponse<EventResponse>> createEvent(EventRequest event, @Context UriInfo uriInfo) {
        return eventService.createEvent(event)
                .map(response -> {
                    // Build location URI for the created resource
                    URI location = uriInfo.getAbsolutePathBuilder()
                            .path(String.valueOf(response.id()))
                            .build();
                    // Create REST response with status, location and body
                    return RestResponse.created(location);
                });
    }
}
