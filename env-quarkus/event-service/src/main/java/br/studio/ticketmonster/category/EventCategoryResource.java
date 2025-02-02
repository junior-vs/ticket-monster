package br.studio.ticketmonster.category;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

//Crud Resource for EventCategory
@Path("/event-categories")
@WithTransaction
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventCategoryResource {

    private EventCategoryMapper eventCategoryMapper;

    public EventCategoryResource(EventCategoryMapper eventCategoryMapper) {
        this.eventCategoryMapper = eventCategoryMapper;
    }

    @POST
    public Uni<RestResponse<Void>> create(@Valid EventCategoryRequest eventCategory,
            @Context UriInfo uriInfo) {

        return Uni.createFrom()
                .item(eventCategory)
                .map(eventCategoryMapper::toEntity)
                .call(entity -> entity.persist())
                .map(eventCategoryMapper::toResponse)
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
    public Uni<RestResponse<EventCategoryResponse>> findById(Long id) {
        return EventCategory.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("EventCategory with id " + id + " does not exist"))
                .map(entity -> eventCategoryMapper.toResponse((EventCategory) entity))
                .map(response -> {
                    if (response == null) {
                        return RestResponse.notFound();
                    } else {
                        return RestResponse.ok(response);
                    }
                });
    }

    @GET
    public Uni<RestResponse<List<EventCategoryResponse>>> list(
            @QueryParam("pageIndex") @DefaultValue("0") int pageIndex,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {

        return EventCategory.findAll(Sort.by("id"))
                .page(Page.of(pageIndex, pageSize))
                .list()
                .map(events -> events.stream()
                        .map(t -> eventCategoryMapper.toResponse((EventCategory) t))
                        .collect(Collectors.toList()))
                .map(RestResponse::ok);
    }

    @PUT
    @Path("/{id}")
    public Uni<RestResponse<EventCategoryResponse>> update(Long id, @Valid EventCategoryRequest eventCategory) {
        return EventCategory.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("EventCategory with id " + id + " does not exist"))
                .map(entity -> {
                    EventCategory input = eventCategoryMapper.toEntity(eventCategory);
                    ((EventCategory) entity).update(input);
                    return entity;
                })
                .call(entity -> entity.flush())
                .map(entity -> this.eventCategoryMapper.toResponse((EventCategory) entity))
                .map(response -> RestResponse.ok(response));

    }

    @DELETE
    @Path("/{id}")
    public Uni<RestResponse<Void>> delete(Long id) {
        return EventCategory.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("EventCategory with id " + id + " does not exist"))
                .map(entity ->  entity.delete())
                .map(response ->  RestResponse.ok());
    }

}
