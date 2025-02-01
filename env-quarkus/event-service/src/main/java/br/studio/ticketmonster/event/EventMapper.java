package br.studio.ticketmonster.event;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    Event toEntity(EventRequest event);

    EventResponse toResponse(Event event);

}
