package com.ticket.monster.event.infrastructure.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.ticket.monster.event.application.dto.EventRequest;
import com.ticket.monster.event.application.dto.EventResponse;
import com.ticket.monster.event.domain.model.Event;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface EventMapper {


    @Mapping(target = "mediaItems", ignore = true)
    Event toEntity(EventRequest dto);

    @Mapping(target = "categoryId", source = "category.id")
    EventResponse toResponse(Event event);
}
