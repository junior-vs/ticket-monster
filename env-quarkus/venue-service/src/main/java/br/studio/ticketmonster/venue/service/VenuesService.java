package br.studio.ticketmonster.venue.service;

import java.util.List;
import java.util.UUID;

import br.studio.ticketmonster.venue.infra.mappers.VenueMapper;
import br.studio.ticketmonster.venue.model.VenuesListResponse;
import br.studio.ticketmonster.venue.model.VenuesRequest;
import br.studio.ticketmonster.venue.model.VenuesResponse;
import br.studio.ticketmonster.venue.persistence.repository.VenuesRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class VenuesService {

    @Inject
    VenuesRepository venuesRepository;
    private VenueMapper venueMapper;

    public Uni<List<VenuesListResponse>> getAllVenues(int pageIndex, int pageSize) {
        return venuesRepository.findVenues(pageIndex, pageSize)
                .map(this.venueMapper::toListResponse);
    }

    public Uni<VenuesResponse> getVenue(UUID id) {
        venuesRepository.findById(id).onItem().ifNull().failWith( () -> NotFoundException("Venu jp "));
        return null;
    }

    public Uni<VenuesResponse> createVenue(VenuesRequest venue) {
        return null;
    }

    public Uni<VenuesResponse> updateVenue(UUID id, VenuesRequest venue) {
        return null;
    }

    public Uni<Void> deleteVenue(UUID id) {
        return null;
    }
}
