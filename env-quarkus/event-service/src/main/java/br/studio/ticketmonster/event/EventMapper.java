package br.studio.ticketmonster.event;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.studio.ticketmonster.category.EventCategory;
import br.studio.ticketmonster.category.EventCategoryMapper;
import br.studio.ticketmonster.mediaitem.MediaItemMapper;

@Mapper(uses = {MediaItemMapper.class, EventCategoryMapper.class})
public interface EventMapper {

    /**
     * Maps an Event entity to an EventResponse DTO.
     *
     * @param event the Event entity
     * @return the EventResponse DTO
     */   
    @Mapping(target = "mediaItems", source = "event.mediaItems") 
    EventResponse toResponse(Event event);

    /**
     * Updates an existing Event entity with values from another Event entity.
     *
     * @param input the source Event entity
     * @param output the target Event entity to be updated
     */
    @Mapping(target = "id", ignore = true)
    default void updateEvent(Event input, @MappingTarget Event output) {
        output.update(input);
    }

    /**
     * Maps an EventRequest DTO and an EventCategory entity to an Event entity.
     *
     * @param eventRequest the EventRequest DTO
     * @param category the EventCategory entity
     * @return the Event entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "eventCategory")
    @Mapping(target = "mediaItems", ignore = true)
    @Mapping(target = "description", source = "eventRequest.description")
    Event toEntity(EventRequest eventRequest, EventCategory eventCategory);


    

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "mediaItems", ignore = true)
    Event toEntity(EventRequest eventRequest);

}