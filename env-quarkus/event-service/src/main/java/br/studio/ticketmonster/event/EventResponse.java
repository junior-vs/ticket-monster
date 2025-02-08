package br.studio.ticketmonster.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import br.studio.ticketmonster.category.EventCategoryResponse;
import br.studio.ticketmonster.mediaitem.MediaItemResponse;

public record EventResponse(
                Long id,
                String name,
                String description,
                LocalDate startDate,
                LocalDate endDate,
                String location,
                String uuid,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,
                EventCategoryResponse category,
                List<MediaItemResponse> mediaItems) {

}
