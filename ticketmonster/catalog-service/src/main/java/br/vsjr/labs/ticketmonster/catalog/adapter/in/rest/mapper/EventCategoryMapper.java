package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.mapper;

import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.CategoryResponse;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;

/**
 * Mapeamento MapStruct entre o agregado de dominio {@link EventCategory} e o
 * DTO de transporte {@link CategoryResponse}.
 *
 * <p>O agregado expoe acessores fluentes no estilo record ({@code id()},
 * {@code description()}, {@code createdAt()}), e nao getters JavaBean; por isso
 * os campos sao projetados via {@code expression}, mantendo o dominio livre de
 * convencoes de transporte. A descricao e projetada a partir do Value Object
 * {@code EventCategoryDescription} ({@code description().value()}).
 *
 * <p>{@code componentModel = "cdi"}: a implementacao gerada
 * ({@code EventCategoryMapperImpl}) e um bean {@code @ApplicationScoped}
 * injetavel nos adaptadores de entrada.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface EventCategoryMapper {

    @Mapping(target = "id", expression = "java(category.id())")
    @Mapping(target = "description", expression = "java(category.description().value())")
    @Mapping(target = "createdAt", expression = "java(category.createdAt())")
    CategoryResponse toResponse(EventCategory category);

    List<CategoryResponse> toResponseList(List<EventCategory> categories);
}
