package br.studio.ticketmonster.midiaitem;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MidiaItemMapper {

    @Mapping(target = "id", ignore = true)
    MidiaItem toEntity(MidiaItemRequest midiaItemRequest);

    MidiaItemResponse toResponse(MidiaItem midiaItem);

}
