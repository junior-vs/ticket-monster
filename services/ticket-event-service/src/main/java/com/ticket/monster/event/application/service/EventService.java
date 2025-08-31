package com.ticket.monster.event.application.service;

import java.util.Optional;

import com.ticket.monster.event.application.dto.EventRequest;
import com.ticket.monster.event.application.dto.EventResponse;
import com.ticket.monster.event.domain.model.Event;
import com.ticket.monster.event.infrastructure.mapping.EventMapper;
import com.ticket.monster.event.infrastructure.repository.EventRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EventService {


    EventMapper eventMapper;
    EventRepository eventRepository;

    @Inject
    public EventService(  EventMapper eventMapper, EventRepository eventRepository) {
        this.eventMapper = eventMapper;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventResponse create(EventRequest dto) {
        Event event = eventMapper.toEntity(dto);
        eventRepository.persist(event);

        return eventMapper.toResponse(event);
    }

    public Optional<Event> findById(Long id) {
        return Optional.empty();
    }

    @Transactional
    public Event update(Long id, EventRequest dto) {
        return null;
    }

    @Transactional
    public boolean delete(Long id) {

        return false;
    }
}
