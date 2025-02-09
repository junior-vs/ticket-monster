package br.studio.ticketmonster.event;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import br.studio.ticketmonster.mediaitem.MediaItemRequest;
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
@Tag(name = "Event", description = "Event CRUD operations")
@Path("/events")
@WithTransaction
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    EventService eventService;

    public EventResource(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "Create a new Event")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Event created"),
            @APIResponse(responseCode = "400", description = "Invalid input"),
            @APIResponse(responseCode = "409", description = "Event already exists") })
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

    @Operation(summary = "Find an Event by ID")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Event found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponse.class))),
            @APIResponse(responseCode = "404", description = "Event not found") })
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
     * 
     * @param pageSize the size of the page to retrieve
     * 
     * @return a REST response with the list of events
     */
    @Operation(summary = "List all Events")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Events found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventSimpleResponse.class))),
            @APIResponse(responseCode = "404", description = "Events not found") })
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

    @Operation(summary = "Update Event")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Event updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponse.class))),
            @APIResponse(responseCode = "404", description = "Event not found") })
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

    /**
     * Delete an event.
     * 
     * @param id
     * @return
     */
    @Operation(summary = "Delete an existing Event")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Event deleted"),
            @APIResponse(responseCode = "404", description = "Event not found") })
    @DELETE
    @Path("/{id}")
    public Uni<RestResponse<Void>> deleteEvent(@PathParam("id") Long id) {
        return eventService.deleteEvent(id)
                .map(RestResponse::ok);

    }


    @Operation(summary = "List all Events by EventCategory")
    @APIResponse(responseCode = "200", description = "Events found",
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = EventSimpleResponse.class)))
    @APIResponse(responseCode = "404", description = "Events not found")
    @GET
    @Path("/category/{idCategory}/events")
    public Uni<RestResponse<List<EventSimpleResponse>>> listEventsByCategory(@PathParam("idCategory") Long idCategory,
            @QueryParam("pageIndex") @DefaultValue("0") int pageIndex,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {

                return eventService.listEventsByCategory(idCategory, pageIndex, pageSize)
                .map(RestResponse::ok);
        
    }

    @Operation(summary = "List all Events by start date and end date")
    @APIResponse(responseCode = "200", description = "Events found",
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = EventSimpleResponse.class)))
    @APIResponse(responseCode = "404", description = "Events not found")
    @GET
    @Path("/date")

    public Uni<RestResponse<List<EventSimpleResponse>>> listEventsByDate(@QueryParam("startDate") LocalDate startDate,
            @QueryParam("endDate") LocalDate endDate,
            @QueryParam("pageIndex") @DefaultValue("0") int pageIndex,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        return eventService.listEventsByDate(startDate, endDate, pageIndex, pageSize)
                .map(response -> {
                    if (response.isEmpty()) {
                        return RestResponse.notFound();
                    } else {
                        return RestResponse.ok(response);
                    }
                });
    }

    @Operation(summary = "Include media item in event")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Media item included in event"),
            @APIResponse(responseCode = "404", description = "Event not found") })
    @POST
    @Path("/{id}/media")
    public Uni<RestResponse<Void>> includeMedia(@PathParam("id") Long eventId, List<@Valid MediaItemRequest> mediaItems) {
        return eventService.includeMedia(eventId, mediaItems)
                .map(response -> RestResponse.accepted());
    }

    @Operation(summary = "Remove media item from event")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Media item removed from event"),
            @APIResponse(responseCode = "404", description = "Event or media item not found") })
    @DELETE
    @Path("/{id}/media/{idMedia}")
    public Uni<RestResponse<Void>> removeMedia(@PathParam("id") Long eventId, @PathParam("idMedia") Long idMedia) {
        return eventService.removeMedia(eventId, idMedia)
                .map(response -> RestResponse.ok());
    }

}
