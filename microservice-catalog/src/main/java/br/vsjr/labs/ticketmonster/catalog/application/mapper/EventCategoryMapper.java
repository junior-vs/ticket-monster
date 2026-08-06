package br.vsjr.labs.ticketmonster.catalog.application.mapper;

import br.vsjr.labs.ticketmonster.catalog.adapter.out.dto.CategoryResponse;
import br.vsjr.labs.ticketmonster.catalog.adapter.in.dto.EventCategoryVO;
import br.vsjr.labs.ticketmonster.catalog.domain.entity.EventCategoryEntity;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryDescription;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class EventCategoryMapper {

    public EventCategoryEntity toEntity(EventCategory category) {
        return category == null ? null : new EventCategoryEntity(category);
    }

    public EventCategory toDomain(EventCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new EventCategory(map(entity.getId()), map(entity.getDescription()), entity.getCreatedAt());
    }

    public EventCategory toDomain(EventCategoryVO entity) {
        if (entity == null) {
            return null;
        }
        return new EventCategory(map(entity.id()), map(entity.description()), entity.createdAt());
    }

    public EventCategoryVO toVO(EventCategory entity) {
        if (entity == null) {
            return null;
        }
        return new EventCategoryVO(map(entity.id()), map(entity.description()), entity.createdAt());
    }

    public CategoryResponse toResponse(EventCategory entity) {
        if (entity == null) {
            return null;
        }
        return new CategoryResponse(map(entity.id()), map(entity.description()), entity.createdAt());
    }

    private EventCategoryId map(UUID value) {
        return value == null ? null : new EventCategoryId(value);
    }

    private UUID map(EventCategoryId value) {
        return value == null ? null : value.value();
    }

    private EventCategoryDescription map(String value) {
        return value == null ? null : new EventCategoryDescription(value);
    }

    private String map(EventCategoryDescription value) {
        return value == null ? null : value.value();
    }

}
