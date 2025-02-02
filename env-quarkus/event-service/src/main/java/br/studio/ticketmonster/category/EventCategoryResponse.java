package br.studio.ticketmonster.category;

import java.time.LocalDateTime;

public record EventCategoryResponse(
        Long id,
        String description, 
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {


}
