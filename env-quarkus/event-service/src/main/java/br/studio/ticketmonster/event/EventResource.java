package br.studio.ticketmonster.event;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.inject.Default;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST resource for managing crud events.
 * <p>
 * 
 */
@Path("/events")
@WithTransaction
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    EventService eventService;

    public EventResource(EventService eventService) {
        this.eventService = eventService;
    }

    @POST
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

    @GET
    @Path("/{id}")
    public Uni<RestResponse<EventResponse>> findById(Long id) {
        return eventService.findById(id)
                .map(response -> {
                    if (response == null) {
                        return RestResponse.notFound();
                    } else {
                        return RestResponse.ok(response);
                    }
                });
    }

    // lista com paginação
    @GET
    public Uni<RestResponse<List<EventResponse>>> list(@QueryParam("page") @DefaultValue("0") int pageIndex,
            @QueryParam("size") @DefaultValue("20") int pageSize) {
        return eventService.list(pageIndex, pageSize)
                .map(response -> {
                    if (response.isEmpty()) {
                        return RestResponse.notFound();
                    } else {
                        return RestResponse.ok(response);
                    }
                });
    }

}
