package com.ticket.monster.event.application.service;

import com.ticket.monster.event.application.dto.EventCategoryRequest;
import com.ticket.monster.event.application.dto.EventCategoryResponse;
import com.ticket.monster.event.infrastructure.mapping.EventCategoryMapper;
import com.ticket.monster.event.infrastructure.repository.EventCategoryRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EventCategoryService {

    final EventCategoryRepository eventCategoryRepository;
    final EventCategoryMapper eventCategoryMapper;

    @Inject
    public EventCategoryService(final EventCategoryRepository eventCategoryRepository,
            final EventCategoryMapper eventCategoryMapper) {
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
    public Uni<Optional<EventCategoryResponse>> findByIdOptional(final Long id) {
        return eventCategoryRepository
                .findById(id)
                .map(eventCategoryMapper::toResponse)
                .map(Optional::ofNullable)
                .onItem()
                .ifNull().continueWith(Optional::empty);

    }

    public Uni<EventCategoryResponse> findById(final Long id) {
        return eventCategoryRepository
                .findById(id)
                .map(eventCategoryMapper::toResponse)
                .onItem()
                .ifNull().failWith(new NotFoundException("Event category not found"));

    }

    public Uni<List<EventCategoryResponse>> findAll() {
        return eventCategoryRepository
                .findAll()
                .list()
                .map(eventCategories -> eventCategories
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
