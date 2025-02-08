package br.studio.ticketmonster.mediaitem;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
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

@Path("/midia-items")
@WithTransaction
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MediaItemResource {

    private MediaItemMapper mediaItemMapper;

    public MediaItemResource(MediaItemMapper mediaItemMapper) {
        this.mediaItemMapper = mediaItemMapper;
    }

    @POST
    public Uni<RestResponse<Void>> create(MediaItemRequest MidiaItemRequest, @Context UriInfo uriInfo) {

        return Uni.createFrom().item(MidiaItemRequest)
                .map(mediaItemMapper::toEntity)
                .call(entity -> entity.persist())
                .map(mediaItemMapper::toResponse)
                .map(response -> {
                    URI location = uriInfo.getAbsolutePathBuilder()
                            .path(String.valueOf(response.id()))
                            .build();
                    return RestResponse.created(location);
                });

    }

    @GET
    @Path("/{id}")
    public Uni<RestResponse<MediaItemResponse>> findById(Long id) {
        return MediaItem.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("MidiaItem with id " + id + " does not exist"))
                .map(entity -> mediaItemMapper.toResponse((MediaItem) entity))
                .map(response -> RestResponse.ok(response));
    }

    @GET
    public Uni<RestResponse<List<MediaItemResponse>>> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        return MediaItem.findAll(Sort.by("id")).page(Page.of(page, size))
                .list()
                .map(midias -> midias.stream().map(entity -> mediaItemMapper.toResponse((MediaItem) entity)).toList())
                .map(response -> RestResponse.ok(response));
    }

    @PUT
    @Path("/{id}")
    public Uni<RestResponse<MediaItemResponse>> update(Long id, MediaItemRequest mediaItemRequest,
            @Context UriInfo uriInfo) {

        return MediaItem.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("MediaItem with id " + id + " does not exist"))
                .map(entity -> {
                    MediaItem request = mediaItemMapper.toEntity(mediaItemRequest);
                    ((MediaItem) entity).update(request);
                    return entity;
                })
                .call(entity -> entity.flush())
                .map(entity -> this.mediaItemMapper.toResponse((MediaItem) entity))
                .map(response -> RestResponse.ok(response));
    }

    @DELETE
    @Path("/{id}")
    public Uni<RestResponse<Void>> delete(Long id) {
        return MediaItem.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("MediaItem with id " + id + " does not exist"))
                .call(entity -> entity.delete())
                .map(response -> RestResponse.ok());
    }

}
