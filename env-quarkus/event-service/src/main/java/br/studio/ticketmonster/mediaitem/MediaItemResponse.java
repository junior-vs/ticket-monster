package br.studio.ticketmonster.mediaitem;

import java.time.LocalDateTime;

public record MediaItemResponse(
        Long id,
        MediaType mediaType,
        String url,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

}
