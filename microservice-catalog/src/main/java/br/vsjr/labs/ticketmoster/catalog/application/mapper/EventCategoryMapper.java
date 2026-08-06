package br.vsjr.labs.ticketmoster.catalog.application.mapper;

import br.vsjr.labs.ticketmoster.catalog.adapter.in.dto.EventCategoryVO;
import br.vsjr.labs.ticketmoster.catalog.domain.entity.EventCategoryEntity;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategoryDescription;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategoryId;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(config = QuarkusMappingConfig.class)
public interface EventCategoryMapper {

    EventCategoryEntity toEntity(EventCategory category);
    EventCategory toDomain(EventCategoryEntity entity);
    EventCategory toDomain(EventCategoryVO entity);
    EventCategoryVO toVO(EventCategory entity);

    default EventCategoryId map(UUID value) {
        return value == null ? null : new EventCategoryId(value);
    }

    default UUID map(EventCategoryId value) {
        return value == null ? null : value.value();
    }

    default EventCategoryDescription map(String value) {
        return value == null ? null : new EventCategoryDescription(value);
    }

    default String map(EventCategoryDescription value) {
        return value == null ? null : value.value();
    }

}
