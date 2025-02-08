package br.studio.ticketmonster.category;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface EventCategoryMapper {

   

    EventCategoryResponse toResponse(EventCategory eventCategory);

    List<EventCategoryResponse> toResponse(List<EventCategory> eventCategories);

    /**
     * Maps a single EventCategoryRequest to an EventCategory entity.
     *
     * @param eventCategoryRequest the EventCategoryRequest DTO
     * @return the EventCategory entity
     */
    @Mapping(target = "id", ignore = true)
    EventCategory toEntity(EventCategoryRequest eventCategoryRequest);

}
