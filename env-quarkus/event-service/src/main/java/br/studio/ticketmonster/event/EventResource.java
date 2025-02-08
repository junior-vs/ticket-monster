package br.studio.ticketmonster.event;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
    public Uni<RestResponse<EventResponse>> createEvent(@Valid EventRequest event, @Context UriInfo uriInfo) {
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
        return eventService.findByIdWithMediaItems(id)
                .map(response -> {
                    if (response == null) {
                        return RestResponse.notFound();
                    } else {
                        return RestResponse.ok(response);
                    }
                });
    }

    /*
     * List all events.
     * 
     * @param pageIndex the index of the page to retrieve
     * @param pageSize the size of the page to retrieve
     * @return a REST response with the list of events
     */
    @GET
    public Uni<RestResponse<List<EventSimpleResponse>>> list(@QueryParam("page") @DefaultValue("0") int pageIndex,
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

    // update event
    @PUT
    @Path("/{id}")
    public Uni<RestResponse<EventResponse>> updateEvent(@PathParam("id") Long id, @Valid EventRequest event) {
        return eventService.updateEvent(id, event)
                .map(response -> {
                    if (response == null) {
                        return RestResponse.notFound();
                    } else {
                        return RestResponse.ok(response);
                    }
                });
    }

    @DELETE
    @Path("/{id}")
    public Uni<RestResponse<Void>> deleteEvent(@PathParam("id") Long id) {
        return eventService.deleteEvent(id)
                .map(RestResponse::ok);
                
    }

}
