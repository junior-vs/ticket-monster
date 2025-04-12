package br.studio.ticketmonster.venue.persistence.repository;

import java.util.List;
import java.util.UUID;

import br.studio.ticketmonster.venue.persistence.entity.Venues;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VenuesRepository implements PanacheRepositoryBase<Venues, UUID> {

    public Uni<List<Venues>> findVenues(int pageIndex, int pageSize) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAvenues'");
    }
}
