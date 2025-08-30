package com.ticket.monster.event.infrastructure.mapping;

import com.ticket.monster.event.application.dto.EventCategoryRequest;
import com.ticket.monster.event.application.dto.EventCategoryResponse;
import com.ticket.monster.event.domain.model.EventCategory;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.JAKARTA_CDI;

@Mapper(componentModel = JAKARTA_CDI)
public interface EventCategoryMapper {

    @Mapping(target = "events", ignore = true)
    @Mapping(target = "update", ignore = true)
    public EventCategory toEntity(@Valid EventCategoryRequest req);

    public EventCategoryResponse toResponse(EventCategory eventCategory);
}
