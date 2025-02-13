package br.studio.ticketmonster.venue.service;

import java.util.List;
import java.util.UUID;

import br.studio.ticketmonster.venue.model.VenuesListResponse;
import br.studio.ticketmonster.venue.model.VenuesRequest;
import br.studio.ticketmonster.venue.model.VenuesResponse;
import br.studio.ticketmonster.venue.persistence.repository.VenuesRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VenuesService {

    @Inject
    VenuesRepository venuesRepository;

    public Uni<List<VenuesListResponse>> getAllVenues() {
     return null;
    }

    public Uni<VenuesResponse> getVenue(UUID id) {
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
 