package br.studio.ticketmonster.event;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    Event toEntity(EventRequest event);

    EventResponse toResponse(Event event);

    @Mapping(target = "id", ignore = true)
    default void updatEvent(Event input,  @MappingTarget Event output){
        output.update(input);
    }

}
