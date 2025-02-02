package br.studio.ticketmonster.category;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface EventCategoryMapper {

    @Mapping(target = "id", ignore = true)
    EventCategory toEntity(EventCategoryRequest eventCategoryRequest);

    EventCategoryResponse toResponse(EventCategory eventCategory);

}
