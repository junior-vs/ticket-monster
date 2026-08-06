package br.vsjr.labs.ticketmoster.catalog.domain.model;

import br.vsjr.labs.ticketmoster.catalog.adapter.in.dto.EventCategoryVO;
import br.vsjr.labs.ticketmoster.catalog.domain.entity.EventCategoryEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventCategory(@Valid @NotNull EventCategoryId id, @Valid @NotNull EventCategoryDescription description, @NotNull Instant createdAt) {

    public EventCategory(EventCategoryDescription description) {
        this(new EventCategoryId(UUID.randomUUID()), description, Instant.now());
    }

    public EventCategory updateEventCategory(EventCategoryDescription newDescription) {
        return new EventCategory(this.id, newDescription, this.createdAt);
    }

    public EventCategory(EventCategoryEntity entity) {
        this(new EventCategoryId(entity.getId()), new EventCategoryDescription(entity.getDescription()), entity.getCreatedAt());
    }

    public EventCategory(EventCategoryVO vo) {
        this(new EventCategoryId(vo.id()), new EventCategoryDescription(vo.description()), vo.createdAt());
    }
}
