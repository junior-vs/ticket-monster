package br.studio.ticketmonster.mediaitem;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MediaItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    MediaItem toEntity(MediaItemRequest midiaItemRequest);

    MediaItemResponse toResponse(MediaItem midiaItem);

    List<MediaItemResponse> toResponse(List<MediaItem> midiaItems);

}
