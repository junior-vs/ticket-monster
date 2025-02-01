package br.studio.ticketmonster.event;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventService {

    EventMapper eventMapper;
    EventRepository eventRepository;

    public EventService(EventMapper eventMapper, EventRepository eventRepository) {
        this.eventMapper = eventMapper;
        this.eventRepository = eventRepository;
    }

    /**
     * Create an event
     * 
     * @param event
     * @return
     */

    @WithTransaction
    public Uni<EventResponse> createEvent(EventRequest event) {
        return Uni.createFrom()
                .item(event) // Start with EventRequest
                .map(eventMapper::toEntity) // Transform to Entity
                .chain(eventRepository::persist) // Persist entity
                .map(eventMapper::toResponse); // Transform to Response
    }

}
