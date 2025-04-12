package br.studio.ticketmonster.venue.rest;

import java.util.List;
import java.util.UUID;

import org.jboss.resteasy.reactive.RestResponse;

import br.studio.ticketmonster.venue.model.VenuesListResponse;
import br.studio.ticketmonster.venue.model.VenuesRequest;
import br.studio.ticketmonster.venue.model.VenuesResponse;
import br.studio.ticketmonster.venue.service.VenuesService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
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

@Path("/venues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VenuesResource {

    @Inject
    VenuesService venuesService;

    @GET
    public Uni<RestResponse<List<VenuesListResponse>>> getAllVenues(
            @QueryParam("pageIndex") @DefaultValue("0") int pageIndex,
            @QueryParam("pageSize") @DefaultValue("15") int pageSize) {
        return venuesService.getAllVenues(pageIndex, pageSize).map(RestResponse::ok);
    }

    @GET
    @Path("/{id}")
    public Uni<RestResponse<VenuesResponse>> getVenue(@PathParam("id") UUID id) {
        return venuesService.getVenue(id).map(RestResponse::ok);
    }

    @POST
    public Uni<RestResponse<Void>> createVenue(@Valid VenuesRequest venue, @Context UriInfo uriInfo) {
        return venuesService.createVenue(venue)
                .map(v -> uriInfo.getAbsolutePathBuilder().path(v.id().toString()).build())
                .map(uri -> RestResponse.created(uri));
    }

    @PUT
    @Path("/{id}")
    public Uni<RestResponse<VenuesResponse>> updateVenue(@PathParam("id") UUID id, VenuesRequest venue) {
        return venuesService.updateVenue(id, venue)
                .map(v -> RestResponse.ok(v));
    }

    @DELETE
    @Path("/{id}")
    public Uni<RestResponse<Void>> deleteVenue(@PathParam("id") UUID id) {
        return venuesService.deleteVenue(id)
                .map(t -> RestResponse.noContent());
    }
}
