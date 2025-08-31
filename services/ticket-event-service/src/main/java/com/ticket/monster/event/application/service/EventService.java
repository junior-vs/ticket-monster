package com.ticket.monster.event.application.service;

import java.util.List;

import com.ticket.monster.event.application.dto.EventRequest;
import com.ticket.monster.event.application.dto.EventResponse;
import com.ticket.monster.event.infrastructure.mapping.EventMapper;
import com.ticket.monster.event.infrastructure.repository.EventCategoryRepository;
import com.ticket.monster.event.infrastructure.repository.EventRepository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class EventService {

    EventMapper eventMapper;
    EventRepository eventRepository;
    EventCategoryRepository eventCategoryRepository;

    @Inject
    public EventService(EventMapper eventMapper, EventRepository eventRepository,
            EventCategoryRepository eventCategoryRepository) {
        this.eventMapper = eventMapper;
        this.eventRepository = eventRepository;
        this.eventCategoryRepository = eventCategoryRepository;
    }

    /**
     * Retrieves all events and maps them to EventResponse DTOs.
     *
     * @return a Uni emitting an immutable list of EventResponse objects
     */
    public Uni<List<EventResponse>> findAll() {
        // Fetch all events, map to DTOs, and collect as an unmodifiable list for
        // immutability
        return eventRepository.findAll().list()
                .map(events -> events.stream()
                        .map(eventMapper::toResponse)
                        .toList() // Java 16+ returns an unmodifiable list
                );
    }

    // find by id
    public Uni<EventResponse> findById(Long id) {
        return eventRepository.findById(id)
                .map(eventMapper::toResponse);
    }

    // create event
    public Uni<EventResponse> create(final EventRequest dto) {
        // Find category, map to event, persist, and map to response in a reactive chain
        return eventCategoryRepository.findById(dto.categoryId())
                .onItem().ifNull().failWith(new NotFoundException("Event category not found"))
                .map(category -> eventMapper.toEntity(dto, category))
                .flatMap(eventRepository::persist)
                .map(eventMapper::toResponse);
    }

    //update Event
    public Uni<EventResponse> update(final Long id, final EventRequest dto) {
        return eventRepository.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Event not found"))
                .flatMap(existingEvent -> eventCategoryRepository.findById(dto.categoryId())
                        .onItem().ifNull().failWith(new NotFoundException("Event category not found"))
                        .map(category -> {
                            // Map updated fields from DTO to existing entity
                            existingEvent.update(dto, category);
                            return existingEvent;
                        }))
                .flatMap(eventRepository::persist)
                .map(eventMapper::toResponse);
    }

    //delete Event
    public Uni<Void> delete(final Long id) {
        return eventRepository.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Event not found"))
                .flatMap(eventRepository::delete);
    }


}
