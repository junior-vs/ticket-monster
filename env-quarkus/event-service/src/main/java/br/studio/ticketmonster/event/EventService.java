package br.studio.ticketmonster.event;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@WithTransaction
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

    public Uni<EventResponse> createEvent(EventRequest event) {
        return Uni.createFrom()
                .item(event) // Start with EventRequest
                .map(eventMapper::toEntity) // Transform to Entity
                .chain(eventRepository::persist) // Persist entity
                .map(eventMapper::toResponse); // Transform to Response
    }

    public Uni<EventResponse> findById(Long id) {
        return eventRepository.findById(id)
                .map(eventMapper::toResponse);
    }

    public Uni<List<EventResponse>> list(int pageIndex, int pageSize) {

        return eventRepository.findAll(Sort.by("id", Direction.Ascending))
                .page(Page.of(pageIndex, pageSize))
                .list()
                .map(events -> events.stream()
                        .map(eventMapper::toResponse)
                        .collect(Collectors.toList()));
    }

}
