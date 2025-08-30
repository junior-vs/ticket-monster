package com.ticket.monster.event.application.service;

import com.ticket.monster.event.application.dto.EventCategoryRequest;
import com.ticket.monster.event.application.dto.EventCategoryResponse;
import com.ticket.monster.event.infrastructure.mapping.EventCategoryMapper;
import com.ticket.monster.event.infrastructure.repository.EventCategoryRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EventCategoryService {

    EventCategoryRepository eventCategoryRepository;
    EventCategoryMapper eventCategoryMapper;

    public EventCategoryService(EventCategoryRepository eventCategoryRepository, EventCategoryMapper eventCategoryMapper) {
        this.eventCategoryRepository = eventCategoryRepository;
        this.eventCategoryMapper = eventCategoryMapper;
    }


    public Uni<EventCategoryResponse> create(@Valid EventCategoryRequest request) {
        return Uni.createFrom()
                .item(request)
                .map(req -> eventCategoryMapper.toEntity(req))
                .flatMap(eventCategoryRepository::persist)
                .map(eventCategory -> eventCategoryMapper.toResponse(eventCategory));
    }

    /**
     * Busca uma categoria de evento pelo ID.
     * Retorna um Uni contendo um Optional com a resposta.
     * Se não encontrar, retorna Optional.empty().
     */
    public Uni<Optional<EventCategoryResponse>> findById(final Long id) {
        return eventCategoryRepository
                .findById(id)
                .map(eventCategoryMapper::toResponse)
                .map(Optional::ofNullable)
                .onItem()
                .ifNull().continueWith(Optional::empty);


    }

    public Uni<List<EventCategoryResponse>> findAll() {
        return eventCategoryRepository
                .findAll()
                .list()
                .map(eventCategories ->
                        eventCategories
                                .stream()
                                .map(eventCategoryMapper::toResponse)
                                .toList());
    }

    public Uni<Optional<EventCategoryResponse>> update(Long id, @Valid EventCategoryRequest request) {

        return eventCategoryRepository
                .findById(id)
                .onItem()
                    .ifNotNull()
                    .transform(eventCategory -> eventCategory.update(request))
                    .flatMap(eventCategoryRepository::persistAndFlush)
                    .map(eventCategory -> Optional.of(eventCategoryMapper.toResponse(eventCategory)))
                .onItem()
                    .ifNull()
                    .continueWith(Optional::empty);


    }

    public Uni<Boolean> delete(Long id) {
        return eventCategoryRepository.deleteById(id);
    }
}
