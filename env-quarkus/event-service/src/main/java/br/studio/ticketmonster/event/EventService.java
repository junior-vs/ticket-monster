package br.studio.ticketmonster.event;

import br.studio.ticketmonster.category.EventCategory;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.util.List;

import static io.quarkus.hibernate.reactive.panache.PanacheEntityBase.findById;

/**
 * Service class for managing events.
 * Provides methods to create, update, delete, and retrieve events.
 */
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

        return Uni.createFrom().item(() -> findById(eventRequest.categoryId()))
                .onItem().ifNull()
                .failWith(new NotFoundException(String.format("Category with id %s does not exist", eventRequest.categoryId())))
                .map(EventCategory.class::cast)
                .map(eventCategory -> eventMapper.toEntity(eventRequest, eventCategory))
                .chain(eventRepository::persist)
                .map(eventMapper::toResponse);
    }

    /**
     * Retrieves a list of events by category.
     *
     * @param idCategory the ID of the event category to filter by
     * @param pageIndex  the index of the page to retrieve
     * @param pageSize   the number of events per page
     * @return a Uni containing a list of EventSimpleResponse objects
     * @throws NotFoundException if the event category with the specified ID does not exist
     */
    public Uni<List<EventSimpleResponse>> listEventsByCategory(Long idCategory, int pageIndex, int pageSize) {
        return Uni.createFrom().item(() -> findById(idCategory))
                .onItem().ifNull().failWith(new NotFoundException(String.format("EventCategory with id %s does not exist", idCategory)))
                .map(EventCategory.class::cast) // cast to EventCategory
                .chain(category -> eventRepository.searchEventsByCategory(category, pageIndex, pageSize)) // call repository method
                .map(events -> events.stream()
                        .map(eventMapper::toSimpleResponse)
                        .toList());


    }


    /**
     * Retrieves an event by its ID along with its associated media items.
     *
     * @param id
     * @return
     */
    public Uni<EventResponse> findByIdWithMediaItems(Long id) {
        return eventRepository.findByIdWithMediaItems(id)
                .onItem().ifNull().failWith(new NotFoundException(String.format("Event with id %s does not exist", id)))
                .map(eventMapper::toResponse);
    }

    /**
     * Retrieves an event by its ID.
     *
     * @param pageIndex
     * @param pageSize
     * @return a Uni containing a list of EventSimpleResponse objects
     */

    public Uni<List<EventSimpleResponse>> list(int pageIndex, int pageSize) {

        return eventRepository.findAll(Sort.by("id", Direction.Ascending))
                .page(Page.of(pageIndex, pageSize))
                .list()
                .map(events -> events.stream()
                        .map(eventMapper::toSimpleResponse)
                        .toList());
    }

    /**
     * Updates an event based on the provided event request.
     *
     * @param id           the ID of the event to update
     * @param eventRequest the request object containing updated event details
     * @return a Uni containing the response object for the updated event
     * @throws NotFoundException if the event with the specified ID does not exist
     */
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


    /**
     * Deletes an event by its ID.
     *
     * @param id the ID of the event to delete
     * @return a Uni containing nothing if the deletion is successful
     * @throws NotFoundException if the event with the specified ID does not exist
     */
    public Uni<Void> deleteEvent(Long id) {
        return eventRepository.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Event with id " + id + " does not exist"))
                .chain(this.eventRepository::delete)
                .map(deleted -> null);
    }

    public Uni<List<EventSimpleResponse>> listEventsByDate(LocalDate startDate, LocalDate endDate, int pageIndex,
                                                           int pageSize) {

        return eventRepository.searchEventsByDate(startDate, endDate, pageIndex, pageSize)
                .map(events -> events.stream()
                        .map(eventMapper::toSimpleResponse)
                        .toList());
    }


}
