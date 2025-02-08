package br.studio.ticketmonster.event;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventRepository implements PanacheRepository<Event> {


    public Uni<Event> findByIdWithMediaItems(Long id) {
        return find("#Event.findByIdWithMediaItems", Parameters.with("id", id)).firstResult();
    }
    

}
