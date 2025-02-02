package br.studio.ticketmonster.midiaitem;

import java.time.LocalDateTime;

public record MidiaItemResponse(
        Long id,
        MediaType mediaType,
        String url,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

}
