package br.studio.ticketmonster.venue.persistence.repository;

import java.util.UUID;

import br.studio.ticketmonster.venue.persistence.entity.Venues;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VenuesRepository implements PanacheRepositoryBase<Venues, UUID> {
}
