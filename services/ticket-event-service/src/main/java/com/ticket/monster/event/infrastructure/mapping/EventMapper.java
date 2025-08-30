package com.ticket.monster.event.infrastructure.mapping;

import com.ticket.monster.event.application.dto.EventRequest;
import com.ticket.monster.event.application.dto.EventResponse;
import com.ticket.monster.event.domain.model.Event;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.awt.event.ComponentEvent;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface EventMapper {


    Event toEntity(EventRequest dto);

    EventResponse toResponse(Event event);
}
