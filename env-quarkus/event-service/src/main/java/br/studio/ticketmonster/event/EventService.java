package br.studio.ticketmonster.event;

import java.util.List;
import java.util.stream.Collectors;

import br.studio.ticketmonster.category.EventCategory;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
@WithTransaction
public class EventService {

    final EventMapper eventMapper;
    final EventRepository eventRepository;

    public EventService(EventMapper eventMapper, EventRepository eventRepository) {
        this.eventMapper = eventMapper;
        this.eventRepository = eventRepository;
    }

    /**
     * Create an event
     * 
     * 
     */

    /**
     * Creates a new event based on the provided event request.
     *
     * @param eventRequest the request object containing event details
     * @return a Uni containing the response object for the created event
     * @throws NotFoundException if the category with the specified ID does not exist
     */
    public Uni<EventResponse> createEvent(@Valid EventRequest eventRequest) {

        return Uni.createFrom().item(() -> EventCategory.findById(eventRequest.categoryId()))
                .onItem().ifNull()
                .failWith(new NotFoundException("Category with id " + eventRequest.categoryId() + " does not exist"))
                .map(EventCategory.class::cast)
                .map(eventCategory -> {
                    var event = eventMapper.toEntity(eventRequest, eventCategory);
                    return event;
                })
                .chain(eventRepository::persist)
                .map(eventMapper::toResponse);
    }

    public Uni<EventResponse> findByIdWithMediaItems(Long id) {
        return eventRepository.findByIdWithMediaItems(id)
                .onItem().ifNull().failWith(new NotFoundException("Event with id " + id + " does not exist"))
                .map(eventMapper::toResponse);
    }

    public Uni<List<EventSimpleResponse>> list(int pageIndex, int pageSize) {

        return eventRepository.findAll(Sort.by("id", Direction.Ascending))
                .page(Page.of(pageIndex, pageSize))
                .list()
                .map(events -> events.stream()
                        .map(eventMapper::toSimpleResponse)
                        .collect(Collectors.toList()));
    }

    public Uni<EventResponse> updateEvent(Long id, EventRequest eventRequest) {

        return eventRepository.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Event with id " + id + " does not exist"))
                .map(Event.class::cast)
                .map(existingEvent -> {
                    Event input = eventMapper.toEntity(eventRequest);                    
                    eventMapper.updateEvent(input, existingEvent);
                    return existingEvent;
                })
                .chain(eventRepository::persistAndFlush)
                .map(eventMapper::toResponse);

    }

    public Uni<Void> deleteEvent(Long id) {
        return eventRepository.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Event with id " + id + " does not exist"))
                .chain(eventRepository::delete)
                .map(deleted -> null);
    }

}
